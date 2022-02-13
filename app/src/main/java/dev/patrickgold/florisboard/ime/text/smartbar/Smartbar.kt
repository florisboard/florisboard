/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.smartbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.ui.components.horizontalTween
import dev.patrickgold.florisboard.app.ui.components.verticalTween
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.snygg.ui.snyggBorder
import dev.patrickgold.florisboard.snygg.ui.snyggShadow
import dev.patrickgold.florisboard.snygg.ui.solidColor
import dev.patrickgold.jetpref.datastore.model.observeAsState

private const val AnimationDuration = 200

private val VerticalEnterTransition = EnterTransition.verticalTween(AnimationDuration)
private val VerticalExitTransition = ExitTransition.verticalTween(AnimationDuration)

private val HorizontalEnterTransition = EnterTransition.horizontalTween(AnimationDuration)
private val HorizontalExitTransition = ExitTransition.horizontalTween(AnimationDuration)

private val NoEnterTransition = EnterTransition.horizontalTween(0)
private val NoExitTransition = ExitTransition.horizontalTween(0)

private val AnimationTween = tween<Float>(AnimationDuration)
private val NoAnimationTween = tween<Float>(0)

@Composable
fun Smartbar() {
    val prefs by florisPreferenceModel()
    val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
    val secondaryRowPlacement by prefs.smartbar.secondaryRowPlacement.observeAsState()

    AnimatedVisibility(
        visible = smartbarEnabled,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        when (secondaryRowPlacement) {
            SecondaryRowPlacement.ABOVE_PRIMARY -> {
                Column {
                    SmartbarSecondaryRow()
                    SmartbarPrimaryRow()
                }
            }
            SecondaryRowPlacement.BELOW_PRIMARY -> {
                Column {
                    SmartbarPrimaryRow()
                    SmartbarSecondaryRow()
                }
            }
            SecondaryRowPlacement.OVERLAY_APP_UI -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FlorisImeSizing.smartbarHeight),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FlorisImeSizing.smartbarHeight * 2)
                            .absoluteOffset(y = -FlorisImeSizing.smartbarHeight),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        SmartbarSecondaryRow()
                    }
                    SmartbarPrimaryRow()
                }
            }
        }
    }
}

@Composable
private fun SmartbarPrimaryRow(modifier: Modifier = Modifier) = key(FlorisImeUi.SmartbarPrimaryRow) {
    val prefs by florisPreferenceModel()
    val primaryRowFlipToggles by prefs.smartbar.primaryRowFlipToggles.observeAsState()
    val secondaryRowEnabled by prefs.smartbar.secondaryRowEnabled.observeAsState()
    val secondaryRowExpanded by prefs.smartbar.secondaryRowExpanded.observeAsState()
    val actionRowExpanded by prefs.smartbar.actionRowExpanded.observeAsState()

    val shouldAnimate by prefs.smartbar.actionRowExpandWithAnimation.observeAsState()

    val rowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarPrimaryRow)
    val actionsToggleStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarPrimaryActionRowToggle)
    val secondaryToggleStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarPrimarySecondaryRowToggle)

    @Composable
    fun ActionRowToggle() {
        IconButton(
            onClick = { prefs.smartbar.actionRowExpanded.set(!actionRowExpanded) },
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .snyggShadow(actionsToggleStyle)
                    .snyggBorder(actionsToggleStyle)
                    .snyggBackground(actionsToggleStyle),
                contentAlignment = Alignment.Center,
            ) {
                val rotation by animateFloatAsState(
                    animationSpec = if (shouldAnimate) AnimationTween else NoAnimationTween,
                    targetValue = if (actionRowExpanded) 180f else 0f,
                )
                Icon(
                    modifier = Modifier.rotate(rotation),
                    painter = painterResource(
                        if (primaryRowFlipToggles) {
                            R.drawable.ic_keyboard_arrow_left
                        } else {
                            R.drawable.ic_keyboard_arrow_right
                        }
                    ),
                    contentDescription = null,
                    tint = actionsToggleStyle.foreground.solidColor(),
                )
            }
        }
    }

    @Composable
    fun RowScope.CenterContent() {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(1f)
                .height(FlorisImeSizing.smartbarHeight),
        ) {
            val enterTransition = if (shouldAnimate) HorizontalEnterTransition else NoEnterTransition
            val exitTransition = if (shouldAnimate) HorizontalExitTransition else NoExitTransition
            androidx.compose.animation.AnimatedVisibility(
                visible = !actionRowExpanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                CandidatesRow()
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = actionRowExpanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                SmartbarActionRow()
            }
        }
    }

    @Composable
    fun SecondaryRowToggle() {
        IconButton(
            onClick = { prefs.smartbar.secondaryRowExpanded.set(!secondaryRowExpanded) },
            enabled = secondaryRowEnabled,
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .snyggShadow(secondaryToggleStyle)
                    .snyggBorder(secondaryToggleStyle)
                    .snyggBackground(secondaryToggleStyle),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = secondaryRowEnabled,
                    enter = VerticalEnterTransition,
                    exit = VerticalExitTransition,
                ) {
                    val transition = updateTransition(secondaryRowExpanded, label = "smartbarSecondaryRowToggleBtn")
                    val alpha by transition.animateFloat(label = "alpha") { if (it) 1f else 0f }
                    val rotation by transition.animateFloat(label = "rotation") { if (it) 180f else 0f }
                    // Expanded icon
                    Icon(
                        modifier = Modifier
                            .alpha(alpha)
                            .rotate(rotation),
                        painter = painterResource(R.drawable.ic_unfold_less),
                        contentDescription = null,
                        tint = secondaryToggleStyle.foreground.solidColor(),
                    )
                    // Not expanded icon
                    Icon(
                        modifier = Modifier
                            .alpha(1f - alpha)
                            .rotate(rotation - 180f),
                        painter = painterResource(R.drawable.ic_unfold_more),
                        contentDescription = null,
                        tint = secondaryToggleStyle.foreground.solidColor(),
                    )
                }
            }
        }
    }

    SideEffect {
        if (!shouldAnimate) {
            prefs.smartbar.actionRowExpandWithAnimation.set(true)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight)
            .snyggBackground(rowStyle),
    ) {
        if (primaryRowFlipToggles) {
            SecondaryRowToggle()
            CenterContent()
            ActionRowToggle()
        } else {
            ActionRowToggle()
            CenterContent()
            SecondaryRowToggle()
        }
    }
}

@Composable
private fun SmartbarSecondaryRow(modifier: Modifier = Modifier) = key(FlorisImeUi.SmartbarSecondaryRow) {
    val prefs by florisPreferenceModel()
    val secondaryRowEnabled by prefs.smartbar.secondaryRowEnabled.observeAsState()
    val secondaryRowExpanded by prefs.smartbar.secondaryRowExpanded.observeAsState()

    val rowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarSecondaryRow)

    AnimatedVisibility(
        visible = secondaryRowEnabled && secondaryRowExpanded,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        SmartbarClipboardCursorRow(
            modifier = modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .snyggBackground(rowStyle),
        )
    }
}
