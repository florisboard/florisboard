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

package dev.patrickgold.florisboard.settings.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.skydoves.colorpickerpreference.ColorPickerPreference
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SettingsFragmentThemeBinding
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import kotlinx.coroutines.*

class ThemeFragment : SettingsMainActivity.SettingsFragment(), CoroutineScope by MainScope(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: SettingsFragmentThemeBinding
    private lateinit var keyboardView: KeyboardView
    private lateinit var prefs: PrefHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefs = PrefHelper.getDefaultInstance(requireContext())
        binding = SettingsFragmentThemeBinding.inflate(inflater, container, false)

        launch(Dispatchers.Default) {
            val themeContext = ContextThemeWrapper(context, FlorisBoard.getDayNightBaseThemeId(prefs.internal.themeCurrentIsNight))
            val layoutManager = LayoutManager(themeContext)
            keyboardView = KeyboardView(themeContext)
            keyboardView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val m = resources.getDimension(R.dimen.keyboard_preview_margin).toInt()
                setMargins(m, m, m, m)
            }
            prefs.sync()
            keyboardView.isPreviewMode = true
            keyboardView.computedLayout = layoutManager.fetchComputedLayoutAsync(KeyboardMode.CHARACTERS, Subtype.DEFAULT).await()
            keyboardView.updateVisibility()
            keyboardView.onApplyThemeAttributes()
            withContext(Dispatchers.Main) {
                binding.root.addView(keyboardView, 0)
            }
        }

        loadThemePrefFragment()

        return binding.root
    }

    private fun loadThemePrefFragment() {
        childFragmentManager
            .beginTransaction()
            .replace(
                binding.prefsFrame.id,
                SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_theme)
            )
            .commit()
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        prefs.sync()
        key ?: return
        if (key == PrefHelper.Internal.THEME_CURRENT_BASED_ON) {
            loadThemePrefFragment()
        }
        if (key.startsWith("theme__")) {
            prefs.internal.themeCurrentIsModified = true
            keyboardView.onApplyThemeAttributes()
            keyboardView.invalidate()
            keyboardView.invalidateAllKeys()
        }
    }

    override fun onResume() {
        prefs.shared.registerOnSharedPreferenceChangeListener(this)
        super.onResume()
    }

    override fun onPause() {
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}
