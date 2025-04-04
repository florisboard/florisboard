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
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

@Composable
fun SnyggIcon(
    elementName: String,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String? = null,
) {
    val theme = LocalSnyggTheme.current
    val style = theme.rememberQuery(elementName, attributes, selector)

    Icon(
        modifier = modifier,
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = style.foreground(),
    )
}

@Composable
fun SnyggIcon(
    elementName: String,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    bitmap: ImageBitmap,
    contentDescription: String? = null,
) {
    val theme = LocalSnyggTheme.current
    val style = theme.rememberQuery(elementName, attributes, selector)

    Icon(
        modifier = modifier,
        bitmap = bitmap,
        contentDescription = contentDescription,
        tint = style.foreground(),
    )
}

@Composable
fun SnyggIcon(
    elementName: String,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    painter: Painter,
    contentDescription: String? = null,
) {
    val theme = LocalSnyggTheme.current
    val style = theme.rememberQuery(elementName, attributes, selector)

    Icon(
        modifier = modifier,
        painter = painter,
        contentDescription = contentDescription,
        tint = style.foreground(),
    )
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
