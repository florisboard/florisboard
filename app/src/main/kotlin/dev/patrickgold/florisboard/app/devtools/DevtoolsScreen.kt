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

package dev.patrickgold.florisboard.app.devtools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.dictionary.FlorisUserDictionaryDatabase
import dev.patrickgold.florisboard.lib.android.AndroidSettings
import dev.patrickgold.florisboard.lib.compose.FlorisConfirmDeleteDialog
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

class DebugOnPurposeCrashException : Exception(
    "Success! The app crashed purposely to display this beautiful screen we all love :)"
)

@Composable
fun DevtoolsScreen() = FlorisScreen {
    title = stringRes(R.string.devtools__title)
    previewFieldVisible = true

    val navController = LocalNavController.current
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

    content {
        SwitchPreference(
            prefs.devtools.enabled,
            title = stringRes(R.string.devtools__enabled__label),
            summary = stringRes(R.string.devtools__enabled__summary),
        )

        PreferenceGroup(title = stringRes(R.string.devtools__title)) {
            SwitchPreference(
                prefs.devtools.showHeapMemoryStats,
                title = stringRes(R.string.devtools__show_heap_memory_stats__label),
                summary = stringRes(R.string.devtools__show_heap_memory_stats__summary),
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            SwitchPreference(
                prefs.devtools.showPrimaryClip,
                title = stringRes(R.string.devtools__show_primary_clip__label),
                summary = stringRes(R.string.devtools__show_primary_clip__summary),
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            SwitchPreference(
                prefs.devtools.showInputStateOverlay,
                title = stringRes(R.string.devtools__show_input_state_overlay__label),
                summary = stringRes(R.string.devtools__show_input_state_overlay__summary),
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            SwitchPreference(
                prefs.devtools.showSpellingOverlay,
                title = stringRes(R.string.devtools__show_spelling_overlay__label),
                summary = stringRes(R.string.devtools__show_spelling_overlay__summary),
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            SwitchPreference(
                prefs.devtools.showKeyTouchBoundaries,
                title = stringRes(R.string.devtools__show_key_touch_boundaries__label),
                summary = stringRes(R.string.devtools__show_key_touch_boundaries__summary),
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            Preference(
                title = stringRes(R.string.devtools__clear_udm_internal_database__label),
                summary = stringRes(R.string.devtools__clear_udm_internal_database__summary),
                onClick = { setShowDialog(true) },
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            Preference(
                title = stringRes(R.string.devtools__reset_flag__label, "flag_name" to "isImeSetUp"),
                summary = stringRes(R.string.devtools__reset_flag_is_ime_set_up__summary),
                onClick = {
                    prefs.internal.isImeSetUp.set(false)
                    navController.navigate(Routes.Setup.Screen) {
                        popUpTo(Routes.Settings.Home) {
                            inclusive = true
                        }
                    }
                },
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            Preference(
                title = stringRes(R.string.devtools__test_crash_report__label),
                summary = stringRes(R.string.devtools__test_crash_report__summary),
                onClick = { throw DebugOnPurposeCrashException() },
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
        }

        PreferenceGroup(title = stringRes(R.string.devtools__group_android__title)) {
            Preference(
                title = stringRes(R.string.devtools__android_settings_global__title),
                onClick = {
                    navController.navigate(
                        Routes.Devtools.AndroidSettings(AndroidSettings.Global.groupId)
                    )
                },
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            Preference(
                title = stringRes(R.string.devtools__android_settings_secure__title),
                onClick = {
                    navController.navigate(
                        Routes.Devtools.AndroidSettings(AndroidSettings.Secure.groupId)
                    )
                },
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            Preference(
                title = stringRes(R.string.devtools__android_settings_system__title),
                onClick = {
                    navController.navigate(
                        Routes.Devtools.AndroidSettings(AndroidSettings.System.groupId)
                    )
                },
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
            Preference(
                title = stringRes(R.string.devtools__android_locales__title),
                onClick = { navController.navigate(Routes.Devtools.AndroidLocales) },
                enabledIf = { prefs.devtools.enabled isEqualTo true },
            )
        }

        PreferenceGroup(title = "prefs.internal.version*") {
            val versionOnInstall by prefs.internal.versionOnInstall.observeAsState()
            Preference(
                title = "prefs.internal.versionOnInstall",
                summary = versionOnInstall,
            )
            val versionLastUse by prefs.internal.versionLastUse.observeAsState()
            Preference(
                title = "prefs.internal.versionLastUse",
                summary = versionLastUse,
            )
            val versionLastChangelog by prefs.internal.versionLastChangelog.observeAsState()
            Preference(
                title = "prefs.internal.versionLastChangelog",
                summary = versionLastChangelog,
            )
        }

        if (showDialog) {
            FlorisConfirmDeleteDialog(
                onConfirm = {
                    DictionaryManager.default().let {
                        it.loadUserDictionariesIfNecessary()
                        it.florisUserDictionaryDao()?.deleteAll()
                    }
                    setShowDialog(false)
                },
                onDismiss = { setShowDialog(false) },
                what = FlorisUserDictionaryDatabase.DB_FILE_NAME,
            )
        }
    }
}
