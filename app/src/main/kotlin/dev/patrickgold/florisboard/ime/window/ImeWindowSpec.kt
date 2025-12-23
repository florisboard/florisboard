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
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.width

sealed interface ImeWindowSpec {
    val props: ImeWindowProps
    val rootInsets: ImeInsets?
    val orientation: ImeOrientation

    val floatingDockToFixedHeight: Dp
        get() = ImeWindowDefaults.FloatingDockToFixedHeight.select(orientation)
    val floatingDockToFixedBorder: Dp
        get() = ImeWindowDefaults.FloatingDockToFixedBorder.select(orientation)

    fun movedBy(offset: DpOffset): ImeWindowSpec

    fun resizedBy(handle: ImeWindowResizeHandle, offset: DpOffset): Pair<ImeWindowSpec, DpOffset>

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

        override fun resizedBy(
            handle: ImeWindowResizeHandle,
            offset: DpOffset,
        ): Pair<ImeWindowSpec, DpOffset> {
            // TODO impl
            return this to offset
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

        override fun resizedBy(
            handle: ImeWindowResizeHandle,
            offset: DpOffset,
        ): Pair<ImeWindowSpec, DpOffset> {
            val rootBounds = rootInsets?.boundsDp ?: return this to DpOffset.Zero

            var keyboardHeight = props.rowHeight * ImeWindowDefaults.KeyboardHeightFactor
            var keyboardWidth = props.keyboardWidth
            var offsetLeft = props.offsetLeft
            var offsetBottom = props.offsetBottom

            if (handle.top) {
                keyboardHeight = (keyboardHeight - offset.y)
                    .coerceIn(ImeWindowDefaults.MinKeyboardHeight..ImeWindowDefaults.MaxKeyboardHeight)
            } else if (handle.bottom) {
                val newKeyboardHeight = (keyboardHeight + offset.y.coerceAtLeast(-offsetBottom))
                    .coerceIn(ImeWindowDefaults.MinKeyboardHeight..ImeWindowDefaults.MaxKeyboardHeight)
                offsetBottom -= (newKeyboardHeight - keyboardHeight)
                keyboardHeight = newKeyboardHeight
            }

            if (handle.left) {
                val newKeyboardWidth = (keyboardWidth - offset.x.coerceAtLeast(-offsetLeft))
                    .coerceIn(ImeWindowDefaults.MinKeyboardWidth..ImeWindowDefaults.MaxKeyboardWidth)
                    .coerceAtMost(rootBounds.width - offsetLeft)
                offsetLeft -= (newKeyboardWidth - keyboardWidth)
                keyboardWidth = newKeyboardWidth
            } else if (handle.right) {
                keyboardWidth = (keyboardWidth + offset.x)
                    .coerceIn(ImeWindowDefaults.MinKeyboardWidth..ImeWindowDefaults.MaxKeyboardWidth)
                    .coerceAtMost(rootBounds.width - offsetLeft)
            }

            val newProps = ImeWindowProps.Floating(
                rowHeight = keyboardHeight / ImeWindowDefaults.KeyboardHeightFactor,
                keyboardWidth = keyboardWidth,
                offsetLeft = offsetLeft,
                offsetBottom = offsetBottom,
            )
            val consumed = DpOffset(
                x = when {
                    handle.left -> -(newProps.keyboardWidth - props.keyboardWidth)
                    else -> (newProps.keyboardWidth - props.keyboardWidth)
                },
                y = when {
                    handle.top -> -(newProps.rowHeight - props.rowHeight)
                    else -> (newProps.rowHeight - props.rowHeight)
                } * ImeWindowDefaults.KeyboardHeightFactor,
            )
            return copy(props = newProps) to consumed
        }
    }
}
