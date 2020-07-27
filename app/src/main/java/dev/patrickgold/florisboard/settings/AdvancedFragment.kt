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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SettingsFragmentAdvancedBinding

class AdvancedFragment : SettingsMainActivity.SettingsFragment() {
    private lateinit var binding: SettingsFragmentAdvancedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SettingsFragmentAdvancedBinding.inflate(inflater, container, false)

        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(
            binding.prefsAdvancedFrame.id,
            SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_advanced)
        )
        transaction.commit()

        return binding.root
    }
}
