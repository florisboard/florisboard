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
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.compose.autoMirrorForRtl
import dev.patrickgold.florisboard.lib.compose.horizontalTween
import dev.patrickgold.florisboard.lib.compose.verticalTween
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBorder
import dev.patrickgold.florisboard.lib.snygg.ui.snyggShadow
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor
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
    val secondaryRowPlacement by prefs.smartbar.secondaryActionsPlacement.observeAsState()

    AnimatedVisibility(
        visible = smartbarEnabled,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        when (secondaryRowPlacement) {
            SecondaryRowPlacement.ABOVE_PRIMARY -> {
                Column {
                    SmartbarSecondaryRow()
                    SmartbarMainRow()
                }
            }
            SecondaryRowPlacement.BELOW_PRIMARY -> {
                Column {
                    SmartbarMainRow()
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
                    SmartbarMainRow()
                }
            }
        }
    }
}

@Composable
private fun SmartbarMainRow(modifier: Modifier = Modifier) {
    val prefs by florisPreferenceModel()
    val flipToggles by prefs.smartbar.flipToggles.observeAsState()
    val primaryActionsExpanded by prefs.smartbar.primaryActionsExpanded.observeAsState()
    val secondaryActionsEnabled by prefs.smartbar.secondaryActionsEnabled.observeAsState()
    val secondaryActionsExpanded by prefs.smartbar.secondaryActionsExpanded.observeAsState()

    val shouldAnimate by prefs.smartbar.primaryActionsExpandWithAnimation.observeAsState()

    val primaryRowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarPrimaryRow)
    val primaryActionsToggleStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarPrimaryActionsToggle)
    val secondaryActionsToggleStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarSecondaryActionsToggle)

    @Composable
    fun PrimaryActionsToggle() {
        IconButton(
            onClick = { prefs.smartbar.primaryActionsExpanded.set(!primaryActionsExpanded) },
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .snyggShadow(primaryActionsToggleStyle)
                    .snyggBorder(primaryActionsToggleStyle)
                    .snyggBackground(primaryActionsToggleStyle),
                contentAlignment = Alignment.Center,
            ) {
                val rotation by animateFloatAsState(
                    animationSpec = if (shouldAnimate) AnimationTween else NoAnimationTween,
                    targetValue = if (primaryActionsExpanded) 180f else 0f,
                )
                Icon(
                    modifier = Modifier
                        .autoMirrorForRtl()
                        .rotate(rotation),
                    painter = painterResource(
                        if (flipToggles) {
                            R.drawable.ic_keyboard_arrow_left
                        } else {
                            R.drawable.ic_keyboard_arrow_right
                        }
                    ),
                    contentDescription = null,
                    tint = primaryActionsToggleStyle.foreground.solidColor(),
                )
            }
        }
    }

    @Composable
    fun RowScope.CenterContent() {
        val primaryActionsRowType by prefs.smartbar.primaryActionsRowType.observeAsState()
        Box(
            modifier = Modifier
                .weight(1f)
                .height(FlorisImeSizing.smartbarHeight),
        ) {
            val enterTransition = if (shouldAnimate) HorizontalEnterTransition else NoEnterTransition
            val exitTransition = if (shouldAnimate) HorizontalExitTransition else NoExitTransition
            androidx.compose.animation.AnimatedVisibility(
                visible = !primaryActionsExpanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                CandidatesRow()
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = primaryActionsExpanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                SmartbarActionRowContent(rowType = primaryActionsRowType)
            }
        }
    }

    @Composable
    fun SecondaryActionsToggle() {
        IconButton(
            onClick = { prefs.smartbar.secondaryActionsExpanded.set(!secondaryActionsExpanded) },
            enabled = secondaryActionsEnabled,
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .snyggShadow(secondaryActionsToggleStyle)
                    .snyggBorder(secondaryActionsToggleStyle)
                    .snyggBackground(secondaryActionsToggleStyle),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = secondaryActionsEnabled,
                    enter = VerticalEnterTransition,
                    exit = VerticalExitTransition,
                ) {
                    val transition = updateTransition(secondaryActionsExpanded, label = "smartbarSecondaryRowToggleBtn")
                    val alpha by transition.animateFloat(label = "alpha") { if (it) 1f else 0f }
                    val rotation by transition.animateFloat(label = "rotation") { if (it) 180f else 0f }
                    // Expanded icon
                    Icon(
                        modifier = Modifier
                            .alpha(alpha)
                            .rotate(rotation),
                        painter = painterResource(R.drawable.ic_unfold_less),
                        contentDescription = null,
                        tint = secondaryActionsToggleStyle.foreground.solidColor(),
                    )
                    // Not expanded icon
                    Icon(
                        modifier = Modifier
                            .alpha(1f - alpha)
                            .rotate(rotation - 180f),
                        painter = painterResource(R.drawable.ic_unfold_more),
                        contentDescription = null,
                        tint = secondaryActionsToggleStyle.foreground.solidColor(),
                    )
                }
            }
        }
    }

    SideEffect {
        if (!shouldAnimate) {
            prefs.smartbar.primaryActionsExpandWithAnimation.set(true)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight)
            .snyggBackground(primaryRowStyle),
    ) {
        if (flipToggles) {
            SecondaryActionsToggle()
            CenterContent()
            PrimaryActionsToggle()
        } else {
            PrimaryActionsToggle()
            CenterContent()
            SecondaryActionsToggle()
        }
    }
}

@Composable
private fun SmartbarActionRowContent(
    rowType: SmartbarRowType,
    modifier: Modifier = Modifier,
) {
    when (rowType) {
        SmartbarRowType.QUICK_ACTIONS -> {
            QuickActionsRow(
                modifier = modifier
                    .fillMaxWidth()
                    .height(FlorisImeSizing.smartbarHeight),
            )
        }
        SmartbarRowType.CLIPBOARD_CURSOR_TOOLS -> {
            SmartbarClipboardCursorRow(
                modifier = modifier
                    .fillMaxWidth()
                    .height(FlorisImeSizing.smartbarHeight),
            )
        }
    }
}

@Composable
private fun SmartbarSecondaryRow(modifier: Modifier = Modifier) {
    val prefs by florisPreferenceModel()
    val secondaryRowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarSecondaryRow)
    val secondaryActionsEnabled by prefs.smartbar.secondaryActionsEnabled.observeAsState()
    val secondaryActionsExpanded by prefs.smartbar.secondaryActionsExpanded.observeAsState()
    val secondaryActionsRowType by prefs.smartbar.secondaryActionsRowType.observeAsState()
    val secondaryActionsPlacement by prefs.smartbar.secondaryActionsPlacement.observeAsState()
    val background = secondaryRowStyle.background.solidColor().let { color ->
        if (secondaryActionsPlacement == SecondaryRowPlacement.OVERLAY_APP_UI) {
            if (color.isUnspecified || color.alpha == 0f) {
                FlorisImeTheme.style.get(FlorisImeUi.Keyboard).background.solidColor(default = Color.Black)
            } else {
                color
            }
        } else {
            color
        }
    }

    AnimatedVisibility(
        visible = secondaryActionsEnabled && secondaryActionsExpanded,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        SmartbarActionRowContent(
            modifier = modifier.background(background),
            rowType = secondaryActionsRowType,
        )
    }
}
