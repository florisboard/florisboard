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

import androidx.compose.runtime.compositionLocalOf
import org.k3lp.lib.text.K3StringOrDescriptor
import kotlin.time.Duration

val LocalInteractionController = compositionLocalOf<InteractionController> {
    error("no touch feedback handler provided")
}

interface InteractionController {
    val timingOptions: InteractionTimingOptions

    val feedbackOptions: InteractionFeedbackOptions

    fun getKeyRepeatTimeout(output: K3StringOrDescriptor? = null): Duration

    fun getKeyRepeatDelay(output: K3StringOrDescriptor? = null): Duration

    fun getLongPressTimeout(output: K3StringOrDescriptor? = null): Duration

    fun getMultiPressTimeout(output: K3StringOrDescriptor? = null): Duration

    fun performFeedback(
        kind: InteractionKind,
        output: K3StringOrDescriptor? = null,
    ) {
        if (feedbackOptions.isAudioFeedbackEnabled(kind)) {
            performAudioFeedback(kind, output)
        }
        if (feedbackOptions.isHapticFeedbackEnabled(kind)) {
            performHapticFeedback(kind, output)
        }
    }

    fun performAudioFeedback(
        kind: InteractionKind,
        output: K3StringOrDescriptor? = null,
    )

    fun performHapticFeedback(
        kind: InteractionKind,
        output: K3StringOrDescriptor? = null,
    )
}
