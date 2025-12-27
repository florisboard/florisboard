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
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width

sealed class ImeWindowConstraints(rootInsets: ImeInsets) {
    val rootBounds = rootInsets.boundsDp

    abstract val minKeyboardWidth: Dp
    abstract val maxKeyboardWidth: Dp
    abstract val defKeyboardWidth: Dp

    abstract val minKeyboardHeight: Dp
    abstract val maxKeyboardHeight: Dp
    abstract val defKeyboardHeight: Dp

    open val smartbarHeightFactor: Float = 0.753f
    open val keyboardHeightFactor: Float by calculation { smartbarHeightFactor + 4f }
    open val defRowHeight: Dp by calculation { defKeyboardHeight / keyboardHeightFactor }

    open val resizeHandleTouchSize: Dp = 48.dp
    open val resizeHandleTouchOffsetFloating: Dp by calculation { resizeHandleTouchSize / 2 }
    open val resizeHandleDrawSize: Dp = 32.dp
    open val resizeHandleDrawPadding: Dp by calculation { (resizeHandleTouchSize - resizeHandleDrawSize) / 2 }
    open val resizeHandleDrawThickness: Dp by calculation { resizeHandleDrawSize / 4 }
    open val resizeHandleDrawCornerRadius: Dp by calculation { resizeHandleDrawSize / 2 }

    open val dockToFixedHeight: Dp by calculation {
        when (rootBounds.inferredOrientation) {
            ImeOrientation.PORTRAIT -> 80.dp
            ImeOrientation.LANDSCAPE -> 50.dp
        }
    }
    open val dockToFixedBorder: Dp = 2.dp

    abstract fun defaultProps(): ImeWindowProps

    protected fun <T> calculation(initializer: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

    sealed class Fixed(rootInsets: ImeInsets) : ImeWindowConstraints(rootInsets) {
        open val minPaddingHorizontal = 0.dp
        open val defPaddingHorizontal = 0.dp

        override val minKeyboardWidth by calculation { min(rootBounds.width - minPaddingHorizontal, 250.dp) }
        override val maxKeyboardWidth by calculation { rootBounds.width - minPaddingHorizontal }
        override val defKeyboardWidth by calculation { rootBounds.width - defPaddingHorizontal }

        override val minKeyboardHeight by calculation { max(rootBounds.height * 0.25f, 210.dp) }
        override val maxKeyboardHeight by calculation { min(rootBounds.height * 0.50f, 400.dp) }
        override val defKeyboardHeight by calculation { min(rootBounds.height * 0.45f, 260.dp) }

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
            override val minPaddingHorizontal = 50.dp
            override val defPaddingHorizontal = 70.dp

            override fun defaultProps(): ImeWindowProps.Fixed {
                return ImeWindowProps.Fixed(
                    rowHeight = defRowHeight * 0.8f,
                    paddingLeft = defPaddingHorizontal,
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

    sealed class Floating(rootInsets: ImeInsets) : ImeWindowConstraints(rootInsets) {
        override val minKeyboardWidth by calculation { min(rootBounds.width, 250.dp) }
        override val maxKeyboardWidth by calculation { min(rootBounds.width, 500.dp) }
        override val defKeyboardWidth by calculation { min(rootBounds.width, 300.dp) }

        override val minKeyboardHeight by calculation { (rootBounds.height * 0.25f).coerceAtLeast(150.dp) }
        override val maxKeyboardHeight by calculation { (rootBounds.height * 0.40f).coerceAtLeast(minKeyboardHeight) }
        override val defKeyboardHeight by calculation { (minKeyboardHeight + maxKeyboardHeight) / 2 }

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
