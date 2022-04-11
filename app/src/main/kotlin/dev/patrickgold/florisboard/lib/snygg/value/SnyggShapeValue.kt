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

import androidx.compose.foundation.shape.CircleShape
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

private const val Rectangle = "rectangle"
private const val Circle = "circle"
private const val CutCorner = "cut-corner"
private const val RoundedCorner = "rounded-corner"

private const val DpUnit = "dp"

sealed interface SnyggShapeValue : SnyggValue {
    val shape: Shape
}

sealed interface SnyggDpShapeValue : SnyggShapeValue {
    override val shape: Shape
    val topStart: Dp
    val topEnd: Dp
    val bottomEnd: Dp
    val bottomStart: Dp
}

sealed interface SnyggPercentShapeValue : SnyggShapeValue {
    override val shape: Shape
    val topStart: Int
    val topEnd: Int
    val bottomEnd: Int
    val bottomStart: Int
}

data class SnyggRectangleShapeValue(override val shape: Shape = RectangleShape) : SnyggShapeValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(Rectangle) { nothing() }
        }

        override fun defaultValue() = SnyggRectangleShapeValue()

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggRectangleShapeValue)
            val map = SnyggIdToValueMap.new()
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            return@runCatching SnyggRectangleShapeValue()
        }
    }

    override fun encoder() = Companion
}

data class SnyggCircleShapeValue(override val shape: Shape = CircleShape) : SnyggShapeValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(Circle) { nothing() }
        }

        override fun defaultValue() = SnyggCircleShapeValue()

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggCircleShapeValue)
            val map = SnyggIdToValueMap.new()
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            return@runCatching SnyggCircleShapeValue()
        }
    }

    override fun encoder() = Companion
}

data class SnyggCutCornerDpShapeValue(
    override val topStart: Dp,
    override val topEnd: Dp,
    override val bottomEnd: Dp,
    override val bottomStart: Dp,
    override val shape: CutCornerShape = CutCornerShape(topStart, topEnd, bottomEnd, bottomStart),
) : SnyggDpShapeValue {
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

        override fun defaultValue() = SnyggCutCornerDpShapeValue(0.dp, 0.dp, 0.dp, 0.dp)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggCutCornerDpShapeValue)
            val map = SnyggIdToValueMap.new(
                CornerSizeTopStart to v.topStart.value,
                CornerSizeTopEnd to v.topEnd.value,
                CornerSizeBottomEnd to v.bottomEnd.value,
                CornerSizeBottomStart to v.bottomStart.value,
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val topStart = map.getOrThrow<Int>(CornerSizeTopStart).dp
            val topEnd = map.getOrThrow<Int>(CornerSizeTopEnd).dp
            val bottomEnd = map.getOrThrow<Int>(CornerSizeBottomEnd).dp
            val bottomStart = map.getOrThrow<Int>(CornerSizeBottomStart).dp
            return@runCatching SnyggCutCornerDpShapeValue(topStart, topEnd, bottomEnd, bottomStart)
        }
    }

    override fun encoder() = Companion
}

data class SnyggCutCornerPercentShapeValue(
    override val topStart: Int,
    override val topEnd: Int,
    override val bottomEnd: Int,
    override val bottomStart: Int,
    override val shape: CutCornerShape = CutCornerShape(topStart, topEnd, bottomEnd, bottomStart),
) : SnyggPercentShapeValue {
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

        override fun defaultValue() = SnyggCutCornerPercentShapeValue(0, 0, 0, 0)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggCutCornerPercentShapeValue)
            val map = SnyggIdToValueMap.new(
                CornerSizeTopStart to v.topStart,
                CornerSizeTopEnd to v.topEnd,
                CornerSizeBottomEnd to v.bottomEnd,
                CornerSizeBottomStart to v.bottomStart,
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val topStart = map.getOrThrow<Int>(CornerSizeTopStart)
            val topEnd = map.getOrThrow<Int>(CornerSizeTopEnd)
            val bottomEnd = map.getOrThrow<Int>(CornerSizeBottomEnd)
            val bottomStart = map.getOrThrow<Int>(CornerSizeBottomStart)
            return@runCatching SnyggCutCornerPercentShapeValue(topStart, topEnd, bottomEnd, bottomStart)
        }
    }

    override fun encoder() = Companion
}

data class SnyggRoundedCornerDpShapeValue(
    override val topStart: Dp,
    override val topEnd: Dp,
    override val bottomEnd: Dp,
    override val bottomStart: Dp,
    override val shape: RoundedCornerShape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart),
) : SnyggDpShapeValue {
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

        override fun defaultValue() = SnyggRoundedCornerDpShapeValue(0.dp, 0.dp, 0.dp, 0.dp)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggRoundedCornerDpShapeValue)
            val map = SnyggIdToValueMap.new(
                CornerSizeTopStart to v.topStart.value,
                CornerSizeTopEnd to v.topEnd.value,
                CornerSizeBottomEnd to v.bottomEnd.value,
                CornerSizeBottomStart to v.bottomStart.value,
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val topStart = map.getOrThrow<Float>(CornerSizeTopStart).dp
            val topEnd = map.getOrThrow<Float>(CornerSizeTopEnd).dp
            val bottomEnd = map.getOrThrow<Float>(CornerSizeBottomEnd).dp
            val bottomStart = map.getOrThrow<Float>(CornerSizeBottomStart).dp
            return@runCatching SnyggRoundedCornerDpShapeValue(topStart, topEnd, bottomEnd, bottomStart)
        }
    }

    override fun encoder() = Companion
}

data class SnyggRoundedCornerPercentShapeValue(
    override val topStart: Int,
    override val topEnd: Int,
    override val bottomEnd: Int,
    override val bottomStart: Int,
    override val shape: RoundedCornerShape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart),
) : SnyggPercentShapeValue {
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

        override fun defaultValue() = SnyggRoundedCornerPercentShapeValue(0, 0, 0, 0)

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggRoundedCornerPercentShapeValue)
            val map = SnyggIdToValueMap.new(
                CornerSizeTopStart to v.topStart,
                CornerSizeTopEnd to v.topEnd,
                CornerSizeBottomEnd to v.bottomEnd,
                CornerSizeBottomStart to v.bottomStart,
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val topStart = map.getOrThrow<Int>(CornerSizeTopStart)
            val topEnd = map.getOrThrow<Int>(CornerSizeTopEnd)
            val bottomEnd = map.getOrThrow<Int>(CornerSizeBottomEnd)
            val bottomStart = map.getOrThrow<Int>(CornerSizeBottomStart)
            return@runCatching SnyggRoundedCornerPercentShapeValue(topStart, topEnd, bottomEnd, bottomStart)
        }
    }

    override fun encoder() = Companion
}
