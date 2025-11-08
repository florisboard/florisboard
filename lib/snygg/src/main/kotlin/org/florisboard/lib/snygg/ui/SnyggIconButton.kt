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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

/**
 * Simple interactive Box with integrated clickable state management.
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param modifier The modifier to be applied to the icon button.
 * @param onClick called when this icon button is clicked
 * @param enabled controls the enabled state of this icon button. When `false`, this component will not respond to user
 *    input, and it will appear visually disabled and disabled to accessibility services.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and emitting [Interaction]s for
 *    this button. You can use this to change the button's appearance or preview the button in different states. Note
 *    that if `null` is provided, interactions will still happen internally.
 * @param content The content displayed on the icon button, expected to be icon.
 */
@Composable
fun SnyggIconButton(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val selector = if (enabled) SnyggSelector.NONE else SnyggSelector.DISABLED
    ProvideSnyggStyle(elementName, attributes, selector) { style ->

        Box(
            modifier = Modifier
                .snyggMargin(style)
                //TODO: We need to apply a size and a clip here
                // otherwise the indication is wrong (see: OnHandedPanel buttons)
                .snyggShadow(style)
                .snyggBorder(style)
                .snyggBackground(style)
                .then(modifier)
                .snyggPadding(style)
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(),
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}


@Preview
@Composable
private fun SimpleSnyggIconButton() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-column" {
            background = rgbaColor(255, 255, 255)
            fontSize = fontSize(20.sp)
            foreground = rgbaColor(0, 0, 255)
            padding = padding(6.dp)
        }
        "preview-icon" {
            background = rgbaColor(180, 180, 180)
            foreground = rgbaColor(0, 0, 0)
            shape = roundedCornerShape(4.dp)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggColumn("preview-column") {
            SnyggText("preview-text", text = "blue text")
            SnyggIconButton(
                "preview-icon",
                onClick = {},
            ) {
                SnyggIcon("preview-icon", imageVector = Icons.Default.Search)
            }
        }
    }
}
