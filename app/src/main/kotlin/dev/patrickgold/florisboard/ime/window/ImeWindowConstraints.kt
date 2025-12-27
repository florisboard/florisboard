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
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width

abstract class ImeWindowConstraints(rootInsets: ImeInsets) {
    val rootBounds = rootInsets.boundsDp

    abstract val defKeyboardWidth: Dp
    abstract val minKeyboardWidth: Dp
    abstract val maxKeyboardWidth: Dp

    abstract val defKeyboardHeight: Dp
    abstract val minKeyboardHeight: Dp
    abstract val maxKeyboardHeight: Dp

    open val smartbarHeightFactor: Float = 0.753f
    open val keyboardHeightFactor: Float by lazy { smartbarHeightFactor + 4f }
    open val defRowHeight: Dp by lazy { defKeyboardHeight / keyboardHeightFactor }

    open val resizeHandleTouchSize: Dp = 48.dp
    open val resizeHandleTouchOffsetFloating: Dp by lazy { resizeHandleTouchSize / 2 }
    open val resizeHandleDrawSize: Dp = 32.dp
    open val resizeHandleDrawPadding: Dp by lazy { (resizeHandleTouchSize - resizeHandleDrawSize) / 2 }
    open val resizeHandleDrawThickness: Dp by lazy { resizeHandleDrawSize / 4 }
    open val resizeHandleDrawCornerRadius: Dp by lazy { resizeHandleDrawSize / 2 }

    open val dockToFixedHeight: Dp by lazy {
        when (rootBounds.inferredOrientation) {
            ImeOrientation.PORTRAIT -> 80.dp
            ImeOrientation.LANDSCAPE -> 50.dp
        }
    }
    open val dockToFixedBorder: Dp = 2.dp

    abstract fun defaultProps(): ImeWindowProps

    abstract class Fixed(rootInsets: ImeInsets) : ImeWindowConstraints(rootInsets) {
        override val defKeyboardWidth = rootBounds.width
        override val minKeyboardWidth = min(rootBounds.width, 250.dp)
        override val maxKeyboardWidth = rootBounds.width

        override val defKeyboardHeight = min(rootBounds.height * 0.45f, 270.dp)
        override val minKeyboardHeight = max(rootBounds.height * 0.25f, 210.dp)
        override val maxKeyboardHeight = min(rootBounds.height * 0.50f, 400.dp)

        open val snapToCenterWidth: Dp = 32.dp

        abstract override fun defaultProps(): ImeWindowProps.Fixed

        class Normal(rootInsets: ImeInsets) : Fixed(rootInsets) {
            override fun defaultProps(): ImeWindowProps.Fixed {
                return ImeWindowProps.Fixed(
                    rowHeight = defRowHeight,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                )
            }
        }

        class Compact(rootInsets: ImeInsets) : Fixed(rootInsets) {
            override fun defaultProps(): ImeWindowProps.Fixed {
                return ImeWindowProps.Fixed(
                    rowHeight = defRowHeight * 0.8f,
                    paddingLeft = 55.dp,
                    paddingRight = 0.dp,
                    paddingBottom = defRowHeight * 0.2f * keyboardHeightFactor,
                )
            }
        }

        class Thumbs(rootInsets: ImeInsets) : Fixed(rootInsets) {
            override fun defaultProps(): ImeWindowProps.Fixed {
                return ImeWindowProps.Fixed(
                    rowHeight = defRowHeight,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                )
            }
        }
    }

    abstract class Floating(rootInsets: ImeInsets) : ImeWindowConstraints(rootInsets) {
        override val defKeyboardWidth = min(rootBounds.width * 0.8f, 300.dp)
        override val minKeyboardWidth = max(rootBounds.width * 0.5f, 250.dp)
        override val maxKeyboardWidth = min(rootBounds.width * 0.8f, 500.dp)

        override val defKeyboardHeight = min(rootBounds.height * 0.35f, 270.dp)
        override val minKeyboardHeight = max(rootBounds.height * 0.25f, 210.dp)
        override val maxKeyboardHeight = min(rootBounds.height * 0.40f, 360.dp)

        abstract override fun defaultProps(): ImeWindowProps.Floating

        class Normal(rootInsets: ImeInsets) : Floating(rootInsets) {
            override fun defaultProps(): ImeWindowProps.Floating {
                return ImeWindowProps.Floating(
                    rowHeight = defRowHeight,
                    keyboardWidth = defKeyboardWidth,
                    offsetLeft = 30.dp,
                    offsetBottom = 30.dp,
                )
            }
        }
    }

    companion object {
        val FallbackSpec = ImeWindowSpec.Fixed(
            fixedMode = ImeWindowMode.Fixed.NORMAL,
            props = ImeWindowProps.Fixed(
                rowHeight = 50.dp,
                paddingLeft = 0.dp,
                paddingRight = 0.dp,
                paddingBottom = 0.dp,
            ),
            fontScale = 1f,
            constraints = Fixed.Normal(ImeInsets.Zero),
        )

        fun of(fixedMode: ImeWindowMode.Fixed, rootInsets: ImeInsets): Fixed {
            return when (fixedMode) {
                ImeWindowMode.Fixed.NORMAL -> Fixed.Normal(rootInsets)
                ImeWindowMode.Fixed.COMPACT -> Fixed.Compact(rootInsets)
                ImeWindowMode.Fixed.THUMBS -> Fixed.Thumbs(rootInsets)
            }
        }

        fun of(floatingMode: ImeWindowMode.Floating, rootInsets: ImeInsets): Floating {
            return when (floatingMode) {
                ImeWindowMode.Floating.NORMAL -> Floating.Normal(rootInsets)
            }
        }
    }
}
