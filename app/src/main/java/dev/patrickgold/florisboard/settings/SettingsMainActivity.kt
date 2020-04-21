package dev.patrickgold.florisboard.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.util.hideAppIcon
import dev.patrickgold.florisboard.util.showAppIcon

class SettingsMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.settings__keyboard,
                SettingsKeyboardFragment()
            )
            .commit()
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.settings__advanced,
                SettingsAdvancedFragment()
            )
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDestroy() {
        // TODO: The app icon show/hide process should happen immediately as the pref is changed,
        //       but this causes the app to close itself. The current way (doing it onDestroy) is
        //       only a workaround, until I've found a solution for this.
        // Set LauncherAlias enabled/disabled state just before destroying this activity
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("advanced__show_app_icon", false)) {
            showAppIcon(this)
        } else {
            hideAppIcon(this)
        }

        super.onDestroy()
    }

    class SettingsKeyboardFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_keyboard, rootKey)
        }
    }

    class SettingsAdvancedFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_advanced, rootKey)
        }
    }
}
