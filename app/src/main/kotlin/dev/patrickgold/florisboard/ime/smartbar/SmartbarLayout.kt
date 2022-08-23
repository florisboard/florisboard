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

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

enum class SmartbarLayout {
    SUGGESTIONS_ONLY,
    ACTIONS_ONLY,
    SUGGESTIONS_ACTIONS_SHARED,
    SUGGESTIONS_ACTIONS_EXTENDED;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = SUGGESTIONS_ONLY,
                label = stringRes(R.string.enum__smartbar_layout__suggestions_only),
                description = stringRes(R.string.enum__smartbar_layout__suggestions_only__description),
            )
            entry(
                key = ACTIONS_ONLY,
                label = stringRes(R.string.enum__smartbar_layout__actions_only),
                description = stringRes(R.string.enum__smartbar_layout__actions_only__description),
            )
            entry(
                key = SUGGESTIONS_ACTIONS_SHARED,
                label = stringRes(R.string.enum__smartbar_layout__suggestions_action_shared),
                description = stringRes(R.string.enum__smartbar_layout__suggestions_action_shared__description),
            )
            entry(
                key = SUGGESTIONS_ACTIONS_EXTENDED,
                label = stringRes(R.string.enum__smartbar_layout__suggestions_actions_extended),
                description = stringRes(R.string.enum__smartbar_layout__suggestions_actions_extended__description),
            )
        }
    }
}
