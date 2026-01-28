/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width

/**
 * The window constraints describe all relevant sizing minimums, maximums, defaults, and scaling factors
 * for given root bounds and window mode + sub-mode.
 *
 * From a definition standpoint, constraints follows a strict inheritance hierarchy, mirroring the window
 * mode + sub-mode model, with this class serving as the base class for all modes and sub-modes.
 *
 * All calculations should be wrapped in [calculation], to ensure they are lazily evaluated in-case a child
 * class overrides a property used in the calculation.
 *
 * All constraints for a specific property and mode + sub-mode should be designed in a way that the
 * condition `0dp >= min >= def >= max` always holds. Any potential floating point rounding errors must
 * be handled by the calculation, e.g. by using proper coerce{AtLeast,AtMost,In} rules.
 *
 * Additionally, all of the above must be valid for all non-negative root bounds, including zero bounds.
 * Negative bounds, or non-real bounds are undefined behavior and most likely lead to a crash.
 */
sealed class ImeWindowConstraints(rootInsets: ImeInsets.Root) {
    val rootBounds = rootInsets.boundsDp
    val formFactor = rootInsets.formFactor

    protected val baselineScreen = BaselineScreens.getValue(formFactor.typeGuess)

    abstract val minKeyboardWidth: Dp
    abstract val maxKeyboardWidth: Dp
    abstract val defKeyboardWidth: Dp

    abstract val minKeyboardHeight: Dp
    abstract val maxKeyboardHeight: Dp
    abstract val defKeyboardHeight: Dp

    abstract val defKeyMarginH: Dp
    abstract val defKeyMarginV: Dp

    open val baselineRowCount: Float = 4f
    open val smartbarDynamicScalingFactor = 0.20f
    open val smartbarStaticScalingFactor by calculation { 0.753f - smartbarDynamicScalingFactor }

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

    abstract val defaultProps: ImeWindowProps

    protected fun <T> calculation(initializer: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

    sealed class Fixed(rootInsets: ImeInsets.Root) : ImeWindowConstraints(rootInsets) {
        protected open val desiredMinPaddingHorizontal = 0.dp
        protected open val desiredDefPaddingHorizontal = 0.dp
        open val minPaddingHorizontal by calculation { rootBounds.width - maxKeyboardWidth }
        open val maxPaddingHorizontal by calculation { rootBounds.width - minKeyboardWidth }
        open val defPaddingHorizontal by calculation { rootBounds.width - defKeyboardWidth }

        override val minKeyboardWidth by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.TABLET_PORTRAIT -> min(rootBounds.width - desiredMinPaddingHorizontal, 400.dp)
                else -> min(rootBounds.width - desiredMinPaddingHorizontal, 250.dp)
            }.coerceIn(0.dp, rootBounds.width)
        }
        override val maxKeyboardWidth by calculation {
            (rootBounds.width - desiredMinPaddingHorizontal).coerceAtLeast(minKeyboardWidth)
        }
        override val defKeyboardWidth by calculation {
            (rootBounds.width - desiredDefPaddingHorizontal).coerceAtLeast(minKeyboardWidth)
        }

        override val minKeyboardHeight by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.27f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.17f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.35f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.16f
            }
            (baselineScreen.height * factor).coerceAtMost(rootBounds.height)
        }
        override val maxKeyboardHeight by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.64f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.38f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.66f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.46f
            }
            (baselineScreen.height * factor).coerceIn(minKeyboardHeight, rootBounds.height)
        }
        override val defKeyboardHeight by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.35f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.22f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.47f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.26f
            }
            (baselineScreen.height * factor).coerceIn(minKeyboardHeight, maxKeyboardHeight)
        }

        override val defKeyMarginH by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET -> 6.dp
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 2.dp
                ImeFormFactor.Type.TABLET_PORTRAIT -> 5.dp
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 2.dp
                ImeFormFactor.Type.PHONE_PORTRAIT -> 2.dp
            }
        }
        override val defKeyMarginV by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET -> 6.dp
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 5.dp
                ImeFormFactor.Type.TABLET_PORTRAIT -> 5.dp
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 5.dp
                ImeFormFactor.Type.PHONE_PORTRAIT -> 5.dp
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

        abstract override val defaultProps: ImeWindowProps.Fixed

        class Normal(rootInsets: ImeInsets.Root) : Fixed(rootInsets) {
            override val defaultProps by calculation {
                ImeWindowProps.Fixed(
                    keyboardHeight = defKeyboardHeight,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                )
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

            override val defaultProps by calculation {
                ImeWindowProps.Fixed(
                    keyboardHeight = defKeyboardHeight,
                    paddingLeft = defPaddingHorizontal,
                    paddingRight = 0.dp,
                    paddingBottom = super.defKeyboardHeight - defKeyboardHeight,
                )
            }
        }

        class Thumbs(rootInsets: ImeInsets.Root) : Fixed(rootInsets) {
            override val defaultProps by calculation {
                ImeWindowProps.Fixed(
                    keyboardHeight = defKeyboardHeight,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                )
            }
        }
    }

    sealed class Floating(rootInsets: ImeInsets.Root) : ImeWindowConstraints(rootInsets) {
        override val minKeyboardWidth by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.25f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.40f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.28f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.55f
            }
            (baselineScreen.width * factor).coerceAtMost(rootBounds.width)
        }
        override val maxKeyboardWidth by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.48f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.70f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.40f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.90f
            }
            (baselineScreen.width * factor).coerceIn(minKeyboardWidth, rootBounds.width)
        }
        override val defKeyboardWidth by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.30f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.45f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.30f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.65f
            }
            (baselineScreen.width * factor).coerceIn(minKeyboardWidth, maxKeyboardWidth)
        }

        override val minKeyboardHeight by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET -> 0.25f
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.30f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.18f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.28f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.20f
            }
            (baselineScreen.height * factor).coerceAtMost(rootBounds.height)
        }
        override val maxKeyboardHeight by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.55f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.35f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.60f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.40f
            }
            (baselineScreen.height * factor).coerceIn(minKeyboardHeight, rootBounds.height)
        }
        override val defKeyboardHeight by calculation {
            val factor = when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 0.35f
                ImeFormFactor.Type.TABLET_PORTRAIT -> 0.22f
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 0.45f
                ImeFormFactor.Type.PHONE_PORTRAIT -> 0.22f
            }
            (baselineScreen.height * factor).coerceIn(minKeyboardHeight, maxKeyboardHeight)
        }

        override val defKeyMarginH by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 2.dp
                ImeFormFactor.Type.TABLET_PORTRAIT -> 2.dp
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 1.5.dp
                ImeFormFactor.Type.PHONE_PORTRAIT -> 2.dp
            }
        }
        override val defKeyMarginV by calculation {
            when (formFactor.typeGuess) {
                ImeFormFactor.Type.DESKTOP,
                ImeFormFactor.Type.LARGE_TABLET,
                ImeFormFactor.Type.TABLET_LANDSCAPE -> 5.dp
                ImeFormFactor.Type.TABLET_PORTRAIT -> 5.dp
                ImeFormFactor.Type.PHONE_LANDSCAPE -> 3.dp
                ImeFormFactor.Type.PHONE_PORTRAIT -> 5.dp
            }
        }

        abstract override val defaultProps: ImeWindowProps.Floating

        class Normal(rootInsets: ImeInsets.Root) : Floating(rootInsets) {
            override val defaultProps by calculation {
                ImeWindowProps.Floating(
                    keyboardHeight = defKeyboardHeight,
                    keyboardWidth = defKeyboardWidth,
                    offsetLeft = 60.dp.coerceAtMost(rootBounds.width - defKeyboardWidth),
                    offsetBottom = 60.dp.coerceAtMost(rootBounds.height - defKeyboardHeight),
                )
            }
        }
    }

    companion object {
        val BaselineScreens = mapOf(
            ImeFormFactor.Type.PHONE_PORTRAIT to DpSize(width = 395.dp, height = 875.dp),
            ImeFormFactor.Type.PHONE_LANDSCAPE to DpSize(width = 835.dp, height = 365.dp),
            ImeFormFactor.Type.TABLET_PORTRAIT to DpSize(width = 800.dp, height = 1310.dp),
            ImeFormFactor.Type.TABLET_LANDSCAPE to DpSize(width = 850.dp, height = 800.dp),
            ImeFormFactor.Type.LARGE_TABLET to DpSize(width = 1335.dp, height = 775.dp),
            ImeFormFactor.Type.DESKTOP to DpSize(width = 0.dp, height = 0.dp),
        )

        /**
         * Constructs a new fixed constraints instance inheriting from given [rootInsets] and [fixedMode].
         */
        fun of(rootInsets: ImeInsets.Root, fixedMode: ImeWindowMode.Fixed): Fixed {
            return when (fixedMode) {
                ImeWindowMode.Fixed.NORMAL -> Fixed.Normal(rootInsets)
                ImeWindowMode.Fixed.COMPACT -> Fixed.Compact(rootInsets)
                ImeWindowMode.Fixed.THUMBS -> Fixed.Thumbs(rootInsets)
            }
        }

        /**
         * Constructs a new floating constraints instance inheriting from given [rootInsets] and [floatingMode].
         */
        fun of(rootInsets: ImeInsets.Root, floatingMode: ImeWindowMode.Floating): Floating {
            return when (floatingMode) {
                ImeWindowMode.Floating.NORMAL -> Floating.Normal(rootInsets)
            }
        }
    }
}
