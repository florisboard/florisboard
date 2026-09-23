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

package dev.patrickgold.florisboard.ime.keyboard3.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.InteractionKind
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.touch.isRepeatable
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.k3lp.lib.text.K3Descriptor
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.lib.text.normalize
import org.k3lp.lib.text.unicode.NormalizationForm

@Composable
fun ImeKeyButton(
    elementName: String? = null,
    output: K3StringOrDescriptor,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    ImeKeyButton(elementName, output, isEnabled, modifier) { display ->
        Display3(
            modifier = Modifier.fillMaxHeight(),
            display = display,
        )
    }
}

@Composable
fun ImeKeyButton(
    elementName: String? = null,
    output: K3StringOrDescriptor,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(K3StringOrDescriptor) -> Unit,
) {
    val imeController = LocalImeController.current
    val interactionController = LocalInteractionController.current
    val scope = rememberCoroutineScope()

    val imeState by imeController.activeState.collectAsState()
    val model by remember { derivedStateOf { imeState.model } }
    val display = remember(model, output) { model.displays.byOutput[output]?.display ?: output }

    val isRepeatable = output.isRepeatable()
    val keyRepeatTimeout = interactionController.getKeyRepeatTimeout(output)
    val keyRepeatDelay = interactionController.getKeyRepeatDelay(output)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val attributes = remember(output) {
        mapOf(
            FlorisImeUi.Attr.Output to output.asAttrValue(),
        )
    }
    val selector = when {
        isPressed -> SnyggSelector.PRESSED
        !isEnabled -> SnyggSelector.DISABLED
        else -> SnyggSelector.NONE
    }

    SnyggBox(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier,
        clickAndSemanticsModifier = Modifier
            .indication(interactionSource, ripple())
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    down.consume()
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    interactionController.performFeedback(InteractionKind.KeyPress)
                    var didRepeatTrigger = false
                    val repeatJob = if (isRepeatable) {
                        scope.launch {
                            delay(keyRepeatTimeout)
                            while (isActive) {
                                imeController.updateState {
                                    emit(output)
                                    didRepeatTrigger = true
                                }
                                interactionController.performFeedback(InteractionKind.KeyRepeat)
                                delay(keyRepeatDelay)
                            }
                        }
                    } else null
                    val up = waitForUpOrCancellation()
                    repeatJob?.cancel()
                    scope.launch {
                        imeController.updateState {
                            if (up != null) {
                                interactionSource.tryEmit(PressInteraction.Release(press))
                                if (!didRepeatTrigger) {
                                    emit(output)
                                }
                            } else {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(display)
    }
}

fun K3StringOrDescriptor.asAttrValue(): String {
    return when (this) {
        is K3String -> normalize(NormalizationForm.NFC).toText()
        is K3Descriptor -> toString()
    }
}
