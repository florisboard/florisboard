/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.key

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.jetpref.ui.compose.entry

/**
 * Enum for the input modes of a text keyboard.
 */
enum class InputMode(val value: Int) {
    /**
     * The default input mode.
     */
    NORMAL(0),
    /**
     * Shift lock is active, but not persistent after character input. Symbol rows are shifted.
     */
    SHIFT_LOCK(1),
    /**
     * Shift lock is active, but persistent after character input = Caps lock. Symbol rows are not shifted.
     */
    CAPS_LOCK(2);

    companion object {
        fun fromInt(int: Int) = values().firstOrNull { it.value == int } ?: NORMAL

        @Composable
        fun listEntries() = listOf(
            entry(
                key = NORMAL,
                label = stringRes(R.string.enum__input_mode__normal),
            ),
            entry(
                key = SHIFT_LOCK,
                label = stringRes(R.string.enum__input_mode__shift_lock),
            ),
            entry(
                key = CAPS_LOCK,
                label = stringRes(R.string.enum__input_mode__caps_lock),
            ),
        )
    }

    fun toInt() = value
}
