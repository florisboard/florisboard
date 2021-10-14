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

package dev.patrickgold.florisboard.oldsettings.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.common.InputMethodUtils
import dev.patrickgold.florisboard.databinding.SettingsFragmentHomeBinding
import dev.patrickgold.florisboard.oldsettings.SettingsMainActivity

class HomeFragment : SettingsMainActivity.SettingsFragment() {
    private lateinit var binding: SettingsFragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentHomeBinding.inflate(inflater, container, false)
        binding.newSettings.setOnClickListener {
            Intent(context, FlorisAppActivity::class.java).apply {
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

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateImeIssueCardsVisibilities()
    }

    private fun updateImeIssueCardsVisibilities() {
        val isImeEnabled = InputMethodUtils.checkIsFlorisboardEnabled(requireContext())
        val isImeSelected = InputMethodUtils.checkIsFlorisboardSelected(requireContext())
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
