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
        val ARROW_DOWN = KeyData(code = KeyCode.ARROW_DOWN, type = KeyType.NAVIGATION)

        /** Predefined key data for [KeyCode.ARROW_LEFT] */
        val ARROW_LEFT = KeyData(code = KeyCode.ARROW_LEFT, type = KeyType.NAVIGATION)

        /** Predefined key data for [KeyCode.ARROW_RIGHT] */
        val ARROW_RIGHT = KeyData(code = KeyCode.ARROW_RIGHT, type = KeyType.NAVIGATION)

        /** Predefined key data for [KeyCode.ARROW_UP] */
        val ARROW_UP = KeyData(code = KeyCode.ARROW_UP, type = KeyType.NAVIGATION)

        /** Predefined key data for [KeyCode.DELETE] */
        val DELETE = KeyData(code = KeyCode.DELETE, type = KeyType.ENTER_EDITING)

        /** Predefined key data for [KeyCode.DELETE_WORD] */
        val DELETE_WORD = KeyData(code = KeyCode.DELETE_WORD, type = KeyType.ENTER_EDITING)

        /** Predefined key data for [KeyCode.MOVE_START_OF_LINE] */
        val MOVE_START_OF_LINE = KeyData(code = KeyCode.MOVE_START_OF_LINE, type = KeyType.NAVIGATION)

        /** Predefined key data for [KeyCode.MOVE_END_OF_LINE] */
        val MOVE_END_OF_LINE = KeyData(code = KeyCode.MOVE_END_OF_LINE, type = KeyType.NAVIGATION)

        /** Predefined key data for [KeyCode.MOVE_START_OF_PAGE] */
        val MOVE_START_OF_PAGE = KeyData(code = KeyCode.MOVE_START_OF_PAGE, type = KeyType.NAVIGATION)

        /** Predefined key data for [KeyCode.MOVE_END_OF_PAGE] */
        val MOVE_END_OF_PAGE = KeyData(code = KeyCode.MOVE_END_OF_PAGE, type = KeyType.NAVIGATION)

        /** Predefined key data for [KeyCode.SHOW_INPUT_METHOD_PICKER] */
        val SHOW_INPUT_METHOD_PICKER = KeyData(code = KeyCode.SHOW_INPUT_METHOD_PICKER, type = KeyType.FUNCTION)

        /** Predefined key data for [KeyCode.SWITCH_TO_TEXT_CONTEXT] */
        val SWITCH_TO_TEXT_CONTEXT = KeyData(code = KeyCode.SWITCH_TO_TEXT_CONTEXT, type = KeyType.SYSTEM_GUI)

        /** Predefined key data for [KeyCode.SHIFT] */
        val SHIFT = KeyData(code = KeyCode.SHIFT, type = KeyType.MODIFIER)

        /** Predefined key data for [KeyCode.SPACE] */
        val SPACE = KeyData(code = KeyCode.SPACE, type = KeyType.CHARACTER)
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
 */
class FlorisKeyData(
    type: KeyType = KeyType.CHARACTER,
    code: Int = 0,
    label: String = "",
    var groupId: Int = GROUP_DEFAULT,
    var variation: KeyVariation = KeyVariation.ALL,
    var popup: PopupSet<KeyData> = PopupSet()
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
