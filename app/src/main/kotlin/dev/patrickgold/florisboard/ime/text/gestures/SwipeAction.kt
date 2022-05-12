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

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

/**
 * Enum for declaring the possible actions for swipe gestures.
 */
enum class SwipeAction {
    NO_ACTION,
    CYCLE_TO_PREVIOUS_KEYBOARD_MODE,
    CYCLE_TO_NEXT_KEYBOARD_MODE,
    DELETE_CHARACTER,
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
    SELECT_CHARACTERS_PRECISELY,
    SELECT_WORDS_PRECISELY,
    SHIFT,
    SHOW_INPUT_METHOD_PICKER,
    SWITCH_TO_PREV_SUBTYPE,
    SWITCH_TO_NEXT_SUBTYPE,
    SWITCH_TO_CLIPBOARD_CONTEXT,
    SWITCH_TO_PREV_KEYBOARD,
    TOGGLE_SMARTBAR_VISIBILITY,
    UNDO;

    companion object {
        @Composable
        fun generalListEntries() = listPrefEntries {
            entry(
                key = NO_ACTION,
                label = stringRes(R.string.enum__swipe_action__no_action),
            )
            entry(
                key = CYCLE_TO_PREVIOUS_KEYBOARD_MODE,
                label = stringRes(R.string.enum__swipe_action__cycle_to_previous_keyboard_mode),
            )
            entry(
                key = CYCLE_TO_NEXT_KEYBOARD_MODE,
                label = stringRes(R.string.enum__swipe_action__cycle_to_next_keyboard_mode),
            )
            entry(
                key = DELETE_WORD,
                label = stringRes(R.string.enum__swipe_action__delete_word),
            )
            entry(
                key = HIDE_KEYBOARD,
                label = stringRes(R.string.enum__swipe_action__hide_keyboard),
            )
            entry(
                key = INSERT_SPACE,
                label = stringRes(R.string.enum__swipe_action__insert_space),
            )
            entry(
                key = MOVE_CURSOR_UP,
                label = stringRes(R.string.enum__swipe_action__move_cursor_up),
            )
            entry(
                key = MOVE_CURSOR_DOWN,
                label = stringRes(R.string.enum__swipe_action__move_cursor_down),
            )
            entry(
                key = MOVE_CURSOR_LEFT,
                label = stringRes(R.string.enum__swipe_action__move_cursor_left),
            )
            entry(
                key = MOVE_CURSOR_RIGHT,
                label = stringRes(R.string.enum__swipe_action__move_cursor_right),
            )
            entry(
                key = MOVE_CURSOR_START_OF_LINE,
                label = stringRes(R.string.enum__swipe_action__move_cursor_start_of_line),
            )
            entry(
                key = MOVE_CURSOR_END_OF_LINE,
                label = stringRes(R.string.enum__swipe_action__move_cursor_end_of_line),
            )
            entry(
                key = MOVE_CURSOR_START_OF_PAGE,
                label = stringRes(R.string.enum__swipe_action__move_cursor_start_of_page),
            )
            entry(
                key = MOVE_CURSOR_END_OF_PAGE,
                label = stringRes(R.string.enum__swipe_action__move_cursor_end_of_page),
            )
            entry(
                key = SHIFT,
                label = stringRes(R.string.enum__swipe_action__shift),
            )
            entry(
                key = REDO,
                label = stringRes(R.string.enum__swipe_action__redo),
            )
            entry(
                key = UNDO,
                label = stringRes(R.string.enum__swipe_action__undo),
            )
            entry(
                key = SWITCH_TO_CLIPBOARD_CONTEXT,
                label = stringRes(R.string.enum__swipe_action__switch_to_clipboard_context),
            )
            entry(
                key = SHOW_INPUT_METHOD_PICKER,
                label = stringRes(R.string.enum__swipe_action__show_input_method_picker),
            )
            entry(
                key = SWITCH_TO_PREV_SUBTYPE,
                label = stringRes(R.string.enum__swipe_action__switch_to_prev_subtype),
            )
            entry(
                key = SWITCH_TO_NEXT_SUBTYPE,
                label = stringRes(R.string.enum__swipe_action__switch_to_next_subtype),
            )
            entry(
                key = SWITCH_TO_PREV_KEYBOARD,
                label = stringRes(R.string.enum__swipe_action__switch_to_prev_keyboard),
            )
            entry(
                key = TOGGLE_SMARTBAR_VISIBILITY,
                label = stringRes(R.string.enum__swipe_action__toggle_smartbar_visibility),
            )
        }

        @Composable
        fun deleteSwipeListEntries() = listPrefEntries {
            entry(
                key = NO_ACTION,
                label = stringRes(R.string.enum__swipe_action__no_action),
            )
            entry(
                key = DELETE_CHARACTERS_PRECISELY,
                label = stringRes(R.string.enum__swipe_action__delete_characters_precisely),
            )
            entry(
                key = DELETE_WORD,
                label = stringRes(R.string.enum__swipe_action__delete_word),
            )
            entry(
                key = DELETE_WORDS_PRECISELY,
                label = stringRes(R.string.enum__swipe_action__delete_words_precisely),
            )
            entry(
                key = SELECT_CHARACTERS_PRECISELY,
                label = stringRes(R.string.enum__swipe_action__select_characters_precisely),
            )
            entry(
                key = SELECT_WORDS_PRECISELY,
                label = stringRes(R.string.enum__swipe_action__select_words_precisely),
            )
        }

        @Composable
        fun deleteLongPressListEntries() = listPrefEntries {
            entry(
                key = DELETE_CHARACTER,
                label = stringRes(R.string.enum__swipe_action__delete_character),
            )
            entry(
                key = DELETE_WORD,
                label = stringRes(R.string.enum__swipe_action__delete_word),
            )
        }
    }
}
