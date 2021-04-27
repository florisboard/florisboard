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

package dev.patrickgold.florisboard.ime.text.layout

import dev.patrickgold.florisboard.ime.extension.Asset
import dev.patrickgold.florisboard.ime.text.key.*
import kotlinx.serialization.Serializable

@Serializable
data class Layout(
    val type: LayoutType,
    override val name: String,
    override val label: String,
    override val authors: List<String>,
    val direction: String,
    val modifier: String? = null,
    val arrangement: List<List<KeyData>> = listOf()
) : Asset {
    companion object {
        val PRE_GENERATED_LOADING_KEYBOARD = Layout(
            type = LayoutType.CHARACTERS,
            name = "__loading_keyboard__",
            label = "__loading_keyboard__",
            authors = listOf(),
            direction = "ltr",
            arrangement = listOf(
                listOf(
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0)
                ),
                listOf(
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0)
                ),
                listOf(
                    AutoTextKeyData(code = KeyCode.SHIFT, type = KeyType.MODIFIER, label = "shift"),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = KeyCode.DELETE, type = KeyType.ENTER_EDITING, label = "delete")
                ),
                listOf(
                    AutoTextKeyData(code = KeyCode.VIEW_SYMBOLS, type = KeyType.SYSTEM_GUI, label = "view_symbols"),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = KeyCode.SPACE, label = "space"),
                    AutoTextKeyData(code = 0),
                    AutoTextKeyData(code = KeyCode.ENTER, type = KeyType.ENTER_EDITING, label = "enter")
                )
            )
        )
    }
}

@Serializable
data class LayoutMetaOnly(
    val type: LayoutType,
    override val name: String,
    override val label: String,
    override val authors: List<String>,
    val direction: String,
    val modifier: String? = null
) : Asset
