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

sealed interface ImeWindowSpec {
    val props: ImeWindowProps
    val rootInsets: ImeInsets?
    val orientation: ImeOrientation

    data class Fixed(
        val mode: ImeWindowMode.Fixed,
        override val props: ImeWindowProps.Fixed,
        override val rootInsets: ImeInsets?,
        override val orientation: ImeOrientation,
    ) : ImeWindowSpec

    data class Floating(
        val mode: ImeWindowMode.Floating,
        override val props: ImeWindowProps.Floating,
        override val rootInsets: ImeInsets?,
        override val orientation: ImeOrientation,
    ) : ImeWindowSpec

    val floatingDockHeight: Dp
        get() = when (orientation) {
            ImeOrientation.PORTRAIT -> ImeWindowDefaults.FloatingDockHeightPortrait
            ImeOrientation.LANDSCAPE -> ImeWindowDefaults.FloatingDockHeightLandscape
        }
}
