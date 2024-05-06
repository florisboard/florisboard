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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun FlorisButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .padding(end = ButtonDefaults.IconSpacing)
                    .size(ButtonDefaults.IconSize),
                imageVector = icon,
                contentDescription = null,
            )
        }
        Text(text = text)
    }
}

@Composable
fun FlorisOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
) {
    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .padding(end = ButtonDefaults.IconSpacing)
                    .size(ButtonDefaults.IconSize),
                imageVector = icon,
                contentDescription = null,
            )
        }
        Text(text = text)
    }
}

@Composable
fun FlorisTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    TextButton(
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .padding(end = ButtonDefaults.IconSpacing)
                    .size(ButtonDefaults.IconSize),
                imageVector = icon,
                contentDescription = null,
            )
        }
        Text(text = text)
    }
}

@Composable
fun FlorisIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    enabled: Boolean = true,
    iconModifier: Modifier = Modifier,
    iconColor: Color = Color.Unspecified,
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        val contentAlpha = if (enabled) 1f else 0.14f
        val contentColor = iconColor.takeOrElse { LocalContentColor.current }.copy(alpha = contentAlpha)
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
        ) {
            Icon(
                modifier = iconModifier,
                imageVector = icon,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun FlorisIconButtonWithInnerPadding(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    enabled: Boolean = true,
    iconModifier: Modifier = Modifier,
    iconColor: Color = Color.Unspecified,
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        val contentAlpha = if (enabled) 1f else 0.14f
        CompositionLocalProvider(
            LocalContentColor provides iconColor,
        ) {
            Box(
                modifier = iconModifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = modifier.alpha(contentAlpha),
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        }
    }
}
