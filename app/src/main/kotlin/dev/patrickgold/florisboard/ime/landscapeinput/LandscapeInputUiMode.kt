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

package dev.patrickgold.florisboard.ime.landscapeinput

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

enum class LandscapeInputUiMode {
    NEVER_SHOW,
    ALWAYS_SHOW,
    DYNAMICALLY_SHOW;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = NEVER_SHOW,
                label = stringRes(R.string.enum__landscape_input_ui_mode__never_show),
            )
            entry(
                key = ALWAYS_SHOW,
                label = stringRes(R.string.enum__landscape_input_ui_mode__always_show),
            )
            entry(
                key = DYNAMICALLY_SHOW,
                label = stringRes(R.string.enum__landscape_input_ui_mode__dynamically_show),
            )
        }
    }
}
