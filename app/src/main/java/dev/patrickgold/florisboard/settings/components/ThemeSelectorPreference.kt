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

package dev.patrickgold.florisboard.settings.components

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.settings.ThemeManagerActivity

/**
 * Custom preference which handles the theme preset selection dialog and shows a summary in the
 * list.
 */
class ThemeSelectorPreference : Preference, SharedPreferences.OnSharedPreferenceChangeListener {
    private var defaultValue: String = when (key) {
        PrefHelper.Theme.DAY_THEME_REF -> "assets:ime/theme/floris_day.json"
        PrefHelper.Theme.NIGHT_THEME_REF -> "assets:ime/theme/floris_night.json"
        else -> ""
    }
    private var dialog: AlertDialog? = null
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    private val themeManager: ThemeManager = ThemeManager.default()

    @Suppress("unused")
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.dialogPreferenceStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        onPreferenceClickListener = OnPreferenceClickListener {
            showThemeSelectorDialog()
            true
        }
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager?) {
        super.onAttachedToHierarchy(preferenceManager)
        summary = generateSummaryText()
        prefs.shared.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetached() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        super.onDetached()
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, keyChanged: String?) {
        if (keyChanged == key) {
            summary = generateSummaryText()
        }
    }

    /**
     * Generates the summary text to display and returns it.
     */
    private fun generateSummaryText(): String {
        when (key) {
            PrefHelper.Theme.DAY_THEME_REF -> {
                val metaIndex = themeManager.indexedDayThemeRefs
                AssetRef.fromString(prefs.theme.dayThemeRef).onSuccess { ref ->
                    metaIndex[ref]?.label?.let { return it }
                }
            }
            PrefHelper.Theme.NIGHT_THEME_REF -> {
                val metaIndex = themeManager.indexedNightThemeRefs
                AssetRef.fromString(prefs.theme.nightThemeRef).onSuccess { ref ->
                    metaIndex[ref]?.label?.let { return it }
                }
            }
        }
        return "!! invalid ref !!"
    }

    /**
     * Shows the theme selector dialog.
     */
    private fun showThemeSelectorDialog() {
        val i = Intent(context, ThemeManagerActivity::class.java)
        i.putExtra(ThemeManagerActivity.EXTRA_KEY, key)
        i.putExtra(ThemeManagerActivity.EXTRA_DEFAULT_VALUE, defaultValue)
        context.startActivity(i)
    }
}
