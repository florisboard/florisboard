/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis

@Composable
actual fun rememberImageRequest(data: Any, useFirstFrame: Boolean): ImageRequest {
    val context = LocalContext.current
    return remember(data, useFirstFrame, context) {
        ImageRequest.Builder(context)
            .data(data)
            .apply {
                if (useFirstFrame) {
                    decoderFactory(VideoFrameDecoder.Factory())
                    videoFrameMillis(0)
                }
            }
            .allowHardware(false) // slower, but hey at least it doesn't crash out of the blue
            .build()
    }
}
