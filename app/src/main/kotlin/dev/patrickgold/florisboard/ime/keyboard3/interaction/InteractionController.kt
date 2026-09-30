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

import androidx.compose.runtime.staticCompositionLocalOf
import dev.patrickgold.florisboard.ime.keyboard3.interaction.InteractionController.ToastHandle
import kotlinx.coroutines.flow.StateFlow
import org.florisboard.lib.kotlin.CurlyArg
import org.florisboard.lib.kotlin.curlyFormat
import org.k3lp.lib.text.K3StringOrDescriptor

val LocalInteractionController = staticCompositionLocalOf<InteractionController> {
    error("no touch feedback handler provided")
}

interface InteractionController {
    val activeSystemTimingOptions: StateFlow<InteractionTimingOptions>

    val activeTimingOptions: StateFlow<InteractionTimingOptions>

    val activeFeedbackOptions: StateFlow<InteractionFeedbackOptions>

    fun performFeedback(
        kind: InteractionKind,
        output: K3StringOrDescriptor? = null,
    ) {
        val feedbackOptions = activeFeedbackOptions.value
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

    suspend fun showToast(text: String, type: ToastType): ToastHandle

    enum class ToastType {
        SHORT,
        LONG;
    }

    interface ToastHandle {
        suspend fun hide()
    }
}

suspend fun InteractionController.showShortToast(text: String): ToastHandle {
    return showToast(text, InteractionController.ToastType.SHORT)
}

suspend fun InteractionController.showShortToast(format: String, vararg args: CurlyArg): ToastHandle {
    val text = format.curlyFormat(*args)
    return showToast(text, InteractionController.ToastType.SHORT)
}

suspend fun InteractionController.showLongToast(text: String): ToastHandle {
    return showToast(text, InteractionController.ToastType.LONG)
}

suspend fun InteractionController.showLongToast(format: String, vararg args: CurlyArg): ToastHandle {
    val text = format.curlyFormat(*args)
    return showToast(text, InteractionController.ToastType.LONG)
}
