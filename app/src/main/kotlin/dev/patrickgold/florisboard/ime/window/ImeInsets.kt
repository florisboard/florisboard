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

package dev.patrickgold.florisboard.ime.window

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.florisboard.lib.compose.toDpRect

/**
 * Insets describe the size and position within the next logical parent. All bounds are given in two
 * units, dp and px. The px are measured by compose, and converted to dp using the current screen density.
 */
sealed interface ImeInsets {
    /**
     * The bounds in dp.
     */
    val boundsDp: DpRect

    /**
     * The bounds in px.
     */
    val boundsPx: IntRect

    /**
     * Insets of the root window. In most cases this will be the screen size, however this is not guaranteed.
     *
     * @property boundsDp The bounds of the root window in dp.
     * @property boundsPx The bounds of the root window in px.
     * @property formFactor The form factor of the available size.
     */
    data class Root(
        override val boundsDp: DpRect,
        override val boundsPx: IntRect,
        val formFactor: ImeFormFactor,
    ) : ImeInsets {
        companion object {
            /**
             * A zero-size root window.
             *
             * All computations must be able to handle any non-negative sized root window, including an empty one.
             */
            val Zero = Root(
                boundsDp = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
                boundsPx = IntRect.Zero,
                formFactor = ImeFormFactor.Zero,
            )

            /**
             * Returns a new root insets instance for given parameters.
             *
             * @param density The density of the screen. Must be provided contextual.
             * @param boundsPx The root window bounds measured in px.
             */
            context(density: Density)
            fun of(boundsPx: IntRect): Root {
                val boundsDp = boundsPx.toDpRect()
                return Root(
                    boundsDp = boundsDp,
                    boundsPx = boundsPx,
                    formFactor = ImeFormFactor.of(boundsDp),
                )
            }
        }
    }

    /**
     * Insets of the window within the root window.
     *
     * @property boundsDp The bounds of the window in dp.
     * @property boundsPx The bounds of the window in px.
     */
    data class Window(
        override val boundsDp: DpRect,
        override val boundsPx: IntRect,
    ) : ImeInsets {
        companion object {
            /**
             * Returns a new window insets instance for given parameters.
             *
             * @param density The density of the screen. Must be provided contextual.
             * @param boundsPx The window bounds measured in px.
             */
            context(density: Density)
            fun of(boundsPx: IntRect): Window {
                val boundsDp = boundsPx.toDpRect()
                return Window(
                    boundsDp = boundsDp,
                    boundsPx = boundsPx,
                )
            }
        }
    }
}
