package dev.patrickgold.florisboard.util

import android.content.Context
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R

fun initDefaultPreferences(context: Context) {
    PreferenceManager.setDefaultValues(context, R.xml.prefs_keyboard, true)
    PreferenceManager.setDefaultValues(context, R.xml.prefs_advanced, true)
}
