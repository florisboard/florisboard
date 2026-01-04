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
import android.graphics.PorterDuff
import android.graphics.drawable.Animatable
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceZOrder
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toRect
import coil3.Bitmap
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import android.graphics.Path as PlatformPath
import android.view.Surface as PlatformSurface

private const val TAG = "SnyggSurfaceView"
private const val DEBUG = false

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
        val density = LocalDensity.current

        val backgroundColor by rememberUpdatedState(style.background(Color.Black))
        val contentScale by rememberUpdatedState(style.contentScale())
        val shape by rememberUpdatedState(style.shape())
        val shadowColor by rememberUpdatedState(style.shadowColor(default = Color.Black))
        val shadowElevation by rememberUpdatedState(style.shadowElevation(default = 8.dp))

        val imageLoader = SingletonImageLoader.get(context)
        val imagePath = remember(style, assetResolver) {
            style.backgroundImage.uriOrNull()?.let { imageUri ->
                assetResolver.resolveAbsolutePath(imageUri).getOrNull()
            }
        }
        var loadedImage by remember { mutableStateOf<Image?>(null, referentialEqualityPolicy()) }

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

        DisposableEffect(loadedImage) {
            loadedImage.startIfIsAnimatable()
            onDispose { loadedImage.stopIfIsAnimatable() }
        }

        Box(modifier) {
            AndroidExternalSurface(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        this.shape = shape
                        this.shadowElevation = with(density) { shadowElevation.toPx() }
                        this.ambientShadowColor = shadowColor
                        this.spotShadowColor = shadowColor
                        this.clip = true
                    },
                isOpaque = false,
                zOrder = AndroidExternalSurfaceZOrder.Behind,
            ) {
                onSurface { surface, initWidth, initHeight ->
                    if (DEBUG) Log.d(TAG, "onCreated: w=$initWidth h=$initHeight")
                    var size = Size(initWidth.toFloat(), initHeight.toFloat())
                    surface.onChanged { newWidth, newHeight ->
                        size = Size(newWidth.toFloat(), newHeight.toFloat())
                        if (DEBUG) Log.d(TAG, "onChanged: w=$newWidth h=$newHeight")
                    }
                    surface.onDestroyed {
                        if (DEBUG) Log.d(TAG, "onDestroyed")
                    }
                    withContext(Dispatchers.Default) {
                        while (true) {
                            withFrameNanos {
                                surface.drawLocked { canvas ->
                                    canvas.doDraw(
                                        backgroundColor = backgroundColor,
                                        image = loadedImage,
                                        contentScale = contentScale,
                                        shape = shape,
                                        size = size,
                                        density = density,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Image?.startIfIsAnimatable() {
    if (this is DrawableImage) {
        val animatedDrawable = drawable
        if (animatedDrawable is Animatable) {
            animatedDrawable.start()
        }
    }
}

private fun Image?.stopIfIsAnimatable() {
    if (this is DrawableImage) {
        val animatedDrawable = drawable
        if (animatedDrawable is Animatable) {
            animatedDrawable.stop()
        }
    }
}

@OptIn(ExperimentalContracts::class)
private inline fun PlatformSurface.drawLocked(block: (canvas: Canvas) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    val canvas: Canvas?
    try {
        canvas = lockCanvas(null)
    } catch (e: Throwable) {
        if (DEBUG) Log.d(TAG, "failed to lock canvas: ${e.message}")
        return
    }
    if (canvas == null) {
        return
    }
    try {
        block(canvas)
    } finally {
        try {
            unlockCanvasAndPost(canvas)
        } catch (e: Throwable) {
            if (DEBUG) Log.d(TAG, "failed to unlock canvas: ${e.message}")
        }
    }
}

private fun Canvas.doDraw(
    backgroundColor: Color,
    image: Image?,
    contentScale: ContentScale,
    shape: Shape,
    size: Size,
    density: Density,
) {
    if (DEBUG) Log.d(TAG, "doDraw: size=$size, backgroundColor=$backgroundColor image=$image shape=$shape")
    drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    val clipPath = shape.toAndroidPath(size, density) // TODO cache between draws
    clipPath(clipPath)
    drawColor(backgroundColor.toArgb())
    when (image) {
        is BitmapImage -> image.bitmap.drawToSurface(this, contentScale, size)
        is DrawableImage -> image.drawToSurface(this, contentScale, size)
    }
}

private fun Bitmap.drawToSurface(canvas: Canvas, contentScale: ContentScale, size: Size) {
    val bitmap = this
    val srcSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
    val scaleFactor = contentScale.computeScaleFactor(srcSize, size)
    val dstSize = srcSize.times(scaleFactor)
    val srcRect = srcSize.toRect().toAndroidRectF().toRect()
    val dstRect = dstSize.toRect().let {
        // Align center behavior
        it.translate(size.center - it.center)
    }.toAndroidRectF().toRect()
    canvas.drawBitmap(bitmap, srcRect, dstRect, null)
}

private fun DrawableImage.drawToSurface(canvas: Canvas, contentScale: ContentScale, size: Size) {
    this.toBitmap().drawToSurface(canvas, contentScale, size)
}

private fun Shape.toAndroidPath(size: Size, density: Density): PlatformPath {
    val outline = createOutline(
        size = size,
        layoutDirection = LayoutDirection.Ltr,
        density = density,
    )
    val path = when (outline) {
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Generic -> outline.path
    }
    return path.asAndroidPath()
}
