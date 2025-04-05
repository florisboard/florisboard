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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

@Composable
fun SnyggColumn(
    elementName: String?,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalSnyggTheme.current
    val style = theme.rememberQuery(elementName, attributes, selector)
    ProvideSnyggParentInfo(style, selector) {
        Column(
            modifier = Modifier
                .snyggMargin(style)
                .snyggShadow(style)
                .snyggBorder(style)
                .snyggBackground(style)
                .then(modifier)
                .snyggPadding(style),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

@Preview
@Composable
private fun SimpleSnyggColumn() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-column" {
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
        SnyggColumn("preview-column") {
            SnyggText("preview-text", text = "hello world")
            SnyggText("preview-text", text = "second text")
        }
    }
}
