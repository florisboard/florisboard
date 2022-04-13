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

package dev.patrickgold.florisboard.lib.snygg.value

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

sealed interface SnyggAppearanceValue : SnyggValue

object RgbaColor {
    const val HexId = "hex"
    val Hex6Matcher = """^#[a-fA-F0-9]{6}$""".toRegex()
    val Hex8Matcher = """^#[a-fA-F0-9]{8}$""".toRegex()

    const val TransparentId = "hex"
    val TransparentMatcher = """^transparent$""".toRegex()

    const val RedId = "r"
    const val RedMin = 0
    const val RedMax = 255
    val Red = RedMin..RedMax

    const val GreenId = "g"
    const val GreenMin = 0
    const val GreenMax = 255
    val Green = GreenMin..GreenMax

    const val BlueId = "b"
    const val BlueMin = 0
    const val BlueMax = 255
    val Blue = BlueMin..BlueMax

    const val AlphaId = "a"
    const val AlphaMin = 0.0f
    const val AlphaMax = 1.0f
    val Alpha = AlphaMin..AlphaMax
}

data class SnyggSolidColorValue(val color: Color) : SnyggAppearanceValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(name = "rgba") {
                commaList {
                    +int(id = RgbaColor.RedId, min = RgbaColor.RedMin, max = RgbaColor.RedMax)
                    +int(id = RgbaColor.GreenId, min = RgbaColor.GreenMin, max = RgbaColor.GreenMax)
                    +int(id = RgbaColor.BlueId, min = RgbaColor.BlueMin, max = RgbaColor.BlueMax)
                    +float(id = RgbaColor.AlphaId, min = RgbaColor.AlphaMin, max = RgbaColor.AlphaMax)
                }
            }
        }

        override val alternativeSpecs = listOf(
            SnyggValueSpec {
                function(name = "rgb") {
                    commaList {
                        +int(id = RgbaColor.RedId, min = RgbaColor.RedMin, max = RgbaColor.RedMax)
                        +int(id = RgbaColor.GreenId, min = RgbaColor.GreenMin, max = RgbaColor.GreenMax)
                        +int(id = RgbaColor.BlueId, min = RgbaColor.BlueMin, max = RgbaColor.BlueMax)
                    }
                }
            },
            SnyggValueSpec {
                string(id = RgbaColor.TransparentId, regex = RgbaColor.TransparentMatcher)
            },
            SnyggValueSpec {
                string(id = RgbaColor.HexId, regex = RgbaColor.Hex6Matcher)
            },
            SnyggValueSpec {
                string(id = RgbaColor.HexId, regex = RgbaColor.Hex8Matcher)
            },
        )

        override fun defaultValue() = SnyggSolidColorValue(Color.Black)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggSolidColorValue)
            val map = SnyggIdToValueMap.new(
                RgbaColor.RedId to (v.color.red * RgbaColor.RedMax).roundToInt(),
                RgbaColor.GreenId to (v.color.green * RgbaColor.GreenMax).roundToInt(),
                RgbaColor.BlueId to (v.color.blue * RgbaColor.BlueMax).roundToInt(),
                RgbaColor.AlphaId to v.color.alpha,
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            try {
                spec.parse(v, map)
                val r = map.getOrThrow<Int>(RgbaColor.RedId).coerceIn(RgbaColor.Red).toFloat() / RgbaColor.RedMax
                val g = map.getOrThrow<Int>(RgbaColor.GreenId).coerceIn(RgbaColor.Green).toFloat() / RgbaColor.GreenMax
                val b = map.getOrThrow<Int>(RgbaColor.BlueId).coerceIn(RgbaColor.Blue).toFloat() / RgbaColor.BlueMax
                val a = map.getOrThrow<Float>(RgbaColor.AlphaId).coerceIn(RgbaColor.Alpha)
                return@runCatching SnyggSolidColorValue(Color(r, g, b, a))
            } catch (e: Exception) {
                for ((n, alternativeSpec) in alternativeSpecs.withIndex()) {
                    try {
                        alternativeSpec.parse(v, map)
                        when (n) {
                            0 -> {
                                val r = map.getOrThrow<Int>(RgbaColor.RedId).coerceIn(RgbaColor.Red).toFloat() / RgbaColor.RedMax
                                val g = map.getOrThrow<Int>(RgbaColor.GreenId).coerceIn(RgbaColor.Green).toFloat() / RgbaColor.GreenMax
                                val b = map.getOrThrow<Int>(RgbaColor.BlueId).coerceIn(RgbaColor.Blue).toFloat() / RgbaColor.BlueMax
                                return@runCatching SnyggSolidColorValue(Color(r, g, b, 1.0f))
                            }
                            1 -> {
                                return@runCatching SnyggSolidColorValue(Color(0.0f, 0.0f, 0.0f, 0.0f))
                            }
                            2 -> {
                                val hexStr = map.getOrThrow<String>(RgbaColor.HexId)
                                val r = hexStr.substring(1..2).toInt(16).toFloat() / RgbaColor.RedMax
                                val g = hexStr.substring(3..4).toInt(16).toFloat() / RgbaColor.GreenMax
                                val b = hexStr.substring(5..6).toInt(16).toFloat() / RgbaColor.BlueMax
                                return@runCatching SnyggSolidColorValue(Color(r, g, b, 1.0f))
                            }
                            3 -> {
                                val hexStr = map.getOrThrow<String>(RgbaColor.HexId)
                                val r = hexStr.substring(1..2).toInt(16).toFloat() / RgbaColor.RedMax
                                val g = hexStr.substring(3..4).toInt(16).toFloat() / RgbaColor.GreenMax
                                val b = hexStr.substring(5..6).toInt(16).toFloat() / RgbaColor.BlueMax
                                val a = hexStr.substring(7..8).toInt(16).toFloat() / 0xFF
                                return@runCatching SnyggSolidColorValue(Color(r, g, b, a))
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
            error("No matching spec found for \"$v\"")
        }
    }

    override fun encoder() = Companion
}

//data class LinearGradient(
//    val dummy: Int,
//) : SnyggAppearanceValue()
//
//data class RadialGradient(
//    val dummy: Int,
//) : SnyggAppearanceValue()
//
