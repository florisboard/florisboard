/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.gestures

import java.util.*

/**
 * Enum for declaring the possible actions for swipe gestures.
 */
enum class SwipeAction {
    NO_ACTION,
    CYCLE_TO_PREVIOUS_KEYBOARD_MODE,
    CYCLE_TO_NEXT_KEYBOARD_MODE,
    DELETE_CHARACTERS_PRECISELY,
    DELETE_WORD,
    DELETE_WORDS_PRECISELY,
    HIDE_KEYBOARD,
    INSERT_SPACE,
    MOVE_CURSOR_UP,
    MOVE_CURSOR_DOWN,
    MOVE_CURSOR_LEFT,
    MOVE_CURSOR_RIGHT,
    MOVE_CURSOR_START_OF_LINE,
    MOVE_CURSOR_END_OF_LINE,
    MOVE_CURSOR_START_OF_PAGE,
    MOVE_CURSOR_END_OF_PAGE,
    REDO,
    SHIFT,
    SHOW_INPUT_METHOD_PICKER,
    SWITCH_TO_PREV_SUBTYPE,
    SWITCH_TO_NEXT_SUBTYPE,
    SWITCH_TO_CLIPBOARD_CONTEXT,
    SWITCH_TO_PREV_KEYBOARD,
    UNDO;

    companion object {
        fun fromString(string: String): SwipeAction {
            return valueOf(string.toUpperCase(Locale.ENGLISH))
        }
    }

    override fun toString(): String {
        return super.toString().toLowerCase(Locale.ENGLISH)
    }
}
