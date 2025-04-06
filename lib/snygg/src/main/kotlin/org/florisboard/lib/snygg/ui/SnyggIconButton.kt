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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

@Composable
fun SnyggIconButton(
    elementName: String?,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .snyggMargin(style)
                .snyggShadow(style)
                .snyggBorder(style)
                .snyggBackground(style)
                .then(modifier)
                // TODO: what is the material3 compliant default padding?
                .snyggPadding(style, default = PaddingValues(2.dp))
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    // TODO: what is the material3 compliant default ripple?
                    indication = ripple(bounded = false),
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun SnyggIconButton(
    elementName: String?,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    SnyggIconButton(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        SnyggIcon(
            elementName = elementName,
            attributes = attributes,
            selector = selector,
            modifier = Modifier,
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun SnyggIconButton(
    elementName: String?,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    bitmap: ImageBitmap,
    contentDescription: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    SnyggIconButton(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        SnyggIcon(
            elementName = elementName,
            attributes = attributes,
            selector = selector,
            modifier = Modifier,
            bitmap = bitmap,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun SnyggIconButton(
    elementName: String?,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    SnyggIconButton(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        SnyggIcon(
            elementName = elementName,
            attributes = attributes,
            selector = selector,
            modifier = Modifier,
            painter = painter,
            contentDescription = contentDescription,
        )
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
            SnyggIconButton("preview-icon",
                onClick = {},
                imageVector = Icons.Default.Search,
            )
        }
    }
}
