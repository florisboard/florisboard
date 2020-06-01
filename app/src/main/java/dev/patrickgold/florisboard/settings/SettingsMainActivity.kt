package dev.patrickgold.florisboard.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.hideAppIcon
import dev.patrickgold.florisboard.util.showAppIcon

private const val FRAGMENT_TAG = "FRAGMENT_TAG"
private const val PREF_RES_ID = "PREF_RES_ID"
private const val SELECTED_ITEM_ID = "SELECTED_ITEM_ID"

class SettingsMainActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var navigationView: BottomNavigationView
    lateinit var prefs: PrefHelper
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PrefHelper(this, PreferenceManager.getDefaultSharedPreferences(this))

        val mode = when (prefs.advanced.settingsTheme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "auto" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        navigationView = findViewById(R.id.settings__navigation)
        navigationView.setOnNavigationItemSelectedListener(this)

        scrollView = findViewById(R.id.settings__scroll_view)

        if (savedInstanceState != null) {
            val selectedId = savedInstanceState.getInt(SELECTED_ITEM_ID)
            navigationView.selectedItemId = selectedId
        } else {
            supportActionBar?.title = String.format(
                resources.getString(R.string.settings__home__title),
                resources.getString(R.string.app_name)
            )
            loadFragment(HomeFragment())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_ITEM_ID, navigationView.selectedItemId)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings__navigation__home -> {
                supportActionBar?.title = String.format(
                    resources.getString(R.string.settings__home__title),
                    resources.getString(R.string.app_name)
                )
                loadFragment(HomeFragment())
                true
            }
            R.id.settings__navigation__keyboard -> {
                supportActionBar?.setTitle(R.string.settings__keyboard__title)
                loadFragment(KeyboardFragment())
                true
            }
            R.id.settings__navigation__looknfeel -> {
                supportActionBar?.setTitle(R.string.settings__looknfeel__title)
                loadFragment(LooknfeelFragment())
                true
            }
            R.id.settings__navigation__gestures -> {
                supportActionBar?.setTitle(R.string.settings__gestures__title)
                loadFragment(GesturesFragment())
                true
            }
            R.id.settings__navigation__advanced -> {
                supportActionBar?.setTitle(R.string.settings__advanced__title)
                loadFragment(AdvancedFragment())
                true
            }
            else -> false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.settings__frame_container, fragment, FRAGMENT_TAG)
        //transaction.addToBackStack(null)
        transaction.commit()
        supportFragmentManager.executePendingTransactions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_main_menu, menu)
        return true
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
            R.id.settings__menu_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key == PrefHelper.Advanced.SETTINGS_THEME) {
            recreate()
        }
        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (fragment != null && fragment.isVisible) {
            if (fragment is LooknfeelFragment) {
                if (key == PrefHelper.Theme.NAME) {
                    fragment.keyboardView.context.setTheme(prefs.theme.getSelectedThemeResId())
                }
                fragment.keyboardView.invalidate()
                fragment.keyboardView.invalidateAllKeys()
                fragment.keyboardView.requestLayout()
                fragment.keyboardView.requestLayoutAllKeys()
            }
        }
    }

    private fun updateLauncherIconStatus() {
        // Set LauncherAlias enabled/disabled state just before destroying/pausing this activity
        if (prefs.advanced.showAppIcon) {
            showAppIcon(this)
        } else {
            hideAppIcon(this)
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
