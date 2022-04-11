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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.kotlin.curlyFormat
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

/**
 * DisplayColorsAs indicates how color strings should be visually presented to the user.
 */
enum class DisplayColorsAs {
    HEX8,
    RGBA;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = HEX8,
                label = stringRes(R.string.enum__display_colors_as__hex8),
                description = stringRes(R.string.general__example_given).curlyFormat("example" to "#4caf50ff"),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = RGBA,
                label = stringRes(R.string.enum__display_colors_as__rgba),
                description = stringRes(R.string.general__example_given).curlyFormat("example" to "rgba(76,175,80,1.0)"),
                showDescriptionOnlyIfSelected = true,
            )
        }
    }
}
