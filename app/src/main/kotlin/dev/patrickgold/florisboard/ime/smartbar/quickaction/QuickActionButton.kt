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
import androidx.compose.runtime.DisposableEffect
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
import dev.patrickgold.compose.tooltip.tooltip
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.computeIconResId
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.android.isOrientationPortrait
import dev.patrickgold.florisboard.lib.compose.stringRes
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
    val actionStyle = FlorisImeTheme.style.get(
        element = FlorisImeUi.SmartbarActionKey,
        code = action.keyCode(),
        isPressed = isPressed,
    )

    // Need to manually cancel an action if this composable suddenly leaves the composition to prevent the key from
    // being stuck in the pressed state
    DisposableEffect(action) {
        onDispose {
            if (action is QuickAction.InsertKey) {
                action.onPointerCancel(context)
            }
        }
    }

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
            .tooltip(action.computeTooltip(evaluator))
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

private fun QuickAction.keyCode(): Int {
    return if (this is QuickAction.InsertKey) data.code else KeyCode.UNSPECIFIED
}

@Composable
private fun QuickAction.computeDisplayName(evaluator: ComputingEvaluator): String {
    return when (this) {
        is QuickAction.InsertKey -> stringRes(when (data.code) {
            KeyCode.ARROW_UP -> R.string.quick_action__arrow_up
            KeyCode.ARROW_DOWN -> R.string.quick_action__arrow_down
            KeyCode.ARROW_LEFT -> R.string.quick_action__arrow_left
            KeyCode.ARROW_RIGHT -> R.string.quick_action__arrow_right
            KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> R.string.quick_action__clipboard_clear_primary_clip
            KeyCode.CLIPBOARD_COPY -> R.string.quick_action__clipboard_copy
            KeyCode.CLIPBOARD_CUT -> R.string.quick_action__clipboard_cut
            KeyCode.CLIPBOARD_PASTE -> R.string.quick_action__clipboard_paste
            KeyCode.CLIPBOARD_SELECT_ALL -> R.string.quick_action__clipboard_select_all
            KeyCode.IME_UI_MODE_CLIPBOARD -> R.string.quick_action__ime_ui_mode_clipboard
            KeyCode.IME_UI_MODE_MEDIA -> R.string.quick_action__ime_ui_mode_media
            KeyCode.SETTINGS -> R.string.quick_action__settings
            KeyCode.UNDO -> R.string.quick_action__undo
            KeyCode.REDO -> R.string.quick_action__redo
            KeyCode.TOGGLE_ACTIONS_OVERFLOW -> R.string.quick_action__toggle_actions_overflow
            KeyCode.TOGGLE_INCOGNITO_MODE -> R.string.quick_action__toggle_incognito_mode
            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect
            KeyCode.VOICE_INPUT -> R.string.quick_action__voice_input
            // TODO: In the future this will be merged into the resize keyboard panel, for now it is a separate action
            KeyCode.COMPACT_LAYOUT_TO_RIGHT -> R.string.quick_action__one_handed_mode
            else -> R.string.general__invalid_fatal
        })
        is QuickAction.InsertText -> data
    }
}

@Composable
private fun QuickAction.computeTooltip(evaluator: ComputingEvaluator): String {
    return when (this) {
        is QuickAction.InsertKey -> stringRes(when (data.code) {
            KeyCode.ARROW_UP -> R.string.quick_action__arrow_up__tooltip
            KeyCode.ARROW_DOWN -> R.string.quick_action__arrow_down__tooltip
            KeyCode.ARROW_LEFT -> R.string.quick_action__arrow_left__tooltip
            KeyCode.ARROW_RIGHT -> R.string.quick_action__arrow_right__tooltip
            KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> R.string.quick_action__clipboard_clear_primary_clip__tooltip
            KeyCode.CLIPBOARD_COPY -> R.string.quick_action__clipboard_copy__tooltip
            KeyCode.CLIPBOARD_CUT -> R.string.quick_action__clipboard_cut__tooltip
            KeyCode.CLIPBOARD_PASTE -> R.string.quick_action__clipboard_paste__tooltip
            KeyCode.CLIPBOARD_SELECT_ALL -> R.string.quick_action__clipboard_select_all__tooltip
            KeyCode.IME_UI_MODE_CLIPBOARD -> R.string.quick_action__ime_ui_mode_clipboard__tooltip
            KeyCode.IME_UI_MODE_MEDIA -> R.string.quick_action__ime_ui_mode_media__tooltip
            KeyCode.SETTINGS -> R.string.quick_action__settings__tooltip
            KeyCode.UNDO -> R.string.quick_action__undo__tooltip
            KeyCode.REDO -> R.string.quick_action__redo__tooltip
            KeyCode.TOGGLE_ACTIONS_OVERFLOW -> R.string.quick_action__toggle_actions_overflow__tooltip
            KeyCode.TOGGLE_INCOGNITO_MODE -> R.string.quick_action__toggle_incognito_mode__tooltip
            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect__tooltip
            KeyCode.VOICE_INPUT -> R.string.quick_action__voice_input__tooltip
            // TODO: In the future this will be merged into the resize keyboard panel, for now it is a separate action
            KeyCode.COMPACT_LAYOUT_TO_RIGHT -> R.string.quick_action__one_handed_mode__tooltip
            else -> R.string.general__invalid_fatal
        })
        is QuickAction.InsertText -> "Insert text '$data'"
    }
}
