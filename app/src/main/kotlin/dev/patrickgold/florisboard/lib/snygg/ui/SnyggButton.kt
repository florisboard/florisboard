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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.lib.snygg.SnyggPropertySet

@Composable
fun SnyggButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    text: String,
    style: SnyggPropertySet,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    val border = remember (style) {
        BorderStroke(style.borderWidth.dpSize(default = 0.dp), style.borderColor.solidColor())
    }
    val elevation = style.shadowElevation.dpSize(default = 0.dp)
    Button(
        modifier = modifier,
        enabled = enabled,
        elevation = ButtonDefaults.elevation(
            defaultElevation = elevation,
            pressedElevation = elevation,
            disabledElevation = elevation,
            hoveredElevation = elevation,
            focusedElevation = elevation,
        ),
        shape = style.shape.shape(),
        border = border,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = style.background.solidColor(default = FlorisImeTheme.fallbackContentColor()),
            contentColor = style.foreground.solidColor(default = FlorisImeTheme.fallbackSurfaceColor()),
        ),
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .padding(end = ButtonDefaults.IconSpacing)
                    .size(ButtonDefaults.IconSize),
                painter = icon,
                contentDescription = null,
            )
        }
        Text(
            text = text,
            fontSize = style.fontSize.spSize(),
        )
    }
}
