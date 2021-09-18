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

package dev.patrickgold.florisboard.ime.onehanded

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.jetpref.ui.compose.entry

/**
 * Static object which contains all possible one-handed mode strings.
 */
object OneHandedMode {
    const val OFF: String = "off"
    const val START: String = "start"
    const val END: String = "end"

    @Composable
    fun listEntries() = listOf(
        entry(
            key = OFF,
            label = stringResource(R.string.enum__one_handed_mode__off),
        ),
        entry(
            key = START,
            label = stringResource(R.string.enum__one_handed_mode__start),
        ),
        entry(
            key = END,
            label = stringResource(R.string.enum__one_handed_mode__end),
        ),
    )
}
