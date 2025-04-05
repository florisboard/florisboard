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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

@Composable
fun SnyggRow(
    elementName: String?,
    attributes: Map<String, Int> = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    val theme = LocalSnyggTheme.current
    val style = theme.rememberQuery(elementName, attributes, selector)
    ProvideSnyggParentInfo(style, selector) {
        Row(
            modifier = Modifier
                .snyggMargin(style)
                .then(modifier)
                .snyggShadow(style)
                .snyggBorder(style)
                .snyggBackground(style)
                .snyggPadding(style),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content,
        )
    }
}

@Preview
@Composable
private fun SimpleSnyggRow() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-row" {
            background = rgbaColor(255, 255, 255)
            foreground = rgbaColor(255, 0, 0)
            padding = padding(10.dp)
        }
        "preview-text" {
            fontSize = fontSize(12.sp)
        }
        "preview-text"("second" to listOf(1)) {
            fontSize = fontSize(6.sp)
            margin = padding(4.dp, 0.dp, 0.dp, 0.dp)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggRow("preview-row") {
            SnyggText("preview-text", text = "hello")
            SnyggText("preview-text", mapOf("second" to 1), text = "world")
        }
    }
}
