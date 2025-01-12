/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiFlags
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.EmojiSymbols
import androidx.compose.material.icons.filled.EmojiTransportation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
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

    fun icon(): ImageVector {
        return when (this) {
            RECENTLY_USED -> Icons.Default.Schedule
            SMILEYS_EMOTION -> Icons.Default.EmojiEmotions
            PEOPLE_BODY -> Icons.Default.EmojiPeople
            ANIMALS_NATURE -> Icons.Default.EmojiNature
            FOOD_DRINK -> Icons.Default.EmojiFoodBeverage
            TRAVEL_PLACES -> Icons.Default.EmojiTransportation
            ACTIVITIES -> Icons.Default.EmojiEvents
            OBJECTS -> Icons.Default.EmojiObjects
            SYMBOLS -> Icons.Default.EmojiSymbols
            FLAGS -> Icons.Default.EmojiFlags
        }
    }
}
