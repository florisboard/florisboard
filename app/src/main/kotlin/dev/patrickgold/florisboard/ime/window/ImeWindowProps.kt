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
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.florisboard.lib.compose.DpSizeSerializer

sealed interface ImeWindowProps {
    val rowHeight: Dp

    fun constrained(
        rootInsets: ImeInsets?,
        orientation: ImeOrientation,
    ): ImeWindowProps

    fun calcFontScale(
        rootInsets: ImeInsets?,
        orientation: ImeOrientation,
    ): Float

    @Serializable
    data class Fixed(
        override val rowHeight: Dp,
        val paddingLeft: Dp,
        val paddingRight: Dp,
        val paddingBottom: Dp,
    ) : ImeWindowProps {
        override fun constrained(
            rootInsets: ImeInsets?,
            orientation: ImeOrientation,
        ): Fixed {
            val rootBounds = rootInsets?.boundsDp ?: return this
            val windowDefaults = ImeWindowDefaults.Fixed.of(orientation)
            // TODO impl
            return this
        }

        override fun calcFontScale(
            rootInsets: ImeInsets?,
            orientation: ImeOrientation,
        ): Float {
            val windowDefaults = ImeWindowDefaults.Fixed.of(orientation)
            val fontScale = rowHeight / windowDefaults.rowHeight
            return fontScale.coerceAtMost(1f)
        }
    }

    @Serializable
    data class Floating(
        override val rowHeight: Dp,
        val keyboardWidth: Dp,
        val offsetLeft: Dp,
        val offsetBottom: Dp,
    ) : ImeWindowProps {
        override fun constrained(
            rootInsets: ImeInsets?,
            orientation: ImeOrientation,
        ): Floating {
            val rootBounds = rootInsets?.boundsDp ?: return this
            val windowDefaults = ImeWindowDefaults.Floating.of(orientation)

            val newRowHeight = rowHeight.coerceIn(
                minimumValue = min(windowDefaults.keyboardHeightMin / windowDefaults.keyboardHeightFactor, rootBounds.height),
                maximumValue = min(windowDefaults.keyboardHeightMax / windowDefaults.keyboardHeightFactor, rootBounds.height),
            )
            val newKeyboardWidth = keyboardWidth.coerceIn(
                minimumValue = min(windowDefaults.keyboardWidthMin, rootBounds.width),
                maximumValue = min(windowDefaults.keyboardWidthMax, rootBounds.width),
            )
            val newOffsetLeft = offsetLeft.coerceIn(
                minimumValue = 0.dp,
                maximumValue = rootBounds.width - newKeyboardWidth,
            )
            val newOffsetBottom = offsetBottom.coerceIn(
                minimumValue = 0.dp,
                maximumValue = rootBounds.height - (newRowHeight * windowDefaults.keyboardHeightFactor),
            )
            return Floating(
                rowHeight = newRowHeight,
                keyboardWidth = newKeyboardWidth,
                offsetLeft = newOffsetLeft,
                offsetBottom = newOffsetBottom,
            )
        }

        override fun calcFontScale(
            rootInsets: ImeInsets?,
            orientation: ImeOrientation,
        ): Float {
            val windowDefaults = ImeWindowDefaults.Floating.of(orientation)
            val fontScale = rowHeight / windowDefaults.rowHeight
            return fontScale.coerceAtMost(1f)
        }
    }
}
