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

import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode

typealias LayoutDataArrangement = List<List<KeyData>>
data class LayoutData(
    val type: LayoutType,
    val name: String,
    val direction: String,
    val modifier: String?,
    val arrangement: LayoutDataArrangement = listOf()
) {
    private fun getComputedLayoutDataArrangement(): ComputedLayoutDataArrangement {
        val ret = mutableListOf<MutableList<KeyData>>()
        for (row in arrangement) {
            val retRow = mutableListOf<KeyData>()
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

typealias ComputedLayoutDataArrangement = MutableList<MutableList<KeyData>>
data class ComputedLayoutData(
    val mode: KeyboardMode,
    val name: String,
    val direction: String,
    val arrangement: ComputedLayoutDataArrangement = mutableListOf()
)
