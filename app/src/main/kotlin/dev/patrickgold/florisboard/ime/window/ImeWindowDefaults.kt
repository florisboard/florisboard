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

import androidx.compose.ui.unit.dp

object ImeWindowDefaults {
    val RowHeight = ByOrientation(portrait = 55.dp, landscape = 45.dp)
    val CompactOffset = ByOrientation(portrait = 55.dp, landscape = 55.dp)

    const val SmartbarHeightFactor = 0.753f
    const val KeyboardHeightFactor = SmartbarHeightFactor + 4f

    val MinKeyboardWidth = 250.dp
    val MaxKeyboardWidth = 500.dp

    val MinKeyboardHeight = 175.dp
    val MaxKeyboardHeight = 375.dp

    val FloatingDockToFixedHeight = ByOrientation(portrait = 80.dp, landscape = 50.dp)
    val FloatingDockToFixedBorder = ByOrientation(portrait = 2.dp, landscape = 2.dp)

    val ResizeHandleTouchSize = 64.dp
    val ResizeHandleTouchOffsetFloating = ResizeHandleTouchSize / 2
    val ResizeHandleDrawSize = 32.dp
    val ResizeHandleDrawPadding = (ResizeHandleTouchSize - ResizeHandleDrawSize) / 2
    val ResizeHandleDrawThickness = ResizeHandleDrawSize / 4
    val ResizeHandleDrawCornerRadius = ResizeHandleDrawSize / 2

    val FallbackSpec = ImeWindowSpec.Fixed(
        mode = ImeWindowMode.Fixed.NORMAL,
        props = ImeWindowProps.Fixed(
            rowHeight = RowHeight.portrait,
            paddingLeft = 0.dp,
            paddingRight = 0.dp,
            paddingBottom = 0.dp,
        ),
        rootInsets = null,
        orientation = ImeOrientation.PORTRAIT,
    )

    val PropsFixedPortrait = mapOf(
        ImeWindowMode.Fixed.NORMAL to ImeWindowProps.Fixed(
            rowHeight = RowHeight.portrait,
            paddingLeft = 0.dp,
            paddingRight = 0.dp,
            paddingBottom = 0.dp,
        ),
        ImeWindowMode.Fixed.COMPACT to ImeWindowProps.Fixed(
            rowHeight = RowHeight.portrait * 0.8f,
            paddingLeft = CompactOffset.portrait,
            paddingRight = 0.dp,
            paddingBottom = (RowHeight.portrait * KeyboardHeightFactor) * 0.2f,
        ),
        ImeWindowMode.Fixed.THUMBS to ImeWindowProps.Fixed(
            rowHeight = RowHeight.portrait,
            paddingLeft = 0.dp,
            paddingRight = 0.dp,
            paddingBottom = 0.dp,
        ),
    )

    val PropsFloatingPortrait = mapOf(
        ImeWindowMode.Floating.NORMAL to ImeWindowProps.Floating(
            rowHeight = RowHeight.portrait,
            keyboardWidth = 350.dp,
            offsetLeft = 30.dp,
            offsetBottom = 30.dp,
        ),
    )

    val PropsFixedLandscape = PropsFixedPortrait // TODO

    val PropsFloatingLandscape = PropsFloatingPortrait // TODO

    data class ByOrientation<T>(
        val portrait: T,
        val landscape: T,
    ) {
        fun select(orientation: ImeOrientation): T = when (orientation) {
            ImeOrientation.PORTRAIT -> portrait
            ImeOrientation.LANDSCAPE -> landscape
        }
    }
}
