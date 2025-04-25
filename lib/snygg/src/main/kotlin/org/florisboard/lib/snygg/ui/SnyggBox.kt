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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.value.SnyggUriValue
import java.io.File

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
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        if (!supportsBackgroundImage) {
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
                content = content,
            )
        } else {
            val assetResolver = LocalSnyggAssetResolver.current
            var contentSize by remember { mutableStateOf(IntSize.Zero) }
            Box(
                modifier = modifier
                    .snyggMargin(style)
                    .onSizeChanged { contentSize = it }
                    .snyggShadow(style)
                    .snyggBorder(style)
                    .snyggBackground(style)
                    .then(clickAndSemanticsModifier)
                    .snyggPadding(style),
                contentAlignment = contentAlignment,
                propagateMinConstraints = propagateMinConstraints,
            ) {
                when (val bg = style.backgroundImage) {
                    is SnyggUriValue -> {
                        val result = assetResolver.resolveAbsolutPath(bg.uri)
                        val path = result.getOrNull()
                        // TODO: silent errors are hard to debug :/
                        if (path != null) {
                            AsyncImage(
                                modifier = with(LocalDensity.current) {
                                    Modifier.size(contentSize.toSize().toDpSize())
                                },
                                model = File(path),
                                contentScale = style.objectFit(),
                                contentDescription = backgroundImageDescription,
                            )
                        }
                    }
                }
                content()
            }
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
