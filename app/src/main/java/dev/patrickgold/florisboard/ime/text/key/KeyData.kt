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

package dev.patrickgold.florisboard.ime.text.key

import dev.patrickgold.florisboard.ime.popup.PopupSet
import dev.patrickgold.florisboard.ime.text.key.FlorisKeyData.Companion.GROUP_DEFAULT

/**
 * Data class which describes a single key and its attributes.
 *
 * @property type The type of the key. Some actions require both [code] and [type] to match in order
 *  to be successfully executed. Defaults to [KeyType.CHARACTER].
 * @property code The UTF-8 encoded code of the character. The code defined here is used as the
 *  data passed to the system. Defaults to 0.
 * @property label The string used to display the key in the UI. Is not used for the actual data
 *  passed to the system. Should normally be the exact same as the [code]. Defaults to an empty
 *  string.
 */
open class KeyData(
    var type: KeyType = KeyType.CHARACTER,
    var code: Int = 0,
    var label: String = ""
) {
    companion object {
        /** Predefined key data for [KeyCode.ARROW_DOWN] */
        val ARROW_DOWN = KeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.ARROW_DOWN,
            label = "arrow_down"
        )

        /** Predefined key data for [KeyCode.ARROW_LEFT] */
        val ARROW_LEFT = KeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.ARROW_LEFT,
            label = "arrow_left"
        )

        /** Predefined key data for [KeyCode.ARROW_RIGHT] */
        val ARROW_RIGHT = KeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.ARROW_RIGHT,
            label = "arrow_right"
        )

        /** Predefined key data for [KeyCode.ARROW_UP] */
        val ARROW_UP = KeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.ARROW_UP,
            label = "arrow_up"
        )

        /** Predefined key data for [KeyCode.CLIPBOARD_COPY] */
        val CLIPBOARD_COPY = KeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_COPY,
            label = "clipboard_copy"
        )

        /** Predefined key data for [KeyCode.CLIPBOARD_CUT] */
        val CLIPBOARD_CUT = KeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_CUT,
            label = "clipboard_cut"
        )

        /** Predefined key data for [KeyCode.CLIPBOARD_PASTE] */
        val CLIPBOARD_PASTE = KeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_PASTE,
            label = "clipboard_paste"
        )

        /** Predefined key data for [KeyCode.CLIPBOARD_SELECT] */
        val CLIPBOARD_SELECT = KeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_SELECT,
            label = "clipboard_select"
        )

        /** Predefined key data for [KeyCode.CLIPBOARD_SELECT_ALL] */
        val CLIPBOARD_SELECT_ALL = KeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_SELECT_ALL,
            label = "clipboard_select_all"
        )

        /** Predefined key data for [KeyCode.DELETE] */
        val DELETE = KeyData(
            type = KeyType.ENTER_EDITING,
            code = KeyCode.DELETE,
            label = "delete"
        )

        /** Predefined key data for [KeyCode.DELETE_WORD] */
        val DELETE_WORD = KeyData(
            type = KeyType.ENTER_EDITING,
            code = KeyCode.DELETE_WORD,
            label = "delete_word"
        )

        /** Predefined key data for [KeyCode.INTERNAL_BATCH_EDIT] */
        val INTERNAL_BATCH_EDIT = KeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.INTERNAL_BATCH_EDIT,
            label = "internal_batch_edit"
        )

        /** Predefined key data for [KeyCode.MOVE_START_OF_LINE] */
        val MOVE_START_OF_LINE = KeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.MOVE_START_OF_LINE,
            label = "move_start_of_line"
        )

        /** Predefined key data for [KeyCode.MOVE_END_OF_LINE] */
        val MOVE_END_OF_LINE = KeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.MOVE_END_OF_LINE,
            label = "move_end_of_line"
        )

        /** Predefined key data for [KeyCode.MOVE_START_OF_PAGE] */
        val MOVE_START_OF_PAGE = KeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.MOVE_START_OF_PAGE,
            label = "move_start_of_page"
        )

        /** Predefined key data for [KeyCode.MOVE_END_OF_PAGE] */
        val MOVE_END_OF_PAGE = KeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.MOVE_END_OF_PAGE,
            label = "move_end_of_page"
        )

        /** Predefined key data for [KeyCode.SHOW_INPUT_METHOD_PICKER] */
        val SHOW_INPUT_METHOD_PICKER = KeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.SHOW_INPUT_METHOD_PICKER,
            label = "show_input_method_picker"
        )

        /** Predefined key data for [KeyCode.SWITCH_TO_TEXT_CONTEXT] */
        val SWITCH_TO_TEXT_CONTEXT = KeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.SWITCH_TO_TEXT_CONTEXT,
            label = "switch_to_text_context"
        )

        /** Predefined key data for [KeyCode.SHIFT] */
        val SHIFT = KeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.SHIFT,
            label = "shift"
        )

        /** Predefined key data for [KeyCode.SHIFT_LOCK] */
        val SHIFT_LOCK = KeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.SHIFT_LOCK,
            label = "shift_lock"
        )

        /** Predefined key data for [KeyCode.SPACE] */
        val SPACE = KeyData(
            type = KeyType.CHARACTER,
            code = KeyCode.SPACE,
            label = "space"
        )

        /** Predefined key data for [KeyCode.UNSPECIFIED] */
        val UNSPECIFIED = KeyData(
            type = KeyType.UNSPECIFIED,
            code = KeyCode.UNSPECIFIED,
            label = "unspecified"
        )
    }

    override fun toString(): String {
        return "KeyData { type=$type code=$code label=\"$label\" }"
    }
}

/**
 * Data class which describes a single key and its attributes, while also providing additional
 * characters via the extended popup menu.
 *
 * @property groupId The Id of the group this key belongs to. An valid number between 0 and INT_MAX
 *  may be used. Custom group Ids can be used, but must not be in the range [0;99], as these group
 *  Ids are reserved for default and internal usage. Defaults to [GROUP_DEFAULT].
 * @property variation Controls if the key should only be shown in some contexts (e.g.: url input)
 *  or if the key should always be visible. Defaults to [KeyVariation.ALL].
 * @property popup List of keys which will be accessible while long pressing the key. Defaults to
 *  an empty set (no extended popup).
 * @property shift An alternative key to use when the keyboard caps state is true. Useful for layouts
 *  such as Colemak and Dvorak. Defaults to null (don't override base uppercase key). This override
 *  property should only be used to provide an uppercase variant of two else not related variants, but
 *  should not be used for providing an uppercase letter (e.g. 'a' -> 'A').
 */
class FlorisKeyData(
    type: KeyType = KeyType.CHARACTER,
    code: Int = 0,
    label: String = "",
    var groupId: Int = GROUP_DEFAULT,
    var variation: KeyVariation = KeyVariation.ALL,
    var popup: PopupSet<KeyData> = PopupSet(),
    var shift: KeyData? = null
) : KeyData(type, code, label) {
    companion object {
        /**
         * Constant for the default group. If not otherwise specified, any key is automatically
         * assigned to this group.
         */
        const val GROUP_DEFAULT: Int = 0

        /**
         * Constant for the Left modifier key group. Any key belonging to this group will get the
         * popups specified for "~left" in the popup mapping.
         */
        const val GROUP_LEFT: Int = 1

        /**
         * Constant for the right modifier key group. Any key belonging to this group will get the
         * popups specified for "~right" in the popup mapping.
         */
        const val GROUP_RIGHT: Int = 2

        /**
         * Constant for the enter modifier key group. Any key belonging to this group will get the
         * popups specified for "~enter" in the popup mapping.
         */
        const val GROUP_ENTER: Int = 3
    }
}
