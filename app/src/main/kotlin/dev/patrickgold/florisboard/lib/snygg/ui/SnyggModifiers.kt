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

package dev.patrickgold.florisboard.lib.snygg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import dev.patrickgold.florisboard.lib.snygg.SnyggPropertySet
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggValue

fun Modifier.snyggBackground(
    style: SnyggPropertySet,
    fallbackColor: Color = Color.Unspecified,
    shape: Shape = style.shape.shape(),
): Modifier {
    return when (val bg = style.background) {
        is SnyggSolidColorValue -> this.background(
            color = bg.color,
            shape = shape,
        )
        else -> if (fallbackColor.isSpecified) {
            this.background(
                color = fallbackColor,
                shape = shape,
            )
        } else {
            this
        }
    }
}

fun Modifier.snyggBorder(
    style: SnyggPropertySet,
    width: Dp = style.borderWidth.dpSize().takeOrElse { 0.dp }.coerceAtLeast(0.dp),
    color: Color = style.borderColor.solidColor(default = Color.Unspecified),
    shape: Shape = style.shape.shape(),
): Modifier {
    return if (color.isSpecified) {
        this.border(width, color, shape)
    } else {
        this
    }
}

fun Modifier.snyggClip(
    style: SnyggPropertySet,
    shape: Shape = style.shape.shape(),
): Modifier {
    return this.clip(shape)
}

fun Modifier.snyggShadow(
    style: SnyggPropertySet,
    elevation: Dp = style.shadowElevation.dpSize().takeOrElse { 0.dp }.coerceAtLeast(0.dp),
    shape: Shape = style.shape.shape(),
): Modifier {
    // TODO: find a performant way to implement shadow color
    return this.shadow(elevation, shape, clip = false)
}

fun SnyggValue.solidColor(default: Color = Color.Transparent): Color {
    return when (this) {
        is SnyggSolidColorValue -> this.color
        else -> default
    }
}

fun SnyggValue.shape(): Shape {
    return when (this) {
        is SnyggShapeValue -> this.shape
        else -> RectangleShape
    }
}

fun SnyggValue.dpSize(default: Dp = Dp.Unspecified): Dp {
    return when (this) {
        is SnyggDpSizeValue -> this.dp
        else -> default
    }
}

fun SnyggValue.spSize(default: TextUnit = TextUnit.Unspecified): TextUnit {
    return when (this) {
        is SnyggSpSizeValue -> this.sp
        else -> default
    }
}
