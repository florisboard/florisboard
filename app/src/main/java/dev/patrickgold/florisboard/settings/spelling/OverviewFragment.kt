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

package dev.patrickgold.florisboard.settings.spelling

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.common.FlorisActivity
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.spelling.SpellingManager

class OverviewFragment : PreferenceFragmentCompat() {
    private val spellingManager get() = SpellingManager.default()

    private val activeSpellCheckerPref: Preference by lazy {
        findPreference(Preferences.Spelling.ACTIVE_SPELLCHECKER)!!
    }
    private val manageDictionariesPref: Preference by lazy {
        findPreference(Preferences.Spelling.MANAGE_DICTIONARIES)!!
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_spelling)

        activeSpellCheckerPref.setOnPreferenceClickListener {
            val componentToLaunch = ComponentName(
                "com.android.settings",
                "com.android.settings.Settings\$SpellCheckersSettingsActivity"
            )

            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.component = componentToLaunch
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            try {
                this.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                (activity as? FlorisActivity<*>)?.let {
                    it.showError(e)
                }
            }
            true
        }

        manageDictionariesPref.setOnPreferenceClickListener {
            (activity as? SpellingActivity)?.setActivePage(SpellingActivity.Page.MANAGE_DICTIONARIES)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        /*activeSpellCheckerPref.summary = spellingManager.getCurrentSpellingServiceName() ?:
            resources.getString(R.string.pref__spelling__active_spellchecker__summary_none)*/
        activeSpellCheckerPref.summary =
            resources.getString(R.string.pref__spelling__active_spellchecker__summary_check_in_system)
        val numInstalledDicts = spellingManager.indexedSpellingDicts.size
        manageDictionariesPref.summary = resources.getQuantityString(
            R.plurals.pref__spelling__manage_dictionaries__summary,
            numInstalledDicts,
            numInstalledDicts
        )
    }
}
