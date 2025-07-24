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

import android.content.Context
import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.stringRes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class QuickAction {
    open fun onPointerDown(context: Context) = Unit

    open fun onPointerUp(context: Context) = Unit

    open fun onPointerCancel(context: Context) = Unit

    @Serializable
    @SerialName("insert_key")
    data class InsertKey(val data: KeyData) : QuickAction() {
        override fun onPointerDown(context: Context) {
            val keyboardManager by context.keyboardManager()
            keyboardManager.inputEventDispatcher.sendDown(data)
        }

        override fun onPointerUp(context: Context) {
            val keyboardManager by context.keyboardManager()
            keyboardManager.inputEventDispatcher.sendUp(data)
            if (!keyboardManager.inputEventDispatcher.isRepeatable(data) &&
                data.code != KeyCode.TOGGLE_ACTIONS_OVERFLOW && data.code != KeyCode.CLIPBOARD_SELECT_ALL) {
                keyboardManager.activeState.isActionsOverflowVisible = false
            }
        }

        override fun onPointerCancel(context: Context) {
            val keyboardManager by context.keyboardManager()
            keyboardManager.inputEventDispatcher.sendCancel(data)
        }
    }

    @Serializable
    @SerialName("insert_text")
    data class InsertText(val data: String) : QuickAction() {
        override fun onPointerUp(context: Context) {
            val editorInstance by context.editorInstance()
            editorInstance.commitText(data)
        }
    }
}

fun QuickAction.keyData(): KeyData {
    return if (this is QuickAction.InsertKey) data else TextKeyData.UNSPECIFIED
}

@Composable
fun QuickAction.computeDisplayName(evaluator: ComputingEvaluator): String {
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
            KeyCode.LANGUAGE_SWITCH -> R.string.quick_action__language_switch
            KeyCode.SETTINGS -> R.string.quick_action__settings
            KeyCode.UNDO -> R.string.quick_action__undo
            KeyCode.REDO -> R.string.quick_action__redo
            KeyCode.TOGGLE_ACTIONS_OVERFLOW -> R.string.quick_action__toggle_actions_overflow
            KeyCode.TOGGLE_INCOGNITO_MODE -> R.string.quick_action__toggle_incognito_mode
            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect
            KeyCode.VOICE_INPUT -> R.string.quick_action__voice_input
            // TODO: In the future this will be merged into the resize keyboard panel, for now it is a separate action
            KeyCode.TOGGLE_COMPACT_LAYOUT -> R.string.quick_action__one_handed_mode
            KeyCode.DRAG_MARKER -> if (evaluator.state.debugShowDragAndDropHelpers) {
                R.string.quick_action__drag_marker
            } else {
                R.string.general__empty_string
            }
            KeyCode.NOOP -> R.string.quick_action__noop
            else -> R.string.general__invalid_fatal
        })
        is QuickAction.InsertText -> data
    }
}

@Composable
fun QuickAction.computeTooltip(evaluator: ComputingEvaluator): String {
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
            KeyCode.LANGUAGE_SWITCH -> R.string.quick_action__language_switch__tooltip
            KeyCode.SETTINGS -> R.string.quick_action__settings__tooltip
            KeyCode.UNDO -> R.string.quick_action__undo__tooltip
            KeyCode.REDO -> R.string.quick_action__redo__tooltip
            KeyCode.TOGGLE_ACTIONS_OVERFLOW -> R.string.quick_action__toggle_actions_overflow__tooltip
            KeyCode.TOGGLE_INCOGNITO_MODE -> R.string.quick_action__toggle_incognito_mode__tooltip
            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect__tooltip
            KeyCode.VOICE_INPUT -> R.string.quick_action__voice_input__tooltip
            // TODO: In the future this will be merged into the resize keyboard panel, for now it is a separate action
            KeyCode.TOGGLE_COMPACT_LAYOUT -> R.string.quick_action__one_handed_mode__tooltip
            KeyCode.DRAG_MARKER -> if (evaluator.state.debugShowDragAndDropHelpers) {
                R.string.quick_action__drag_marker__tooltip
            } else {
                R.string.general__empty_string
            }
            KeyCode.NOOP -> R.string.quick_action__noop__tooltip
            else -> R.string.general__invalid_fatal
        })
        is QuickAction.InsertText -> "Insert text '$data'"
    }
}
