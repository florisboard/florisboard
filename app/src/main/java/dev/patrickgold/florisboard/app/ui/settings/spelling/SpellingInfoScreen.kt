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

package dev.patrickgold.florisboard.app.ui.settings.spelling

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.launchUrl
import dev.patrickgold.florisboard.spellingManager
import dev.patrickgold.jetpref.ui.compose.Preference
import dev.patrickgold.jetpref.ui.compose.annotations.ExperimentalJetPrefUi

@OptIn(ExperimentalJetPrefUi::class)
@Composable
fun SpellingInfoScreen() = FlorisScreen(
    title = stringRes(R.string.settings__spelling__dict_sources_info),
    iconSpaceReserved = false,
) {
    val context = LocalContext.current
    val spellingManager by context.spellingManager()

    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringRes(R.string.settings__spelling__dict_sources_info__intro),
    )
    for (source in spellingManager.config.importSources) {
        Preference(
            title = source.label,
            summary = source.url,
            onClick = {
                source.url?.let { launchUrl(context, it) }
            },
        )
    }
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringRes(R.string.settings__spelling__dict_sources_info__other),
    )
}
