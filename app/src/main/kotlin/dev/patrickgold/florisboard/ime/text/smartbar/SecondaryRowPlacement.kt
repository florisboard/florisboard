/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.smartbar

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

/**
 * Enum class defining the possible placements for the Smartbar secondary row.
 */
enum class SecondaryRowPlacement {
    ABOVE_PRIMARY,
    BELOW_PRIMARY,
    OVERLAY_APP_UI;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = ABOVE_PRIMARY,
                label = stringRes(R.string.enum__secondary_row_placement__above_primary),
                description = stringRes(R.string.enum__secondary_row_placement__above_primary__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = BELOW_PRIMARY,
                label = stringRes(R.string.enum__secondary_row_placement__below_primary),
                description = stringRes(R.string.enum__secondary_row_placement__below_primary__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = OVERLAY_APP_UI,
                label = stringRes(R.string.enum__secondary_row_placement__overlay_app_ui),
                description = stringRes(R.string.enum__secondary_row_placement__overlay_app_ui__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    }
}
