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

package dev.patrickgold.florisboard.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun FlorisButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    text: String,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.padding(end = 8.dp),
                painter = icon,
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
    icon: Painter? = null,
    text: String,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
) {
    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.padding(end = 8.dp),
                painter = icon,
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
    icon: Painter? = null,
    text: String,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    TextButton(
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.padding(end = 8.dp),
                painter = icon,
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
    icon: Painter,
    enabled: Boolean = true,
    iconModifier: Modifier = Modifier,
    iconColor: Color = Color.Unspecified,
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else 0.14f
        CompositionLocalProvider(
            LocalContentAlpha provides contentAlpha,
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
                    painter = icon,
                    contentDescription = null,
                )
            }
        }
    }
}
