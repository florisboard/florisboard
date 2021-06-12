/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.settings.spelling

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SpellingFragmentManageDictionariesBinding
import dev.patrickgold.florisboard.ime.spelling.SpellingManager

class ManageDictionariesFragment : Fragment() {
    private val spellingManager get() = SpellingManager.default()

    private lateinit var binding: SpellingFragmentManageDictionariesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SpellingFragmentManageDictionariesBinding.inflate(inflater, container, false)

        binding.dictIndexFailedCardTextView.text = String.format(
            resources.getString(R.string.settings__spelling__dictionary_index_invalid),
            resources.getString(R.string.floris_app_name)
        )

        binding.dictIndexFailedCard.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__issue_tracker_url))
            )
            startActivity(browserIntent)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (spellingManager.indexSpellingDicts()) {
            binding.dictIndexFailedCard.isVisible = false
            binding.noDictInstalledCard.isVisible = spellingManager.indexedSpellingDicts.isEmpty()
        } else {
            binding.dictIndexFailedCard.isVisible = true
            binding.noDictInstalledCard.isVisible = false
        }
    }
}
