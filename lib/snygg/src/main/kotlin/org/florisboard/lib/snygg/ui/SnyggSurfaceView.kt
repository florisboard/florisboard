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

import android.graphics.PixelFormat
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.Image
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector

/**
 * Specialized layout composable rendering a background color/image to a [SurfaceView].
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 * @param modifier The modifier to be applied to the layout.
 * @param backgroundImageDescription The content description of the background image.
 *
 * @since 0.5.0-beta04
 *
 * @see [Box]
 * @see [SurfaceView]
 */
@Composable
fun SnyggSurfaceView(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    backgroundImageDescription: String? = null,
) {
    ProvideSnyggStyle(elementName, attributes, selector) { style ->
        val assetResolver = LocalSnyggAssetResolver.current
        val context = LocalContext.current
        val imageLoader = SingletonImageLoader.get(context)

        val backgroundColor = style.background(Color.Black)
        val imagePath = remember(style, assetResolver) {
            style.backgroundImage.uriOrNull()?.let { imageUri ->
                assetResolver.resolveAbsolutePath(imageUri).getOrNull()
            }
        }
        var image by remember { mutableStateOf<Image?>(null) }

        LaunchedEffect(imagePath) {
            if (imagePath == null) {
                image = null
                return@LaunchedEffect
            }
            val request = ImageRequest.Builder(context)
                .data(imagePath)
                .allowHardware(false)
                .build()
            val imageResult = imageLoader.execute(request)
            image = if (imageResult is SuccessResult) {
                imageResult.image
            } else {
                null
            }
        }

        var showSurfaceView by remember { mutableStateOf(false) }
        LifecycleResumeEffect(Unit) {
            showSurfaceView = true
            onPauseOrDispose {
                showSurfaceView = false
            }
        }

        if (showSurfaceView) {
            var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
            AndroidView(
                modifier = modifier,
                factory = { context ->
                    Log.d("SnyggSurfaceView", "creating new instance")
                    SurfaceView(context).apply {
                        setZOrderOnTop(false)
                        holder.setFormat(PixelFormat.TRANSPARENT)
                    }
                },
                update = { surfaceView = it },
            )
            surfaceView?.let { surfaceView ->
                LaunchedEffect(surfaceView, backgroundColor, image) {
                    Log.d("SnyggSurfaceView", "drawToSurface(backgroundColor=$backgroundColor, image=$image)")
                    val surface = surfaceView.holder.surface
                    Log.d("SnyggSurfaceView", "drawToSurface: surface.isValid=${surface.isValid}")
                    if (surface.isValid) {
                        val canvas = surface.lockCanvas(null)
                        try {
                            canvas.drawColor(backgroundColor.toArgb())
                            image?.draw(canvas)
                        } finally {
                            surface.unlockCanvasAndPost(canvas)
                        }
                    }
                }
            }
        }
    }
}
