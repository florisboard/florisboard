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

package dev.patrickgold.florisboard.app.ui.settings

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.util.AndroidVersion
import dev.patrickgold.jetpref.ui.compose.ListPreference
import dev.patrickgold.jetpref.ui.compose.Preference
import dev.patrickgold.jetpref.ui.compose.PreferenceGroup
import dev.patrickgold.jetpref.ui.compose.SwitchPreference
import dev.patrickgold.jetpref.ui.compose.entry

@Composable
fun AdvancedScreen() = FlorisScreen(title = stringResource(R.string.settings__advanced__title)) {
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

    ListPreference(
        prefs.advanced.settingsTheme,
        title = stringResource(R.string.pref__advanced__settings_theme__label),
        entries = listOf(
            entry(
                key = AppTheme.AUTO,
                label = stringResource(R.string.settings__system_default),
            ),
            entry(
                key = AppTheme.LIGHT,
                label = stringResource(R.string.pref__advanced__settings_theme__light),
            ),
            entry(
                key = AppTheme.DARK,
                label = stringResource(R.string.pref__advanced__settings_theme__dark),
            ),
            entry(
                key = AppTheme.AMOLED_DARK,
                label = stringResource(R.string.pref__advanced__settings_theme__amoled_dark),
            ),
        ),
    )
    ListPreference(
        prefs.advanced.settingsLanguage,
        title = stringResource(R.string.pref__advanced__settings_language__label),
        entries = listOf(
            entry(
                key = "auto",
                label = stringResource(R.string.settings__system_default),
            ),
        ),
    )
    SwitchPreference(
        prefs.advanced.showAppIcon,
        title = stringResource(R.string.pref__advanced__show_app_icon__label),
        summary = when {
            AndroidVersion.ATLEAST_Q -> stringResource(R.string.pref__advanced__show_app_icon__summary_atleast_q)
            else -> null
        },
        enabledIf = { AndroidVersion.ATMOST_P },
    )
    SwitchPreference(
        prefs.advanced.forcePrivateMode,
        title = stringResource(R.string.pref__advanced__force_private_mode__label),
        summary = stringResource(R.string.pref__advanced__force_private_mode__summary),
    )

    PreferenceGroup(title = stringResource(R.string.settings__devtools__title)) {
        SwitchPreference(
            prefs.devtools.enabled,
            title = stringResource(R.string.pref__devtools__enabled__label),
            summary = stringResource(R.string.pref__devtools__enabled__summary),
        )
        SwitchPreference(
            prefs.devtools.showHeapMemoryStats,
            title = stringResource(R.string.pref__devtools__show_heap_memory_stats__label),
            summary = stringResource(R.string.pref__devtools__show_heap_memory_stats__summary),
            enabledIf = { prefs.devtools.enabled isEqualTo true },
        )
        // TODO: remove this preference once word suggestions are re-implemented in 0.3.15
        SwitchPreference(
            prefs.devtools.overrideWordSuggestionsMinHeapRestriction,
            title = "Override min heap size restriction for word suggestions",
            summary = "This allows you to use word suggestions even if your heap size is not intended for it and can break FlorisBoard",
            enabledIf = { prefs.devtools.enabled isEqualTo true },
        )
        Preference(
            title = stringResource(R.string.pref__devtools__clear_udm_internal_database__label),
            summary = stringResource(R.string.pref__devtools__clear_udm_internal_database__summary),
            onClick = { setShowDialog(true) },
            enabledIf = { prefs.devtools.enabled isEqualTo true },
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { setShowDialog(false) },
            title = { Text(
                text = stringResource(R.string.assets__action__delete_confirm_title)
            ) },
            text = { Text(
                text = stringResource(R.string.assets__action__delete_confirm_message)
            ) },
            confirmButton = {
                Button(
                    onClick = {
                        DictionaryManager.default().let {
                            it.loadUserDictionariesIfNecessary()
                            it.florisUserDictionaryDao()?.deleteAll()
                        }
                        setShowDialog(false)
                    }
                ) {
                    Text(stringResource(R.string.assets__action__delete))
                }
            },
            dismissButton = {
                Button(
                    onClick = { setShowDialog(false) }
                ) {
                    Text(stringResource(R.string.assets__action__cancel))
                }
            },
        )
    }
}
