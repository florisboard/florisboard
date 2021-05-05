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

package dev.patrickgold.florisboard.settings.fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper

class TypingInnerFragment : PreferenceFragmentCompat() {
    companion object {
        private const val USER_DICTIONARY_SETTINGS_INTENT_ACTION: String =
            "android.settings.USER_DICTIONARY_SETTINGS"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_typing)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            PrefHelper.Dictionary.MANAGE_SYSTEM_USER_DICTIONARY -> {
                val intent = Intent(USER_DICTIONARY_SETTINGS_INTENT_ACTION)
                startActivity(intent)
                true
            }
            PrefHelper.Dictionary.MANAGE_INTERNAL_USER_DICTIONARY -> {
                // NYI
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}
