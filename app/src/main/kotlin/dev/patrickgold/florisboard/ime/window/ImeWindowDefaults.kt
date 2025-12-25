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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ImeWindowDefaults {
    abstract class OrientationIndependent {
        val smartbarHeightFactor = 0.753f
        val keyboardHeightFactor = smartbarHeightFactor + 4f

        val resizeHandleTouchSize = 48.dp
        val resizeHandleTouchOffsetFloating = resizeHandleTouchSize / 2
        val resizeHandleDrawSize = 32.dp
        val resizeHandleDrawPadding = (resizeHandleTouchSize - resizeHandleDrawSize) / 2
        val resizeHandleDrawThickness = resizeHandleDrawSize / 4
        val resizeHandleDrawCornerRadius = resizeHandleDrawSize / 2
    }

    abstract class OrientationDependent : OrientationIndependent() {
        abstract val rowHeight: Dp

        abstract val keyboardWidthMin: Dp
        abstract val keyboardWidthMax: Dp

        abstract val keyboardHeightMin: Dp
        abstract val keyboardHeightMax: Dp

        abstract val oneHandedPadding: Dp
        abstract val oneHandedPaddingMin: Dp

        abstract val fixedSnapToCenterWidth: Dp

        abstract val floatingDockToFixedHeight: Dp
        abstract val floatingDockToFixedBorder: Dp

        val propsFixed by lazy {
            mapOf(
                ImeWindowMode.Fixed.NORMAL to ImeWindowProps.Fixed(
                    rowHeight = rowHeight,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                ),
                ImeWindowMode.Fixed.COMPACT to ImeWindowProps.Fixed(
                    rowHeight = rowHeight * 0.8f,
                    paddingLeft = oneHandedPadding,
                    paddingRight = 0.dp,
                    paddingBottom = (rowHeight * keyboardHeightFactor) * 0.2f,
                ),
                ImeWindowMode.Fixed.THUMBS to ImeWindowProps.Fixed(
                    rowHeight = rowHeight,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                ),
            )
        }

        val propsFloating by lazy {
            mapOf(
                ImeWindowMode.Floating.NORMAL to ImeWindowProps.Floating(
                    rowHeight = rowHeight,
                    keyboardWidth = 350.dp,
                    offsetLeft = 30.dp,
                    offsetBottom = 30.dp,
                ),
            )
        }
    }

    object Portrait : OrientationDependent() {
        override val rowHeight = 55.dp

        override val keyboardWidthMin = 250.dp
        override val keyboardWidthMax = 500.dp

        override val keyboardHeightMin = 200.dp
        override val keyboardHeightMax = 375.dp

        override val oneHandedPadding = 55.dp
        override val oneHandedPaddingMin = 40.dp

        override val fixedSnapToCenterWidth = 32.dp

        override val floatingDockToFixedHeight = 80.dp
        override val floatingDockToFixedBorder = 2.dp
    }

    object Landscape : OrientationDependent() {
        override val rowHeight = 45.dp

        override val keyboardWidthMin = 250.dp
        override val keyboardWidthMax = 500.dp

        override val keyboardHeightMin = 200.dp
        override val keyboardHeightMax = 375.dp

        override val oneHandedPadding = 55.dp
        override val oneHandedPaddingMin = 40.dp

        override val fixedSnapToCenterWidth = 32.dp

        override val floatingDockToFixedHeight = 50.dp
        override val floatingDockToFixedBorder = 2.dp
    }

    val FallbackSpec = ImeWindowSpec.Fixed(
        mode = ImeWindowMode.Fixed.NORMAL,
        props = ImeWindowProps.Fixed(
            rowHeight = Portrait.rowHeight,
            paddingLeft = 0.dp,
            paddingRight = 0.dp,
            paddingBottom = 0.dp,
        ),
        rootInsets = null,
        orientation = ImeOrientation.PORTRAIT,
    )

    fun of(orientation: ImeOrientation): OrientationDependent = when (orientation) {
        ImeOrientation.PORTRAIT -> Portrait
        ImeOrientation.LANDSCAPE -> Landscape
    }
}
