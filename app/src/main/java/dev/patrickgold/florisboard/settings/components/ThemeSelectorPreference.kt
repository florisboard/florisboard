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
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeMetaOnly
import timber.log.Timber

/**
 * Custom preference which handles the theme preset selection dialog and shows a summary in the
 * list.
 */
class ThemeSelectorPreference : Preference, SharedPreferences.OnSharedPreferenceChangeListener {
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

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key == PrefHelper.Internal.THEME_CURRENT_IS_MODIFIED) {
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
        //sharedPreferences.edit().putString(key, "assets:ime/theme/floris_day.json").commit()
        /*val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = ThemeSelectorDialogBinding.inflate(inflater)
        val selectedThemeView = ThemeSelectorListItemBinding.inflate(inflater)
        selectedThemeView.title.text = generateSummaryText()
        dialogView.content.addView(selectedThemeView.root, 1)
        metaDataCache.clear()
        ThemeMetaOnly.loadAllFromDir(context, "ime/theme").forEach { metaData ->
            metaDataCache[metaData.name] = metaData
        }
        for ((themeKey, metaData) in metaDataCache) {
            if (themeKey == prefs.internal.themeCurrentBasedOn && !prefs.internal.themeCurrentIsModified) {
                continue
            }
            val availableThemeView = ThemeSelectorListItemBinding.inflate(inflater)
            availableThemeView.title.text = metaData.displayName
            availableThemeView.root.setOnClickListener {
                applyThemePreset(metaData.name)
                dialog?.dismiss()
            }
            dialogView.content.addView(availableThemeView.root)
        }
        AlertDialog.Builder(context).apply {
            setTitle(this@ThemePresetSelectorPreference.title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(android.R.string.ok) { _, _ ->
                //
            }
            setNeutralButton(R.string.settings__default) { _, _ ->
                //
            }
            setNegativeButton(android.R.string.cancel, null)
            setOnDismissListener { summary = generateSummaryText() }
            create()
            dialog = show()
            dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        }*/
    }

    /**
     * Applies the Theme for given [themeKey] to the preferences. Overrides any custom user-defined
     * theme in the shared prefs, if existent.
     *
     * @param themeKey The key of the Theme preset to be applied.
     */
    private fun applyThemePreset(themeKey: String) {
        /*val theme = Theme.fromJsonFile(context, "ime/theme/$themeKey.json") ?: return
        Theme.writeThemeToPrefs(prefs, theme)*/
    }
}