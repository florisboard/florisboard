/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import androidx.compose.ui.graphics.Color
import org.florisboard.lib.color.ColorMappings
import org.florisboard.lib.color.ColorPalette
import kotlin.math.roundToInt

sealed interface SnyggAppearanceValue : SnyggValue

object RgbaColor {
    const val HexId = "hex"
    val Hex6Matcher = """^#[a-fA-F0-9]{6}$""".toRegex()
    val Hex8Matcher = """^#[a-fA-F0-9]{8}$""".toRegex()

    const val TransparentId = "transparent"
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

data class SnyggStaticColorValue(val color: Color) : SnyggAppearanceValue {
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

        override fun defaultValue() = SnyggStaticColorValue(Color.Black)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggStaticColorValue)
            val map = snyggIdToValueMapOf(
                RgbaColor.RedId to (v.color.red * RgbaColor.RedMax).roundToInt(),
                RgbaColor.GreenId to (v.color.green * RgbaColor.GreenMax).roundToInt(),
                RgbaColor.BlueId to (v.color.blue * RgbaColor.BlueMax).roundToInt(),
                RgbaColor.AlphaId to v.color.alpha,
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = snyggIdToValueMapOf()
            runCatching { spec.parse(v, map) }.onSuccess {
                val r = map.getOrThrow<Int>(RgbaColor.RedId).coerceIn(RgbaColor.Red).toFloat() / RgbaColor.RedMax
                val g = map.getOrThrow<Int>(RgbaColor.GreenId).coerceIn(RgbaColor.Green).toFloat() / RgbaColor.GreenMax
                val b = map.getOrThrow<Int>(RgbaColor.BlueId).coerceIn(RgbaColor.Blue).toFloat() / RgbaColor.BlueMax
                val a = map.getOrThrow<Float>(RgbaColor.AlphaId).coerceIn(RgbaColor.Alpha)
                return@runCatching SnyggStaticColorValue(Color(r, g, b, a))
            }
            runCatching { alternativeSpecs[0].parse(v, map) }.onSuccess {
                val r = map.getOrThrow<Int>(RgbaColor.RedId).coerceIn(RgbaColor.Red).toFloat() / RgbaColor.RedMax
                val g = map.getOrThrow<Int>(RgbaColor.GreenId).coerceIn(RgbaColor.Green).toFloat() / RgbaColor.GreenMax
                val b = map.getOrThrow<Int>(RgbaColor.BlueId).coerceIn(RgbaColor.Blue).toFloat() / RgbaColor.BlueMax
                return@runCatching SnyggStaticColorValue(Color(r, g, b, 1.0f))
            }
            runCatching { alternativeSpecs[1].parse(v, map) }.onSuccess {
                return@runCatching SnyggStaticColorValue(Color(0.0f, 0.0f, 0.0f, 0.0f))
            }
            runCatching { alternativeSpecs[2].parse(v, map) }.onSuccess {
                val hexStr = map.getOrThrow<String>(RgbaColor.HexId)
                val r = hexStr.substring(1..2).toInt(16).toFloat() / RgbaColor.RedMax
                val g = hexStr.substring(3..4).toInt(16).toFloat() / RgbaColor.GreenMax
                val b = hexStr.substring(5..6).toInt(16).toFloat() / RgbaColor.BlueMax
                return@runCatching SnyggStaticColorValue(Color(r, g, b, 1.0f))
            }
            runCatching { alternativeSpecs[3].parse(v, map) }.onSuccess {
                val hexStr = map.getOrThrow<String>(RgbaColor.HexId)
                val r = hexStr.substring(1..2).toInt(16).toFloat() / RgbaColor.RedMax
                val g = hexStr.substring(3..4).toInt(16).toFloat() / RgbaColor.GreenMax
                val b = hexStr.substring(5..6).toInt(16).toFloat() / RgbaColor.BlueMax
                val a = hexStr.substring(7..8).toInt(16).toFloat() / 0xFF
                return@runCatching SnyggStaticColorValue(Color(r, g, b, a))
            }
            error("No matching spec found for \"$v\"")
        }
    }

    override fun encoder() = Companion
}

/// Dynamic Color Value

private const val ColorNameId = "name"
private val ColorNames = ColorPalette.colorNames.joinToString("|")
private val ColorName = """^$ColorNames\$""".toRegex()

data class SnyggDynamicLightColorValue(val colorName: String) : SnyggAppearanceValue {
    companion object : SnyggValueEncoder {
        private const val FunctionName = "dynamic-light-color"

        override val spec = SnyggValueSpec {
            function(name = FunctionName) {
                commaList {
                    +string(id = ColorNameId, regex = ColorName)
                }
            }
        }

        override fun defaultValue() = SnyggDynamicLightColorValue(ColorPalette.Primary.id)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggDynamicLightColorValue)
            val map = snyggIdToValueMapOf(ColorNameId to v.colorName)
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = snyggIdToValueMapOf()
            spec.parse(v, map)
            val colorName = map.getOrThrow<String>(ColorNameId)
            return@runCatching SnyggDynamicLightColorValue(colorName)
        }
    }

    override fun encoder() = Companion
}

data class SnyggDynamicDarkColorValue(val colorName: String) : SnyggAppearanceValue {
    companion object : SnyggValueEncoder {
        private const val FunctionName = "dynamic-dark-color"

        override val spec = SnyggValueSpec {
            function(name = FunctionName) {
                commaList {
                    +string(id = ColorNameId, regex = ColorName)
                }
            }
        }

        override fun defaultValue() = SnyggDynamicDarkColorValue(ColorPalette.Primary.id)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggDynamicDarkColorValue)
            val map = snyggIdToValueMapOf(ColorNameId to v.colorName)
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = snyggIdToValueMapOf()
            spec.parse(v, map)
            val colorName = map.getOrThrow<String>(ColorNameId)
            return@runCatching SnyggDynamicDarkColorValue(colorName)
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
