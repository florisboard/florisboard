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

object ImeIcons {
    val ArrowDown = flIcon("arrow_down")
    val ArrowLeft = flIcon("arrow_left")
    val ArrowRight = flIcon("arrow_right")
    val ArrowUp = flIcon("arrow_up")
    val Backspace = flIcon("backspace")
    val ClipboardClearPrimaryClip = flIcon("clipboard_clear_primary_clip")
    val ClipboardCopy = flIcon("clipboard_copy")
    val ClipboardCut = flIcon("clipboard_cut")
    val ClipboardPaste = flIcon("clipboard_paste")
    val Close = flIcon("close")
    val Delete = flIcon("delete")
    val DragMarker = flIcon("drag_marker")
    val Enter = flIcon("enter")
    val HideKeyboard = flIcon("hide_keyboard")
    val LanguageSwitch = flIcon("language_switch")
    val ClipboardPanel = flIcon("clipboard_panel")
    val MediaPanel = flIcon("media_panel")
    val TextPanel = flIcon("text_panel")
    val Noop = flIcon("noop")
    val Redo = flIcon("redo")
    val SelectAll = flIcon("select_all")
    val Settings = flIcon("settings")
    val ShowKeyboard = flIcon("show_keyboard")
    val SpaceBar = flIcon("space_bar")
    val ToggleActionsOverflow = flIcon("toggle_actions_overflow")
    val ToggleAutocorrect = flIcon("toggle_autocorrect")
    val ToggleCompactLayout = flIcon("toggle_compact_layout")
    val ToggleFloatingWindow = flIcon("toggle_floating_window")
    val ToggleResizeMode = flIcon("toggle_resize_mode")
    val Undo = flIcon("undo")
    val Voice = flIcon("voice")

    private fun flIcon(name: String): K3Descriptor {
        return K3Descriptor("fl", "icon", name)
    }
}
