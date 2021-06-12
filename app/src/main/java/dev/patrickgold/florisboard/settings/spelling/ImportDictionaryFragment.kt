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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.patrickgold.florisboard.databinding.SpellingSheetImportDictionaryBinding
import dev.patrickgold.florisboard.ime.spelling.SpellingConfig
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import dev.patrickgold.florisboard.util.initItems
import dev.patrickgold.florisboard.util.setOnSelectedListener

class ImportDictionaryFragment : BottomSheetDialogFragment() {
    private val spellingManager get() = SpellingManager.default()

    private lateinit var binding: SpellingSheetImportDictionaryBinding

    private var selectedImportSource: SpellingConfig.ImportSource? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SpellingSheetImportDictionaryBinding.inflate(inflater, container, false)

        binding.s1SourceSpinner.initItems(spellingManager.importSourceLabels)
        binding.s1SourceSpinner.setOnSelectedListener { n ->
            if (n <= 0 || n > spellingManager.importSourceLabels.size) {
                binding.s2Group.isEnabled = false
                binding.s3Group.isEnabled = false
                selectedImportSource = null
            } else {
                binding.s2Group.isEnabled = true
                binding.s3Group.isEnabled = false
                selectedImportSource = spellingManager.config.importSources[n - 1]
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
    }
}
