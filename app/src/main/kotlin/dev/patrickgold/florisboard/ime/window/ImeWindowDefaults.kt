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
    val RowHeightPortrait = 55.dp
    val RowHeightLandscape = 45.dp
    val CompactOffsetPortrait = 55.dp
    val CompactOffsetLandscape = 55.dp

    const val SmartbarHeightFactor = 0.753f
    const val KeyboardHeightFactor = SmartbarHeightFactor + 4f

    val MinKeyboardWidth = 200.dp
    val MaxKeyboardWidth = 500.dp

    val MinKeyboardHeight = 175.dp
    val MaxKeyboardHeight = 375.dp

    val FloatingDockHeightPortrait = 80.dp
    val FloatingDockHeightLandscape = 50.dp

    val ResizeHandleTouchSize = 32.dp
    val ResizeHandleThickness = ResizeHandleTouchSize / 4
    val ResizeHandleCornerRadius = ResizeHandleThickness / 2

    val FallbackSpec = ImeWindowSpec.Fixed(
        mode = ImeWindowMode.Fixed.NORMAL,
        props = ImeWindowProps.Fixed(
            rowHeight = RowHeightPortrait,
            paddingLeft = 0.dp,
            paddingRight = 0.dp,
            paddingBottom = 0.dp,
        ),
        rootInsets = null,
        orientation = ImeOrientation.PORTRAIT,
    )

    val PropsFixedPortrait = mapOf(
        ImeWindowMode.Fixed.NORMAL to ImeWindowProps.Fixed(
            rowHeight = RowHeightPortrait,
            paddingLeft = 0.dp,
            paddingRight = 0.dp,
            paddingBottom = 0.dp,
        ),
        ImeWindowMode.Fixed.COMPACT to ImeWindowProps.Fixed(
            rowHeight = RowHeightPortrait * 0.8f,
            paddingLeft = CompactOffsetPortrait,
            paddingRight = 0.dp,
            paddingBottom = (RowHeightPortrait * KeyboardHeightFactor) * 0.2f,
        ),
        ImeWindowMode.Fixed.THUMBS to ImeWindowProps.Fixed(
            rowHeight = RowHeightPortrait,
            paddingLeft = 0.dp,
            paddingRight = 0.dp,
            paddingBottom = 0.dp,
        ),
    )

    val PropsFloatingPortrait = mapOf(
        ImeWindowMode.Floating.NORMAL to ImeWindowProps.Floating(
            rowHeight = RowHeightPortrait,
            keyboardWidth = 350.dp,
            offsetLeft = 30.dp,
            offsetBottom = 30.dp,
        ),
    )

    val PropsFixedLandscape = PropsFixedPortrait // TODO

    val PropsFloatingLandscape = PropsFloatingPortrait // TODO
}
