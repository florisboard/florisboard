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

abstract class ImeWindowDefaults {
    abstract val rowHeight: Dp

    abstract val keyboardWidthMin: Dp
    abstract val keyboardWidthMax: Dp

    abstract val keyboardHeightMin: Dp
    abstract val keyboardHeightMax: Dp

    open val smartbarHeightFactor = 0.753f
    open val keyboardHeightFactor = smartbarHeightFactor + 4f

    abstract val oneHandedPadding: Dp
    abstract val oneHandedPaddingMin: Dp

    open val resizeHandleTouchSize = 48.dp
    open val resizeHandleTouchOffsetFloating = resizeHandleTouchSize / 2
    open val resizeHandleDrawSize = 32.dp
    open val resizeHandleDrawPadding = (resizeHandleTouchSize - resizeHandleDrawSize) / 2
    open val resizeHandleDrawThickness = resizeHandleDrawSize / 4
    open val resizeHandleDrawCornerRadius = resizeHandleDrawSize / 2

    abstract class Fixed : ImeWindowDefaults() {
        val props by lazy {
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

        open val snapToCenterWidth = 32.dp

        object Portrait : Fixed() {
            override val rowHeight = 55.dp

            override val keyboardWidthMin = 250.dp
            override val keyboardWidthMax = 500.dp

            override val keyboardHeightMin = 200.dp
            override val keyboardHeightMax = 375.dp

            override val oneHandedPadding = 55.dp
            override val oneHandedPaddingMin = 40.dp
        }

        object Landscape : Fixed() {
            override val rowHeight = 45.dp

            override val keyboardWidthMin = 250.dp
            override val keyboardWidthMax = 500.dp

            override val keyboardHeightMin = 200.dp
            override val keyboardHeightMax = 375.dp

            override val oneHandedPadding = 55.dp
            override val oneHandedPaddingMin = 40.dp
        }

        companion object {
            fun of(orientation: ImeOrientation): Fixed {
                return when (orientation) {
                    ImeOrientation.PORTRAIT -> Portrait
                    ImeOrientation.LANDSCAPE -> Landscape
                }
            }
        }
    }

    abstract class Floating : ImeWindowDefaults() {
        val props by lazy {
            mapOf(
                ImeWindowMode.Floating.NORMAL to ImeWindowProps.Floating(
                    rowHeight = rowHeight,
                    keyboardWidth = 350.dp,
                    offsetLeft = 30.dp,
                    offsetBottom = 30.dp,
                ),
            )
        }

        abstract val dockToFixedHeight: Dp
        abstract val dockToFixedBorder: Dp

        object Portrait : Floating() {
            override val rowHeight = 55.dp

            override val keyboardWidthMin = 250.dp
            override val keyboardWidthMax = 500.dp

            override val keyboardHeightMin = 200.dp
            override val keyboardHeightMax = 375.dp

            override val oneHandedPadding = 55.dp
            override val oneHandedPaddingMin = 40.dp

            override val dockToFixedHeight = 80.dp
            override val dockToFixedBorder = 2.dp
        }

        object Landscape : Floating() {
            override val rowHeight = 45.dp

            override val keyboardWidthMin = 250.dp
            override val keyboardWidthMax = 500.dp

            override val keyboardHeightMin = 200.dp
            override val keyboardHeightMax = 375.dp

            override val oneHandedPadding = 55.dp
            override val oneHandedPaddingMin = 40.dp

            override val dockToFixedHeight = 50.dp
            override val dockToFixedBorder = 2.dp
        }

        companion object {
            fun of(orientation: ImeOrientation): Floating {
                return when (orientation) {
                    ImeOrientation.PORTRAIT -> Portrait
                    ImeOrientation.LANDSCAPE -> Landscape
                }
            }
        }
    }

    companion object {
        val FallbackSpec = ImeWindowSpec.Fixed(
            fixedMode = ImeWindowMode.Fixed.NORMAL,
            props = ImeWindowProps.Fixed(
                rowHeight = Fixed.Portrait.rowHeight,
                paddingLeft = 0.dp,
                paddingRight = 0.dp,
                paddingBottom = 0.dp,
            ),
            fontScale = 1f,
            rootInsets = null,
            orientation = ImeOrientation.PORTRAIT,
        )

        fun of(mode: ImeWindowMode, orientation: ImeOrientation): ImeWindowDefaults {
            return when (mode) {
                ImeWindowMode.FIXED -> Fixed.of(orientation)
                ImeWindowMode.FLOATING -> Floating.of(orientation)
            }
        }
    }
}
