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

package dev.patrickgold.florisboard.app.settings.spelling

import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.ext.ExtensionList
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.lib.compose.FlorisInfoCard
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes

@Composable
fun ManageSpellingDictsScreen() = FlorisScreen {
    title = stringRes(R.string.settings__spelling__manage_dicts__title)
    previewFieldVisible = true

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    floatingActionButton {
        FloatingActionButton(
            onClick = { navController.navigate(Routes.Settings.ImportSpellingArchive) },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Add dictionary",
            )
        }
    }

    content {
        FlorisInfoCard(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
            text = stringRes(R.string.settings__spelling__dict_sources_info__title),
            onClick = { navController.navigate(Routes.Settings.SpellingInfo) },
        )

        val spellingDicts by extensionManager.spellingDicts.observeAsState()
        if (spellingDicts != null && spellingDicts!!.isNotEmpty()) {
            ExtensionList(
                extList = spellingDicts!!,
                summaryProvider = { ext ->
                    "${ext.spelling.locale.languageTag()} | ${ext.meta.version} | ${ext.spelling.originalSourceId}"
                },
            )
        } else {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringRes(R.string.settings__spelling__manage_dicts__no_dicts_installed),
            )
        }
    }
}
