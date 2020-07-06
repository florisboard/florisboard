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
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.util.LocaleUtils
import java.util.*

class KeyboardFragment : Fragment() {
    private lateinit var prefs: PrefHelper
    private lateinit var subtypeManager: SubtypeManager
    private lateinit var binding: SettingsFragmentKeyboardBinding
    /**
     * Must always have a reference to an open AlertDialog to dismiss the AlertDialog in the event
     * of onDestroy(), if this is not done a memory leak will most likely happen!
     */
    private var activeDialogWindow: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = (activity as SettingsMainActivity).prefs
        subtypeManager = (activity as SettingsMainActivity).subtypeManager
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
            binding.prefsKeyboardFrame.id,
            SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_keyboard)
        )
        transaction.commit()

        return binding.root
    }

    override fun onDestroy() {
        activeDialogWindow?.dismiss()
        super.onDestroy()
    }

    private fun showAddSubtypeDialog() {
        val dialogView =
            SettingsFragmentKeyboardSubtypeDialogBinding.inflate(layoutInflater)
        val languageAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            subtypeManager.imeConfig.defaultSubtypesLanguageNames
        )
        dialogView.languageSpinner.adapter = languageAdapter
        val layoutAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            subtypeManager.imeConfig.characterLayouts
        )
        dialogView.layoutSpinner.adapter = layoutAdapter
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__keyboard__subtype_add_title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(R.string.settings__keyboard__subtype_add) { _, _ ->
                val languageCode = subtypeManager.imeConfig.defaultSubtypesLanguageCodes[dialogView.languageSpinner.selectedItemPosition]
                val layoutName = subtypeManager.imeConfig.characterLayouts[dialogView.layoutSpinner.selectedItemPosition]
                subtypeManager.addSubtype(LocaleUtils.stringToLocale(languageCode), layoutName)
                updateSubtypeListView()
            }
            setNegativeButton(R.string.settings__keyboard__subtype_cancel) { _, _ -> }
            setOnDismissListener { activeDialogWindow = null }
            create()
            activeDialogWindow = show()
        }
    }

    private fun showEditSubtypeDialog(id: Int) {
        val subtype = subtypeManager.getSubtypeById(id) ?: return
        val dialogView =
            SettingsFragmentKeyboardSubtypeDialogBinding.inflate(layoutInflater)
        val languageAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            subtypeManager.imeConfig.defaultSubtypesLanguageNames
        )
        dialogView.languageSpinner.adapter = languageAdapter
        dialogView.languageSpinner.setSelection(
            subtypeManager.imeConfig.defaultSubtypesLanguageCodes.indexOf(subtype.locale.toString())
        )
        val layoutAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            subtypeManager.imeConfig.characterLayouts
        )
        dialogView.layoutSpinner.adapter = layoutAdapter
        dialogView.layoutSpinner.setSelection(
            subtypeManager.imeConfig.characterLayouts.indexOf(subtype.layout)
        )
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__keyboard__subtype_edit_title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(R.string.settings__keyboard__subtype_apply) { _, _ ->
                val languageCode = subtypeManager.imeConfig.defaultSubtypesLanguageCodes[dialogView.languageSpinner.selectedItemPosition]
                val layoutName = subtypeManager.imeConfig.characterLayouts[dialogView.layoutSpinner.selectedItemPosition]
                subtype.locale = LocaleUtils.stringToLocale(languageCode)
                subtype.layout = layoutName
                subtypeManager.modifySubtypeWithSameId(subtype)
                updateSubtypeListView()
            }
            setNeutralButton(R.string.settings__keyboard__subtype_delete) { _, _ ->
                subtypeManager.removeSubtype(subtype)
                updateSubtypeListView()
            }
            setNegativeButton(R.string.settings__keyboard__subtype_cancel) { _, _ -> }
            setOnDismissListener { activeDialogWindow = null }
            create()
            activeDialogWindow = show()
        }
    }

    private fun updateSubtypeListView() {
        val subtypes = subtypeManager.subtypes
        binding.subtypeListView.removeAllViews()
        if (subtypes.isEmpty()) {
            binding.subtypeNotConfWarning.visibility = View.VISIBLE
        } else {
            binding.subtypeNotConfWarning.visibility = View.GONE
            for (subtype in subtypes) {
                val itemView =
                    SettingsFragmentKeyboardSubtypeListItemBinding.inflate(layoutInflater)
                itemView.title.text = subtype.locale.displayName
                itemView.caption.text = subtype.layout.toUpperCase(Locale.getDefault())
                itemView.root.setOnClickListener { showEditSubtypeDialog(subtype.id) }
                binding.subtypeListView.addView(itemView.root)
            }
        }
    }
}
