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

package dev.patrickgold.florisboard.ime.text.layout

import dev.patrickgold.florisboard.ime.text.key.FlorisKeyData
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode

typealias LayoutDataArrangement = List<List<FlorisKeyData>>
data class LayoutData(
    val type: LayoutType,
    val name: String,
    val direction: String,
    val modifier: String?,
    val arrangement: LayoutDataArrangement = listOf()
) {
    private fun getComputedLayoutDataArrangement(): ComputedLayoutDataArrangement {
        val ret = mutableListOf<MutableList<FlorisKeyData>>()
        for (row in arrangement) {
            val retRow = mutableListOf<FlorisKeyData>()
            for (keyData in row) {
                retRow.add(keyData)
            }
            ret.add(retRow)
        }
        return ret
    }

    fun toComputedLayoutData(keyboardMode: KeyboardMode): ComputedLayoutData {
        return ComputedLayoutData(
            keyboardMode, name, direction, getComputedLayoutDataArrangement()
        )
    }
}

typealias ComputedLayoutDataArrangement = MutableList<MutableList<FlorisKeyData>>
data class ComputedLayoutData(
    val mode: KeyboardMode,
    val name: String,
    val direction: String,
    val arrangement: ComputedLayoutDataArrangement = mutableListOf()
) {
    companion object {
        val PRE_GENERATED_LOADING_KEYBOARD = ComputedLayoutData(
            mode = KeyboardMode.CHARACTERS,
            name = "__loading_keyboard__",
            direction = "ltr",
            arrangement = mutableListOf(
                mutableListOf(
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0)
                ),
                mutableListOf(
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0)
                ),
                mutableListOf(
                    FlorisKeyData(code = KeyCode.SHIFT, type = KeyType.MODIFIER, label = "shift"),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = KeyCode.DELETE, type = KeyType.ENTER_EDITING, label = "delete")
                ),
                mutableListOf(
                    FlorisKeyData(code = KeyCode.VIEW_SYMBOLS, type = KeyType.SYSTEM_GUI, label = "view_symbols"),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = KeyCode.SPACE, label = "space"),
                    FlorisKeyData(code = 0),
                    FlorisKeyData(code = KeyCode.ENTER, type = KeyType.ENTER_EDITING, label = "enter")
                )
            )
        )
    }
}
