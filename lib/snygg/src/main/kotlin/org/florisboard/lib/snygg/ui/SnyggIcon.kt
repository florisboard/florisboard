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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

/**
 * Simple Icon composable, which displays a given [imageVector] annotated by the [contentDescription].
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 * @param modifier The modifier to be applied to the Icon.
 * @param imageVector The imageVector which will be drawn as Icon.
 * @param contentDescription Text used by accessibility services to describe what this icon represents.
 * This should always be provided unless this icon is used for decorative purposes,
 * and does not represent a meaningful action that a user can take.
 *
 * @since 0.5.0-alpha01
 *
 * @see [Icon]
 */
@Composable
fun SnyggIcon(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String? = null,
) {
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        Icon(
            modifier = modifier.snyggIconSize(style),
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = style.foreground(),
        )
    }
}

/**
 * Simple Icon composable, which displays a given [bitmap] annotated by the [contentDescription].
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 * @param modifier The modifier to be applied to the Icon.
 * @param bitmap The imageBitmap which will be drawn as Icon.
 * @param contentDescription Text used by accessibility services to describe what this icon represents.
 * This should always be provided unless this icon is used for decorative purposes,
 * and does not represent a meaningful action that a user can take.
 *
 * @since 0.5.0-alpha01
 *
 * @see [Icon]
 */
@Composable
fun SnyggIcon(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    bitmap: ImageBitmap,
    contentDescription: String? = null,
) {
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        Icon(
            modifier = modifier.snyggIconSize(style),
            bitmap = bitmap,
            contentDescription = contentDescription,
            tint = style.foreground(),
        )
    }
}

/**
 * Simple Icon composable, which displays a given [painter] annotated by the [contentDescription].
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 * @param modifier The modifier to be applied to the Icon.
 * @param painter The painter which will be drawn as Icon.
 * @param contentDescription Text used by accessibility services to describe what this icon represents.
 * This should always be provided unless this icon is used for decorative purposes,
 * and does not represent a meaningful action that a user can take.
 *
 * @since 0.5.0-alpha01
 *
 * @see [Icon]
 */
@Composable
fun SnyggIcon(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    painter: Painter,
    contentDescription: String? = null,
) {
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        Icon(
            modifier = modifier.snyggIconSize(style),
            painter = painter,
            contentDescription = contentDescription,
            tint = style.foreground(),
        )
    }
}

@Preview
@Composable
private fun SimpleSnyggIcon() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-column" {
            fontSize = fontSize(20.sp)
            foreground = rgbaColor(0, 0, 255)
        }
        "preview-icon" {
            padding = padding(6.dp)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggColumn("preview-column") {
            SnyggText("preview-text", text = "blue text")
            SnyggIcon("preview-icon", imageVector = Icons.Default.Search)
        }
    }
}
