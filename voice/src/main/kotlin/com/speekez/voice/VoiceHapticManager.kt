/*
 * Copyright (C) 2025 SpeekEZ
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

package com.speekez.voice

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import org.florisboard.lib.android.systemVibratorOrNull

/**
 * VoiceHapticManager handles specific haptic feedback patterns for the SpeekEZ voice pipeline.
 *
 * Patterns:
 * - Recording start: short click (50ms, amplitude 100)
 * - Recording stop: double click (50ms, 50ms pause, 50ms)
 * - Success: soft tick (30ms, amplitude 60)
 * - Error: long buzz (200ms, amplitude 200)
 */
class VoiceHapticManager(private val context: Context) {
    private val vibrator = context.systemVibratorOrNull()

    /**
     * Provider to check if SpeekEZ haptic feedback is enabled in preferences.
     */
    var hapticEnabledProvider: () -> Boolean = { true }

    /**
     * Performs haptic feedback for recording start: short click (50ms, amplitude 100).
     */
    fun vibrateStart() {
        vibrateOneShot(50, 100)
    }

    /**
     * Performs haptic feedback for recording stop: double click (50ms, 50ms pause, 50ms).
     */
    fun vibrateStop() {
        // Double click: 50ms on, 50ms off, 50ms on
        // Timings: [delay before first, first on, off, second on]
        val timings = longArrayOf(0, 50, 50, 50)
        val amplitudes = intArrayOf(0, 100, 0, 100)
        vibrateWaveform(timings, amplitudes)
    }

    /**
     * Performs haptic feedback for successful transcription: soft tick (30ms, amplitude 60).
     */
    fun vibrateSuccess() {
        vibrateOneShot(30, 60)
    }

    /**
     * Performs haptic feedback for recording/processing error: long buzz (200ms, amplitude 200).
     */
    fun vibrateError() {
        vibrateOneShot(200, 200)
    }

    private fun vibrateOneShot(duration: Long, amplitude: Int) {
        if (!isHapticEnabled()) return
        val v = vibrator ?: return

        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createOneShot(duration, amplitude.coerceIn(1, 255))
        } else {
            VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        if (!isHapticEnabled()) return
        val v = vibrator ?: return

        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            // Fallback for devices without amplitude control:
            // Use DEFAULT_AMPLITUDE for any non-zero amplitude in the waveform.
            val defaultAmplitudes = amplitudes.map { if (it > 0) VibrationEffect.DEFAULT_AMPLITUDE else 0 }.toIntArray()
            VibrationEffect.createWaveform(timings, defaultAmplitudes, -1)
        }
        v.vibrate(effect)
    }

    private fun isHapticEnabled(): Boolean {
        // Check SpeekEZ setting first
        if (!hapticEnabledProvider()) return false

        // Check system haptic setting
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }
}
