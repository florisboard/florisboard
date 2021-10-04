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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.databinding.SettingsActivityBinding
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.oldsettings.fragments.*
import dev.patrickgold.florisboard.util.AppVersionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

internal const val FRAGMENT_TAG = "FRAGMENT_TAG"
private const val PREF_RES_ID = "PREF_RES_ID"
private const val SELECTED_ITEM_ID = "SELECTED_ITEM_ID"

class SettingsMainActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    CoroutineScope by MainScope() {

    lateinit var binding: SettingsActivityBinding
    lateinit var layoutManager: LayoutManager
    private val prefs get() = Preferences.default()
    private val newPrefs by florisPreferenceModel()
    val subtypeManager: SubtypeManager get() = SubtypeManager.default()

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutManager = LayoutManager(this)

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

        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
        binding.bottomNavigation.selectedItemId =
            savedInstanceState?.getInt(SELECTED_ITEM_ID) ?: R.id.settings__navigation__home

        AppVersionUtils.updateVersionOnInstallAndLastUse(this, prefs)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_ITEM_ID, binding.bottomNavigation.selectedItemId)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings__navigation__home -> {
                supportActionBar?.title = String.format(
                    resources.getString(R.string.settings__home__title),
                    resources.getString(R.string.floris_app_name)
                )
                loadFragment(HomeFragment())
                true
            }
            R.id.settings__navigation__keyboard -> {
                supportActionBar?.setTitle(R.string.settings__keyboard__title)
                loadFragment(PrefFragment.createFromResource(R.xml.prefs_keyboard))
                true
            }
            R.id.settings__navigation__typing -> {
                supportActionBar?.setTitle(R.string.settings__typing__title)
                loadFragment(TypingFragment())
                true
            }
            R.id.settings__navigation__theme -> {
                supportActionBar?.setTitle(R.string.settings__theme__title)
                loadFragment(PrefFragment.createFromResource(R.xml.prefs_theme))
                true
            }
            R.id.settings__navigation__gestures -> {
                supportActionBar?.setTitle(R.string.settings__gestures__title)
                loadFragment(PrefFragment.createFromResource(R.xml.prefs_gestures))
                true
            }
            else -> false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.pageFrame.id, fragment, FRAGMENT_TAG)
            .commit()
    }

    override fun onBackPressed() {
        if (binding.bottomNavigation.selectedItemId != R.id.settings__navigation__home) {
            binding.bottomNavigation.selectedItemId = R.id.settings__navigation__home
        } else {
            super.onBackPressed()
        }
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

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {}

    override fun onResume() {
        prefs.shared.registerOnSharedPreferenceChangeListener(this)
        super.onResume()
    }

    override fun onPause() {
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
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

    class PrefFragment : PreferenceFragmentCompat() {
        companion object {
            fun createFromResource(prefResId: Int): PrefFragment {
                val args = Bundle()
                args.putInt(PREF_RES_ID, prefResId)
                val fragment = PrefFragment()
                fragment.arguments = args
                return fragment
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(arguments?.getInt(PREF_RES_ID) ?: 0, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            listView.isFocusable = false
            listView.isNestedScrollingEnabled = false
            super.onViewCreated(view, savedInstanceState)
        }
    }
}
