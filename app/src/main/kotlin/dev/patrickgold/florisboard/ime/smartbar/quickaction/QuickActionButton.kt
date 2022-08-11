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

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.computeIconResId
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.android.isOrientationPortrait
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBorder
import dev.patrickgold.florisboard.lib.snygg.ui.snyggShadow
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor

@Composable
fun QuickActionButton(
    action: QuickAction,
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var isPressed by remember { mutableStateOf(false) }
    val actionStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarQuickAction, isPressed = isPressed)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .snyggShadow(actionStyle)
            .snyggBorder(actionStyle)
            .snyggBackground(actionStyle)
            .pointerInput(action) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown()
                        down.consume()
                        isPressed = true
                        action.onPointerDown(context)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            action.onPointerUp(context)
                        } else {
                            action.onPointerCancel(context)
                        }
                        isPressed = false
                    }
                }
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Render foreground
        when (action) {
            is QuickAction.InsertKey -> {
                val (iconResId, label) = remember(action, evaluator) {
                    evaluator.computeIconResId(action.data) to evaluator.computeLabel(action.data)
                }
                if (iconResId != null) {
                    Icon(
                        modifier = Modifier.padding(if (configuration.isOrientationPortrait()) 2.dp else 0.dp),
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
}
