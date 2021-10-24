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

package dev.patrickgold.florisboard.app.ui.settings.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.launchActivity
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.oldsettings.ThemeManagerActivity
import dev.patrickgold.florisboard.util.AndroidVersion
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.ui.compose.ListPreference
import dev.patrickgold.jetpref.ui.compose.LocalTimePickerPreference
import dev.patrickgold.jetpref.ui.compose.Preference
import dev.patrickgold.jetpref.ui.compose.PreferenceGroup
import dev.patrickgold.jetpref.ui.compose.SwitchPreference
import dev.patrickgold.jetpref.ui.compose.annotations.ExperimentalJetPrefUi

@OptIn(ExperimentalJetPrefUi::class)
@Composable
fun ThemeManagerScreen() = FlorisScreen(title = stringRes(R.string.settings__theme__title)) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val dayThemeRef by prefs.theme.dayThemeRef.observeAsState()
    val nightThemeRef by prefs.theme.nightThemeRef.observeAsState()

    ListPreference(
        prefs.theme.mode,
        iconId = R.drawable.ic_brightness_auto,
        title = stringRes(R.string.pref__theme__mode__label),
        entries = ThemeMode.listEntries(),
    )

    PreferenceGroup(
        title = stringRes(R.string.pref__theme__day),
        enabledIf = { prefs.theme.mode isNotEqualTo ThemeMode.ALWAYS_NIGHT },
    ) {
        Preference(
            iconId = R.drawable.ic_light_mode,
            title = stringRes(R.string.pref__theme__any_theme__label),
            summary = dayThemeRef.toString(),
            onClick = {
                // TODO: this currently launches the old UI for theme manager
                launchActivity(context, ThemeManagerActivity::class) {
                    it.putExtra(ThemeManagerActivity.EXTRA_KEY, prefs.theme.dayThemeRef.key)
                    it.putExtra(ThemeManagerActivity.EXTRA_DEFAULT_VALUE, prefs.theme.dayThemeRef.default.toString())
                }
            },
        )
        if (AndroidVersion.ATLEAST_O) {
            LocalTimePickerPreference(
                prefs.theme.sunriseTime,
                iconId = R.drawable.ic_schedule,
                title = stringRes(R.string.pref__theme__sunrise_time__label),
                visibleIf = { prefs.theme.mode isEqualTo ThemeMode.FOLLOW_TIME },
            )
        }
        SwitchPreference(
            prefs.theme.dayThemeAdaptToApp,
            iconId = R.drawable.ic_format_paint,
            title = stringRes(R.string.pref__theme__any_theme_adapt_to_app__label),
            summary = stringRes(R.string.pref__theme__any_theme_adapt_to_app__summary),
        )
    }

    PreferenceGroup(
        title = stringRes(R.string.pref__theme__night),
        enabledIf = { prefs.theme.mode isNotEqualTo ThemeMode.ALWAYS_DAY },
    ) {
        Preference(
            iconId = R.drawable.ic_dark_mode,
            title = stringRes(R.string.pref__theme__any_theme__label),
            summary = nightThemeRef.toString(),
            onClick = {
                // TODO: this currently launches the old UI for theme manager
                launchActivity(context, ThemeManagerActivity::class) {
                    it.putExtra(ThemeManagerActivity.EXTRA_KEY, prefs.theme.nightThemeRef.key)
                    it.putExtra(ThemeManagerActivity.EXTRA_DEFAULT_VALUE, prefs.theme.nightThemeRef.default.toString())
                }
            },
        )
        if (AndroidVersion.ATLEAST_O) {
            LocalTimePickerPreference(
                prefs.theme.sunsetTime,
                iconId = R.drawable.ic_schedule,
                title = stringRes(R.string.pref__theme__sunset_time__label),
                visibleIf = { prefs.theme.mode isEqualTo ThemeMode.FOLLOW_TIME },
            )
        }
        SwitchPreference(
            prefs.theme.nightThemeAdaptToApp,
            iconId = R.drawable.ic_format_paint,
            title = stringRes(R.string.pref__theme__any_theme_adapt_to_app__label),
            summary = stringRes(R.string.pref__theme__any_theme_adapt_to_app__summary),
        )
    }
}
