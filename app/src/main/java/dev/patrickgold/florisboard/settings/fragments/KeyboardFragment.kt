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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.settings.components.DialogSeekBarPreference

class KeyboardFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private var heightFactorCustom: DialogSeekBarPreference? = null
    private var oneHandedModeScaleFactor: DialogSeekBarPreference? = null
    private var utilityKeyAction: ListPreference? = null
    private var sharedPrefs: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_keyboard)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        heightFactorCustom = findPreference(Preferences.Keyboard.HEIGHT_FACTOR_CUSTOM)
        oneHandedModeScaleFactor = findPreference(Preferences.Keyboard.ONE_HANDED_MODE_SCALE_FACTOR)
        utilityKeyAction = findPreference(Preferences.Keyboard.UTILITY_KEY_ACTION)
        onSharedPreferenceChanged(null, Preferences.Keyboard.HEIGHT_FACTOR)
        onSharedPreferenceChanged(null, Preferences.Keyboard.ONE_HANDED_MODE)
        onSharedPreferenceChanged(null, Preferences.Keyboard.UTILITY_KEY_ENABLED)
    }

    override fun onResume() {
        super.onResume()
        sharedPrefs?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPrefs?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Preferences.Keyboard.HEIGHT_FACTOR -> {
                heightFactorCustom?.isVisible = sharedPrefs?.getString(key, "") == "custom"
            }
            Preferences.Keyboard.ONE_HANDED_MODE -> {
                oneHandedModeScaleFactor?.isEnabled = sharedPrefs?.getString(key, "") != OneHandedMode.OFF
            }
            Preferences.Keyboard.UTILITY_KEY_ENABLED -> {
                utilityKeyAction?.isVisible = sharedPrefs?.getBoolean(key, false) == true
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "keyboard__input_feedback" -> {
                // Hard-coded constant and access logic because this whole code base will be completely rewritten soon anyways
                (requireActivity() as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.settings__input_feedback__title)
                requireActivity().findViewById<View>(R.id.scroll_view)?.scrollY = 0
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.page_frame, InputFeedbackFragment())
                    .commit()
                return true
            }
        }
        return false
    }
}
