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

package dev.patrickgold.florisboard.lib.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FlorisChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    shape: Shape = CircleShape,
    @DrawableRes leadingIcons: List<Int> = listOf(),
    @DrawableRes trailingIcons: List<Int> = listOf(),
) {
    val backgroundColor = color.takeOrElse {
        MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.BackgroundOpacity)
    }
    Surface(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        color = backgroundColor,
        shape = shape,
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (leadingIcon in leadingIcons) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp).size(16.dp),
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                )
            }
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            for (trailingIcon in trailingIcons) {
                Icon(
                    modifier = Modifier.padding(start = 8.dp).size(16.dp),
                    painter = painterResource(trailingIcon),
                    contentDescription = null,
                )
            }
        }
    }
}
