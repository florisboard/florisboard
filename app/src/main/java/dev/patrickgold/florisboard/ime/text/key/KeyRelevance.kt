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

import java.util.*

/**
 * The relevance of a popup key. Used by the KeyPopupManager to determine where a key should be
 * placed within the extended popup window and which key should get the initial focus.
 *
 * The importance of the relevance levels is as follows:
 *  - [HINT]: If no [MAIN] relevance key exists, this key will take the initial focus. If a [MAIN]
 *    key exists, the priority defined in the prefs or the smart selection process will decide,
 *    which key takes the focus. Should only be defined once in the popup.
 *  - [MAIN]: If no [HINT] relevance key exists, this key will take the initial focus. If a [HINT]
 *    key exists, the priority defined in the prefs or the smart selection process will decide,
 *    which key takes the focus. There should at any given time only be one
 */
enum class KeyRelevance {
    DEFAULT,
    RELEVANT,
    MAIN,
    HINT;

    companion object {
        fun fromString(string: String): KeyRelevance {
            return valueOf(string.toUpperCase(Locale.ROOT))
        }
    }

    override fun toString(): String {
        return super.toString().toLowerCase(Locale.ROOT)
    }
}
