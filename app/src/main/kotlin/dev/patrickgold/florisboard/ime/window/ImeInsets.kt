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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect

data class ImeInsets(
    val rootBoundsDp: DpRect,
    val rootBoundsPx: IntRect,
    val windowBoundsDp: DpRect,
    val windowBoundsPx: IntRect,
) {
    fun applyTo(
        outInsets: InputMethodService.Insets,
        windowConfig: ImeWindowConfig,
        isFullscreenInputRequired: Boolean,
    ) {
        when (windowConfig.mode) {
            ImeWindowMode.FIXED -> {
                outInsets.contentTopInsets = windowBoundsPx.top
                outInsets.visibleTopInsets = windowBoundsPx.top
            }
            ImeWindowMode.FLOATING -> {
                outInsets.contentTopInsets = rootBoundsPx.bottom
                outInsets.visibleTopInsets = rootBoundsPx.bottom
            }
        }
        when {
            isFullscreenInputRequired -> {
                outInsets.touchableRegion.set(
                    rootBoundsPx.left,
                    rootBoundsPx.top,
                    rootBoundsPx.right,
                    rootBoundsPx.bottom,
                )
            }
            else -> {
                outInsets.touchableRegion.set(
                    windowBoundsPx.left,
                    windowBoundsPx.top,
                    windowBoundsPx.right,
                    windowBoundsPx.bottom,
                )
            }
        }
        outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION
    }

    companion object {
        private val InfiniteDpRect = DpRect(-Dp.Infinity, -Dp.Infinity, Dp.Infinity, Dp.Infinity)

        val Indeterminate = ImeInsets(
            rootBoundsDp = InfiniteDpRect,
            rootBoundsPx = IntRect.Zero,
            windowBoundsDp = InfiniteDpRect,
            windowBoundsPx = IntRect.Zero,
        )
    }
}
