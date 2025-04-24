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

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.value.SnyggUriValue

@Composable
fun SnyggBox(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    clickAndSemanticsModifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    supportsBackgroundImage: Boolean = false,
    backgroundImageDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val assetResolver = LocalSnyggAssetResolver.current
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        @Composable
        fun SnyggImage() {
            when (val bg = style.backgroundImage) {
                is SnyggUriValue -> {
                    val result = assetResolver.resolveAbsolutPath(bg.uri)
                    val path = result.getOrNull() ?: return
                    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
                    if (bitmap == null) return
                    Image(
                        bitmap = bitmap,
                        contentScale = style.objectFit(),
                        contentDescription = backgroundImageDescription,
                    )
                }
            }
        }

        Box(
            modifier = modifier
                .snyggMargin(style)
                .snyggShadow(style)
                .snyggBorder(style)
                .snyggBackground(style)
                .then(clickAndSemanticsModifier)
                .snyggPadding(style),
            contentAlignment = contentAlignment,
            propagateMinConstraints = propagateMinConstraints,
        ) {
            if (supportsBackgroundImage) {
                SnyggImage()
            }
            content()
        }
    }
}

@Preview
@Composable
private fun SimpleSnyggBox() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-surface" {
            background = rgbaColor(255, 255, 255)
            foreground = rgbaColor(255, 0, 0)
            padding = padding(10.dp)
            shape = roundedCornerShape(20)
            clip = yes()
        }
        "preview-text" {
            fontSize = fontSize(12.sp)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggBox("preview-surface") {
            SnyggColumn("column") {
                SnyggText("preview-text", text = "hello world")
                SnyggText("preview-text", text = "second text")
            }
        }
    }
}
