/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3

import org.k3lp.lib.text.K3Descriptor

object ImeActions {
    val Backspace = k3Action("backspace")
    val BackspaceWord = k3Action("backspace_word")
    val Delete = k3Action("delete") // TODO k3lp does not support this yet?
    val DeleteWord = k3Action("delete_word")
    val Enter = k3Action("enter")

    val ArrowDown = flAction("arrow_down")
    val ArrowLeft = flAction("arrow_left")
    val ArrowRight = flAction("arrow_right")
    val ArrowUp = flAction("arrow_up")

    val ClipboardCopy = flAction("clipboard_copy")
    val ClipboardCut = flAction("clipboard_cut")
    val ClipboardPaste = flAction("clipboard_paste")
    val ClipboardClearHistory = flAction("clipboard_clear_history")
    val ClipboardClearFullHistory = flAction("clipboard_clear_full_history")
    val ClipboardClearPrimaryClip = flAction("clipboard_clear_primary_clip")
    val SelectAll = flAction("select_all")

    val ShowImeWindow = flAction("show_ime_window")
    val HideImeWindow = flAction("hide_ime_window")
    val ToggleFloatingWindow = flAction("toggle_floating_window")
    val ToggleCompactLayout = flAction("toggle_compact_layout")
    val CompactLayoutToLeft = flAction("compact_layout_to_left")
    val CompactLayoutToRight = flAction("compact_layout_to_right")
    val SplitLayout = flAction("split_layout")
    val MergeLayout = flAction("merge_layout")
    val ToggleResizeMode = flAction("toggle_resize_mode")

    val ShowTextPanel = flAction("show_text_panel")
    val ShowMediaPanel = flAction("show_media_panel")
    val ShowClipboardPanel = flAction("show_clipboard_panel")

    val Undo = flAction("undo")
    val Redo = flAction("redo")

    val LanguageSwitch = flAction("language_switch")
    // TODO subtype rework?

    val Settings = flAction("settings")
    val ToggleActionsEditor = flAction("toggle_actions_editor")
    val ToggleActionsOverflow = flAction("toggle_actions_overflow")
    val ToggleAutocorrect = flAction("toggle_autocorrect")
    val TogglePersonalizedLearning = flAction("toggle_personalized_learning")

    val ExternalVoiceInput = flAction("external_voice_input")

    val NoopDragMarker = flAction("noop_drag_marker")
    val NoopSpacer = flAction("noop_spacer")

    val Repeatable = listOf(
        ArrowDown,
        ArrowLeft,
        ArrowRight,
        ArrowUp,
        Backspace,
        Delete,
        Undo,
        Redo,
    )

    private fun flAction(name: String): K3Descriptor {
        return K3Descriptor("fl", "action", name)
    }

    private fun k3Action(name: String): K3Descriptor {
        return K3Descriptor("k3", "action", name)
    }
}
