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
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.florisboard.lib.compose.DpSizeSerializer

@Serializable
sealed interface ImeWindowSpec {
    val rowHeight: Dp

    @Serializable
    data class Fixed(
        override val rowHeight: Dp,
        val paddingLeft: Dp,
        val paddingRight: Dp,
        val paddingBottom: Dp,
    ) : ImeWindowSpec {
        companion object {
            val DefaultNormal = Fixed(
                rowHeight = DefaultRowHeight,
                paddingLeft = 0.dp,
                paddingRight = 0.dp,
                paddingBottom = 0.dp,
            )
            val DefaultCompact = Fixed(
                rowHeight = DefaultRowHeight * 0.8f,
                paddingLeft = DefaultCompactOffset,
                paddingRight = 0.dp,
                paddingBottom = (DefaultRowHeight * 4.7f) * 0.2f,
            )
        }
    }

    @Serializable
    data class Floating(
        override val rowHeight: Dp,
        val width: Dp,
        val offsetLeft: Dp,
        val offsetBottom: Dp,
    ) : ImeWindowSpec {
        companion object {
            val DefaultFloating = Floating(
                rowHeight = DefaultRowHeight * 0.8f,
                width = 350.dp,
                offsetLeft = 30.dp,
                offsetBottom = 30.dp
            )
        }
    }

    companion object {
        private val DefaultRowHeight = 55.dp
        private val DefaultCompactOffset = 70.dp

        const val KeyboardHeightFactor = 4.7f

        val MinKeyboardWidth = 300.dp
    }
}
