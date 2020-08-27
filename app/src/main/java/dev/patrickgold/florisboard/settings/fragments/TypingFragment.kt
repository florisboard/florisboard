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

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.ListItemBinding
import dev.patrickgold.florisboard.databinding.SettingsFragmentTypingBinding
import dev.patrickgold.florisboard.databinding.SettingsFragmentTypingSubtypeDialogBinding
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import dev.patrickgold.florisboard.util.LocaleUtils

class TypingFragment : SettingsMainActivity.SettingsFragment() {
    private lateinit var binding: SettingsFragmentTypingBinding
    /**
     * Must always have a reference to the open AlertDialog to dismiss the AlertDialog in the event
     * of onDestroy(), if this is not done a memory leak will most likely happen!
     */
    private var activeDialogWindow: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SettingsFragmentTypingBinding.inflate(inflater, container, false)
        binding.subtypeAddBtn.setOnClickListener { showAddSubtypeDialog() }

        updateSubtypeListView()

        childFragmentManager
            .beginTransaction()
            .replace(
                binding.prefsFrame.id,
                SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_typing)
            )
            .commit()

        return binding.root
    }

    override fun onDestroy() {
        activeDialogWindow?.dismiss()
        super.onDestroy()
    }

    private fun showAddSubtypeDialog() {
        val dialogView =
            SettingsFragmentTypingSubtypeDialogBinding.inflate(layoutInflater)
        val languageAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            subtypeManager.imeConfig.defaultSubtypesLanguageNames
        )
        dialogView.languageSpinner.adapter = languageAdapter
        // Add listener to languageSpinner to automatically pre-select the preferred layout for the
        // selected language in layoutSpinner.
        dialogView.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selectedCode = subtypeManager.imeConfig.defaultSubtypesLanguageCodes[pos]
                val defaultSubtype = subtypeManager.getDefaultSubtypeForLocale(LocaleUtils.stringToLocale(selectedCode)) ?: return
                dialogView.layoutSpinner.setSelection(
                    subtypeManager.imeConfig.characterLayouts.keys.toList().indexOf(defaultSubtype.preferredLayout)
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Auto-generated stub method
            }
        }
        val layoutAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            subtypeManager.imeConfig.characterLayouts.values.toList()
        )
        dialogView.layoutSpinner.adapter = layoutAdapter
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__localization__subtype_add_title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(R.string.settings__localization__subtype_add, null)
            setNegativeButton(R.string.settings__localization__subtype_cancel) { _, _ -> }
            setOnDismissListener { activeDialogWindow = null }
            create()
            activeDialogWindow = show()
            // Overriding positive button click listener here to prevent dismiss() when subtype
            // already exists.
            activeDialogWindow?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val languageCode = subtypeManager.imeConfig.defaultSubtypesLanguageCodes[dialogView.languageSpinner.selectedItemPosition]
                val layoutName = subtypeManager.imeConfig.characterLayouts.keys.toList()[dialogView.layoutSpinner.selectedItemPosition]
                val success = subtypeManager.addSubtype(LocaleUtils.stringToLocale(languageCode), layoutName)
                if (!success) {
                    dialogView.errorBox.setText(R.string.settings__localization__subtype_error_already_exists)
                    dialogView.errorBox.visibility = View.VISIBLE
                } else {
                    updateSubtypeListView()
                    activeDialogWindow?.dismiss()
                }
            }
        }
    }

    private fun showEditSubtypeDialog(id: Int) {
        val subtype = subtypeManager.getSubtypeById(id) ?: return
        val dialogView =
            SettingsFragmentTypingSubtypeDialogBinding.inflate(layoutInflater)
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
            subtypeManager.imeConfig.characterLayouts.values.toList()
        )
        dialogView.layoutSpinner.adapter = layoutAdapter
        dialogView.layoutSpinner.setSelection(
            subtypeManager.imeConfig.characterLayouts.keys.toList().indexOf(subtype.layout)
        )
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__localization__subtype_edit_title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(R.string.settings__localization__subtype_apply) { _, _ ->
                val languageCode = subtypeManager.imeConfig.defaultSubtypesLanguageCodes[dialogView.languageSpinner.selectedItemPosition]
                val layoutName = subtypeManager.imeConfig.characterLayouts.keys.toList()[dialogView.layoutSpinner.selectedItemPosition]
                subtype.locale = LocaleUtils.stringToLocale(languageCode)
                subtype.layout = layoutName
                subtypeManager.modifySubtypeWithSameId(subtype)
                updateSubtypeListView()
            }
            setNeutralButton(R.string.settings__localization__subtype_delete) { _, _ ->
                subtypeManager.removeSubtype(subtype)
                updateSubtypeListView()
            }
            setNegativeButton(R.string.settings__localization__subtype_cancel) { _, _ -> }
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
                    ListItemBinding.inflate(layoutInflater)
                itemView.title.text = subtype.locale.displayName
                itemView.summary.text = subtypeManager.imeConfig.characterLayouts[subtype.layout]
                itemView.root.setOnClickListener { showEditSubtypeDialog(subtype.id) }
                binding.subtypeListView.addView(itemView.root)
            }
        }
    }
}
