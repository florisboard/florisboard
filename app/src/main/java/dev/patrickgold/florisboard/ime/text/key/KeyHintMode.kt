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

/**
 * Enum for the key hint modes.
 */
enum class KeyHintMode {
    DISABLED,
    ENABLED_HINT_PRIORITY,
    ENABLED_ACCENT_PRIORITY,
    ENABLED_SMART_PRIORITY;

    companion object {
        fun fromString(string: String): KeyHintMode {
            return valueOf(string.uppercase())
        }
    }

    override fun toString(): String {
        return super.toString().lowercase()
    }
}
