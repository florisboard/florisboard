package dev.patrickgold.florisboard.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R

class SettingsLauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set default preference values if user has not used preferences screen
        PreferenceManager.setDefaultValues(this, R.xml.prefs_kbd, true)

        startActivity(Intent(this, SettingsMainActivity::class.java))
    }
}
