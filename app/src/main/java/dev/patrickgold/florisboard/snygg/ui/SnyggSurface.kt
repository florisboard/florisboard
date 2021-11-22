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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalAbsoluteElevation
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.snygg.value.SnyggValue

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SnyggSurface(
    modifier: Modifier = Modifier,
    background: SnyggValue,
    shape: SnyggValue? = null,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    clickAndSemanticsModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shapeValue = shape?.shape() ?: RectangleShape
    val color = background.solidColor()
    val contentColor = contentColorFor(color)
    val elevationOverlay = LocalElevationOverlay.current
    val absoluteElevation = LocalAbsoluteElevation.current + elevation
    val backgroundColor = if (color == MaterialTheme.colors.surface && elevationOverlay != null) {
        elevationOverlay.apply(color, absoluteElevation)
    } else {
        color
    }
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalAbsoluteElevation provides absoluteElevation,
    ) {
        Box(
            modifier
                .shadow(elevation, shapeValue, clip = false)
                .then(if (border != null) Modifier.border(border, shapeValue) else Modifier)
                .background(
                    color = backgroundColor,
                    shape = shapeValue,
                )
                .clip(shapeValue)
                .then(clickAndSemanticsModifier),
            propagateMinConstraints = true,
            content = content,
        )
    }
}