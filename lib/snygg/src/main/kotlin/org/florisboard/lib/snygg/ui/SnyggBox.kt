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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.emptySelectors

@Composable
fun SnyggBox(
    elementName: String,
    attributes: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val theme = LocalSnyggTheme.current
    val style = theme.rememberQuery(elementName, attributes, emptySelectors())
    ProvideSnyggParentStyle(style) {
        Box(
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
private fun SimpleSnyggSurface() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-surface" {
            background = rgbaColor(255, 255, 255)
            foreground = rgbaColor(255, 0, 0)
            padding = padding(10.dp)
        }
        "preview-text" {
            fontSize = fontSize(12.sp)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggSurface("preview-surface") {
            SnyggColumn("column") {
                SnyggText("preview-text", text = "hello world")
                SnyggText("preview-text", text = "second text")
            }
        }
    }
}
