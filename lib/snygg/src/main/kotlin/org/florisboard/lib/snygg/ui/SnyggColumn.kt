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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.emptySelectors

@Composable
fun SnyggColumn(
    elementName: String,
    attributes: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalSnyggTheme.current
    val style = theme.rememberQuery(elementName, attributes, emptySelectors())
    ProvideSnyggParentStyle(style) {
        Column(
            modifier = modifier
                .snyggMargin(style)
                .snyggShadow(style)
                .snyggBorder(style)
                .snyggBackground(style)
                .snyggPadding(style),
            content = content,
        )
    }
}

@Preview
@Composable
fun SimpleSnyggColumn() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-text" {
            background = rgbaColor(255, 255, 255)
            foreground = rgbaColor(0, 0, 0)
            borderColor = rgbaColor(0, 0, 255)
            borderWidth = size(1.dp)
            shadowElevation = size(6.dp)
            shadowColor = rgbaColor(0, 255, 0)
            margin = padding(16.dp)
            padding = padding(6.dp)
        }
        "preview-text"("attr" to listOf(1)) {
            foreground = rgbaColor(255, 0, 0)
            borderWidth = size(0.dp)
            fontSize = fontSize(10.sp)
            fontStyle = fontStyle(FontStyle.Italic)
            fontWeight = fontWeight(FontWeight.Bold)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggColumn("preview-column") {
            SnyggText("preview-text", text = "black text")
            SnyggText("preview-text", mapOf("attr" to 1), text = "red text")
        }
    }
}
