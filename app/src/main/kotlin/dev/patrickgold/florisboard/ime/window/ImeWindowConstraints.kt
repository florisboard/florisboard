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
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width

sealed class ImeWindowConstraints(rootInsets: ImeInsets.Root) {
    val rootBounds = rootInsets.boundsDp
    val formFactor = rootInsets.formFactor

    abstract val minKeyboardWidth: Dp
    abstract val maxKeyboardWidth: Dp
    abstract val defKeyboardWidth: Dp

    abstract val minKeyboardHeight: Dp
    abstract val maxKeyboardHeight: Dp
    abstract val defKeyboardHeight: Dp

    open val smartbarHeightFactor: Float = 0.753f
    open val keyboardHeightFactor: Float by calculation { smartbarHeightFactor + 4f }

    open val resizeHandleTouchSize: Dp = 48.dp
    open val resizeHandleTouchOffsetFloating: Dp by calculation { resizeHandleTouchSize / 2 }
    open val resizeHandleDrawSize: Dp = 32.dp
    open val resizeHandleDrawPadding: Dp by calculation { (resizeHandleTouchSize - resizeHandleDrawSize) / 2 }
    open val resizeHandleDrawThickness: Dp by calculation { resizeHandleDrawSize / 4 }
    open val resizeHandleDrawCornerRadius: Dp by calculation { resizeHandleDrawSize / 2 }

    open val dockToFixedHeight: Dp by calculation {
        when (formFactor.typeGuess) {
            ImeFormFactor.Type.DESKTOP,
            ImeFormFactor.Type.LARGE_TABLET,
            ImeFormFactor.Type.TABLET_LANDSCAPE,
            ImeFormFactor.Type.TABLET_PORTRAIT -> 80.dp
            ImeFormFactor.Type.PHONE_LANDSCAPE -> 50.dp
            ImeFormFactor.Type.PHONE_PORTRAIT -> 80.dp
        }
    }
    open val dockToFixedBorder: Dp = 2.dp

    abstract fun defaultProps(): ImeWindowProps

    protected fun <T> calculation(initializer: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

    sealed class Fixed(rootInsets: ImeInsets.Root) : ImeWindowConstraints(rootInsets) {
        protected open val desiredMinPaddingHorizontal = 0.dp
        protected open val desiredDefPaddingHorizontal = 0.dp
        open val minPaddingHorizontal by calculation { rootBounds.width - maxKeyboardWidth }
        open val defPaddingHorizontal by calculation { rootBounds.width - defKeyboardWidth }

        override val minKeyboardWidth by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.TABLET_PORTRAIT -> min(rootBounds.width - desiredMinPaddingHorizontal, 400.dp)
                else -> min(rootBounds.width - desiredMinPaddingHorizontal, 250.dp)
            }.coerceAtLeast(0.dp).coerceAtMost(rootBounds.width)
        }
        override val maxKeyboardWidth by calculation {
            (rootBounds.width - desiredMinPaddingHorizontal).coerceAtLeast(minKeyboardWidth)
        }
        override val defKeyboardWidth by calculation {
            (rootBounds.width - desiredDefPaddingHorizontal).coerceAtLeast(minKeyboardWidth)
        }

        override val minKeyboardHeight by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.height * 0.32f).coerceAtLeast(150.dp)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.height * 0.20f).coerceAtLeast(150.dp)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.height * 0.42f).coerceAtLeast(150.dp)
                ImeFormFactor.Type.PHONE_PORTRAIT -> (rootBounds.height * 0.23f).coerceAtLeast(210.dp)
            }.coerceAtMost(rootBounds.height)
        }
        override val maxKeyboardHeight by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.height * 0.70f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.height * 0.38f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.height * 0.66f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.PHONE_PORTRAIT -> (rootBounds.height * 0.46f).coerceAtLeast(minKeyboardHeight)
            }
        }
        override val defKeyboardHeight by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.height * 0.42f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.height * 0.26f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.height * 0.55f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.PHONE_PORTRAIT -> (rootBounds.height * 0.31f).coerceAtLeast(minKeyboardHeight)
            }
        }

        open val snapToCenterWidth: Dp by lazy {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP -> 80.dp
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE,
                ImeFormFactor.Type.TABLET_PORTRAIT -> 64.dp
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 48.dp
                ImeFormFactor.Type.PHONE_PORTRAIT -> 32.dp
            }
        }

        abstract override fun defaultProps(): ImeWindowProps.Fixed

        class Normal(rootInsets: ImeInsets.Root) : Fixed(rootInsets) {
            override fun defaultProps(): ImeWindowProps.Fixed {
                return ImeWindowProps.Fixed(
                    keyboardHeight = defKeyboardHeight,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                ).constrained(this)
            }
        }

        class Compact(rootInsets: ImeInsets.Root) : Fixed(rootInsets) {
            override val defKeyboardHeight by calculation {
                (super.defKeyboardHeight * 0.8f).coerceAtLeast(minKeyboardHeight)
            }

            override val desiredMinPaddingHorizontal by calculation {
                when (formFactor.typeGuess) {
                    ImeFormFactor.Type.DESKTOP,
                    ImeFormFactor.Type.LARGE_TABLET,
                    ImeFormFactor.Type.TABLET_LANDSCAPE,
                    ImeFormFactor.Type.TABLET_PORTRAIT,
                    ImeFormFactor.Type.PHONE_LANDSCAPE,
                    ImeFormFactor.Type.PHONE_PORTRAIT -> 50.dp
                }
            }
            override val desiredDefPaddingHorizontal by calculation {
                when (formFactor.typeGuess) {
                    ImeFormFactor.Type.DESKTOP,
                    ImeFormFactor.Type.LARGE_TABLET,
                    ImeFormFactor.Type.TABLET_LANDSCAPE,
                    ImeFormFactor.Type.TABLET_PORTRAIT,
                    ImeFormFactor.Type.PHONE_LANDSCAPE,
                    ImeFormFactor.Type.PHONE_PORTRAIT -> 70.dp
                }
            }

            override fun defaultProps(): ImeWindowProps.Fixed {
                return ImeWindowProps.Fixed(
                    keyboardHeight = defKeyboardHeight,
                    paddingLeft = defPaddingHorizontal,
                    paddingRight = 0.dp,
                    paddingBottom = super.defKeyboardHeight - defKeyboardHeight,
                ).constrained(this)
            }
        }

        class Thumbs(rootInsets: ImeInsets.Root) : Fixed(rootInsets) {
            override fun defaultProps(): ImeWindowProps.Fixed {
                return ImeWindowProps.Fixed(
                    keyboardHeight = defKeyboardHeight,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                ).constrained(this)
            }
        }
    }

    sealed class Floating(rootInsets: ImeInsets.Root) : ImeWindowConstraints(rootInsets) {
        override val minKeyboardWidth by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.width * 0.25f).coerceAtLeast(150.dp)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.width * 0.40f).coerceAtLeast(150.dp)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.width * 0.28f).coerceAtLeast(150.dp)
                ImeFormFactor.Type.PHONE_PORTRAIT -> rootBounds.width.coerceAtMost(250.dp)
            }.coerceAtMost(rootBounds.width)
        }
        override val maxKeyboardWidth by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.width * 0.48f).coerceAtLeast(minKeyboardWidth)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.width * 0.70f).coerceAtLeast(minKeyboardWidth)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.width * 0.40f).coerceAtLeast(minKeyboardWidth)
                ImeFormFactor.Type.PHONE_PORTRAIT -> rootBounds.width.coerceAtMost(500.dp)
            }
        }
        override val defKeyboardWidth by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.width * 0.30f).coerceAtLeast(minKeyboardWidth)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.width * 0.45f).coerceAtLeast(minKeyboardWidth)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.width * 0.28f).coerceAtLeast(minKeyboardWidth)
                ImeFormFactor.Type.PHONE_PORTRAIT -> rootBounds.width.coerceAtMost(300.dp)
            }
        }

        override val minKeyboardHeight by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.height * 0.30f).coerceAtLeast(150.dp)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.height * 0.18f).coerceAtLeast(150.dp)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.height * 0.20f).coerceAtLeast(130.dp)
                ImeFormFactor.Type.PHONE_PORTRAIT -> (rootBounds.height * 0.20f).coerceAtLeast(150.dp)
            }.coerceAtMost(rootBounds.height)
        }
        override val maxKeyboardHeight by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.height * 0.55f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.height * 0.35f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.height * 0.55f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.PHONE_PORTRAIT -> (rootBounds.height * 0.40f).coerceAtLeast(minKeyboardHeight)
            }
        }
        override val defKeyboardHeight by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> (rootBounds.height * 0.35f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.TABLET_PORTRAIT -> (rootBounds.height * 0.22f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.PHONE_LANDSCAPE -> (rootBounds.height * 0.25f).coerceAtLeast(minKeyboardHeight)
                ImeFormFactor.Type.PHONE_PORTRAIT -> (rootBounds.height * 0.25f).coerceAtLeast(minKeyboardHeight)
            }
        }

        abstract override fun defaultProps(): ImeWindowProps.Floating

        class Normal(rootInsets: ImeInsets.Root) : Floating(rootInsets) {
            override fun defaultProps(): ImeWindowProps.Floating {
                return ImeWindowProps.Floating(
                    keyboardHeight = defKeyboardHeight,
                    keyboardWidth = defKeyboardWidth,
                    offsetLeft = 60.dp,
                    offsetBottom = 60.dp,
                ).constrained(this)
            }
        }
    }

    companion object {
        val FallbackSpec = ImeWindowSpec.Fixed(
            fixedMode = ImeWindowMode.Fixed.NORMAL,
            props = ImeWindowProps.Fixed(
                keyboardHeight = 250.dp,
                paddingLeft = 0.dp,
                paddingRight = 0.dp,
                paddingBottom = 0.dp,
            ),
            fontScale = 1f,
            constraints = Fixed.Normal(ImeInsets.Root.Zero),
        )

        fun of(rootInsets: ImeInsets.Root, fixedMode: ImeWindowMode.Fixed): Fixed {
            return when (fixedMode) {
                ImeWindowMode.Fixed.NORMAL -> Fixed.Normal(rootInsets)
                ImeWindowMode.Fixed.COMPACT -> Fixed.Compact(rootInsets)
                ImeWindowMode.Fixed.THUMBS -> Fixed.Thumbs(rootInsets)
            }
        }

        fun of(rootInsets: ImeInsets.Root, floatingMode: ImeWindowMode.Floating): Floating {
            return when (floatingMode) {
                ImeWindowMode.Floating.NORMAL -> Floating.Normal(rootInsets)
            }
        }
    }
}
