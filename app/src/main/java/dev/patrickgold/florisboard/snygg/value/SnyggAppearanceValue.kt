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

package dev.patrickgold.florisboard.snygg.value

import androidx.compose.ui.graphics.Color

sealed interface SnyggAppearanceValue : SnyggValue

object RgbaColor {
    const val RedMin = 0
    const val RedMax = 255
    val Red = RedMin..RedMax

    const val GreenMin = 0
    const val GreenMax = 255
    val Green = GreenMin..GreenMax

    const val BlueMin = 0
    const val BlueMax = 255
    val Blue = BlueMin..BlueMax

    const val AlphaMin = 0.0f
    const val AlphaMax = 1.0f
    val Alpha = AlphaMin..AlphaMax
}

data class SnyggSolidColorValue(val color: Color) : SnyggAppearanceValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            rgbaColor()
        }

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggSolidColorValue)
            val map = SnyggIdToValueMap.new(
                "r" to (v.color.red * RgbaColor.RedMax),
                "g" to (v.color.green * RgbaColor.GreenMax),
                "b" to (v.color.blue * RgbaColor.BlueMax),
                "a" to v.color.alpha,
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val r = map.getOrThrow<Int>("r").coerceIn(RgbaColor.Red).toFloat() / RgbaColor.RedMax
            val g = map.getOrThrow<Int>("g").coerceIn(RgbaColor.Green).toFloat() / RgbaColor.GreenMax
            val b = map.getOrThrow<Int>("b").coerceIn(RgbaColor.Blue).toFloat() / RgbaColor.BlueMax
            val a = map.getOrThrow<Float>("a").coerceIn(RgbaColor.Alpha)
            return@runCatching SnyggSolidColorValue(Color(r, g, b, a))
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
