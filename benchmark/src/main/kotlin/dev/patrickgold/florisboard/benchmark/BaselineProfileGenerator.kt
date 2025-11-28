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

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception

@ExperimentalBaselineProfilesApi
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    // System property override for flexibility in CI/locally:
    // -Dbenchmark.targetPackage=dev.patrickgold.florisboard.debug
    private val targetPackage: String
        get() = System.getProperty("benchmark.targetPackage", "dev.patrickgold.florisboard")

    @Test
    fun startup() =
        baselineProfileRule.collectBaselineProfile(packageName = targetPackage) {
            try {
                // Reset to home so the baseline collection starts from a known state
                pressHome()
                // Basic defensiveness: ensure package exists, otherwise log and skip gracefully
                val pkg = targetPackage
                try {
                    val pm = device.executeShellCommand("pm list packages $pkg")
                    if (pm == null || !pm.contains(pkg)) {
                        println("BaselineProfileGenerator: target package '$pkg' not installed on device; skipping startup profile collection.")
                        return@collectBaselineProfile
                    }
                } catch (e: Exception) {
                    println("BaselineProfileGenerator: failed to query package manager: ${e.message}. Proceeding with best-effort collection.")
                }

                // This block defines the app's critical user journey. Here we are interested in
                // optimizing for app startup. But you can also navigate and scroll
                // through your most important UI.
                startActivityAndWait()
                device.waitForIdle()
                println("BaselineProfileGenerator: baseline collection finished for package $targetPackage")
            } catch (e: Exception) {
                println("BaselineProfileGenerator: unexpected error during baseline collection: ${e.message}")
                throw e
            }
        }
}
