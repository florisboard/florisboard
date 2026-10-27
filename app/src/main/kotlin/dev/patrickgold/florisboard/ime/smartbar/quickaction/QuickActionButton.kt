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

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.compose.tooltip.PlainTooltip
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.ai.AiRewriter
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggText

enum class QuickActionBarType {
    INTERACTIVE_BUTTON,
    INTERACTIVE_TILE,
    EDITOR_TILE;
}

@Composable
fun QuickActionButton(
    action: QuickAction,
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
    type: QuickActionBarType = QuickActionBarType.INTERACTIVE_BUTTON,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAiPromptDialog by remember { mutableStateOf(false) }
    var pendingRewriteTarget by remember { mutableStateOf<AiRewriter.RewriteTarget?>(null) }
    // Get the inputFeedbackController through the FlorisImeService companion-object.
    val inputFeedbackController = FlorisImeService.inputFeedbackController()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isEnabled = type == QuickActionBarType.EDITOR_TILE || evaluator.evaluateEnabled(action.keyData())
    val elementName = when (type) {
        QuickActionBarType.INTERACTIVE_BUTTON -> FlorisImeUi.SmartbarActionKey
        QuickActionBarType.INTERACTIVE_TILE -> FlorisImeUi.SmartbarActionTile
        QuickActionBarType.EDITOR_TILE -> FlorisImeUi.SmartbarActionsEditorTile
    }.elementName
    val attributes = mapOf(FlorisImeUi.Attr.Code to action.keyData().code)
    val selector = when {
        isPressed -> SnyggSelector.PRESSED
        !isEnabled -> SnyggSelector.DISABLED
        else -> null
    }

    // Need to manually cancel an action if this composable suddenly leaves the composition to prevent the key from
    // being stuck in the pressed state
    DisposableEffect(action, isEnabled) {
        onDispose {
            if (action is QuickAction.InsertKey) {
                action.onPointerCancel(context)
            }
        }
    }

    PlainTooltip(action.computeTooltip(evaluator), enabled = type == QuickActionBarType.INTERACTIVE_BUTTON) {
        SnyggBox(
            elementName = elementName,
            attributes = attributes,
            selector = selector,
            modifier = modifier,
            clickAndSemanticsModifier = Modifier
                .aspectRatio(1f)
                .indication(interactionSource, LocalIndication.current)
                .pointerInput(action, isEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        if (isEnabled && type != QuickActionBarType.EDITOR_TILE) {
                            val press = PressInteraction.Press(down.position)
                            inputFeedbackController?.keyPress(TextKeyData.UNSPECIFIED)
                            interactionSource.tryEmit(press)
                            action.onPointerDown(context)
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                up.consume()
                                interactionSource.tryEmit(PressInteraction.Release(press))
                                if (action is QuickAction.AiRewrite) {
                                    val target = AiRewriter.captureTarget(context)
                                    if (target != null) {
                                        pendingRewriteTarget = target
                                        showAiPromptDialog = true
                                    }
                                } else {
                                    action.onPointerUp(context)
                                }
                            } else {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                action.onPointerCancel(context)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Render foreground
                when (action) {
                    is QuickAction.InsertKey -> {
                        val (imageVector, label) = remember(action, evaluator) {
                            evaluator.computeImageVector(action.data) to evaluator.computeLabel(action.data)
                        }
                        if (imageVector != null) {
                            SnyggBox(
                                elementName = "$elementName-icon",
                                attributes = attributes,
                                selector = selector,
                            ) {
                                SnyggIcon(imageVector = imageVector)
                            }
                        } else if (label != null) {
                            SnyggText(
                                elementName = "$elementName-text",
                                attributes = attributes,
                                selector = selector,
                                text = label,
                            )
                        }
                    }

                    is QuickAction.InsertText -> {
                        SnyggText(
                            elementName = "$elementName-text",
                            attributes = attributes,
                            selector = selector,
                            text = action.data.firstOrNull().toString().ifBlank { "?" },
                        )
                    }

                    is QuickAction.AiRewrite -> {
                        SnyggBox(
                            elementName = "$elementName-icon",
                            attributes = attributes,
                            selector = selector,
                        ) {
                            SnyggIcon(imageVector = Icons.Default.AutoAwesome)
                        }
                    }
                }

                // Render additional info if this is a tile
                if (type != QuickActionBarType.INTERACTIVE_BUTTON) {
                    SnyggText(
                        elementName = "$elementName-text",
                        attributes = attributes,
                        selector = selector,
                        text = action.computeDisplayName(evaluator = evaluator),
                    )
                }
            }
        }
    }

    if (showAiPromptDialog && action is QuickAction.AiRewrite) {
        AiPromptDialog(
            onDismiss = {
                showAiPromptDialog = false
                pendingRewriteTarget = null
            },
            onSend = { prompt ->
                val target = pendingRewriteTarget
                showAiPromptDialog = false
                pendingRewriteTarget = null
                if (target != null) {
                    coroutineScope.launch {
                        AiRewriter.rewrite(
                            context = context,
                            prompt = prompt.ifBlank { AiRewriter.DefaultPrompt },
                            target = target,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun AiPromptDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var prompt by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringRes(R.string.ai_rewrite__prompt_dialog__title))
        },
        text = {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringRes(R.string.ai_rewrite__prompt_dialog__label))
                },
                minLines = 3,
                maxLines = 5,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSend(prompt) }) {
                Text(text = stringRes(R.string.ai_rewrite__prompt_dialog__send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringRes(R.string.action__cancel))
            }
        },
    )
}
