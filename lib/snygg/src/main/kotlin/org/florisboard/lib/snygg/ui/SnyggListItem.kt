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

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ListItem
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

/**
 * Simple list item composable.
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param modifier The modifier to be applied to the [ListItem].
 * @param onClick The clickable action for this item.
 * @param text The text of the list item.
 * @param leadingImageVector The leading icon of the list item.
 * @param trailingImageVector The trailing icon of the list item.
 * @param interactionSource The interaction source for the onClick action.
 * @param indication The indication of the list item during interaction. Defaults to a ripple effect.
 * @param enabled If the clickable action is enabled or not. False will set the DISABLED selector for style querying.
 *
 * @since 0.5.0-alpha03
 */
@Composable
fun SnyggListItem(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    leadingImageVector: ImageVector? = null,
    trailingImageVector: ImageVector? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication = ripple(),
    enabled: Boolean = true,
) {
    val selector = if (enabled) SnyggSelector.NONE else SnyggSelector.DISABLED
    val decoratedModifier = modifier.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = null,
        role = null,
        onClick = onClick,
    )
    SnyggRow(elementName, attributes, selector,
        modifier = decoratedModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingImageVector != null) {
            SnyggBox(
                elementName = "$elementName-icon-leading",
                attributes = attributes,
                selector = selector,
            ) {
                SnyggIcon(imageVector = leadingImageVector)
            }
        }
        SnyggText(
            elementName = "$elementName-text",
            attributes = attributes,
            selector = selector,
            modifier = Modifier.fillMaxWidth(),
            text = text,
        )
        if (trailingImageVector != null) {
            SnyggBox(
                elementName = "$elementName-icon-trailing",
                attributes = attributes,
                selector = selector,
            ) {
                SnyggIcon(imageVector = trailingImageVector)
            }
        }
    }
}

@Preview
@Composable
private fun SimpleSnyggListItem() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview" {
            background = rgbaColor(255, 255, 255)
            fontSize = fontSize(16.sp)
            foreground = rgbaColor(0, 0, 0)
        }
        "preview-list-item" {
            background = rgbaColor(240, 220, 220)
            foreground = rgbaColor(0, 0, 0)
            padding = padding(all = 16.dp)
        }
        "preview-list-item-icon-leading" {
            fontSize = fontSize(24.sp)
            padding = padding(0.dp, 0.dp, 16.dp, 0.dp)
        }
        "preview-list-item"("layout" to listOf("center")) {
            textAlign = textAlign(TextAlign.Center)
        }
        "preview-list-item"("layout" to listOf("long")) {
            textMaxLines = textMaxLines(1)
            textOverflow = textOverflow(TextOverflow.Ellipsis)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggColumn("preview") {
            SnyggListItem("preview-list-item",
                leadingImageVector = Icons.Default.Search,
                onClick = {},
                text = "hello",
            )
            SnyggListItem("preview-list-item",
                mapOf("layout" to "center"),
                leadingImageVector = Icons.Default.Search,
                onClick = {},
                text = "hello",
            )
            SnyggListItem("preview-list-item",
                mapOf("layout" to "long"),
                leadingImageVector = Icons.Default.Search,
                onClick = {},
                text = "hello world this is a very long list item text label",
            )
        }
    }
}
