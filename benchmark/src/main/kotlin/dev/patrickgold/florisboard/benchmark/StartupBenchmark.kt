/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.os.Build
import java.lang.Exception
import kotlin.math.max

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance from a cold state.
 *
 * NOTE: Enhancements added for:
 * - interoperability across Android API levels
 * - validation of target package and IME service presence
 * - retries and fallbacks for enabling/setting IME
 * - configurability via system properties (useful for update/downgrade scenarios)
 * - graceful handling when the device lacks required features (mitigations)
 */

@RunWith(AndroidJUnit4ClassRunner::class)
class ColdStartupBenchmark : AbstractStartupBenchmark(StartupMode.COLD)

@RunWith(AndroidJUnit4ClassRunner::class)
class WarmStartupBenchmark : AbstractStartupBenchmark(StartupMode.WARM)

@RunWith(AndroidJUnit4ClassRunner::class)
class HotStartupBenchmark : AbstractStartupBenchmark(StartupMode.HOT)

abstract class AbstractStartupBenchmark(private val startupMode: StartupMode) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    // Allow overriding target package and iterations via system properties for flexibility when testing
    private val targetPackage: String
        get() = System.getProperty("benchmark.targetPackage", "dev.patrickgold.florisboard")

    private val imeServiceClass: String
        get() = System.getProperty("benchmark.imeService", "$targetPackage/.FlorisImeService")

    private val iterationsFromProp: Int
        get() = try {
            max(1, System.getProperty("benchmark.iterations")?.toInt() ?: 10)
        } catch (e: Exception) {
            10
        }

    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfileDisabled() = startup(
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Disable, warmupIterations = 1)
    )

    @Test
    fun startupBaselineProfile() = startup(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    @Test
    fun startupFullCompilation() = startup(CompilationMode.Full())

    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        iterations = iterationsFromProp,
        startupMode = startupMode,
        setupBlock = {
            // Prepare a robust environment before starting the activity:
            // - Press home to reset UI
            // - Attempt to enable and set the IME only if present and supported
            pressHome()

            // Basic device compatibility info for logs/debugging
            try {
                println("Macrobenchmark device API level: ${android.os.Build.VERSION.SDK_INT}")
            } catch (_: Throwable) { /* ignore */ }

            val pkg = targetPackage
            val service = imeServiceClass

            try {
                if (!isPackageInstalled(pkg)) {
                    println("Target package '$pkg' not installed on device. Skipping IME setup.")
                } else {
                    if (!isImeServiceDeclared(pkg, service)) {
                        println("IME service '$service' not declared in package '$pkg'. Skipping IME setup.")
                    } else {
                        // Try enabling and setting IME with retries and verification (mitigates transient errors)
                        val maxRetries = 3
                        var attempt = 0
                        var enabled = false
                        while (attempt < maxRetries && !enabled) {
                            attempt++
                            try {
                                enableImeSafe(service)
                                // verify IME is enabled/selected
                                val imeList = device.executeShellCommand("ime list -a")
                                if (imeList.contains(service)) {
                                    // setting IME
                                    device.executeShellCommand("ime set $service")
                                    // confirm current IME
                                    val current = device.executeShellCommand("settings get secure default_input_method")
                                    if (current != null && current.contains(service)) {
                                        enabled = true
                                        println("IME '$service' enabled and set successfully on attempt $attempt")
                                    } else {
                                        println("IME '$service' set command executed but verification failed on attempt $attempt")
                                    }
                                } else {
                                    println("IME '$service' not present in ime list after enable; attempt $attempt")
                                }
                            } catch (e: Exception) {
                                println("Attempt $attempt to enable/set IME failed: ${e.message}")
                            }
                            if (!enabled) {
                                // small backoff between retries
                                Thread.sleep(500L * attempt)
                            }
                        }
                        if (!enabled) {
                            println("Final status: IME '$service' could not be enabled/selected. Continuing without changing IME.")
                        }
                    }
                }
            } catch (e: Exception) {
                // Any unexpected error should not block running the benchmark; just log and continue.
                println("Unexpected error during setupBlock: ${e.message}")
            }
        }
    ) {
        // The measured block: start target activity and wait for idle
        try {
            startActivityAndWait()
            // Wait longer on older devices or when device is busy - mitigations for instability
            val waitMs = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) 7000L else 5000L
            device.waitForIdle(waitMs)
        } catch (e: Exception) {
            // Log and rethrow to make test failure visible, but with a helpful message
            println("Error during measured startup block: ${e.message}")
            throw e
        }
    }

    /**
     * Check if a package is installed on the device.
     * Uses 'pm list packages' for basic compatibility across API levels.
     */
    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            val out = device.executeShellCommand("pm list packages $pkg")
            out?.contains(pkg) ?: false
        } catch (e: Exception) {
            println("isPackageInstalled: failed to query pm: ${e.message}")
            false
        }
    }

    /**
     * Quick heuristic to check if the expected IME service class is declared in package.
     * Uses dumpsys package which is available across Android versions, though output varies.
     */
    private fun isImeServiceDeclared(pkg: String, service: String): Boolean {
        return try {
            val out = device.executeShellCommand("dumpsys package $pkg")
            // Look for the service class name or short class name in the package dump
            out != null && (out.contains(service) || out.contains(".FlorisImeService"))
        } catch (e: Exception) {
            println("isImeServiceDeclared: failed to dumpsys package: ${e.message}")
            false
        }
    }

    /**
     * Attempt to enable IME safely. Will not throw on unsupported devices.
     */
    private fun enableImeSafe(service: String) {
        try {
            // Enabling IME may require runtime permissions or user confirmation on some devices;
            // we attempt the command and rely on verification afterwards.
            device.executeShellCommand("ime enable $service")
        } catch (e: Exception) {
            println("enableImeSafe: ime enable failed: ${e.message}")
            // swallow - verification will detect absence
        }
    }
}
