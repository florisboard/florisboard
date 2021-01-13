/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.settings.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.settings.components.ThemeSelectorPreference

class ThemeFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private var dayThemeGroup: PreferenceCategory? = null
    private var nightThemeGroup: PreferenceCategory? = null
    private var dayThemeRef: ThemeSelectorPreference? = null
    private var nightThemeRef: ThemeSelectorPreference? = null
    private var sunrisePref: Preference? = null
    private var sunsetPref: Preference? = null
    private lateinit var prefs: PrefHelper

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_theme)

        prefs = PrefHelper.getDefaultInstance(requireContext())
        dayThemeGroup = findPreference("theme__day_group")
        nightThemeGroup = findPreference("theme__night_group")
        dayThemeRef = findPreference(PrefHelper.Theme.DAY_THEME_REF)
        nightThemeRef = findPreference(PrefHelper.Theme.NIGHT_THEME_REF)
        sunrisePref = findPreference(PrefHelper.Theme.SUNRISE_TIME)
        sunsetPref = findPreference(PrefHelper.Theme.SUNSET_TIME)
        onSharedPreferenceChanged(prefs.shared, PrefHelper.Theme.MODE)
    }

    private fun refreshUi() {
        when (prefs.theme.mode) {
            ThemeMode.ALWAYS_DAY -> {
                dayThemeGroup?.isEnabled = true
                nightThemeGroup?.isEnabled = false
                sunrisePref?.isVisible = false
                sunsetPref?.isVisible = false
            }
            ThemeMode.ALWAYS_NIGHT -> {
                dayThemeGroup?.isEnabled = false
                nightThemeGroup?.isEnabled = true
                sunrisePref?.isVisible = false
                sunsetPref?.isVisible = false
            }
            ThemeMode.FOLLOW_SYSTEM -> {
                dayThemeGroup?.isEnabled = true
                nightThemeGroup?.isEnabled = true
                sunrisePref?.isVisible = false
                sunsetPref?.isVisible = false
            }
            ThemeMode.FOLLOW_TIME -> {
                dayThemeGroup?.isEnabled = true
                nightThemeGroup?.isEnabled = true
                sunrisePref?.isVisible = true
                sunsetPref?.isVisible = true
            }
        }
        refreshThemeSelectors()
    }

    private fun refreshThemeSelectors() {
        dayThemeRef?.onSharedPreferenceChanged(null, dayThemeRef?.key)
        nightThemeRef?.onSharedPreferenceChanged(null, nightThemeRef?.key)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        prefs.sync()
        key ?: return
        if (key == PrefHelper.Theme.MODE) {
            refreshUi()
        }
    }

    override fun onResume() {
        prefs.shared.registerOnSharedPreferenceChangeListener(this)
        refreshThemeSelectors()
        super.onResume()
    }

    override fun onPause() {
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }
}
