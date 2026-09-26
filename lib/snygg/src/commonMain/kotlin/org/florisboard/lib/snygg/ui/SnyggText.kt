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

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

/**
 * Simple text composable, which displays the given [text].
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 * @param modifier The modifier to be applied to the Text.
 * @param text The text of the element.
 *
 * @since 0.5.0-alpha01
 *
 * @see [Text]
 */
@Composable
fun SnyggText(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    text: String,
) {
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        Text(
            modifier = modifier
                .snyggMargin(style)
                .snyggShadow(style)
                .snyggBorder(style)
                .snyggBackground(style, allowClip = false)
                .snyggPadding(style),
            text = text,
            color = style.foreground(),
            fontSize = style.fontSize(),
            fontStyle = style.fontStyle(),
            fontWeight = style.fontWeight(),
            fontFamily = style.fontFamily(LocalSnyggPreloadedCustomFontFamilies.current),
            letterSpacing = style.letterSpacing(),
            lineHeight = style.lineHeight(),
            textAlign = style.textAlign(),
            textDecoration = style.textDecorationLine(),
            maxLines = style.textMaxLines(),
            overflow = style.textOverflow(),
        )
    }
}

@Preview
@Composable
private fun SimpleSnyggText() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-column" {
            fontSize = fontSize(20.sp)
            foreground = rgbaColor(0, 0, 255)
        }
        "preview-text" {
            background = rgbaColor(255, 255, 255)
            foreground = inherit()
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
            letterSpacing = fontSize(4.sp)
            textDecorationLine = textDecorationLine(TextDecoration.LineThrough)
        }
        "preview-text"("long" to listOf(1)) {
            fontFamily = genericFontFamily(FontFamily.Serif)
            fontSize = fontSize(10.sp)
            textMaxLines = textMaxLines(1)
            textOverflow = textOverflow(TextOverflow.Ellipsis)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggColumn("preview-column", modifier = Modifier.widthIn(max = 150.dp)) {
            SnyggText("preview-text", text = "black text")
            SnyggText("preview-text", mapOf("attr" to 1), text = "red text")
            SnyggText("preview-text", mapOf("long" to 1),
                text = "this is a very long paragraph that will definitely not fit")
        }
    }
}
