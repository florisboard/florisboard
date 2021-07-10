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
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.settings.UdmActivity
import dev.patrickgold.florisboard.settings.spelling.SpellingActivity

class TypingInnerFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_typing)
        findPreference<Preference>(Preferences.Suggestion.API30_INLINE_SUGGESTIONS_ENABLED)?.let {
            it.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            Preferences.Correction.MANAGE_SPELL_CHECKER -> {
                val intent = Intent(context, SpellingActivity::class.java)
                startActivity(intent)
                true
            }
            Preferences.Dictionary.MANAGE_SYSTEM_USER_DICTIONARY -> {
                val intent = Intent(context, UdmActivity::class.java)
                intent.putExtra(UdmActivity.EXTRA_USER_DICTIONARY_TYPE, UdmActivity.USER_DICTIONARY_TYPE_SYSTEM)
                startActivity(intent)
                true
            }
            Preferences.Dictionary.MANAGE_FLORIS_USER_DICTIONARY -> {
                val intent = Intent(context, UdmActivity::class.java)
                intent.putExtra(UdmActivity.EXTRA_USER_DICTIONARY_TYPE, UdmActivity.USER_DICTIONARY_TYPE_FLORIS)
                startActivity(intent)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}
