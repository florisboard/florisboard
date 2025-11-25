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

package dev.patrickgold.florisboard.ime.gpt

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.widget.Toast
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.gptManager

/**
 * Activity for capturing screenshots to be used with AI vision features.
 */
class AiScreenshotActivity : Activity() {
    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1002
    }

    private val gptManager by gptManager()
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        // Request permission for screen capture
        @Suppress("DEPRECATION")
        startActivityForResult(
            mediaProjectionManager?.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION
        )
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == RESULT_OK && data != null) {
                    // Delay slightly to allow the permission dialog to dismiss
                    Handler(Looper.getMainLooper()).postDelayed({
                        captureScreen(resultCode, data)
                    }, 500)
                } else {
                    Toast.makeText(this, R.string.gpt__screenshot_failed, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun captureScreen(resultCode: Int, data: Intent) {
        try {
            val (width, height, density) = getScreenMetrics()

            imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
            
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null, null
            )

            // Capture after a small delay
            Handler(Looper.getMainLooper()).postDelayed({
                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width
                    
                    val bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    image.close()
                    
                    // Crop to actual screen size if needed
                    val croppedBitmap = if (bitmap.width > width) {
                        Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    } else {
                        bitmap
                    }
                    
                    val success = gptManager.setImageFromBitmap(croppedBitmap)
                    if (success) {
                        Toast.makeText(this, R.string.gpt__screenshot_captured, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.gpt__screenshot_failed, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, R.string.gpt__screenshot_failed, Toast.LENGTH_SHORT).show()
                }
                
                cleanup()
                finish()
            }, 100)
            
        } catch (e: Exception) {
            Toast.makeText(this, R.string.gpt__screenshot_failed, Toast.LENGTH_SHORT).show()
            cleanup()
            finish()
        }
    }

    private fun getScreenMetrics(): Triple<Int, Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            Triple(bounds.width(), bounds.height(), resources.displayMetrics.densityDpi)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        }
    }

    private fun cleanup() {
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }
}
