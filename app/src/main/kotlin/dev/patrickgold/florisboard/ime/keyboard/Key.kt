/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.patrickgold.florisboard.lib.FlorisRect

/**
 * Abstract class describing the smallest computed unit in a computed keyboard. Each key represents exactly one key
 * displayed in the UI. It allows to save the absolute location within the parent keyboard, save touch and visual
 * bounds, managing the state (enabled, pressed, visibility) as well as layout sizing factors. Each key in this IME
 * inherits from this base key class. This allows for a inter-operable usage of a key without knowing the exact
 * subclass upfront.
 *
 * @property data The base key data this key represents.This can be anything - from a basic text key to an emoji key
 *  to a complex selector.
 */
abstract class Key(open val data: AbstractKeyData) {
    /**
     * Specifies whether this key is enabled or not.
     */
    open var isEnabled: Boolean by mutableStateOf(true)

    /**
     * Specifies whether this key is actively pressed or not. Is used by the parent keyboard view to draw the key
     * differently to indicate this state.
     */
    open var isPressed: Boolean by mutableStateOf(false)

    /**
     * Specifies whether this key is visible or not. Is used by the parent keyboard view to omit this key in the
     * layout and drawing process. A `false`-value is equivalent to `VISIBILITY_GONE` on Android's View class.
     */
    open var isVisible: Boolean by mutableStateOf(true)

    /**
     * The touch bounds of this key. All bounds defined here are absolute coordinates within the parent keyboard.
     */
    open val touchBounds: FlorisRect = FlorisRect.empty()

    /**
     * The visible bounds of this key. All bounds defined here are absolute coordinates within the parent keyboard.
     */
    open val visibleBounds: FlorisRect = FlorisRect.empty()

    /**
     * Specifies how much this key is willing to shrink if too many keys are in a keyboard row. A value of 0.0
     * indicates that the key does not want to shrink in such scenario. This value should not be set manually, only
     * by the key's compute method and is used in the layout process to determine the real key width.
     */
    open var flayShrink: Float = 0f

    /**
     * Specifies how much this key is willing to grow if too few keys are in a keyboard row. A value of 0.0
     * indicates that the key does not want to grow in such scenario. This value should not be set manually, only
     * by the key's compute method and is used in the layout process to determine the real key width.
     */
    open var flayGrow: Float = 0f

    /**
     * Specifies the relative proportional width this key aims to get in respective to the keyboard view's desired key
     * width. A value of 1.0 indicates that the key wants to be exactly as wide as the desired key width, a value of
     * 0.0 is basically equivalent to setting [isVisible] to false. This value should not be set manually, only
     * by the key's compute method and is used in the layout process to determine the real key width.
     */
    open var flayWidthFactor: Float = 0f

    /**
     * The computed UI label of this key. This value is used by the keyboard view to temporarily save the label string
     * for UI rendering and should not be set manually.
     */
    open var label: String? = null

    /**
     * The computed UI hint label of this key. This value is used by the keyboard view to temporarily save the hint
     * label string for UI rendering and should not be set manually.
     */
    open var hintedLabel: String? = null

    /**
     * The computed UI drawable ID of this key. This value is used by the keyboard view to temporarily save the
     * drawable ID for UI rendering and should not be set manually.
     */
    open var foregroundDrawableId: Int? = null
}
