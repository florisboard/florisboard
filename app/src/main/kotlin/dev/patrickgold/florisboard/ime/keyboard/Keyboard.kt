/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.keyboard

import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.keyboard.TextKey
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboard

val PlaceholderLoadingKeyboard = TextKeyboard(
    arrangement = arrayOf(
        arrayOf(
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
        ),
        arrayOf(
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
        ),
        arrayOf(
            TextKey(data = TextKeyData(code = KeyCode.SHIFT, type = KeyType.MODIFIER, label = "shift")),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = KeyCode.DELETE, type = KeyType.ENTER_EDITING, label = "delete")),
        ),
        arrayOf(
            TextKey(data = TextKeyData(code = KeyCode.VIEW_SYMBOLS, type = KeyType.SYSTEM_GUI, label = "view_symbols")),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = KeyCode.SPACE, label = "space")),
            TextKey(data = TextKeyData(code = 0)),
            TextKey(data = TextKeyData(code = KeyCode.ENTER, type = KeyType.ENTER_EDITING, label = "enter")),
        ),
    ),
    mode = KeyboardMode.CHARACTERS,
    extendedPopupMapping = null,
    extendedPopupMappingDefault = null,
)

/**
 * Abstract class describing a computed keyboard. The exact implementation is dependent on the subclass and the
 * structure can vary quite much between different subclasses.
 */
abstract class Keyboard {
    /**
     * The mode of this keyboard, used to let computers and the layout process behave differently based on different
     * modes.
     */
    abstract val mode: KeyboardMode

    /**
     * Returns the key for given [pointerX] and [pointerY] coords or null if no key touch hit box is defined at the
     * given coords.
     *
     * @param pointerX The x-coordinate of the input event, absolute within the parent keyboard view.
     * @param pointerY The y-coordinate of the input event, absolute within the parent keyboard view.
     *
     * @return The key for given coords or null if no key touch hit box is defined for this position.
     */
    abstract fun getKeyForPos(pointerX: Float, pointerY: Float): Key?

    /**
     * Returns an iterator which allows to loop through all keys within the layout, independent of the actual
     * structure and layout.
     */
    abstract fun keys(): Iterator<Key>

    /**
     * Layouts the keys according the the dimensions and parameters provided by given arguments. This method's
     * exact behavior is highly dependent aon the actual subclass.
     */
    abstract fun layout(
        keyboardWidth: Float,
        keyboardHeight: Float,
        desiredKey: Key,
        extendTouchBoundariesDownwards: Boolean,
    )
}
