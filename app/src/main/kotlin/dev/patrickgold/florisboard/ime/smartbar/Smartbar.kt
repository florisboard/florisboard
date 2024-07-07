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

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionButton
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsRow
import dev.patrickgold.florisboard.ime.smartbar.quickaction.ToggleOverflowPanelAction
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.horizontalTween
import dev.patrickgold.florisboard.lib.compose.verticalTween
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.vectorResource
import org.florisboard.lib.snygg.ui.snyggBackground
import org.florisboard.lib.snygg.ui.snyggBorder
import org.florisboard.lib.snygg.ui.snyggShadow
import org.florisboard.lib.snygg.ui.solidColor

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
    val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.observeAsState()

    AnimatedVisibility(
        visible = smartbarEnabled,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        when (extendedActionsPlacement) {
            ExtendedActionsPlacement.ABOVE_CANDIDATES -> {
                Column {
                    SmartbarSecondaryRow()
                    SmartbarMainRow()
                }
            }

            ExtendedActionsPlacement.BELOW_CANDIDATES -> {
                Column {
                    SmartbarMainRow()
                    SmartbarSecondaryRow()
                }
            }

            ExtendedActionsPlacement.OVERLAY_APP_UI -> {
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
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    val smartbarLayout by prefs.smartbar.layout.observeAsState()
    val flipToggles by prefs.smartbar.flipToggles.observeAsState()
    val sharedActionsExpanded by prefs.smartbar.sharedActionsExpanded.observeAsState()
    val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.observeAsState()

    val shouldAnimate by prefs.smartbar.sharedActionsExpandWithAnimation.observeAsState()

    val smartbarStyle = FlorisImeTheme.style.get(FlorisImeUi.Smartbar)
    val primaryActionsToggleStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarSharedActionsToggle)
    val secondaryActionsToggleStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarExtendedActionsToggle)

    @Composable
    fun SharedActionsToggle() {
        IconButton(
            onClick = {
                if (/* was */ sharedActionsExpanded) {
                    keyboardManager.activeState.isActionsOverflowVisible = false
                }
                prefs.smartbar.sharedActionsExpanded.set(!sharedActionsExpanded)
            },
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .snyggShadow(primaryActionsToggleStyle)
                    .snyggBorder(context, primaryActionsToggleStyle)
                    .snyggBackground(context, primaryActionsToggleStyle),
                contentAlignment = Alignment.Center,
            ) {
                val transition = updateTransition(sharedActionsExpanded, label = "sharedActionsExpandedToggleBtn")
                val rotation by transition.animateFloat(
                    transitionSpec = {
                        if (shouldAnimate) AnimationTween else NoAnimationTween
                    },
                    label = "rotation",
                ) {
                    if (it) 180f else 0f
                }
                val arrowIcon = if (flipToggles) {
                    Icons.AutoMirrored.Default.KeyboardArrowLeft
                } else {
                    Icons.AutoMirrored.Default.KeyboardArrowRight
                }
                val incognitoIcon = vectorResource(id = R.drawable.ic_incognito)
                val incognitoDisplayMode = prefs.keyboard.incognitoDisplayMode.observeAsState()
                val isIncognitoMode = keyboardManager.activeState.isIncognitoMode
                val icon = if (isIncognitoMode) {
                    when (incognitoDisplayMode.value) {
                        IncognitoDisplayMode.REPLACE_SHARED_ACTIONS_TOGGLE -> incognitoIcon!!
                        IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD -> arrowIcon
                    }
                } else {
                    arrowIcon
                }
                Icon(
                    modifier = Modifier.rotate(if (incognitoDisplayMode.value == IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD) rotation else 0f),
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryActionsToggleStyle.foreground.solidColor(
                        context,
                        default = FlorisImeTheme.fallbackContentColor()
                    ),
                )
            }
        }
    }

    @Composable
    fun RowScope.CenterContent() {
        val expanded = sharedActionsExpanded && smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED
        Box(
            modifier = Modifier
                .weight(1f)
                .height(FlorisImeSizing.smartbarHeight),
        ) {
            val enterTransition = if (shouldAnimate) HorizontalEnterTransition else NoEnterTransition
            val exitTransition = if (shouldAnimate) HorizontalExitTransition else NoExitTransition
            androidx.compose.animation.AnimatedVisibility(
                visible = !expanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                CandidatesRow()
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                QuickActionsRow(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(FlorisImeSizing.smartbarHeight),
                    elementName = FlorisImeUi.SmartbarSharedActionsRow,
                )
            }
        }
    }

    @Composable
    fun ExtendedActionsToggle() {
        IconButton(
            onClick = {
                if (/* was */ extendedActionsExpanded) {
                    keyboardManager.activeState.isActionsOverflowVisible = false
                }
                prefs.smartbar.extendedActionsExpanded.set(!extendedActionsExpanded)
            },
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .snyggShadow(secondaryActionsToggleStyle)
                    .snyggBorder(context, secondaryActionsToggleStyle)
                    .snyggBackground(context, secondaryActionsToggleStyle),
                contentAlignment = Alignment.Center,
            ) {
                val transition = updateTransition(extendedActionsExpanded, label = "smartbarSecondaryRowToggleBtn")
                val alpha by transition.animateFloat(label = "alpha") { if (it) 1f else 0f }
                val rotation by transition.animateFloat(label = "rotation") { if (it) 180f else 0f }
                // Expanded icon
                Icon(
                    modifier = Modifier
                        .alpha(alpha)
                        .rotate(rotation),
                    imageVector = Icons.Default.UnfoldLess,
                    contentDescription = null,
                    tint = secondaryActionsToggleStyle.foreground.solidColor(context),
                )
                // Not expanded icon
                Icon(
                    modifier = Modifier
                        .alpha(1f - alpha)
                        .rotate(rotation - 180f),
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = null,
                    tint = secondaryActionsToggleStyle.foreground.solidColor(context),
                )
            }
        }
    }

    @Composable
    fun StickyAction() {
        val actionArrangement by prefs.smartbar.actionArrangement.observeAsState()
        val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()

        val action = when {
            actionArrangement.stickyAction != null -> {
                actionArrangement.stickyAction
            }

            smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED && sharedActionsExpanded -> {
                ToggleOverflowPanelAction
            }

            else -> null
        }

        if (action != null) {
            QuickActionButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                action = action,
                evaluator = evaluator,
            )
        } else {
            Spacer(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .aspectRatio(1f),
            )
        }
    }

    SideEffect {
        if (!shouldAnimate) {
            prefs.smartbar.sharedActionsExpandWithAnimation.set(true)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight)
            .snyggBackground(context, smartbarStyle),
    ) {
        when (smartbarLayout) {
            SmartbarLayout.SUGGESTIONS_ONLY -> {
                CandidatesRow()
            }

            SmartbarLayout.ACTIONS_ONLY -> {
                QuickActionsRow(elementName = FlorisImeUi.SmartbarSharedActionsRow)
            }

            SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED -> {
                if (!flipToggles) {
                    SharedActionsToggle()
                    CenterContent()
                    StickyAction()
                } else {
                    StickyAction()
                    CenterContent()
                    SharedActionsToggle()
                }
            }

            SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED -> {
                if (!flipToggles) {
                    ExtendedActionsToggle()
                    CenterContent()
                    StickyAction()
                } else {
                    StickyAction()
                    CenterContent()
                    ExtendedActionsToggle()
                }
            }
        }
    }
}

@Composable
private fun SmartbarSecondaryRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs by florisPreferenceModel()
    val smartbarLayout by prefs.smartbar.layout.observeAsState()
    val secondaryRowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarExtendedActionsRow)
    val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.observeAsState()
    val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.observeAsState()
    val background = secondaryRowStyle.background.solidColor(context).let { color ->
        if (extendedActionsPlacement == ExtendedActionsPlacement.OVERLAY_APP_UI) {
            if (color.isUnspecified || color.alpha == 0f) {
                FlorisImeTheme.style.get(FlorisImeUi.Keyboard).background.solidColor(context, default = Color.Black)
            } else {
                color
            }
        } else {
            color
        }
    }

    AnimatedVisibility(
        visible = smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED && extendedActionsExpanded,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        QuickActionsRow(
            modifier = modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .background(background),
            elementName = FlorisImeUi.SmartbarExtendedActionsRow,
        )
    }
}
