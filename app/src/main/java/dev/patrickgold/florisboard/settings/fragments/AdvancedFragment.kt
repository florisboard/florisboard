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

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.dictionary.FlorisUserDictionaryDatabase

class AdvancedFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_advanced)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            Preferences.Devtools.CLEAR_UDM_INTERNAL_DATABASE -> {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle(R.string.assets__action__delete_confirm_title)
                    setMessage(String.format(resources.getString(R.string.assets__action__delete_confirm_message), FlorisUserDictionaryDatabase.DB_FILE_NAME))
                    setPositiveButton(R.string.assets__action__delete) { _, _ ->
                        DictionaryManager.default().let {
                            it.loadUserDictionariesIfNecessary()
                            it.florisUserDictionaryDao()?.deleteAll()
                        }
                    }
                    setNegativeButton(android.R.string.cancel, null)
                    create()
                    show()
                }
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}
