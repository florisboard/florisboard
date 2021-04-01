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
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R

class GesturesFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_gestures)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val preference: Preference? = findPreference("glide__show_trail")
        preference?.isEnabled = sharedPreferences.getBoolean("glide__enabled", false)
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    override fun onPause()
    {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "glide__enabled"){
            val preference: Preference? = findPreference("glide__show_trail")
            preference?.isEnabled = sharedPreferences.getBoolean("glide__enabled", false)
        }
    }

}
