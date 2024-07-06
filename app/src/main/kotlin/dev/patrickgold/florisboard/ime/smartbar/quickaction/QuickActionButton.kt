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

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.compose.tooltip.tooltip
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.ui.shape
import org.florisboard.lib.snygg.ui.snyggBorder
import org.florisboard.lib.snygg.ui.snyggClip
import org.florisboard.lib.snygg.ui.snyggShadow
import org.florisboard.lib.snygg.ui.solidColor

private val BackgroundAnimationSpec = tween<Color>(durationMillis = 150, easing = FastOutSlowInEasing)
private val DebugHelperColor = Color.Red.copy(alpha = 0.5f)

enum class QuickActionBarType {
    INTERACTIVE_BUTTON,
    INTERACTIVE_TILE,
    STATIC_TILE;
}

@Composable
fun QuickActionButton(
    action: QuickAction,
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
    type: QuickActionBarType = QuickActionBarType.INTERACTIVE_BUTTON,
) {
    val context = LocalContext.current
    // Get the inputFeedbackController through the FlorisImeService companion-object.
    val inputFeedbackController = FlorisImeService.inputFeedbackController()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isEnabled = type == QuickActionBarType.STATIC_TILE || evaluator.evaluateEnabled(action.keyData())
    val element = when (type) {
        QuickActionBarType.INTERACTIVE_BUTTON -> FlorisImeUi.SmartbarActionKey
        else -> FlorisImeUi.SmartbarActionTile
    }
    // We always need to know both state's styles to animate smoothly
    val actionStyleNotPressed = FlorisImeTheme.style.get(
        element = element,
        code = action.keyData().code,
        isPressed = false,
        isDisabled = !isEnabled,
    )
    val actionStylePressed = FlorisImeTheme.style.get(
        element = element,
        code = action.keyData().code,
        isPressed = true,
        isDisabled = !isEnabled,
    )
    val actionStyle = if (isPressed) actionStylePressed else actionStyleNotPressed
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) {
            actionStylePressed.background.solidColor(context)
        } else {
            if (actionStyleNotPressed.background.solidColor(context).alpha == 0f) {
                actionStylePressed.background.solidColor(context).copy(0f)
            } else {
                actionStyleNotPressed.background.solidColor(context)
            }
        },
        animationSpec = BackgroundAnimationSpec, label = "bgColor",
    )
    val fgColor = when (action.keyData().code) {
        KeyCode.DRAG_MARKER -> {
            DebugHelperColor
        }

        else -> {
            actionStyle.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor())
        }
    }
    val fgAlpha = if (action.keyData().code == KeyCode.NOOP) 0.5f else 1f

    // Need to manually cancel an action if this composable suddenly leaves the composition to prevent the key from
    // being stuck in the pressed state
    DisposableEffect(action, isEnabled) {
        onDispose {
            if (action is QuickAction.InsertKey) {
                action.onPointerCancel(context)
            }
        }
    }

    val tooltipModifier = if (type == QuickActionBarType.INTERACTIVE_BUTTON) {
        Modifier.tooltip(action.computeTooltip(evaluator))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .alpha(fgAlpha)
            .snyggShadow(actionStyle)
            .snyggBorder(context, actionStyle)
            .background(bgColor, actionStyle.shape.shape())
            .snyggClip(actionStyle)
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(action, isEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    if (isEnabled && type != QuickActionBarType.STATIC_TILE) {
                        val press = PressInteraction.Press(down.position)
                        inputFeedbackController?.keyPress(TextKeyData.UNSPECIFIED)
                        interactionSource.tryEmit(press)
                        action.onPointerDown(context)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            interactionSource.tryEmit(PressInteraction.Release(press))
                            action.onPointerUp(context)
                        } else {
                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                            action.onPointerCancel(context)
                        }
                    }
                }
            }
            .then(tooltipModifier)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Render foreground
            Box(
                modifier = Modifier.size(FlorisImeSizing.smartbarHeight * 0.5f),
                contentAlignment = Alignment.Center,
            ) {
                when (action) {
                    is QuickAction.InsertKey -> {
                        val (imageVector, label) = remember(action, evaluator) {
                            evaluator.computeImageVector(action.data) to evaluator.computeLabel(action.data)
                        }
                        if (imageVector != null) {
                            Icon(
                                imageVector = imageVector,
                                contentDescription = null,
                                tint = fgColor,
                            )
                        } else if (label != null) {
                            Text(
                                text = label,
                                color = fgColor,
                            )
                        }
                    }

                    is QuickAction.InsertText -> {
                        Text(
                            text = action.data.firstOrNull().toString().ifBlank { "?" },
                            color = fgColor,
                            fontSize = 16.sp,
                        )
                    }
                }
            }

            // Render additional info if this is a tile
            if (type != QuickActionBarType.INTERACTIVE_BUTTON) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = action.computeDisplayName(evaluator = evaluator),
                    color = fgColor,
                    fontSize = if (type == QuickActionBarType.STATIC_TILE) 10.sp else 13.sp,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}
