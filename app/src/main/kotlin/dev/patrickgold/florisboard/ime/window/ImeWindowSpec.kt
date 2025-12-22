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
import androidx.compose.ui.unit.DpOffset

sealed interface ImeWindowSpec {
    val props: ImeWindowProps
    val rootInsets: ImeInsets?
    val orientation: ImeOrientation

    val floatingDockHeight: Dp
        get() = when (orientation) {
            ImeOrientation.PORTRAIT -> ImeWindowDefaults.FloatingDockHeightPortrait
            ImeOrientation.LANDSCAPE -> ImeWindowDefaults.FloatingDockHeightLandscape
        }

    fun movedBy(offset: DpOffset): ImeWindowSpec

    fun resizedTo(offset: DpOffset, handle: ImeWindowResizeHandle): ImeWindowSpec

    data class Fixed(
        val mode: ImeWindowMode.Fixed,
        override val props: ImeWindowProps.Fixed,
        override val rootInsets: ImeInsets?,
        override val orientation: ImeOrientation,
    ) : ImeWindowSpec {
        override fun movedBy(
            offset: DpOffset,
        ): ImeWindowSpec {
            // TODO impl
            return this
        }

        override fun resizedTo(
            offset: DpOffset,
            handle: ImeWindowResizeHandle,
        ): ImeWindowSpec {
            // TODO impl
            return this
        }
    }

    data class Floating(
        val mode: ImeWindowMode.Floating,
        override val props: ImeWindowProps.Floating,
        override val rootInsets: ImeInsets?,
        override val orientation: ImeOrientation,
    ) : ImeWindowSpec {
        override fun movedBy(
            offset: DpOffset,
        ): ImeWindowSpec {
            val newProps = props.copy(
                offsetLeft = props.offsetLeft + offset.x,
                offsetBottom = props.offsetBottom - offset.y,
            )
            return copy(props = newProps.constrained(rootInsets))
        }

        override fun resizedTo(
            offset: DpOffset,
            handle: ImeWindowResizeHandle,
        ): ImeWindowSpec {
            // TODO impl
            return this
        }
    }
}
