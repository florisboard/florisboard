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

package dev.patrickgold.florisboard.ime.media.emoji

import java.util.*

/**
 * Enum for emoji category.
 * List taken from https://unicode.org/Public/emoji/13.0/emoji-test.txt
 */
enum class EmojiCategory {
    SMILEYS_EMOTION,
    PEOPLE_BODY,
    ANIMALS_NATURE,
    FOOD_DRINK,
    TRAVEL_PLACES,
    ACTIVITIES,
    OBJECTS,
    SYMBOLS,
    FLAGS;

    override fun toString(): String {
        return super.toString().replace("_", " & ")
    }

    companion object {
        fun fromString(string: String): EmojiCategory {
            return valueOf(string.replace(" & ", "_").toUpperCase(Locale.ENGLISH))
        }
    }
}
