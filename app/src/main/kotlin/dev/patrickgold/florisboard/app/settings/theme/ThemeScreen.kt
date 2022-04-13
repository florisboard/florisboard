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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.lib.android.launchUrl
import dev.patrickgold.florisboard.lib.compose.FlorisInfoCard
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun ThemeScreen() = FlorisScreen {
    title = stringRes(R.string.settings__theme__title)
    previewFieldVisible = true

    val context = LocalContext.current
    val navController = LocalNavController.current

    content {
        val themeMode by prefs.theme.mode.observeAsState()
        val dayThemeId by prefs.theme.dayThemeId.observeAsState()
        val nightThemeId by prefs.theme.nightThemeId.observeAsState()

        Card(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("If you want to give feedback on the new stylesheet editor and theme engine, please do so in below linked feedback thread:\n")
                Button(onClick = {
                    context.launchUrl("https://github.com/florisboard/florisboard/discussions/1531")
                }) {
                    Text("Open Feedback Thread")
                }
            }
        }

        ListPreference(
            prefs.theme.mode,
            iconId = R.drawable.ic_brightness_auto,
            title = stringRes(R.string.pref__theme__mode__label),
            entries = ThemeMode.listEntries(),
        )
        if (themeMode == ThemeMode.FOLLOW_TIME) {
            FlorisInfoCard(
                modifier = Modifier.padding(8.dp),
                text = """
                The theme mode "Follow time" is not available in this beta release.
            """.trimIndent()
            )
        }
        Preference(
            iconId = R.drawable.ic_palette,
            title = stringRes(R.string.settings__theme_manager__title_manage),
            onClick = {
                navController.navigate(Routes.Settings.ThemeManager(ThemeManagerScreenAction.MANAGE))
            },
        )

        PreferenceGroup(
            title = stringRes(R.string.pref__theme__day),
            enabledIf = { prefs.theme.mode isNotEqualTo ThemeMode.ALWAYS_NIGHT },
        ) {
            Preference(
                iconId = R.drawable.ic_light_mode,
                title = stringRes(R.string.pref__theme__any_theme__label),
                summary = dayThemeId.toString(),
                onClick = {
                    navController.navigate(Routes.Settings.ThemeManager(ThemeManagerScreenAction.SELECT_DAY))
                },
            )
            SwitchPreference(
                prefs.theme.dayThemeAdaptToApp,
                iconId = R.drawable.ic_format_paint,
                title = stringRes(R.string.pref__theme__any_theme_adapt_to_app__label),
                summary = stringRes(R.string.pref__theme__any_theme_adapt_to_app__summary),
                visibleIf = { false },
            )
        }

        PreferenceGroup(
            title = stringRes(R.string.pref__theme__night),
            enabledIf = { prefs.theme.mode isNotEqualTo ThemeMode.ALWAYS_DAY },
        ) {
            Preference(
                iconId = R.drawable.ic_dark_mode,
                title = stringRes(R.string.pref__theme__any_theme__label),
                summary = nightThemeId.toString(),
                onClick = {
                    navController.navigate(Routes.Settings.ThemeManager(ThemeManagerScreenAction.SELECT_NIGHT))
                },
            )
            SwitchPreference(
                prefs.theme.nightThemeAdaptToApp,
                iconId = R.drawable.ic_format_paint,
                title = stringRes(R.string.pref__theme__any_theme_adapt_to_app__label),
                summary = stringRes(R.string.pref__theme__any_theme_adapt_to_app__summary),
                visibleIf = { false },
            )
        }
    }
}
