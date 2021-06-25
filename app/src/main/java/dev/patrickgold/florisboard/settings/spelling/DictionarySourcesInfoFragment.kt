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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.patrickgold.florisboard.databinding.ListItemBinding
import dev.patrickgold.florisboard.databinding.SpellingSheetDictionarySourcesInfoBinding
import dev.patrickgold.florisboard.ime.spelling.SpellingManager

class DictionarySourcesInfoFragment : BottomSheetDialogFragment() {
    private val spellingManager get() = SpellingManager.default()
    private lateinit var binding: SpellingSheetDictionarySourcesInfoBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SpellingSheetDictionarySourcesInfoBinding.inflate(inflater, container, false)

        for (source in spellingManager.config.importSources) {
            val listItem = ListItemBinding.inflate(LayoutInflater.from(binding.sourceList.context), binding.sourceList, false)
            listItem.title.text = source.label
            val url = source.url ?: continue
            listItem.summary.text = url
            listItem.root.setOnClickListener {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
                startActivity(browserIntent)
            }
            binding.sourceList.addView(listItem.root)
        }

        return binding.root
    }
}
