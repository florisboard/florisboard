/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3.interaction

import androidx.compose.ui.util.fastCoerceIn
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@JvmInline
value class InteractionFeedbackOptions private constructor(val packedValue: Int) {
    fun isAudioFeedbackEnabled(kind: InteractionKind): Boolean {
        val mask = 0x1.shl(kind.ordinal * NUM_TYPE_BITS + OFF_AUDIO)
        return packedValue.and(mask) != 0
    }

    fun isHapticFeedbackEnabled(kind: InteractionKind): Boolean {
        val mask = 0x1.shl(kind.ordinal * NUM_TYPE_BITS + OFF_HAPTIC)
        return packedValue.and(mask) != 0
    }

    fun getAudioVolume(): Int {
        val value = packedValue.ushr(OFF_AUDIO_VOLUME).and(MASK_AUDIO_VOLUME)
        return value.fastCoerceIn(MIN_AUDIO_VOLUME, MAX_AUDIO_VOLUME)
    }

    class Builder {
        private var packedValue: Int = 0

        fun enableAudioFeedback(kind: InteractionKind) {
            val mask = 0x1.shl(kind.ordinal * NUM_TYPE_BITS + OFF_AUDIO)
            packedValue = packedValue.or(mask)
        }

        fun enableHapticFeedback(kind: InteractionKind) {
            val mask = 0x1.shl(kind.ordinal * NUM_TYPE_BITS + OFF_HAPTIC)
            packedValue = packedValue.or(mask)
        }

        fun setAudioVolume(value: Int) {
            val value = value.fastCoerceIn(MIN_AUDIO_VOLUME, MAX_AUDIO_VOLUME)
            packedValue = packedValue.or(value.shl(OFF_AUDIO_VOLUME))
        }

        fun build(): InteractionFeedbackOptions {
            return InteractionFeedbackOptions(packedValue)
        }
    }

    companion object {
        private const val NUM_TYPE_BITS: Int = 2
        private const val OFF_AUDIO: Int = 0
        private const val OFF_HAPTIC: Int = 1

        private const val MIN_AUDIO_VOLUME: Int = 0
        private const val MAX_AUDIO_VOLUME: Int = 100
        private const val MASK_AUDIO_VOLUME: Int = 0x7F
        private const val OFF_AUDIO_VOLUME: Int = 32 - 7

        val Fallback = InteractionFeedbackOptions(0)
    }
}

inline fun buildInteractionFeedbackOptions(
    builderAction: InteractionFeedbackOptions.Builder.() -> Unit,
): InteractionFeedbackOptions {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return InteractionFeedbackOptions.Builder().apply(builderAction).build()
}
