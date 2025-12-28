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

import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass

/**
 * See https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
 */
data class ImeFormFactor(
    val sizeClass: WindowSizeClass,
    val typeGuess: Type,
) {
    enum class Type {
        PHONE_PORTRAIT,
        PHONE_LANDSCAPE,
        TABLET_PORTRAIT,
        TABLET_LANDSCAPE,
        LARGE_TABLET,
        DESKTOP;
    }

    companion object {
        val Zero = ImeFormFactor(
            sizeClass = WindowSizeClass(0f, 0f),
            typeGuess = Type.PHONE_PORTRAIT,
        )

        fun of(boundsDp: DpRect): ImeFormFactor {
            typealias BP = WindowSizeClass.Companion
            val sizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(
                widthDp = boundsDp.width.value,
                heightDp = boundsDp.height.value,
            )
            val typeGuess = when {
                sizeClass.isWidthAtLeastBreakpoint(BP.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND) -> {
                    Type.DESKTOP
                }
                sizeClass.isWidthAtLeastBreakpoint(BP.WIDTH_DP_LARGE_LOWER_BOUND) -> {
                    Type.LARGE_TABLET
                }
                sizeClass.isAtLeastBreakpoint(BP.WIDTH_DP_MEDIUM_LOWER_BOUND, BP.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> {
                    Type.TABLET_PORTRAIT
                }
                sizeClass.isAtLeastBreakpoint(BP.WIDTH_DP_EXPANDED_LOWER_BOUND, BP.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> {
                    Type.TABLET_LANDSCAPE
                }
                sizeClass.isHeightAtLeastBreakpoint(BP.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> {
                    Type.PHONE_PORTRAIT
                }
                else -> Type.PHONE_LANDSCAPE
            }
            android.util.Log.d("TYPEGUESS", "$boundsDp, $typeGuess")
            return ImeFormFactor(sizeClass, typeGuess)
        }
    }
}
