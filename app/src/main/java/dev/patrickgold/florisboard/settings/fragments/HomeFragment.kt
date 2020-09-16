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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SettingsFragmentHomeBinding
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import dev.patrickgold.florisboard.setup.SetupActivity

class HomeFragment : SettingsMainActivity.SettingsFragment() {
    private lateinit var binding: SettingsFragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SettingsFragmentHomeBinding.inflate(inflater, container, false)
        binding.imeNotEnabledCard.setOnClickListener {
            Intent(context, SetupActivity::class.java).apply {
                putExtra(SetupActivity.EXTRA_SHOW_SINGLE_STEP, SetupActivity.Step.ENABLE_IME)
                startActivity(this)
            }
        }
        binding.imeNotSelectedCard.setOnClickListener {
            Intent(context, SetupActivity::class.java).apply {
                putExtra(SetupActivity.EXTRA_SHOW_SINGLE_STEP, SetupActivity.Step.MAKE_DEFAULT)
                startActivity(this)
            }
        }
        binding.repoUrlCard.setOnClickListener {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__repo_url))
            ).apply {
                startActivity(this)
            }
        }
        binding.localizationCard.setOnClickListener {
            settingsMainActivity.binding.bottomNavigation.selectedItemId = R.id.settings__navigation__typing
        }
        binding.themeCard.setOnClickListener {
            settingsMainActivity.binding.bottomNavigation.selectedItemId = R.id.settings__navigation__theme
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateImeIssueCardsVisibilities()
    }

    private fun updateImeIssueCardsVisibilities() {
        val isImeEnabled = FlorisBoard.checkIfImeIsEnabled(requireContext())
        val isImeSelected = FlorisBoard.checkIfImeIsSelected(requireContext())
        binding.imeNotEnabledCard.visibility =
            if (isImeEnabled) {
                View.GONE
            } else {
                View.VISIBLE
            }
        binding.imeNotSelectedCard.visibility =
            if (!isImeEnabled || isImeSelected) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }
}
