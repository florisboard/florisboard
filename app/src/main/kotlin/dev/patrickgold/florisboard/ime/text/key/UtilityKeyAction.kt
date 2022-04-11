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
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

/**
 * Enum for declaring the utility key actions.
 */
enum class UtilityKeyAction {
    SWITCH_TO_EMOJIS,
    SWITCH_LANGUAGE,
    SWITCH_KEYBOARD_APP,
    DYNAMIC_SWITCH_LANGUAGE_EMOJIS,
    DISABLED;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = SWITCH_TO_EMOJIS,
                label = stringRes(R.string.enum__utility_key_action__switch_to_emojis),
            )
            entry(
                key = SWITCH_LANGUAGE,
                label = stringRes(R.string.enum__utility_key_action__switch_language),
            )
            entry(
                key = SWITCH_KEYBOARD_APP,
                label = stringRes(R.string.enum__utility_key_action__switch_keyboard_app),
            )
            entry(
                key = DYNAMIC_SWITCH_LANGUAGE_EMOJIS,
                label = stringRes(R.string.enum__utility_key_action__dynamic_switch_language_emojis),
            )
        }
    }
}
