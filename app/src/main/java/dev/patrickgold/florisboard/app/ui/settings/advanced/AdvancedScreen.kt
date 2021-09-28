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

package dev.patrickgold.florisboard.app.ui.settings.advanced

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.res.stringRes
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.dictionary.FlorisUserDictionaryDatabase
import dev.patrickgold.florisboard.util.AndroidVersion
import dev.patrickgold.jetpref.ui.compose.JetPrefAlertDialog
import dev.patrickgold.jetpref.ui.compose.ListPreference
import dev.patrickgold.jetpref.ui.compose.ListPreferenceEntry
import dev.patrickgold.jetpref.ui.compose.Preference
import dev.patrickgold.jetpref.ui.compose.PreferenceGroup
import dev.patrickgold.jetpref.ui.compose.SwitchPreference
import dev.patrickgold.jetpref.ui.compose.entry

class DebugOnPurposeCrashException : Exception(
    "Success! The app crashed purposely to display this beautiful screen we all love :)"
)

@Composable
fun AdvancedScreen() = FlorisScreen(title = stringRes(R.string.settings__advanced__title)) {
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

    ListPreference(
        prefs.advanced.settingsTheme,
        title = stringRes(R.string.pref__advanced__settings_theme__label),
        entries = listOf(
            entry(
                key = AppTheme.AUTO,
                label = stringRes(R.string.settings__system_default),
            ),
            entry(
                key = AppTheme.LIGHT,
                label = stringRes(R.string.pref__advanced__settings_theme__light),
            ),
            entry(
                key = AppTheme.DARK,
                label = stringRes(R.string.pref__advanced__settings_theme__dark),
            ),
            entry(
                key = AppTheme.AMOLED_DARK,
                label = stringRes(R.string.pref__advanced__settings_theme__amoled_dark),
            ),
        ),
    )
    ListPreference(
        prefs.advanced.settingsLanguage,
        title = stringRes(R.string.pref__advanced__settings_language__label),
        entries = listOf(
            "auto",
            "ar",
            "bg",
            "bs",
            "ca",
            "ckb-IR",
            "cs",
            "da",
            "de",
            "el",
            "en",
            "eo",
            "es",
            "fa",
            "fi",
            "fr",
            "hr",
            "hu",
            "in",
            "it",
            "iw",
            "kmr-TR",
            "ko-KR",
            "lv-LV",
            "mk",
            "nds-DE",
            "nl",
            "no",
            "pl",
            "pt",
            "pt-BR",
            "ru",
            "sk",
            "sl",
            "sr",
            "sv",
            "tr",
            "uk",
            "zgh",
        ).map {
            if (it == "auto") {
                entry(
                    key = "auto",
                    label = stringRes(R.string.settings__system_default),
                )
            } else {
                FlorisLocale.fromTag(it).listEntry()
            }
        },
    )
    SwitchPreference(
        prefs.advanced.showAppIcon,
        title = stringRes(R.string.pref__advanced__show_app_icon__label),
        summary = when {
            AndroidVersion.ATLEAST_Q -> stringRes(R.string.pref__advanced__show_app_icon__summary_atleast_q)
            else -> null
        },
        enabledIf = { AndroidVersion.ATMOST_P },
    )
    SwitchPreference(
        prefs.advanced.forcePrivateMode,
        title = stringRes(R.string.pref__advanced__force_private_mode__label),
        summary = stringRes(R.string.pref__advanced__force_private_mode__summary),
    )

    PreferenceGroup(title = stringRes(R.string.settings__devtools__title)) {
        SwitchPreference(
            prefs.devtools.enabled,
            title = stringRes(R.string.pref__devtools__enabled__label),
            summary = stringRes(R.string.pref__devtools__enabled__summary),
        )
        SwitchPreference(
            prefs.devtools.showHeapMemoryStats,
            title = stringRes(R.string.pref__devtools__show_heap_memory_stats__label),
            summary = stringRes(R.string.pref__devtools__show_heap_memory_stats__summary),
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
            title = stringRes(R.string.pref__devtools__clear_udm_internal_database__label),
            summary = stringRes(R.string.pref__devtools__clear_udm_internal_database__summary),
            onClick = { setShowDialog(true) },
            enabledIf = { prefs.devtools.enabled isEqualTo true },
        )
        Preference(
            title = stringRes(R.string.pref__devtools__reset_flag__label, "flag_name" to "isImeSetUp"),
            summary = stringRes(R.string.pref__devtools__reset_flag_is_ime_set_up__summary),
            onClick = { prefs.internal.isImeSetUp.set(false) },
            enabledIf = { prefs.devtools.enabled isEqualTo true },
        )
        Preference(
            title = stringRes(R.string.pref__devtools__test_crash_report__label),
            summary = stringRes(R.string.pref__devtools__test_crash_report__summary),
            onClick = { throw DebugOnPurposeCrashException() },
            enabledIf = { prefs.devtools.enabled isEqualTo true },
        )
    }

    if (showDialog) {
        JetPrefAlertDialog(
            title = stringRes(R.string.assets__action__delete_confirm_title),
            confirmLabel = stringRes(R.string.assets__action__delete),
            onConfirm = {
                DictionaryManager.default().let {
                    it.loadUserDictionariesIfNecessary()
                    it.florisUserDictionaryDao()?.deleteAll()
                }
                setShowDialog(false)
            },
            dismissLabel = stringRes(R.string.assets__action__cancel),
            onDismiss = { setShowDialog(false) },
        ) {
            Text(
                text = stringRes(
                    R.string.assets__action__delete_confirm_message,
                    "database_name" to FlorisUserDictionaryDatabase.DB_FILE_NAME,
                )
            )
        }
    }
}

private fun FlorisLocale.listEntry(): ListPreferenceEntry<String> {
    return entry(languageTag(), displayName(this))
}
