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

@file:UseSerializers(DpSizeSerializer::class)

package dev.patrickgold.florisboard.ime.window

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.unit.width
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.florisboard.lib.compose.DpSizeSerializer
import kotlin.math.sqrt

/**
 * The window props describe the necessary properties for window placement and sizing.
 */
sealed interface ImeWindowProps {
    /**
     * The keyboard height describes the baseline height the keyboard would have for 4 rows and 0 smartbar rows.
     */
    val keyboardHeight: Dp

    fun keyboardWidth(constraints: ImeWindowConstraints): Dp

    fun calcKeyMarginH(constraints: ImeWindowConstraints): Dp {
        val factor = keyboardWidth(constraints) / constraints.defKeyboardWidth
        return constraints.defKeyMarginH * sqrt(factor)
    }

    fun calcKeyMarginV(constraints: ImeWindowConstraints): Dp {
        val factor = keyboardHeight / constraints.defKeyboardHeight
        return constraints.defKeyMarginV * sqrt(factor)
    }

    fun calcFontScale(constraints: ImeWindowConstraints): Float {
        val factor = keyboardHeight / constraints.defKeyboardHeight
        return sqrt(factor)
    }

    /**
     * The window props for fixed mode. It assumes the window is docked to the bottom root window edge and
     * fills the maximum available root window width.
     *
     * @property keyboardHeight The baseline keyboard height, which may increase for more layout rows or
     *  Smartbar configuration. Must be a non-negative real number.
     * @property paddingLeft The padding from the left window edge to the inner window.
     *  Must be a non-negative real number.
     * @property paddingRight The padding from the right window edge to the inner window.
     *  Must be a non-negative real number.
     * @property paddingBottom The padding from the bottom window edge to the inner window.
     *  Must be a non-negative real number.
     */
    @Serializable
    data class Fixed(
        override val keyboardHeight: Dp,
        val paddingLeft: Dp,
        val paddingRight: Dp,
        val paddingBottom: Dp,
    ) : ImeWindowProps {
        /**
         * Constrains the props using given [constraints]. This function ensures that the props do not push
         * the window out of the root window, which would render the IME non-interactive.
         */
        fun constrained(constraints: ImeWindowConstraints.Fixed): Fixed {
            val rootBounds = constraints.rootBounds
            val defaultProps = constraints.defaultProps
            val newKeyboardHeight = keyboardHeight
                .takeOrElse { defaultProps.keyboardHeight }
                .coerceIn(constraints.minKeyboardHeight, constraints.maxKeyboardHeight)
            var newPaddingLeft = paddingLeft
                .takeOrElse { constraints.defPaddingHorizontal }
                .coerceIn(0.dp, rootBounds.width)
            var newPaddingRight = paddingRight
                .takeOrElse { 0.dp }
                .coerceIn(0.dp, rootBounds.width)
            val newPaddingHorizontal = (newPaddingLeft + newPaddingRight)
                .coerceIn(constraints.minPaddingHorizontal, constraints.maxPaddingHorizontal)
            newPaddingLeft = newPaddingHorizontal.coerceAtMost(newPaddingLeft)
            newPaddingRight = newPaddingHorizontal - newPaddingLeft
            val newPaddingBottom = paddingBottom
                .takeOrElse { 0.dp }
                .coerceIn(0.dp, constraints.maxKeyboardHeight - newKeyboardHeight)
            return Fixed(
                keyboardHeight = newKeyboardHeight,
                paddingLeft = newPaddingLeft,
                paddingRight = newPaddingRight,
                paddingBottom = newPaddingBottom,
            )
        }

        override fun keyboardWidth(constraints: ImeWindowConstraints): Dp {
            val rootBounds = constraints.rootBounds
            return (rootBounds.width - paddingLeft - paddingRight).coerceIn(0.dp, rootBounds.width)
        }
    }

    /**
     * The window props for floating mode. It assumes the window can be freely placed within the root window.
     *
     * @property keyboardHeight The baseline keyboard height, which may increase for more layout rows or
     *  Smartbar configuration. Must be a non-negative real number.
     * @property keyboardWidth The keyboard width, which is fixed and cannot increase.
     *  Must be a non-negative real number.
     * @property offsetLeft The offset from the left root window edge to the left window edge.
     *  Must be a non-negative real number.
     * @property offsetBottom The offset from the bottom root window edge to the bottom window edge.
     *  Must be a non-negative real number.
     */
    @Serializable
    data class Floating(
        override val keyboardHeight: Dp,
        val keyboardWidth: Dp,
        val offsetLeft: Dp,
        val offsetBottom: Dp,
    ) : ImeWindowProps {
        /**
         * Constrains the props using given [constraints]. This function ensures that the props do not push
         * the window out of the root window, which would render the IME non-interactive.
         */
        fun constrained(constraints: ImeWindowConstraints.Floating): Floating {
            val defaultProps = constraints.defaultProps
            val newKeyboardHeight = keyboardHeight
                .takeOrElse { defaultProps.keyboardHeight }
                .coerceIn(constraints.minKeyboardHeight, constraints.maxKeyboardHeight)
            val newKeyboardWidth = keyboardWidth
                .takeOrElse { defaultProps.keyboardWidth }
                .coerceIn(constraints.minKeyboardWidth, constraints.maxKeyboardWidth)
            val newOffsetLeft = offsetLeft
                .takeOrElse { defaultProps.offsetLeft }
                .coerceIn(0.dp, constraints.rootBounds.width - newKeyboardWidth)
            val newOffsetBottom = offsetBottom
                .takeOrElse { defaultProps.offsetBottom }
                .coerceIn(0.dp, constraints.rootBounds.height - newKeyboardHeight)
            return Floating(
                keyboardHeight = newKeyboardHeight,
                keyboardWidth = newKeyboardWidth,
                offsetLeft = newOffsetLeft,
                offsetBottom = newOffsetBottom,
            )
        }

        override fun keyboardWidth(constraints: ImeWindowConstraints): Dp {
            return keyboardWidth
        }
    }
}
