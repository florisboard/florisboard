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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggValue
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
            Surface(
                modifier = modifier.requiredSize(spec.iconSize),
                color = MaterialTheme.colors.background,
                elevation = spec.elevation,
                shape = spec.boxShape,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .checkeredBackground(gridSize = spec.gridSize)
                        .background(value.color),
                )
            }
        }
        is SnyggShapeValue -> {
            Box(
                modifier = modifier
                    .requiredSize(spec.iconSizeMinusBorder)
                    .border(spec.borderWith, MaterialTheme.colors.onBackground, value.alwaysPercentShape())
            )
        }
        is SnyggDpSizeValue -> {
            Icon(
                modifier = modifier.requiredSize(spec.iconSize),
                painter = painterResource(R.drawable.ic_straighten),
                contentDescription = null,
            )
        }
        is SnyggSpSizeValue -> {
            Icon(
                modifier = modifier.requiredSize(spec.iconSize),
                painter = painterResource(R.drawable.ic_format_size),
                contentDescription = null,
            )
        }
        is SnyggDefinedVarValue -> {
            val realValue = definedVariables[value.key]
            if (realValue == null) {
                Icon(
                    modifier = modifier.requiredSize(spec.iconSize),
                    painter = painterResource(R.drawable.ic_link),
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
                            .background(MaterialTheme.colors.background, spec.boxShape),
                    )
                    Icon(
                        modifier = Modifier.requiredSize(smallSpec.iconSize),
                        painter = painterResource(R.drawable.ic_link),
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

private const val AlwaysPercentUpscaleFactor = 3

fun SnyggShapeValue.alwaysPercentShape(): Shape {
    return when (this) {
        is SnyggRoundedCornerDpShapeValue -> {
            RoundedCornerShape(
                this.topStart.value.toInt() * AlwaysPercentUpscaleFactor,
                this.topEnd.value.toInt() * AlwaysPercentUpscaleFactor,
                this.bottomEnd.value.toInt() * AlwaysPercentUpscaleFactor,
                this.bottomStart.value.toInt() * AlwaysPercentUpscaleFactor,
            )
        }
        is SnyggCutCornerDpShapeValue -> {
            CutCornerShape(
                this.topStart.value.toInt() * AlwaysPercentUpscaleFactor,
                this.topEnd.value.toInt() * AlwaysPercentUpscaleFactor,
                this.bottomEnd.value.toInt() * AlwaysPercentUpscaleFactor,
                this.bottomStart.value.toInt() * AlwaysPercentUpscaleFactor,
            )
        }
        else -> this.shape
    }
}
