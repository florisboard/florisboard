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

package dev.patrickgold.florisboard.ime.text.keyboard

import android.graphics.Rect
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.popup.PopupSet
import dev.patrickgold.florisboard.ime.text.key.*
import java.util.*

abstract class Key(open val data: KeyData) {
    open var isEnabled: Boolean = true
    open var isPressed: Boolean = false
    open val touchBounds: Rect = Rect()
    open val visibleBounds: Rect = Rect()
    open val visibleDrawableBounds: Rect = Rect()
    open val visibleLabelBounds: Rect = Rect()

    open var flayShrink: Double = 0.0
    open var flayGrow: Double = 0.0
    open var flayWidthFactor: Double = 0.0

    abstract fun compute(
        caps: Boolean = false,
        variation: KeyVariation,
        subtype: Subtype = Subtype.DEFAULT
    )
}

class TextKey(override val data: KeyData) : Key(data) {
    var computedData: TextKeyData = TextKeyData.UNSPECIFIED
        private set
    var computedPopups: PopupSet<TextKeyData> = PopupSet()
        private set

    override fun compute(caps: Boolean, variation: KeyVariation, subtype: Subtype) {
        val computed = data.compute(caps, variation)
        computedData = when (computed) {
            is TextKeyData -> {
                computed
            }
            is PopupAwareTextKeyData -> {
                TextKeyData.obtain(computed)
            }
            else -> {
                TextKeyData.UNSPECIFIED
            }
        }

        flayShrink = when (null) {
            KeyboardMode.NUMERIC,
            KeyboardMode.NUMERIC_ADVANCED,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> 1.0
            else -> when (computedData.code) {
                KeyCode.SHIFT,
                KeyCode.DELETE -> 0.5
                KeyCode.VIEW_CHARACTERS,
                KeyCode.VIEW_SYMBOLS,
                KeyCode.VIEW_SYMBOLS2,
                KeyCode.ENTER -> 0.0
                else -> 1.0
            }
        }
        flayGrow = when (null) {
            KeyboardMode.NUMERIC,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> 0.0
            KeyboardMode.NUMERIC_ADVANCED -> when (computedData.type) {
                KeyType.NUMERIC -> 1.0
                else -> 0.0
            }
            else -> when (computedData.code) {
                KeyCode.SPACE -> 1.0
                else -> 0.0
            }
        }
        flayWidthFactor = when (null) {
            KeyboardMode.NUMERIC,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> 2.68
            KeyboardMode.NUMERIC_ADVANCED -> when (computedData.code) {
                44, 46 -> 1.00
                KeyCode.VIEW_SYMBOLS, 61 -> 1.34
                else -> 1.56
            }
            else -> when (computedData.code) {
                KeyCode.SHIFT,
                KeyCode.DELETE -> 1.56
                KeyCode.VIEW_CHARACTERS,
                KeyCode.VIEW_SYMBOLS,
                KeyCode.VIEW_SYMBOLS2,
                KeyCode.ENTER -> 1.56
                else -> 1.00
            }
        }
    }
}

class EmojiKey(override val data: KeyData) : Key(data) {
    var computedData: EmojiKeyData = EmojiKeyData(listOf())
        private set
    var computedPopups: PopupSet<EmojiKeyData> = PopupSet()
        private set

    override fun compute(caps: Boolean, variation: KeyVariation, subtype: Subtype) {
        //computedData = data
        //computedPopups = computedData.popup
    }
}
