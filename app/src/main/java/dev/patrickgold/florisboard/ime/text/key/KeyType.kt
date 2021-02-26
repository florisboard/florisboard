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

import com.squareup.moshi.FromJson
import java.util.*

/**
 * Enum for declaring the type of the key.
 * List of possible key types:
 *  [Wikipedia](https://en.wikipedia.org/wiki/Keyboard_layout#Key_types)
 */
enum class KeyType {
    CHARACTER,
    ENTER_EDITING,
    FUNCTION,
    LOCK,
    MODIFIER,
    NAVIGATION,
    SYSTEM_GUI,
    NUMERIC,
    PLACEHOLDER,
    UNSPECIFIED;

    companion object {
        fun fromString(string: String): KeyType {
            return valueOf(string.toUpperCase(Locale.ENGLISH))
        }
    }
}

class KeyTypeAdapter {
    @FromJson
    fun fromJson(raw: String): KeyType {
        return KeyType.fromString(raw)
    }
}
