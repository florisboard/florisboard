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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SpellingFragmentManageDictionariesBinding
import dev.patrickgold.florisboard.ime.spelling.SpellingDict
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.settings.OnListItemCLickListener

class SpellingDictEntryAdapter(
    private val data: List<SpellingDict.Meta>,
    private val onListItemCLickListener: OnListItemCLickListener
) : RecyclerView.Adapter<SpellingDictEntryAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val onListItemCLickListener: OnListItemCLickListener) :
        RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(android.R.id.title)
        val summaryView: TextView = view.findViewById(android.R.id.summary)

        init {
            view.setOnClickListener {
                onListItemCLickListener.onListItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val listItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return ViewHolder(listItemView, onListItemCLickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleView.text = data[position].title
        holder.summaryView.text = StringBuilder().run {
            append(data[position].locale.languageTag())
            append(" | ")
            append(data[position].version)
            append(" | ")
            append(data[position].originalSourceId)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}

class ManageDictionariesFragment : Fragment() {
    private val assetManager get() = AssetManager.default()
    private val spellingManager get() = SpellingManager.default()
    private lateinit var binding: SpellingFragmentManageDictionariesBinding

    private var indexedDictRefs: List<FlorisRef> = listOf()
    private var indexedDictMetas: List<SpellingDict.Meta> = listOf()

    private val dictEntryClickListener = object : OnListItemCLickListener {
        override fun onListItemClick(pos: Int) {
            AlertDialog.Builder(requireContext()).apply {
                setTitle("Delete?")
                setCancelable(true)
                setPositiveButton("Yes") { _, _ ->
                    indexedDictRefs.getOrNull(pos)?.let { assetManager.deleteExtension(it) }
                    buildUi()
                }
                setNegativeButton("No", null)
                show()
            }
        }
    }

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

        binding.dictSourcesInfoCard.setOnClickListener {
            DictionarySourcesInfoFragment().apply {
                show(this@ManageDictionariesFragment.requireActivity().supportFragmentManager, null)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        buildUi()
    }

    fun buildUi() {
        if (spellingManager.indexSpellingDicts()) {
            binding.dictIndexFailedCard.isVisible = false
            binding.noDictInstalledCard.isVisible = spellingManager.indexedSpellingDicts.isEmpty()

            indexedDictRefs = spellingManager.indexedSpellingDicts.keys.toList()
            indexedDictMetas = spellingManager.indexedSpellingDicts.values.toList()
            binding.recyclerView.adapter = SpellingDictEntryAdapter(
                indexedDictMetas,
                dictEntryClickListener
            )
        } else {
            binding.dictIndexFailedCard.isVisible = true
            binding.noDictInstalledCard.isVisible = false
        }
    }
}
