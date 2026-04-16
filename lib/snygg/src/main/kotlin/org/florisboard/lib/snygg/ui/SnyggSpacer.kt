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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

/**
 * Simple spacer composable.
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 * @param modifier The modifier to be applied to the [Spacer].
 *
 * @since 0.5.0-alpha01
 *
 * @see [Spacer]
 */
@Composable
fun SnyggSpacer(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
) {
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        Spacer(
            modifier = modifier
                .snyggMargin(style)
                .snyggShadow(style)
                .snyggBackground(style, default = style.foreground())
                .snyggPadding(style),
        )
    }
}

@Preview
@Composable
private fun SimpleSnyggSpacer() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-row" {
            background = rgbaColor(255, 255, 255)
            fontSize = fontSize(16.sp)
            foreground = rgbaColor(0, 0, 0)
            padding = padding(12.dp)
        }
        "preview-spacer" {
            margin = padding(horizontal = 12.dp, vertical = 0.dp)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggRow("preview-row") {
            SnyggText("preview-text", text = "hello")
            SnyggSpacer("preview-spacer",
                modifier = Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .align(Alignment.CenterVertically))
            SnyggText("preview-text", text = "world")
        }
    }
}
