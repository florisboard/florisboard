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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import dev.patrickgold.florisboard.snygg.value.SnyggShapeValue
import dev.patrickgold.florisboard.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue

fun Modifier.snyggBackground(
    value: SnyggValue,
    shape: Shape = RectangleShape,
): Modifier {
    return when (value) {
        is SnyggSolidColorValue -> this.background(value.color, shape)
        else -> this
    }
}

fun SnyggValue.solidColor(): Color {
    return when (this) {
        is SnyggSolidColorValue -> this.color
        else -> Color.Unspecified
    }
}

fun SnyggValue.shape(): Shape {
    return when (this) {
        is SnyggShapeValue -> this.shape
        else -> RectangleShape
    }
}
