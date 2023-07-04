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

package dev.patrickgold.florisboard.ime.keyboard

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

enum class IncognitoMode {
    FORCE_OFF,
    FORCE_ON,
    DYNAMIC_ON_OFF;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = FORCE_OFF,
                label = stringRes(R.string.enum__incognito_mode__force_off),
                description = stringRes(R.string.enum__incognito_mode__force_off__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = DYNAMIC_ON_OFF,
                label = stringRes(R.string.enum__incognito_mode__dynamic_on_off),
                description = stringRes(R.string.enum__incognito_mode__dynamic_on_off__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = FORCE_ON,
                label = stringRes(R.string.enum__incognito_mode__force_on),
                description = stringRes(R.string.enum__incognito_mode__force_on__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    }
}
