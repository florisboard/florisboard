/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastRoundToInt

fun Modifier.layoutNormalized(bounds: Rect) =
    this.layout { measurable, constraints ->
        val effConstraints = Constraints.fixed(
            width = (constraints.maxWidth * bounds.width).fastRoundToInt(),
            height = (constraints.maxHeight * bounds.height).fastRoundToInt(),
        )
        val placeable = measurable.measure(effConstraints)
        val offset = IntOffset(
            x = (constraints.maxWidth * bounds.topLeft.x).fastRoundToInt(),
            y = (constraints.maxHeight * bounds.topLeft.y).fastRoundToInt(),
        )
        layout(placeable.width, placeable.height) { placeable.place(offset) }
    }

fun Modifier.scaleToFitHorizontally() = composed {
    var scale by remember { mutableFloatStateOf(1f) }
    this
        .layout { measurable, constraints ->
            val infConstraints = constraints.copy(maxWidth = Int.MAX_VALUE)
            val placeable = measurable.measure(infConstraints)
            val scaleMaxWidth = constraints.maxWidth * 0.7f
            scale = when {
                placeable.width > scaleMaxWidth -> {
                    scaleMaxWidth / placeable.width.toFloat()
                }
                else -> 1f
            }
            layout(placeable.width, placeable.height) { placeable.place(IntOffset.Zero) }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
