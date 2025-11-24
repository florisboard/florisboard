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

import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.Bitmap
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.florisboard.lib.android.AndroidVersion
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
        var loadedImage by remember { mutableStateOf<Image?>(null) }
        val contentScale = style.contentScale()

        LaunchedEffect(imagePath) {
            if (imagePath == null) {
                loadedImage = null
                return@LaunchedEffect
            }
            val request = ImageRequest.Builder(context)
                .data(imagePath)
                .allowHardware(false)
                .build()
            val imageResult = imageLoader.execute(request)
            loadedImage = when (imageResult) {
                is SuccessResult -> when (val image = imageResult.image) {
                    is BitmapImage -> image.also { it.bitmap.prepareToDraw() }
                    is DrawableImage -> image
                    else -> null
                }
                else -> null
            }
        }

        var showSurfaceView by remember { mutableStateOf(false) }
        LifecycleResumeEffect(Unit) {
            showSurfaceView = true
            onPauseOrDispose {
                showSurfaceView = false
            }
        }

        val surfaceView = remember {
            Log.d("SnyggSurfaceView", "creating new instance")
            SurfaceView(context).apply {
                if (AndroidVersion.ATLEAST_API34_U) {
                    setSurfaceLifecycle(SurfaceView.SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT)
                }
                setZOrderOnTop(false)
                holder.setFormat(PixelFormat.TRANSPARENT)
            }
        }

        if (showSurfaceView) {
            var parentSize by remember { mutableStateOf(IntSize.Zero) }
            Box(modifier.onSizeChanged { parentSize = it }) {
                AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { surfaceView },
                    update = { view ->
                        val lp = view.layoutParams
                        if (lp == null || lp.width != parentSize.width || lp.height != parentSize.height) {
                            view.layoutParams = ViewGroup.LayoutParams(parentSize.width, parentSize.height)
                            view.requestLayout()
                        }
                        Log.d("SnyggSurfaceView", "updateSize(height=${view.height},width=${view.width})")
                    }
                )
            }
            LaunchedEffect(surfaceView, backgroundColor, loadedImage, contentScale, parentSize) {
                val image = loadedImage
                if (image is DrawableImage && image.drawable is Animatable) {
                    // Slow path, need animation
                    val fps = 30L // TODO: read frame delays from drawable
                    val animatedDrawable = image.drawable as Animatable
                    try {
                        animatedDrawable.start()
                        while (isActive) {
                            surfaceView.drawToSurface(backgroundColor, loadedImage, contentScale)
                            delay(1000L / fps)
                        }
                    } finally {
                        animatedDrawable.stop()
                    }
                } else {
                    // Fast path, render once and be done with it
                    surfaceView.drawToSurface(backgroundColor, loadedImage, contentScale)
                }
            }
        }
    }
}

private fun SurfaceView.drawToSurface(
    color: Color,
    image: Image?,
    contentScale: ContentScale,
) {
    Log.d("SnyggSurfaceView", "drawToSurface(color=$color, image=$image)")
    val surface = holder.surface
    if (!surface.isValid) {
        Log.w("SnyggSurfaceView", "drawToSurface: surface.isValid=false, may indicate state issue")
        return
    }
    val canvas = surface.lockCanvas(null)
    try {
        canvas.drawColor(color.toArgb())
        when (image) {
            is BitmapImage -> image.bitmap.drawToSurface(canvas, contentScale)
            is DrawableImage -> image.drawToSurface(canvas, contentScale)
        }
    } finally {
        surface.unlockCanvasAndPost(canvas)
    }
}

private fun Bitmap.drawToSurface(canvas: Canvas, contentScale: ContentScale) {
    val bitmap = this
    val srcSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
    val canvasSize = Size(canvas.width.toFloat(), canvas.height.toFloat())
    val scaleFactor = contentScale.computeScaleFactor(srcSize, canvasSize)
    Log.d(
        "SnyggSurfaceView",
        "drawToSurface: srcSize=$srcSize, dstSize=$canvasSize, scaleFactor=$scaleFactor"
    )
    val dstSize = srcSize.times(scaleFactor)
    val srcRect = srcSize.toRect().toAndroidRectF().toRect()
    val dstRect = dstSize.toRect().let {
        // Align center behavior
        it.translate(canvasSize.center - it.center)
    }.toAndroidRectF().toRect()
    canvas.drawBitmap(bitmap, srcRect, dstRect, null)
}

private fun DrawableImage.drawToSurface(canvas: Canvas, contentScale: ContentScale) {
    this.toBitmap().drawToSurface(canvas, contentScale)
}

