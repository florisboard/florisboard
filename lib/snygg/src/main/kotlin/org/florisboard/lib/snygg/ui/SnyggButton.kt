/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package org.florisboard.lib.snygg.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector

@Composable
fun SnyggButton(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    text: String,
    enabled: Boolean = true,
) {
    val theme = LocalSnyggTheme.current
    val styleDefault = theme.rememberQuery(elementName, attributes)
    val stylePressed = theme.rememberQuery(elementName, attributes, SnyggSelector.PRESSED)
    val styleFocus = theme.rememberQuery(elementName, attributes, SnyggSelector.FOCUS)
    val styleHover = theme.rememberQuery(elementName, attributes, SnyggSelector.HOVER)
    val styleDisabled = theme.rememberQuery(elementName, attributes, SnyggSelector.DISABLED)

    Button(
        modifier = modifier,
        enabled = enabled,
        shape = styleDefault.shape(),
        colors = ButtonDefaults.buttonColors(
            containerColor = styleDefault.background(),
            contentColor = styleDefault.foreground(),
            disabledContainerColor = styleDisabled.background(),
            disabledContentColor = styleDisabled.foreground(),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = styleDefault.shadowElevation.dpSize(),
            pressedElevation = stylePressed.shadowElevation.dpSize(),
            focusedElevation = styleFocus.shadowElevation.dpSize(),
            hoveredElevation = styleHover.shadowElevation.dpSize(),
            disabledElevation = styleDisabled.shadowElevation.dpSize(),
        ),
        onClick = onClick,
    ) {
        if (icon != null) {
            SnyggIcon(
                modifier = Modifier
                    .padding(end = ButtonDefaults.IconSpacing)
                    .size(ButtonDefaults.IconSize),
                painter = icon,
                contentDescription = null,
            )
        }
        SnyggText(text = text, modifier = modifier)
    }
}
