/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.input

/**
 * Enum for the input shift states of a text keyboard.
 */
enum class InputShiftState(val value: Int) {
    /**
     * The default input mode, no shift modifier is active.
     */
    UNSHIFTED(0),
    /**
     * Shift is active, but resets to [UNSHIFTED] after a single input. Symbol rows are shifted.
     * Indicates that this shift was manually activated, e.g. by pressing the shift key.
     */
    SHIFTED_MANUAL(1),
    /**
     * Shift is active, but resets to [UNSHIFTED] after a single input. Symbol rows are not shifted.
     * Indicates that this shift was automatically activated through the auto-capitalization feature.
     */
    SHIFTED_AUTOMATIC(2),
    /**
     * Caps lock is active and persists after input. Symbol rows are not shifted.
     */
    CAPS_LOCK(3);

    companion object {
        fun fromInt(int: Int) = values().firstOrNull { it.value == int } ?: UNSHIFTED
    }

    fun toInt() = value
}
