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
 * Enum class defining the display mode for the candidates.
 */
enum class CandidatesDisplayMode {
    CLASSIC,
    DYNAMIC,
    DYNAMIC_SCROLLABLE;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = CLASSIC,
                label = stringRes(R.string.enum__candidates_display_mode__classic),
            )
            entry(
                key = DYNAMIC,
                label = stringRes(R.string.enum__candidates_display_mode__dynamic),
            )
            entry(
                key = DYNAMIC_SCROLLABLE,
                label = stringRes(R.string.enum__candidates_display_mode__dynamic_scrollable),
            )
        }
    }
}
