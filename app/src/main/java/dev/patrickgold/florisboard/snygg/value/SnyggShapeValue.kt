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

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val CornerSizeTopStart = "cornerSizeTopStart"
private const val CornerSizeTopEnd = "cornerSizeTopEnd"
private const val CornerSizeBottomEnd = "cornerSizeBottomEnd"
private const val CornerSizeBottomStart = "cornerSizeBottomStart"

private const val Rectangle = "rectangleShape"
private const val CutCorner = "cutCornerShape"
private const val RoundedCorner = "roundedCornerShape"

private const val DpUnit = "dp"

sealed interface SnyggShapeValue : SnyggValue {
    val shape: Shape
}

data class SnyggRectangleShapeValue(override val shape: Shape) : SnyggShapeValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(Rectangle) { nothing() }
        }

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggRectangleShapeValue)
            val map = SnyggIdToValueMap.new()
            return spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            return@runCatching SnyggRectangleShapeValue(shape = RectangleShape)
        }
    }

    override fun encoder() = Companion
}

data class SnyggCutCornerShapeDpValue(
    override val shape: CutCornerShape,
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
) : SnyggShapeValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(CutCorner) {
                commaList {
                    +float(id = CornerSizeTopStart, unit = DpUnit)
                    +float(id = CornerSizeTopEnd, unit = DpUnit)
                    +float(id = CornerSizeBottomEnd, unit = DpUnit)
                    +float(id = CornerSizeBottomStart, unit = DpUnit)
                }
            }
        }

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggCutCornerShapeDpValue)
            val map = SnyggIdToValueMap.new(
                CornerSizeTopStart to v.topStart.value,
                CornerSizeTopEnd to v.topEnd.value,
                CornerSizeBottomEnd to v.bottomEnd.value,
                CornerSizeBottomStart to v.bottomStart.value,
            )
            return spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val topStart = map.getOrThrow<Int>(CornerSizeTopStart).dp
            val topEnd = map.getOrThrow<Int>(CornerSizeTopEnd).dp
            val bottomEnd = map.getOrThrow<Int>(CornerSizeBottomEnd).dp
            val bottomStart = map.getOrThrow<Int>(CornerSizeBottomStart).dp
            return@runCatching SnyggCutCornerShapeDpValue(
                shape = CutCornerShape(topStart, topEnd, bottomEnd, bottomStart),
                topStart, topEnd, bottomEnd, bottomStart,
            )
        }
    }

    override fun encoder() = Companion
}

data class SnyggCutCornerShapePercentValue(
    override val shape: CutCornerShape,
    val topStart: Int,
    val topEnd: Int,
    val bottomEnd: Int,
    val bottomStart: Int,
) : SnyggShapeValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(CutCorner) {
                commaList {
                    +percentageInt(id = CornerSizeTopStart)
                    +percentageInt(id = CornerSizeTopEnd)
                    +percentageInt(id = CornerSizeBottomEnd)
                    +percentageInt(id = CornerSizeBottomStart)
                }
            }
        }

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggCutCornerShapePercentValue)
            val map = SnyggIdToValueMap.new(
                CornerSizeTopStart to v.topStart,
                CornerSizeTopEnd to v.topEnd,
                CornerSizeBottomEnd to v.bottomEnd,
                CornerSizeBottomStart to v.bottomStart,
            )
            return spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val topStart = map.getOrThrow<Int>(CornerSizeTopStart)
            val topEnd = map.getOrThrow<Int>(CornerSizeTopEnd)
            val bottomEnd = map.getOrThrow<Int>(CornerSizeBottomEnd)
            val bottomStart = map.getOrThrow<Int>(CornerSizeBottomStart)
            return@runCatching SnyggCutCornerShapePercentValue(
                shape = CutCornerShape(topStart, topEnd, bottomEnd, bottomStart),
                topStart, topEnd, bottomEnd, bottomStart,
            )
        }
    }

    override fun encoder() = Companion
}

data class SnyggRoundedCornerShapeDpValue(
    override val shape: RoundedCornerShape,
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
) : SnyggShapeValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(RoundedCorner) {
                commaList {
                    +float(id = CornerSizeTopStart, unit = DpUnit)
                    +float(id = CornerSizeTopEnd, unit = DpUnit)
                    +float(id = CornerSizeBottomEnd, unit = DpUnit)
                    +float(id = CornerSizeBottomStart, unit = DpUnit)
                }
            }
        }

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggRoundedCornerShapeDpValue)
            val map = SnyggIdToValueMap.new(
                CornerSizeTopStart to v.topStart.value,
                CornerSizeTopEnd to v.topEnd.value,
                CornerSizeBottomEnd to v.bottomEnd.value,
                CornerSizeBottomStart to v.bottomStart.value,
            )
            return spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val topStart = map.getOrThrow<Float>(CornerSizeTopStart).dp
            val topEnd = map.getOrThrow<Float>(CornerSizeTopEnd).dp
            val bottomEnd = map.getOrThrow<Float>(CornerSizeBottomEnd).dp
            val bottomStart = map.getOrThrow<Float>(CornerSizeBottomStart).dp
            return@runCatching SnyggRoundedCornerShapeDpValue(
                shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart),
                topStart, topEnd, bottomEnd, bottomStart,
            )
        }
    }

    override fun encoder() = Companion
}

data class SnyggRoundedCornerShapePercentValue(
    override val shape: RoundedCornerShape,
    val topStart: Int,
    val topEnd: Int,
    val bottomEnd: Int,
    val bottomStart: Int,
) : SnyggShapeValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(RoundedCorner) {
                commaList {
                    +percentageInt(id = CornerSizeTopStart)
                    +percentageInt(id = CornerSizeTopEnd)
                    +percentageInt(id = CornerSizeBottomEnd)
                    +percentageInt(id = CornerSizeBottomStart)
                }
            }
        }

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggRoundedCornerShapePercentValue)
            val map = SnyggIdToValueMap.new(
                CornerSizeTopStart to v.topStart,
                CornerSizeTopEnd to v.topEnd,
                CornerSizeBottomEnd to v.bottomEnd,
                CornerSizeBottomStart to v.bottomStart,
            )
            return spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val topStart = map.getOrThrow<Int>(CornerSizeTopStart)
            val topEnd = map.getOrThrow<Int>(CornerSizeTopEnd)
            val bottomEnd = map.getOrThrow<Int>(CornerSizeBottomEnd)
            val bottomStart = map.getOrThrow<Int>(CornerSizeBottomStart)
            return@runCatching SnyggRoundedCornerShapePercentValue(
                shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart),
                topStart, topEnd, bottomEnd, bottomStart,
            )
        }
    }

    override fun encoder() = Companion
}
