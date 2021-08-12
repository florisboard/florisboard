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

package dev.patrickgold.florisboard.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SettingsActivityBinding
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.settings.fragments.*
import dev.patrickgold.florisboard.util.AppVersionUtils
import dev.patrickgold.florisboard.util.PackageManagerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

internal const val FRAGMENT_TAG = "FRAGMENT_TAG"
private const val PREF_RES_ID = "PREF_RES_ID"
private const val SELECTED_ITEM_ID = "SELECTED_ITEM_ID"
private const val ADVANCED_REQ_CODE = 0x145F

class SettingsMainActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    CoroutineScope by MainScope() {

    lateinit var binding: SettingsActivityBinding
    lateinit var layoutManager: LayoutManager
    private val prefs get() = Preferences.default()
    val subtypeManager: SubtypeManager get() = SubtypeManager.default()

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutManager = LayoutManager()

        val mode = when (prefs.advanced.settingsTheme) {
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
                loadFragment(KeyboardFragment())
                true
            }
            R.id.settings__navigation__typing -> {
                supportActionBar?.setTitle(R.string.settings__typing__title)
                loadFragment(TypingFragment())
                true
            }
            R.id.settings__navigation__theme -> {
                supportActionBar?.setTitle(R.string.settings__theme__title)
                loadFragment(ThemeFragment())
                true
            }
            R.id.settings__navigation__gestures -> {
                supportActionBar?.setTitle(R.string.settings__gestures__title)
                loadFragment(GesturesFragment())
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_main_menu, menu)
        return true
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
            R.id.settings__menu_help -> {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(resources.getString(R.string.florisboard__repo_url))
                )
                startActivity(browserIntent)
                true
            }
            R.id.settings__menu_advanced -> {
                startActivityForResult(Intent(this, AdvancedActivity::class.java), ADVANCED_REQ_CODE)
                true
            }
            R.id.settings__menu_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ADVANCED_REQ_CODE) {
            if (resultCode == AdvancedActivity.RESULT_APPLY_THEME) {
                recreate()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {}

    private fun updateLauncherIconStatus() {
        // Set LauncherAlias enabled/disabled state just before destroying/pausing this activity
        if (prefs.advanced.showAppIcon) {
            PackageManagerUtils.showAppIcon(this)
        } else {
            PackageManagerUtils.hideAppIcon(this)
        }
    }

    override fun onResume() {
        prefs.shared.registerOnSharedPreferenceChangeListener(this)
        super.onResume()
    }

    override fun onPause() {
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        updateLauncherIconStatus()
        super.onPause()
    }

    override fun onDestroy() {
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        updateLauncherIconStatus()
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
