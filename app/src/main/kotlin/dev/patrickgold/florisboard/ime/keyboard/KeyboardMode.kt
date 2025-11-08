/*
 * Copyright (C) 2020-2025 The FlorisBoard Contributors
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

enum class KeyboardMode(val value: Int) {
    UNSPECIFIED(-1),
    CHARACTERS(0),
    @Deprecated(message = "TODO: remove")
    EDITING(1),
    SYMBOLS(2),
    SYMBOLS2(3),
    NUMERIC(4),
    NUMERIC_ADVANCED(5),
    PHONE(6),
    PHONE2(7),
    @Deprecated(message = "TODO: remove")
    SMARTBAR_CLIPBOARD_CURSOR_ROW(8),
    @Deprecated(message = "TODO: remove")
    SMARTBAR_NUMBER_ROW(9),
    SMARTBAR_QUICK_ACTIONS(10);

    companion object {
        fun fromInt(int: Int) = entries.firstOrNull { it.value == int } ?: CHARACTERS
    }

    override fun toString() = name.lowercase()

    fun toInt() = value
}
