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

/**
 * The window mode affects the placement and allowable positions of the window within the root window. It is
 * a two-layer enum, with the mode representing the main differentiation between fixed and floating placement.
 * For each mode there is at least one sub-mode, which influences the layout within the placed window.
 */
enum class ImeWindowMode {
    /**
     * The window is docked to the bottom edge of the root window and fills the maximum available
     * root window width. The height is dependent on the layout and smartbar configuration.
     */
    FIXED,

    /**
     * The window may be positioned anywhere within the root window, similar to a window on a desktop.
     */
    FLOATING;

    /**
     * Further differentiation of the mode within fixed.
     * Any sub-mode only affects the layout within the placed window.
     */
    enum class Fixed {
        /**
         * The layout occupies the full window width, minus paddings.
         */
        NORMAL,

        /**
         * The layout occupies a good portion of the window width. In the area covered by the paddings,
         * controls for the one-handed mode will be shown. This mode should enforce a minimum padding.
         *
         * The side of the one-handed mode is determined by which of the two paddings (left vs right) has
         * a larger numerical value. On equal numerical values, padding left should take precedence.
         */
        COMPACT,

        /**
         * TODO Placeholder, to be implemented in the future.
         */
        THUMBS;
    }

    /**
     * Further differentiation of the mode within floating.
     * Any sub-mode only affects the layout within the placed window.
     */
    enum class Floating {
        /**
         * The layout occupies the full window width, minus paddings.
         */
        NORMAL;
    }

    /**
     * Returns the enum name in lowercase for Snygg attribute usage.
     */
    override fun toString() = name.lowercase()
}
