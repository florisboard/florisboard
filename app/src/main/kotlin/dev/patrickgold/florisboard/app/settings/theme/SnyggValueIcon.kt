/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggMaterialYouValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggShapeValue
import org.florisboard.lib.snygg.value.SnyggSolidColorValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggValue
import dev.patrickgold.jetpref.material.ui.checkeredBackground

object SnyggValueIcon {
    interface Spec {
        val borderWith: Dp
        val boxShape: Shape
        val elevation: Dp
        val gridSize: Dp
        val iconSize: Dp
        val iconSizeMinusBorder: Dp
    }

    object Small : Spec {
        override val borderWith = Dp.Hairline
        override val boxShape = RoundedCornerShape(4.dp)
        override val elevation = 4.dp
        override val gridSize = 2.dp
        override val iconSize = 16.dp
        override val iconSizeMinusBorder = 16.dp
    }

    object Normal : Spec {
        override val borderWith = 1.dp
        override val boxShape = RoundedCornerShape(8.dp)
        override val elevation = 4.dp
        override val gridSize = 3.dp
        override val iconSize = 24.dp
        override val iconSizeMinusBorder = 22.dp
    }
}

@Composable
internal fun SnyggValueIcon(
    value: SnyggValue,
    definedVariables: Map<String, SnyggValue>,
    modifier: Modifier = Modifier,
    spec: SnyggValueIcon.Spec = SnyggValueIcon.Normal,
) {
    when (value) {
        is SnyggSolidColorValue -> {
            SnyggValueColorBox(modifier = modifier, spec = spec, backgroundColor = value.color)
        }

        is SnyggMaterialYouValue -> {
            SnyggValueColorBox(modifier = modifier, spec = spec, backgroundColor = value.loadColor(LocalContext.current))
        }

        is SnyggShapeValue -> {
            Box(
                modifier = modifier
                    .requiredSize(spec.iconSizeMinusBorder)
                    .border(spec.borderWith, MaterialTheme.colorScheme.onBackground, value.alwaysPercentShape())
            )
        }
        is SnyggDpSizeValue -> {
            Icon(
                modifier = modifier.requiredSize(spec.iconSize),
                imageVector = Icons.Default.Straighten,
                contentDescription = null,
            )
        }
        is SnyggSpSizeValue -> {
            Icon(
                modifier = modifier.requiredSize(spec.iconSize),
                imageVector = Icons.Default.FormatSize,
                contentDescription = null,
            )
        }
        is SnyggDefinedVarValue -> {
            val realValue = definedVariables[value.key]
            if (realValue == null) {
                Icon(
                    modifier = modifier.requiredSize(spec.iconSize),
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                )
            } else {
                val smallSpec = SnyggValueIcon.Small
                Box(modifier = modifier
                    .requiredSize(spec.iconSize)
                    .offset(y = (-2).dp)) {
                    SnyggValueIcon(
                        modifier = Modifier.offset(x = 8.dp, y = 8.dp),
                        value = realValue,
                        definedVariables = definedVariables,
                        spec = smallSpec,
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 1.dp)
                            .requiredSize(smallSpec.iconSize)
                            .padding(vertical = 2.dp)
                            .background(MaterialTheme.colorScheme.background, spec.boxShape),
                    )
                    Icon(
                        modifier = Modifier.requiredSize(smallSpec.iconSize),
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                    )
                }
            }
        }
        else -> {
            // Render nothing
        }
    }
}

@Composable
internal fun SnyggValueColorBox(
    modifier: Modifier,
    spec: SnyggValueIcon.Spec,
    backgroundColor: Color
) {
    Surface(
        modifier = modifier.requiredSize(spec.iconSize),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = spec.elevation,
        shape = spec.boxShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .checkeredBackground(gridSize = spec.gridSize)
                .background(backgroundColor),
        )
    }
}

private const val UpscaleFactor = 3
private const val UpscaleMaxAbsoluteValue = 100

fun SnyggShapeValue.alwaysPercentShape(): Shape {
    return when (this) {
        is SnyggRoundedCornerDpShapeValue -> {
            RoundedCornerShape(
                (this.topStart.value.toInt() * UpscaleFactor).coerceAtMost(UpscaleMaxAbsoluteValue),
                (this.topEnd.value.toInt() * UpscaleFactor).coerceAtMost(UpscaleMaxAbsoluteValue),
                (this.bottomEnd.value.toInt() * UpscaleFactor).coerceAtMost(UpscaleMaxAbsoluteValue),
                (this.bottomStart.value.toInt() * UpscaleFactor).coerceAtMost(UpscaleMaxAbsoluteValue),
            )
        }
        is SnyggCutCornerDpShapeValue -> {
            CutCornerShape(
                (this.topStart.value.toInt() * UpscaleFactor).coerceAtMost(UpscaleMaxAbsoluteValue),
                (this.topEnd.value.toInt() * UpscaleFactor).coerceAtMost(UpscaleMaxAbsoluteValue),
                (this.bottomEnd.value.toInt() * UpscaleFactor).coerceAtMost(UpscaleMaxAbsoluteValue),
                (this.bottomStart.value.toInt() * UpscaleFactor).coerceAtMost(UpscaleMaxAbsoluteValue),
            )
        }
        else -> this.shape
    }
}
