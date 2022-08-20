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
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.compose.tooltip.tooltip
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.computeIconResId
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.snygg.ui.shape
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBorder
import dev.patrickgold.florisboard.lib.snygg.ui.snyggClip
import dev.patrickgold.florisboard.lib.snygg.ui.snyggShadow
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor

private val BackgroundAnimationSpec = tween<Color>(durationMillis = 150, easing = FastOutSlowInEasing)

@Composable
fun QuickActionButton(
    action: QuickAction,
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
    displayAsTile: Boolean = false,
) {
    val context = LocalContext.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isEnabled = evaluator.evaluateEnabled(action.keyData())
    // We always need to know both state's styles to animate smoothly
    val actionStyleNotPressed = FlorisImeTheme.style.get(
        element = if (displayAsTile) FlorisImeUi.SmartbarActionTile else FlorisImeUi.SmartbarActionKey,
        code = action.keyData().code,
        isPressed = false,
        isDisabled = !isEnabled,
    )
    val actionStylePressed = FlorisImeTheme.style.get(
        element = if (displayAsTile) FlorisImeUi.SmartbarActionTile else FlorisImeUi.SmartbarActionKey,
        code = action.keyData().code,
        isPressed = true,
        isDisabled = !isEnabled,
    )
    val actionStyle = if (isPressed) actionStylePressed else actionStyleNotPressed
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) {
            actionStylePressed.background.solidColor()
        } else {
            if (actionStyleNotPressed.background.solidColor().alpha == 0f) {
                actionStylePressed.background.solidColor().copy(0f)
            } else {
                actionStyleNotPressed.background.solidColor()
            }
        },
        animationSpec = BackgroundAnimationSpec,
    )

    // Need to manually cancel an action if this composable suddenly leaves the composition to prevent the key from
    // being stuck in the pressed state
    DisposableEffect(action, isEnabled) {
        onDispose {
            if (action is QuickAction.InsertKey) {
                action.onPointerCancel(context)
            }
        }
    }

    val tooltipModifier = if (!displayAsTile) {
        Modifier.tooltip(action.computeTooltip(evaluator))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .snyggShadow(actionStyle)
            .snyggBorder(actionStyle)
            .background(bgColor, actionStyle.shape.shape())
            .snyggClip(actionStyle)
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(action, isEnabled) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown()
                        down.consume()
                        if (isEnabled) {
                            val press = PressInteraction.Press(down.position)
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
            }
            .then(tooltipModifier)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Render foreground
            Box(modifier = Modifier.size(FlorisImeSizing.smartbarHeight * 0.5f)) {
                when (action) {
                    is QuickAction.InsertKey -> {
                        val (iconResId, label) = remember(action, evaluator) {
                            evaluator.computeIconResId(action.data) to evaluator.computeLabel(action.data)
                        }
                        if (iconResId != null) {
                            Icon(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                tint = actionStyle.foreground.solidColor(default = FlorisImeTheme.fallbackContentColor()),
                            )
                        } else if (label != null) {
                            Text(
                                text = label,
                                color = actionStyle.foreground.solidColor(default = FlorisImeTheme.fallbackContentColor()),
                            )
                        }
                    }
                    is QuickAction.InsertText -> {
                        Text(
                            text = action.data.firstOrNull().toString().ifBlank { "?" },
                            color = actionStyle.foreground.solidColor(default = FlorisImeTheme.fallbackContentColor()),
                        )
                    }
                }
            }

            // Render additional info if this is a tile
            if (displayAsTile) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = action.computeDisplayName(evaluator = evaluator),
                    color = actionStyle.foreground.solidColor(default = FlorisImeTheme.fallbackContentColor()),
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}
