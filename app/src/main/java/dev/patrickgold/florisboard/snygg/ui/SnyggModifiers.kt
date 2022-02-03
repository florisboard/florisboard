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

package dev.patrickgold.florisboard.snygg.ui

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.patrickgold.florisboard.snygg.value.SnyggDpSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggShapeValue
import dev.patrickgold.florisboard.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue

fun Modifier.snyggBackground(
    background: SnyggValue,
    shape: SnyggValue? = null,
): Modifier {
    return when (background) {
        is SnyggSolidColorValue -> this.background(
            color = background.color,
            shape = shape?.shape() ?: RectangleShape,
        )
        else -> this
    }
}

fun Modifier.snyggClip(
    shape: SnyggValue,
): Modifier {
    return this.clip(shape.shape())
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

fun SnyggValue.dpSize(): Dp {
    return when (this) {
        is SnyggDpSizeValue -> this.dp
        else -> Dp.Unspecified
    }
}

fun SnyggValue.spSize(): TextUnit {
    return when (this) {
        is SnyggSpSizeValue -> this.sp
        else -> TextUnit.Unspecified
    }
}
