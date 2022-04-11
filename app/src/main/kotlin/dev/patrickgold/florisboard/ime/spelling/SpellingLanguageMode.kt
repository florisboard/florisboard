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

package dev.patrickgold.florisboard.ime.spelling

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

/**
 * Enum for the spelling language modes.
 */
enum class SpellingLanguageMode {
    USE_SYSTEM_LANGUAGES,
    USE_KEYBOARD_SUBTYPES;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = USE_SYSTEM_LANGUAGES,
                label = stringRes(R.string.enum__spelling_language_mode__use_system_languages),
            )
            entry(
                key = USE_KEYBOARD_SUBTYPES,
                label = stringRes(R.string.enum__spelling_language_mode__use_keyboard_subtypes),
            )
        }
    }
}
