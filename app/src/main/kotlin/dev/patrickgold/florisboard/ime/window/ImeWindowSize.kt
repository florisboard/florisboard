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
sealed interface ImeWindowSize {
    val rowHeight: Dp

    @Serializable
    data class Fixed(
        override val rowHeight: Dp,
        val offsetLeft: Dp,
        val offsetRight: Dp,
        val offsetBottom: Dp,
    ) : ImeWindowSize {
        companion object {
            val DefaultCompact = Fixed(
                rowHeight = DefaultRowHeight * 0.8f,
                offsetLeft = DefaultCompactOffset,
                offsetRight = 0.dp,
                offsetBottom = (DefaultRowHeight * 4.7f) * 0.2f,
            )
        }
    }

    @Serializable
    data class Floating(
        override val rowHeight: Dp,
        val width: Dp,
        val topLeftX: Dp,
        val topLeftY: Dp,
    ) : ImeWindowSize

    companion object {
        private val DefaultRowHeight = 55.dp
        private val DefaultCompactOffset = 70.dp
    }
}
