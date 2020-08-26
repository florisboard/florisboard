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

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SettingsFragmentLooknfeelBinding
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import kotlinx.coroutines.*

class LooknfeelFragment : SettingsMainActivity.SettingsFragment(), CoroutineScope by MainScope() {
    private lateinit var binding: SettingsFragmentLooknfeelBinding
    private lateinit var keyboardView: KeyboardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SettingsFragmentLooknfeelBinding.inflate(inflater, container, false)

        launch(Dispatchers.Default) {
            val themeContext = ContextThemeWrapper(context, prefs.theme.getSelectedThemeResId())
            val layoutManager = LayoutManager(themeContext)
            keyboardView = KeyboardView(themeContext)
            keyboardView.prefs = prefs
            keyboardView.isPreviewMode = true
            keyboardView.computedLayout = layoutManager.fetchComputedLayoutAsync(KeyboardMode.CHARACTERS, Subtype.DEFAULT).await()
            keyboardView.updateVisibility()
            withContext(Dispatchers.Main) {
                binding.themeLinearLayout.addView(keyboardView, 0)
            }
        }

        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(
            binding.prefsLooknfeelFrame.id,
            SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_keyboard)
        )
        transaction.replace(
            binding.prefsThemeFrame.id,
            SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_theme)
        )
        transaction.commit()

        return binding.root
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}
