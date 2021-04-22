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
import dev.patrickgold.florisboard.ime.text.key.TextKeyData
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import kotlinx.serialization.Serializable

@Serializable
data class Layout(
    val type: LayoutType,
    override val name: String,
    override val label: String,
    override val authors: List<String>,
    val direction: String,
    val modifier: String? = null,
    val arrangement: Array<Array<KeyData>> = arrayOf()
) : Asset {
    companion object {
        val PRE_GENERATED_LOADING_KEYBOARD = Layout(
            type = LayoutType.CHARACTERS,
            name = "__loading_keyboard__",
            label = "__loading_keyboard__",
            authors = listOf(),
            direction = "ltr",
            arrangement = arrayOf(
                arrayOf(
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0)
                ),
                arrayOf(
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0)
                ),
                arrayOf(
                    TextKeyData(code = KeyCode.SHIFT, type = KeyType.MODIFIER, label = "shift"),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = KeyCode.DELETE, type = KeyType.ENTER_EDITING, label = "delete")
                ),
                arrayOf(
                    TextKeyData(code = KeyCode.VIEW_SYMBOLS, type = KeyType.SYSTEM_GUI, label = "view_symbols"),
                    TextKeyData(code = 0),
                    TextKeyData(code = 0),
                    TextKeyData(code = KeyCode.SPACE, label = "space"),
                    TextKeyData(code = 0),
                    TextKeyData(code = KeyCode.ENTER, type = KeyType.ENTER_EDITING, label = "enter")
                )
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Layout

        if (type != other.type) return false
        if (name != other.name) return false
        if (label != other.label) return false
        if (authors != other.authors) return false
        if (direction != other.direction) return false
        if (modifier != other.modifier) return false
        if (!arrangement.contentDeepEquals(other.arrangement)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + authors.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + (modifier?.hashCode() ?: 0)
        result = 31 * result + arrangement.contentDeepHashCode()
        return result
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
