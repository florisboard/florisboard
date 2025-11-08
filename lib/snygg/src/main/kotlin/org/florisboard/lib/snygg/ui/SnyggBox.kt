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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

/**
 * Simple layout composable with [content]
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 * @param modifier The modifier to be applied to the layout.
 * @param clickAndSemanticsModifier The modifier to be applied to the layout after drawing the background.
 * @param contentAlignment The default alignment inside the Box.
 * @param propagateMinConstraints Whether the incoming min constraints should be passed to content.
 * @param supportsBackgroundImage controls if this Box supports background images.
 * @param backgroundImageDescription The content description of the background image.
 * @param allowClip If clipping should be allowed on this box.
 * @param content The content of the Box
 *
 * @since 0.5.0-alpha01
 *
 * @see [Box]
 */
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
    allowClip: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        val assetResolver = LocalSnyggAssetResolver.current
        val context = LocalContext.current
        val imagePath = when {
            supportsBackgroundImage -> {
                style.backgroundImage.uriOrNull()?.let { imageUri ->
                    assetResolver.resolveAbsolutePath(imageUri).getOrNull()
                }
            }
            else -> null
        }
        Box(
            modifier = modifier
                .snyggMargin(style)
                .snyggShadow(style)
                .snyggBorder(style)
                .snyggBackground(style, allowClip = allowClip)
                .then(clickAndSemanticsModifier)
                .snyggPadding(style),
            contentAlignment = contentAlignment,
            propagateMinConstraints = propagateMinConstraints,
        ) {
            if (imagePath != null) {
                AsyncImage(
                    modifier = Modifier.matchParentSize(),
                    // https://github.com/coil-kt/coil/issues/159
                    model = ImageRequest.Builder(context)
                        .data(imagePath)
                        .allowHardware(false) // slower, but hey at least it doesn't crash out of the blue
                        .build(),
                    contentScale = style.contentScale(),
                    contentDescription = backgroundImageDescription,
                )
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
