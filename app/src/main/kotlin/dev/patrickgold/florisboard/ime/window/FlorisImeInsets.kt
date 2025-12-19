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

package dev.patrickgold.florisboard.ime.window

import android.inputmethodservice.InputMethodService
import androidx.compose.ui.unit.IntRect

data class FlorisImeInsets(
    val rootBounds: IntRect,
    val windowBounds: IntRect,
) {
    fun applyTo(
        outInsets: InputMethodService.Insets,
        windowMode: FlorisImeWindowMode,
        isFullscreenInputRequired: Boolean,
    ) {
        when (windowMode.windowType) {
            FlorisImeWindowType.FIXED -> {
                outInsets.contentTopInsets = windowBounds.top
                outInsets.visibleTopInsets = windowBounds.top
            }
            FlorisImeWindowType.FLOATING -> {
                outInsets.contentTopInsets = rootBounds.bottom
                outInsets.visibleTopInsets = rootBounds.bottom
            }
        }
        when {
            isFullscreenInputRequired -> {
                outInsets.touchableRegion.set(
                    rootBounds.left,
                    rootBounds.top,
                    rootBounds.right,
                    rootBounds.bottom,
                )
            }
            else -> {
                outInsets.touchableRegion.set(
                    windowBounds.left,
                    windowBounds.top,
                    windowBounds.right,
                    windowBounds.bottom,
                )
            }
        }
        outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION
    }
}
