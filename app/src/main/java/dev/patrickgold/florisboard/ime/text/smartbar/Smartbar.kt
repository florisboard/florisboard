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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.snygg.ui.solidColor
import dev.patrickgold.jetpref.datastore.model.observeAsState

private const val SmartbarAnimationDuration = 200
private val SmartbarVerticalEnterTransition =
    fadeIn(tween(SmartbarAnimationDuration)) + expandVertically(tween(SmartbarAnimationDuration))
private val SmartbarVerticalExitTransition =
    fadeOut(tween(SmartbarAnimationDuration)) + shrinkVertically(tween(SmartbarAnimationDuration))
private val SmartbarHorizontalEnterTransition =
    fadeIn(tween(SmartbarAnimationDuration)) + expandHorizontally(tween(SmartbarAnimationDuration))
private val SmartbarHorizontalExitTransition =
    fadeOut(tween(SmartbarAnimationDuration)) + shrinkHorizontally(tween(SmartbarAnimationDuration))

@Composable
fun Smartbar() {
    val prefs by florisPreferenceModel()
    val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
    val showSecondaryRowBelowPrimary by prefs.smartbar.showSecondaryRowBelowPrimary.observeAsState()

    AnimatedVisibility(
        visible = smartbarEnabled,
        enter = SmartbarVerticalEnterTransition,
        exit = SmartbarVerticalExitTransition,
    ) {
        Column {
            if (showSecondaryRowBelowPrimary) {
                SmartbarPrimaryRow()
                SmartbarSecondaryRow()
            } else {
                SmartbarSecondaryRow()
                SmartbarPrimaryRow()
            }
        }
    }
}

@Composable
private fun SmartbarPrimaryRow() = key(FlorisImeUi.SmartbarPrimaryRow) {
    val prefs by florisPreferenceModel()
    val actionRowExpanded by prefs.smartbar.actionRowExpanded.observeAsState()
    val secondaryRowExpanded by prefs.smartbar.secondaryRowExpanded.observeAsState()

    val rowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarPrimaryRow)
    val actionsToggleStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarPrimaryActionRowToggle)
    val secondaryToggleStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarPrimarySecondaryRowToggle)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight)
            .snyggBackground(rowStyle.background),
    ) {
        IconButton(
            modifier = Modifier.padding(end = 8.dp),
            onClick = { prefs.smartbar.actionRowExpanded.set(!actionRowExpanded) },
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .snyggBackground(actionsToggleStyle.background, actionsToggleStyle.shape),
                contentAlignment = Alignment.Center,
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (actionRowExpanded) 180f else 0f,
                )
                Icon(
                    modifier = Modifier.rotate(rotation),
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                    contentDescription = null,
                    tint = actionsToggleStyle.foreground.solidColor(),
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(FlorisImeSizing.smartbarHeight),
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !actionRowExpanded,
                enter = SmartbarHorizontalEnterTransition,
                exit = SmartbarHorizontalExitTransition,
            ) {
                Text(
                    text = "primary row",
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = actionRowExpanded,
                enter = SmartbarHorizontalEnterTransition,
                exit = SmartbarHorizontalExitTransition,
            ) {
                SmartbarActionRow()
            }
        }
        IconButton(
            modifier = Modifier.padding(start = 8.dp),
            onClick = {
                if (actionRowExpanded) {
                    //
                } else {
                    prefs.smartbar.secondaryRowExpanded.set(!secondaryRowExpanded)
                }
            },
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .snyggBackground(secondaryToggleStyle.background, secondaryToggleStyle.shape),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !actionRowExpanded,
                    enter = SmartbarVerticalEnterTransition,
                    exit = SmartbarVerticalExitTransition,
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
                androidx.compose.animation.AnimatedVisibility(
                    visible = actionRowExpanded,
                    enter = SmartbarHorizontalEnterTransition,
                    exit = SmartbarHorizontalExitTransition,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_horiz),
                        contentDescription = null,
                        tint = secondaryToggleStyle.foreground.solidColor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartbarSecondaryRow() = key(FlorisImeUi.SmartbarSecondaryRow) {
    val prefs by florisPreferenceModel()
    val secondaryRowExpanded by prefs.smartbar.secondaryRowExpanded.observeAsState()

    val rowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarSecondaryRow)

    AnimatedVisibility(
        visible = secondaryRowExpanded,
        enter = SmartbarVerticalEnterTransition,
        exit = SmartbarVerticalExitTransition,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .snyggBackground(rowStyle.background),
        ) {
            Text(text = "secondary row")
        }
    }
}
