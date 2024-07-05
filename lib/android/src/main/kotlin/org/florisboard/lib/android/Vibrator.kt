/*
 * Copyright (C) 2022 Patrick Goldinger
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

package org.florisboard.lib.android

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

fun Context.systemVibratorOrNull(): Vibrator? {
    return if (AndroidVersion.ATLEAST_API31_S) {
        this.systemServiceOrNull(VibratorManager::class)?.defaultVibrator
    } else {
        this.systemServiceOrNull(Vibrator::class)
    }?.takeIf { it.hasVibrator() }
}

fun Vibrator.vibrate(duration: Int, strength: Int, factor: Double = 1.0) {
    if (duration == 0 || strength == 0) return
    val effectiveDuration = (duration * factor).toLong().coerceAtLeast(1L)
    if (AndroidVersion.ATLEAST_API26_O) {
        val effectiveStrength = when {
            this.hasAmplitudeControl() -> (255.0 * ((strength * factor) / 100.0)).toInt().coerceIn(1, 255)
            else -> VibrationEffect.DEFAULT_AMPLITUDE
        }
        Log.d("Vibrator", "Perform haptic with duration=$effectiveDuration and strength=$effectiveStrength")
        val effect = VibrationEffect.createOneShot(effectiveDuration, effectiveStrength)
        this.vibrate(effect)
    } else {
        Log.d("Vibrator", "Perform haptic with duration=$effectiveDuration")
        @Suppress("DEPRECATION")
        this.vibrate(effectiveDuration)
    }
}
