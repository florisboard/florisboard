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

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import dev.patrickgold.florisboard.ime.keyboard3.ImeState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.florisboard.lib.compose.stringRes
import org.k3lp.lib.text.K3Descriptor

@Serializable
sealed class QuickAction {
    abstract fun migrateToK3DescriptorOrNull(): InsertK3Descriptor?

    @Serializable
    @SerialName("insert_k3descriptor")
    data class InsertK3Descriptor(val descriptor: K3Descriptor) : QuickAction() {
        override fun migrateToK3DescriptorOrNull(): InsertK3Descriptor {
            return this
        }
    }

    @Deprecated("InsertKey is deprecated, and must exclusively be used in the preference migration logic!")
    @Serializable
    @SerialName("insert_key")
    data class InsertKey(val data: KeyData) : QuickAction() {
        override fun migrateToK3DescriptorOrNull(): InsertK3Descriptor? {
            return when (data.code) {
                KeyCode.UNDO -> InsertK3Descriptor(ImeActions.Undo)
                KeyCode.REDO -> InsertK3Descriptor(ImeActions.Redo)
                KeyCode.SETTINGS -> InsertK3Descriptor(ImeActions.Settings)
                KeyCode.TOGGLE_FLOATING_WINDOW -> InsertK3Descriptor(ImeActions.ToggleFloatingWindow)
                KeyCode.TOGGLE_RESIZE_MODE -> InsertK3Descriptor(ImeActions.ToggleResizeMode)
                KeyCode.IME_UI_MODE_CLIPBOARD -> InsertK3Descriptor(ImeActions.ShowClipboardPanel)
                KeyCode.IME_UI_MODE_MEDIA -> InsertK3Descriptor(ImeActions.ShowMediaPanel)
                KeyCode.TOGGLE_COMPACT_LAYOUT, KeyCode.COMPACT_LAYOUT_TO_RIGHT -> InsertK3Descriptor(ImeActions.ToggleCompactLayout)
                KeyCode.TOGGLE_AUTOCORRECT -> InsertK3Descriptor(ImeActions.ToggleAutocorrect)
                KeyCode.TOGGLE_INCOGNITO_MODE -> InsertK3Descriptor(ImeActions.TogglePersonalizedLearning)
                KeyCode.ARROW_UP -> InsertK3Descriptor(ImeActions.ArrowUp)
                KeyCode.ARROW_DOWN -> InsertK3Descriptor(ImeActions.ArrowDown)
                KeyCode.ARROW_LEFT -> InsertK3Descriptor(ImeActions.ArrowLeft)
                KeyCode.ARROW_RIGHT -> InsertK3Descriptor(ImeActions.ArrowRight)
                KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> InsertK3Descriptor(ImeActions.ClipboardClearPrimaryClip)
                KeyCode.CLIPBOARD_COPY -> InsertK3Descriptor(ImeActions.ClipboardCopy)
                KeyCode.CLIPBOARD_CUT -> InsertK3Descriptor(ImeActions.ClipboardCut)
                KeyCode.CLIPBOARD_PASTE -> InsertK3Descriptor(ImeActions.ClipboardPaste)
                KeyCode.CLIPBOARD_SELECT_ALL -> InsertK3Descriptor(ImeActions.SelectAll)
                KeyCode.LANGUAGE_SWITCH -> InsertK3Descriptor(ImeActions.LanguageSwitch)
                KeyCode.FORWARD_DELETE -> InsertK3Descriptor(ImeActions.Delete)
                KeyCode.IME_HIDE_UI -> InsertK3Descriptor(ImeActions.HideImeWindow)
                else -> null
            }
        }
    }

    @Deprecated("InsertText is deprecated, and must exclusively be used in the preference migration logic!")
    @Serializable
    @SerialName("insert_text")
    data class InsertText(val data: String) : QuickAction() {
        override fun migrateToK3DescriptorOrNull(): InsertK3Descriptor? {
            return null
        }
    }
}

@Composable
fun QuickAction.computeDisplayName(imeState: ImeState): String {
    return when (this) {
        is QuickAction.InsertK3Descriptor -> stringRes(when (descriptor) {
            ImeActions.Delete -> R.string.quick_action__forward_delete
            ImeActions.ArrowDown -> R.string.quick_action__arrow_down
            ImeActions.ArrowLeft -> R.string.quick_action__arrow_left
            ImeActions.ArrowRight -> R.string.quick_action__arrow_right
            ImeActions.ArrowUp -> R.string.quick_action__arrow_up
            ImeActions.ClipboardClearPrimaryClip -> R.string.quick_action__clipboard_clear_primary_clip
            ImeActions.ClipboardCopy -> R.string.quick_action__clipboard_copy
            ImeActions.ClipboardCut -> R.string.quick_action__clipboard_cut
            ImeActions.ClipboardPaste -> R.string.quick_action__clipboard_paste
            ImeActions.SelectAll -> R.string.quick_action__clipboard_select_all
            ImeActions.Settings -> R.string.quick_action__settings
            ImeActions.ShowMediaPanel -> R.string.quick_action__ime_ui_mode_media
            ImeActions.ShowClipboardPanel -> R.string.quick_action__ime_ui_mode_clipboard
            ImeActions.HideImeWindow -> R.string.quick_action__ime_hide_ui
            ImeActions.LanguageSwitch -> R.string.quick_action__language_switch
            ImeActions.ToggleActionsOverflow -> R.string.quick_action__toggle_actions_overflow
            ImeActions.ToggleAutocorrect -> R.string.quick_action__toggle_autocorrect
            // TODO: In the future this will be merged into the resize keyboard panel, for now it is a separate action
            ImeActions.ToggleCompactLayout -> R.string.quick_action__one_handed_mode
            ImeActions.ToggleFloatingWindow -> R.string.quick_action__floating_window_mode
            ImeActions.TogglePersonalizedLearning -> R.string.quick_action__toggle_incognito_mode
            ImeActions.ToggleResizeMode -> R.string.quick_action__resize_mode
            ImeActions.Undo -> R.string.quick_action__undo
            ImeActions.Redo -> R.string.quick_action__redo
            ImeActions.ExternalVoiceInput -> R.string.quick_action__voice_input
            ImeActions.NoopDragMarker -> if (imeState.flags.debugShowDragAndDropHelpers) {
                R.string.quick_action__drag_marker
            } else {
                R.string.general__empty_string
            }
            ImeActions.NoopSpacer -> R.string.quick_action__noop
            else -> R.string.general__invalid_fatal
        })
        else -> "unsupported"
    }
}

@Composable
fun QuickAction.computeTooltip(imeState: ImeState): String {
    return when (this) {
        is QuickAction.InsertK3Descriptor -> stringRes(when (descriptor) {
            ImeActions.ArrowDown -> R.string.quick_action__arrow_down__tooltip
            ImeActions.ArrowLeft -> R.string.quick_action__arrow_left__tooltip
            ImeActions.ArrowRight -> R.string.quick_action__arrow_right__tooltip
            ImeActions.ArrowUp -> R.string.quick_action__arrow_up__tooltip
            ImeActions.ClipboardClearPrimaryClip -> R.string.quick_action__clipboard_clear_primary_clip__tooltip
            ImeActions.ClipboardCopy -> R.string.quick_action__clipboard_copy__tooltip
            ImeActions.ClipboardCut -> R.string.quick_action__clipboard_cut__tooltip
            ImeActions.ClipboardPaste -> R.string.quick_action__clipboard_paste__tooltip
            ImeActions.SelectAll -> R.string.quick_action__clipboard_select_all__tooltip
            ImeActions.Settings -> R.string.quick_action__settings__tooltip
            ImeActions.ShowMediaPanel -> R.string.quick_action__ime_ui_mode_media__tooltip
            ImeActions.ShowClipboardPanel -> R.string.quick_action__ime_ui_mode_clipboard__tooltip
            ImeActions.HideImeWindow -> R.string.quick_action__ime_hide_ui__tooltip
            ImeActions.LanguageSwitch -> R.string.quick_action__language_switch__tooltip
            ImeActions.ToggleActionsOverflow -> R.string.quick_action__toggle_actions_overflow__tooltip
            ImeActions.ToggleAutocorrect -> R.string.quick_action__toggle_autocorrect__tooltip
            // TODO: In the future this will be merged into the resize keyboard panel, for now it is a separate action
            ImeActions.ToggleCompactLayout -> R.string.quick_action__one_handed_mode__tooltip
            ImeActions.ToggleFloatingWindow -> R.string.quick_action__floating_window_mode__tooltip
            ImeActions.TogglePersonalizedLearning -> R.string.quick_action__toggle_incognito_mode__tooltip
            ImeActions.ToggleResizeMode -> R.string.quick_action__resize_mode__tooltip
            ImeActions.Undo -> R.string.quick_action__undo__tooltip
            ImeActions.Redo -> R.string.quick_action__redo__tooltip
            ImeActions.ExternalVoiceInput -> R.string.quick_action__voice_input__tooltip
            ImeActions.NoopDragMarker -> if (imeState.flags.debugShowDragAndDropHelpers) {
                R.string.quick_action__drag_marker__tooltip
            } else {
                R.string.general__empty_string
            }
            ImeActions.NoopSpacer -> R.string.quick_action__noop__tooltip
            else -> R.string.general__invalid_fatal
        })
        else -> "unsupported"
    }
}
