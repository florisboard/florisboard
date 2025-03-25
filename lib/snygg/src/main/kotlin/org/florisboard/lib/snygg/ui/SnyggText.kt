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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
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
fun SnyggText(
    elementName: String,
    vararg attributes: Pair<String, Int>,
    modifier: Modifier = Modifier,
    text: String,
) {
    val theme = LocalSnyggTheme.current
    val style = theme.query(elementName, mapOf(*attributes), emptySelectors())
    Text(
        modifier = modifier
            .snyggMargin(style)
            .snyggShadow(style)
            .snyggBorder(style)
            .snyggBackground(style)
            .snyggPadding(style),
        text = text,
        color = style.foreground.colorOrDefault(LocalContentColor.current),
        fontSize = style.fontSize.spSize(),
        fontStyle = style.fontStyle.fontStyle(),
        fontWeight = style.fontWeight.fontWeight(),
    )
}

@Preview
@Composable
fun SimpleSnyggText() {
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
            fontSize = size(10.sp)
            fontStyle = fontStyle(FontStyle.Italic)
            fontWeight = fontWeight(FontWeight.Bold)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        Column {
            SnyggText("preview-text", text = "black text")
            SnyggText("preview-text", "attr" to 1, text = "red text")
        }
    }
}
