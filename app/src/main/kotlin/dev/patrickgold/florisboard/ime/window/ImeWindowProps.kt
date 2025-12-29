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

@file:UseSerializers(DpSizeSerializer::class)

package dev.patrickgold.florisboard.ime.window

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.florisboard.lib.compose.DpSizeSerializer

sealed interface ImeWindowProps {
    val keyboardHeight: Dp

    fun constrained(constraints: ImeWindowConstraints): ImeWindowProps

    fun calcFontScale(constraints: ImeWindowConstraints): Float

    @Serializable
    data class Fixed(
        override val keyboardHeight: Dp,
        val paddingLeft: Dp,
        val paddingRight: Dp,
        val paddingBottom: Dp,
    ) : ImeWindowProps {
        override fun constrained(constraints: ImeWindowConstraints): Fixed {
            // TODO impl
            return this
        }

        override fun calcFontScale(constraints: ImeWindowConstraints): Float {
            val fontScale = keyboardHeight / constraints.defKeyboardHeight
            return fontScale.coerceAtMost(1f)
        }
    }

    @Serializable
    data class Floating(
        override val keyboardHeight: Dp,
        val keyboardWidth: Dp,
        val offsetLeft: Dp,
        val offsetBottom: Dp,
    ) : ImeWindowProps {
        override fun constrained(constraints: ImeWindowConstraints): Floating {
            val newKeyboardHeight = keyboardHeight.coerceIn(
                minimumValue = constraints.minKeyboardHeight,
                maximumValue = constraints.maxKeyboardHeight,
            )
            val newKeyboardWidth = keyboardWidth.coerceIn(
                minimumValue = constraints.minKeyboardWidth,
                maximumValue = constraints.maxKeyboardWidth,
            )
            val newOffsetLeft = offsetLeft.coerceIn(
                minimumValue = 0.dp,
                maximumValue = constraints.rootBounds.width - newKeyboardWidth,
            )
            val newOffsetBottom = offsetBottom.coerceIn(
                minimumValue = 0.dp,
                maximumValue = constraints.rootBounds.height - newKeyboardHeight,
            )
            return Floating(
                keyboardHeight = newKeyboardHeight,
                keyboardWidth = newKeyboardWidth,
                offsetLeft = newOffsetLeft,
                offsetBottom = newOffsetBottom,
            )
        }

        override fun calcFontScale(constraints: ImeWindowConstraints): Float {
            val fontScale = keyboardHeight / constraints.defKeyboardHeight
            return fontScale.coerceAtMost(1f)
        }
    }
}
