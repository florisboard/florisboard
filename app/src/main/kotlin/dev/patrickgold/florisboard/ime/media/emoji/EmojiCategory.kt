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

package dev.patrickgold.florisboard.ime.media.emoji

import dev.patrickgold.florisboard.R

enum class EmojiCategory(val id: String) {
    RECENTLY_USED("recently_used"),
    SMILEYS_EMOTION("smileys_emotion"),
    PEOPLE_BODY("people_body"),
    ANIMALS_NATURE("animals_nature"),
    FOOD_DRINK("food_drink"),
    TRAVEL_PLACES("travel_places"),
    ACTIVITIES("activities"),
    OBJECTS("objects"),
    SYMBOLS("symbols"),
    FLAGS("flags");

    fun iconId(): Int {
        return when (this) {
            RECENTLY_USED -> R.drawable.ic_schedule
            SMILEYS_EMOTION -> R.drawable.ic_emoji_emotions
            PEOPLE_BODY -> R.drawable.ic_emoji_people
            ANIMALS_NATURE -> R.drawable.ic_emoji_nature
            FOOD_DRINK -> R.drawable.ic_emoji_food_beverage
            TRAVEL_PLACES -> R.drawable.ic_emoji_transportation
            ACTIVITIES -> R.drawable.ic_emoji_events
            OBJECTS -> R.drawable.ic_emoji_objects
            SYMBOLS -> R.drawable.ic_emoji_symbols
            FLAGS -> R.drawable.ic_emoji_flags
        }
    }
}
