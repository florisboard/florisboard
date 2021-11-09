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

package dev.patrickgold.florisboard.oldsettings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.databinding.SettingsActivityBinding
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.oldsettings.fragments.*
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

internal const val FRAGMENT_TAG = "FRAGMENT_TAG"

class SettingsMainActivity : AppCompatActivity(),
    CoroutineScope by MainScope() {

    lateinit var binding: SettingsActivityBinding
    private val newPrefs by florisPreferenceModel()
    val subtypeManager by subtypeManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        val mode = when (newPrefs.advanced.settingsTheme.get().id) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "auto" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOTE: using findViewById() instead of view binding because the binding does not include
        //       a reference to the included layout...
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.settings__localization__title)
        loadFragment(TypingFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.pageFrame.id, fragment, FRAGMENT_TAG)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    abstract class SettingsFragment : Fragment() {
        protected lateinit var settingsMainActivity: SettingsMainActivity
        protected lateinit var subtypeManager: SubtypeManager

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            settingsMainActivity = activity as SettingsMainActivity
            subtypeManager = settingsMainActivity.subtypeManager
        }
    }
}
