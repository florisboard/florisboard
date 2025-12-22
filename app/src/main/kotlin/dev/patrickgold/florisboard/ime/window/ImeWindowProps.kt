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
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.florisboard.lib.compose.DpSizeSerializer

sealed interface ImeWindowProps {
    val rowHeight: Dp

    @Serializable
    data class Fixed(
        override val rowHeight: Dp,
        val paddingLeft: Dp,
        val paddingRight: Dp,
        val paddingBottom: Dp,
    ) : ImeWindowProps

    @Serializable
    data class Floating(
        override val rowHeight: Dp,
        val keyboardWidth: Dp,
        val offsetLeft: Dp,
        val offsetBottom: Dp,
    ) : ImeWindowProps
}
