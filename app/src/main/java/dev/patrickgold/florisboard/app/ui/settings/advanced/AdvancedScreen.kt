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

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.common.android.AndroidVersion
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

@Composable
fun AdvancedScreen() = FlorisScreen {
    title = stringRes(R.string.settings__advanced__title)

    content {
        ListPreference(
            prefs.advanced.settingsTheme,
            title = stringRes(R.string.pref__advanced__settings_theme__label),
            entries = listPrefEntries {
                entry(
                    key = AppTheme.AUTO,
                    label = stringRes(R.string.settings__system_default),
                )
                entry(
                    key = AppTheme.LIGHT,
                    label = stringRes(R.string.pref__advanced__settings_theme__light),
                )
                entry(
                    key = AppTheme.DARK,
                    label = stringRes(R.string.pref__advanced__settings_theme__dark),
                )
                entry(
                    key = AppTheme.AMOLED_DARK,
                    label = stringRes(R.string.pref__advanced__settings_theme__amoled_dark),
                )
            },
        )
        ListPreference(
            prefs.advanced.settingsLanguage,
            title = stringRes(R.string.pref__advanced__settings_language__label),
            entries = listPrefEntries {
                listOf(
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
                ).map { languageTag ->
                    if (languageTag == "auto") {
                        entry(
                            key = "auto",
                            label = stringRes(R.string.settings__system_default),
                        )
                    } else {
                        val locale = FlorisLocale.fromTag(languageTag)
                        entry(locale.languageTag(), locale.displayName(locale))
                    }
                }
            }
        )
        SwitchPreference(
            prefs.advanced.showAppIcon,
            title = stringRes(R.string.pref__advanced__show_app_icon__label),
            summary = when {
                AndroidVersion.ATLEAST_API29_Q -> stringRes(R.string.pref__advanced__show_app_icon__summary_atleast_q)
                else -> null
            },
            enabledIf = { AndroidVersion.ATMOST_API28_P },
        )
        SwitchPreference(
            prefs.advanced.forcePrivateMode,
            title = stringRes(R.string.pref__advanced__force_private_mode__label),
            summary = stringRes(R.string.pref__advanced__force_private_mode__summary),
        )
    }
}
