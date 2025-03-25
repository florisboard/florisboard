/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package org.florisboard.lib.snygg.value

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

private const val FontStyleId = "fontStyle"
private const val FontWeightNamedId = "fontWeightNamed"
private const val FontWeightNumericId = "fontWeightNumeric"

sealed interface SnyggFontValue : SnyggValue

data class SnyggFontStyleValue(val fontStyle: FontStyle) : SnyggFontValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            keywords(FontStyleId, listOf("normal", "italic"))
        }

        override fun defaultValue() = SnyggFontStyleValue(FontStyle.Normal)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggFontStyleValue)
            val map = snyggIdToValueMapOf(
                FontStyleId to when (v.fontStyle) {
                    FontStyle.Normal -> "normal"
                    FontStyle.Italic -> "italic"
                    else -> error("unreachable")
                }
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = snyggIdToValueMapOf()
            spec.parse(v, map)
            val fontStyle = when (map.getOrThrow<String>(FontStyleId)) {
                "normal" -> FontStyle.Normal
                "italic" -> FontStyle.Italic
                else -> error("Invalid font style supplied")
            }
            return@runCatching SnyggFontStyleValue(fontStyle)
        }
    }

    override fun encoder() = Companion
}

data class SnyggFontWeightValue(val fontWeight: FontWeight) : SnyggFontValue {
    companion object : SnyggValueEncoder {
        internal val Weights = mapOf(
            "thin" to FontWeight(100),
            "extra-light" to FontWeight(200),
            "light" to FontWeight(300),
            "normal" to FontWeight(400),
            "medium" to FontWeight(500),
            "semi-bold" to FontWeight(600),
            "bold" to FontWeight(700),
            "extra-bold" to FontWeight(800),
            "black" to FontWeight(900),
        )
        override val spec = SnyggValueSpec {
            keywords(FontWeightNamedId, Weights.keys.toList())
        }

        override val alternativeSpecs = listOf(
            SnyggValueSpec {
                keywords(FontWeightNumericId, Weights.values.map { it.weight.toString() }.toList())
            }
        )

        override fun defaultValue() = SnyggFontWeightValue(FontWeight.Normal)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggFontWeightValue)
            val map = snyggIdToValueMapOf(
                FontWeightNamedId to Weights.firstNotNullOf { (name, fontWeight) ->
                    if (fontWeight == v.fontWeight) name else null
                }
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = snyggIdToValueMapOf()
            runCatching { spec.parse(v, map) }.onSuccess {
                val name = map.getOrThrow<String>(FontWeightNamedId)
                return@runCatching SnyggFontWeightValue(Weights[name]!!)
            }
            runCatching { alternativeSpecs[0].parse(v, map) }.onSuccess {
                val weightStr = map.getOrThrow<String>(FontWeightNumericId)
                val fontWeight = FontWeight(weightStr.toInt())
                return@runCatching SnyggFontWeightValue(fontWeight)
            }
            error("No matching spec found for \"$v\"")
        }
    }

    override fun encoder() = Companion
}
