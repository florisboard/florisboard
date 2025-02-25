/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.advanced

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ColorPickerPreference
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.ui.isMaterialYou
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.color.ColorMappings


@Composable
fun OtherScreen() = FlorisScreen {
    title = stringRes(R.string.settings__other__title)
    previewFieldVisible = false

    val navController = LocalNavController.current
    val context = LocalContext.current

    content {
        ListPreference(
            prefs.other.settingsTheme,
            icon = Icons.Default.Palette,
            title = stringRes(R.string.pref__other__settings_theme__label),
            entries = enumDisplayEntriesOf(AppTheme::class),
        )
        ColorPickerPreference(
            pref = prefs.other.accentColor,
            title = stringRes(R.string.pref__other__settings_accent_color__label),
            defaultValueLabel = stringRes(R.string.action__default),
            icon = Icons.Default.FormatColorFill,
            defaultColors = ColorMappings.colors,
            showAlphaSlider = false,
            enableAdvancedLayout = false,
            colorOverride = {
                if (it.isMaterialYou(context)) {
                    Color.Unspecified
                } else {
                    it
                }
            }
        )
        ListPreference(
            prefs.other.settingsLanguage,
            icon = Icons.Default.Language,
            title = stringRes(R.string.pref__other__settings_language__label),
            entries = listPrefEntries {
                listOf(
                    "auto",
                    "ar",
                    "bg",
                    "bs",
                    "ca",
                    "ckb",
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
                    "ja",
                    "ko-KR",
                    "ku",
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
                    "zh-CN",
                ).map { languageTag ->
                    if (languageTag == "auto") {
                        entry(
                            key = "auto",
                            label = stringRes(R.string.settings__system_default),
                        )
                    } else {
                        val displayLanguageNamesIn by prefs.localization.displayLanguageNamesIn.observeAsState()
                        val locale = FlorisLocale.fromTag(languageTag)
                        entry(locale.languageTag(), when (displayLanguageNamesIn) {
                            DisplayLanguageNamesIn.SYSTEM_LOCALE -> locale.displayName()
                            DisplayLanguageNamesIn.NATIVE_LOCALE -> locale.displayName(locale)
                        })
                    }
                }
            }
        )
        SwitchPreference(
            prefs.other.showAppIcon,
            icon = Icons.Default.Preview,
            title = stringRes(R.string.pref__other__show_app_icon__label),
            summary = when {
                AndroidVersion.ATLEAST_API29_Q -> stringRes(R.string.pref__other__show_app_icon__summary_atleast_q)
                else -> null
            },
            enabledIf = { AndroidVersion.ATMOST_API28_P },
        )
        Preference(
            icon = Icons.Default.Adb,
            title = stringRes(R.string.devtools__title),
            onClick = { navController.navigate(Routes.Devtools.Home) },
        )

        PreferenceGroup(title = stringRes(R.string.backup_and_restore__title)) {
            Preference(
                onClick = { navController.navigate(Routes.Settings.Backup) },
                icon = Icons.Default.Archive,
                title = stringRes(R.string.backup_and_restore__back_up__title),
                summary = stringRes(R.string.backup_and_restore__back_up__summary),
            )
            Preference(
                onClick = { navController.navigate(Routes.Settings.Restore) },
                icon = Icons.Default.SettingsBackupRestore,
                title = stringRes(R.string.backup_and_restore__restore__title),
                summary = stringRes(R.string.backup_and_restore__restore__summary),
            )
        }
    }
}
