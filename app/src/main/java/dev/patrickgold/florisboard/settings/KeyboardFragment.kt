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

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SettingsFragmentKeyboardBinding
import dev.patrickgold.florisboard.databinding.SettingsFragmentKeyboardSubtypeDialogBinding
import dev.patrickgold.florisboard.databinding.SettingsFragmentKeyboardSubtypeListItemBinding
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.LocaleUtils
import java.util.*

class KeyboardFragment : Fragment() {
    private lateinit var prefs: PrefHelper
    private lateinit var binding: SettingsFragmentKeyboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = (activity as SettingsMainActivity).prefs
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SettingsFragmentKeyboardBinding.inflate(inflater, container, false)
        binding.subtypeAddBtn.setOnClickListener { showAddSubtypeDialog() }

        updateSubtypeListView()

        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(
            binding.prefsCorrectionFrame.id,
            SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_correction)
        )
        transaction.commit()

        return binding.root
    }

    private fun showAddSubtypeDialog() {
        val dialogView =
            SettingsFragmentKeyboardSubtypeDialogBinding.inflate(layoutInflater)
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__keyboard__subtype_add_title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(R.string.settings__keyboard__subtype_add) { _, _ ->
                val languageCode = dialogView.languageSpinner.selectedItem.toString()
                val layoutName = dialogView.layoutSpinner.selectedItem.toString()
                prefs.keyboard.addSubtype(LocaleUtils.stringToLocale(languageCode), layoutName)
                updateSubtypeListView()
            }
            setNegativeButton(R.string.settings__keyboard__subtype_cancel) { _, _ -> }
            create()
            show()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun showEditSubtypeDialog(id: Int) {
        var subtypeIndex = -1
        val subtypes = prefs.keyboard.subtypes
        for ((i, subtype) in subtypes.withIndex()) {
            if (subtype.id == id) {
                subtypeIndex = i
                break
            }
        }
        if (subtypeIndex < 0) {
            return
        }
        val dialogView =
            SettingsFragmentKeyboardSubtypeDialogBinding.inflate(layoutInflater)
        dialogView.languageSpinner.setSelection(
            (dialogView.languageSpinner.adapter as ArrayAdapter<String>).getPosition(subtypes[subtypeIndex].locale.toString())
        )
        dialogView.layoutSpinner.setSelection(
            (dialogView.layoutSpinner.adapter as ArrayAdapter<String>).getPosition(subtypes[subtypeIndex].layoutName)
        )
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__keyboard__subtype_edit_title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(R.string.settings__keyboard__subtype_apply) { _, _ ->
                val languageCode = dialogView.languageSpinner.selectedItem.toString()
                val layoutName = dialogView.layoutSpinner.selectedItem.toString()
                subtypes[subtypeIndex].locale = LocaleUtils.stringToLocale(languageCode)
                subtypes[subtypeIndex].layoutName = layoutName
                prefs.keyboard.subtypes = subtypes
                updateSubtypeListView()
            }
            setNeutralButton(R.string.settings__keyboard__subtype_delete) { _, _ ->
                prefs.keyboard.removeSubtype(subtypes[subtypeIndex])
                updateSubtypeListView()
            }
            setNegativeButton(R.string.settings__keyboard__subtype_cancel) { _, _ -> }
            create()
            show()
        }
    }

    private fun updateSubtypeListView() {
        val subtypes = prefs.keyboard.subtypes
        binding.subtypeListView.removeAllViews()
        if (subtypes.isEmpty()) {
            binding.subtypeNotConfWarning.visibility = View.VISIBLE
        } else {
            binding.subtypeNotConfWarning.visibility = View.GONE
            for (subtype in subtypes) {
                val itemView =
                    SettingsFragmentKeyboardSubtypeListItemBinding.inflate(layoutInflater)
                itemView.title.text = subtype.locale.displayName
                itemView.caption.text = subtype.layoutName.toUpperCase(Locale.getDefault())
                itemView.root.setOnClickListener { showEditSubtypeDialog(subtype.id) }
                binding.subtypeListView.addView(itemView.root)
            }
        }
    }
}
