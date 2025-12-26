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

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.width
import kotlin.math.abs

sealed interface ImeWindowSpec {
    val props: ImeWindowProps
    val fontScale: Float
    val rootInsets: ImeInsets?
    val orientation: ImeOrientation

    fun movedBy(offset: DpOffset): Pair<ImeWindowSpec, DpOffset>

    fun resizedBy(handle: ImeWindowResizeHandle, offset: DpOffset): Pair<ImeWindowSpec, DpOffset>

    data class Fixed(
        val mode: ImeWindowMode.Fixed,
        override val props: ImeWindowProps.Fixed,
        override val fontScale: Float,
        override val rootInsets: ImeInsets?,
        override val orientation: ImeOrientation,
    ) : ImeWindowSpec {
        override fun movedBy(
            offset: DpOffset,
        ): Pair<ImeWindowSpec, DpOffset> {
            val windowDefaults = ImeWindowDefaults.of(orientation)

            var paddingLeft = props.paddingLeft
            var paddingRight = props.paddingRight
            var paddingBottom = props.paddingBottom

            if (offset.x < 0.dp) {
                // move to left
                val newPaddingLeft = (paddingLeft + offset.x)
                    .coerceAtLeast(0.dp)
                paddingRight -= (newPaddingLeft - paddingLeft)
                paddingLeft = newPaddingLeft
            } else {
                // move to right
                val newPaddingRight = (paddingRight - offset.x)
                    .coerceAtLeast(0.dp)
                paddingLeft -= (newPaddingRight - paddingRight)
                paddingRight = newPaddingRight
            }

            if (mode == ImeWindowMode.Fixed.NORMAL && paddingLeft != 0.dp && paddingRight != 0.dp) {
                // maybe snap to center
                if (abs((paddingLeft - paddingRight).value).dp <= windowDefaults.fixedSnapToCenterWidth) {
                    val avgPadding = (paddingLeft + paddingRight) / 2
                    paddingLeft = avgPadding
                    paddingRight = avgPadding
                }
            }

            paddingBottom = (paddingBottom - offset.y)
                .coerceAtLeast(0.dp)
                .coerceAtMost(max(windowDefaults.keyboardHeightMax - props.rowHeight * windowDefaults.keyboardHeightFactor, 0.dp))

            val newProps = props.copy(
                paddingLeft = paddingLeft,
                paddingRight = paddingRight,
                paddingBottom = paddingBottom,
            ).constrained(rootInsets, orientation)
            val newSpec = copy(props = newProps)
            val consumed = DpOffset(
                x = newProps.paddingLeft - props.paddingLeft,
                y = -(newProps.paddingBottom - props.paddingBottom),
            )
            return newSpec to consumed
        }

        override fun resizedBy(
            handle: ImeWindowResizeHandle,
            offset: DpOffset,
        ): Pair<ImeWindowSpec, DpOffset> {
            val rootBounds = rootInsets?.boundsDp ?: return this to DpOffset.Zero
            val windowDefaults = ImeWindowDefaults.of(orientation)

            var keyboardHeight = props.rowHeight * windowDefaults.keyboardHeightFactor
            var paddingLeft = props.paddingLeft
            var paddingRight = props.paddingRight
            var paddingBottom = props.paddingBottom

            if (handle.top) {
                keyboardHeight = (keyboardHeight - offset.y)
                    .coerceAtLeast(windowDefaults.keyboardHeightMin)
                    .coerceAtMost(windowDefaults.keyboardHeightMax - paddingBottom)
            } else if (handle.bottom) {
                val newKeyboardHeight = (keyboardHeight + offset.y.coerceAtMost(paddingBottom))
                    .coerceIn(windowDefaults.keyboardHeightMin..windowDefaults.keyboardHeightMax)
                paddingBottom -= (newKeyboardHeight - keyboardHeight)
                keyboardHeight = newKeyboardHeight
            }

            if (handle.left) {
                val newPaddingLeft = (paddingLeft + offset.x)
                    .coerceAtLeast(0.dp)
                    .coerceAtMost(max(rootBounds.width - paddingRight - windowDefaults.keyboardWidthMin, 0.dp))
                paddingLeft = newPaddingLeft
            } else if (handle.right) {
                val newPaddingRight = (paddingRight - offset.x)
                    .coerceAtLeast(0.dp)
                    .coerceAtMost(max(rootBounds.width - paddingLeft - windowDefaults.keyboardWidthMin, 0.dp))
                paddingRight = newPaddingRight
            }

            val newProps = ImeWindowProps.Fixed(
                rowHeight = keyboardHeight / windowDefaults.keyboardHeightFactor,
                paddingLeft = paddingLeft,
                paddingRight = paddingRight,
                paddingBottom = paddingBottom,
            ).constrained(rootInsets, orientation)
            val newSpec = copy(
                props = newProps,
                //fontScale = newProps.calcFontScale(rootInsets, orientation),
            )
            val consumed = DpOffset(
                x = when {
                    handle.left -> newProps.paddingLeft - props.paddingLeft
                    else -> -(newProps.paddingRight - props.paddingRight)
                },
                y = when {
                    handle.top -> -(newProps.rowHeight - props.rowHeight)
                    else -> (newProps.rowHeight - props.rowHeight)
                } * windowDefaults.keyboardHeightFactor,
            )
            return newSpec to consumed
        }
    }

    data class Floating(
        val mode: ImeWindowMode.Floating,
        override val props: ImeWindowProps.Floating,
        override val fontScale: Float,
        override val rootInsets: ImeInsets?,
        override val orientation: ImeOrientation,
    ) : ImeWindowSpec {
        override fun movedBy(
            offset: DpOffset,
        ): Pair<ImeWindowSpec, DpOffset> {
            val newProps = props.copy(
                offsetLeft = props.offsetLeft + offset.x,
                offsetBottom = props.offsetBottom - offset.y,
            ).constrained(rootInsets, orientation)
            val newSpec = copy(props = newProps)
            val consumed = DpOffset(
                x = newProps.offsetLeft - props.offsetLeft,
                y = -(newProps.offsetBottom - props.offsetBottom),
            )
            return newSpec to consumed
        }

        override fun resizedBy(
            handle: ImeWindowResizeHandle,
            offset: DpOffset,
        ): Pair<ImeWindowSpec, DpOffset> {
            val rootBounds = rootInsets?.boundsDp ?: return this to DpOffset.Zero
            val windowDefaults = ImeWindowDefaults.of(orientation)

            var keyboardHeight = props.rowHeight * windowDefaults.keyboardHeightFactor
            var keyboardWidth = props.keyboardWidth
            var offsetLeft = props.offsetLeft
            var offsetBottom = props.offsetBottom

            if (handle.top) {
                keyboardHeight = (keyboardHeight - offset.y)
                    .coerceIn(windowDefaults.keyboardHeightMin..windowDefaults.keyboardHeightMax)
            } else if (handle.bottom) {
                val newKeyboardHeight = (keyboardHeight + offset.y.coerceAtLeast(-offsetBottom))
                    .coerceIn(windowDefaults.keyboardHeightMin..windowDefaults.keyboardHeightMax)
                offsetBottom -= (newKeyboardHeight - keyboardHeight)
                keyboardHeight = newKeyboardHeight
            }

            if (handle.left) {
                val newKeyboardWidth = (keyboardWidth - offset.x.coerceAtLeast(-offsetLeft))
                    .coerceIn(windowDefaults.keyboardWidthMin..windowDefaults.keyboardWidthMax)
                    .coerceAtMost(rootBounds.width - offsetLeft)
                offsetLeft -= (newKeyboardWidth - keyboardWidth)
                keyboardWidth = newKeyboardWidth
            } else if (handle.right) {
                keyboardWidth = (keyboardWidth + offset.x)
                    .coerceIn(windowDefaults.keyboardWidthMin..windowDefaults.keyboardWidthMax)
                    .coerceAtMost(rootBounds.width - offsetLeft)
            }

            val newProps = ImeWindowProps.Floating(
                rowHeight = keyboardHeight / windowDefaults.keyboardHeightFactor,
                keyboardWidth = keyboardWidth,
                offsetLeft = offsetLeft,
                offsetBottom = offsetBottom,
            ).constrained(rootInsets, orientation)
            val newSpec = copy(
                props = newProps,
                //fontScale = newProps.calcFontScale(rootInsets, orientation),
            )
            val consumed = DpOffset(
                x = when {
                    handle.left -> -(newProps.keyboardWidth - props.keyboardWidth)
                    else -> (newProps.keyboardWidth - props.keyboardWidth)
                },
                y = when {
                    handle.top -> -(newProps.rowHeight - props.rowHeight)
                    else -> (newProps.rowHeight - props.rowHeight)
                } * windowDefaults.keyboardHeightFactor,
            )
            return newSpec to consumed
        }
    }
}
