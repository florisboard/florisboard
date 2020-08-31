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

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SettingsFragmentKeyboardBinding
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import kotlinx.coroutines.*

class KeyboardFragment : SettingsMainActivity.SettingsFragment(), CoroutineScope by MainScope() {
    private lateinit var binding: SettingsFragmentKeyboardBinding
    private lateinit var keyboardView: KeyboardView
    private lateinit var prefs: PrefHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefs = PrefHelper.getDefaultInstance(requireContext())
        binding = SettingsFragmentKeyboardBinding.inflate(inflater, container, false)

        binding.themeModifyBtn.setOnClickListener {
            settingsMainActivity.supportFragmentManager
                .beginTransaction()
                .replace(
                    settingsMainActivity.binding.pageFrame.id,
                    ThemeFragment()
                )
                .commit()
                settingsMainActivity.supportActionBar?.setTitle(R.string.settings__theme__title)
        }

        launch(Dispatchers.Default) {
            val themeContext = ContextThemeWrapper(context, prefs.theme.getSelectedThemeResId())
            val layoutManager = LayoutManager(themeContext)
            keyboardView = KeyboardView(themeContext)
            keyboardView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8,8,8,16)
            }
            prefs.sync()
            keyboardView.isPreviewMode = true
            keyboardView.computedLayout = layoutManager.fetchComputedLayoutAsync(KeyboardMode.CHARACTERS, Subtype.DEFAULT).await()
            keyboardView.updateVisibility()
            withContext(Dispatchers.Main) {
                binding.themeLinearLayout.addView(keyboardView, 1)
            }
        }

        childFragmentManager
            .beginTransaction()
            .replace(
                binding.prefsFrame.id,
                SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_keyboard)
            )
            .commit()

        return binding.root
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}
