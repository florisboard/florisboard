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
 * Enum for the key hint modes.
 */
enum class KeyHintMode {
    DISABLED,
    HINT_PRIORITY,
    ACCENT_PRIORITY,
    SMART_PRIORITY;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = ACCENT_PRIORITY,
                label = stringRes(R.string.enum__key_hint_mode__accent_priority),
                description = stringRes(R.string.enum__key_hint_mode__accent_priority__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = HINT_PRIORITY,
                label = stringRes(R.string.enum__key_hint_mode__hint_priority),
                description = stringRes(R.string.enum__key_hint_mode__hint_priority__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = SMART_PRIORITY,
                label = stringRes(R.string.enum__key_hint_mode__smart_priority),
                description = stringRes(R.string.enum__key_hint_mode__smart_priority__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    }
}
