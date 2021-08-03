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
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatSpinner
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.databinding.ListItemBinding
import dev.patrickgold.florisboard.databinding.SettingsFragmentTypingBinding
import dev.patrickgold.florisboard.databinding.SettingsFragmentTypingSubtypeDialogBinding
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypeLayoutMap
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.text.layout.LayoutType
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import dev.patrickgold.florisboard.util.initItems
import dev.patrickgold.florisboard.util.setOnSelectedListener

class TypingFragment : SettingsMainActivity.SettingsFragment() {
    private val layoutManager: LayoutManager
        get() = (activity as? SettingsMainActivity)?.layoutManager!!

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
    ): View {
        binding = SettingsFragmentTypingBinding.inflate(inflater, container, false)
        binding.subtypeAddBtn.setOnClickListener { showAddSubtypeDialog() }

        updateSubtypeListView()

        childFragmentManager
            .beginTransaction()
            .replace(
                binding.prefsFrame.id,
                TypingInnerFragment()
            )
            .commit()

        return binding.root
    }

    override fun onDestroy() {
        activeDialogWindow?.dismiss()
        super.onDestroy()
    }

    private fun getSpinnerForLayoutType(
        dialogView: SettingsFragmentTypingSubtypeDialogBinding,
        layoutType: LayoutType
    ): AppCompatSpinner? {
        return when (layoutType) {
            LayoutType.CHARACTERS -> dialogView.charactersLayoutSpinner
            LayoutType.SYMBOLS -> dialogView.symbolsLayoutSpinner
            LayoutType.SYMBOLS2 -> dialogView.symbols2LayoutSpinner
            LayoutType.NUMERIC -> dialogView.numericLayoutSpinner
            LayoutType.NUMERIC_ADVANCED -> dialogView.numericAdvancedLayoutSpinner
            LayoutType.NUMERIC_ROW -> dialogView.numericRowLayoutSpinner
            LayoutType.PHONE -> dialogView.phoneLayoutSpinner
            LayoutType.PHONE2 -> dialogView.phone2LayoutSpinner
            else -> null
        }
    }

    private fun showAddSubtypeDialog() {
        val dialogView = SettingsFragmentTypingSubtypeDialogBinding.inflate(layoutInflater)

        dialogView.languageSpinner.initItems(
            labels = subtypeManager.imeConfig.defaultSubtypesLanguageNames
        )
        dialogView.languageSpinner.setOnSelectedListener { pos ->
            val selectedCode = subtypeManager.imeConfig.defaultSubtypesLanguageCodes[pos]
            val defaultSubtype = subtypeManager.getDefaultSubtypeForLocale(FlorisLocale.fromTag(selectedCode)) ?: return@setOnSelectedListener
            dialogView.composerSpinner.setSelection(
                subtypeManager.imeConfig.composerNames.indexOf(defaultSubtype.composerName).coerceAtLeast(0)
            )
            dialogView.currencySetSpinner.setSelection(
                subtypeManager.imeConfig.currencySetNames.indexOf(defaultSubtype.currencySetName).coerceAtLeast(0)
            )
            for (layoutType in LayoutType.values()) {
                getSpinnerForLayoutType(dialogView, layoutType)?.setSelection(
                    layoutManager.getMetaNameListFor(layoutType).indexOf(defaultSubtype.preferred[layoutType] ?: "").coerceAtLeast(0)
                )
            }
        }
        dialogView.composerSpinner.initItems(
            labels = subtypeManager.imeConfig.composerLabels
        )
        dialogView.currencySetSpinner.initItems(
            labels = subtypeManager.imeConfig.currencySetLabels
        )
        for (layoutType in LayoutType.values()) {
            getSpinnerForLayoutType(dialogView, layoutType)?.initItems(
                labels = layoutManager.getMetaLabelListFor(layoutType)
            )
        }

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
                val composerName = subtypeManager.imeConfig.composerNames[dialogView.composerSpinner.selectedItemPosition]
                val currencySetName = subtypeManager.imeConfig.currencySetNames[dialogView.currencySetSpinner.selectedItemPosition]
                val layoutMap = SubtypeLayoutMap(
                    characters = layoutManager.getMetaNameListFor(LayoutType.CHARACTERS)[dialogView.charactersLayoutSpinner.selectedItemPosition],
                    symbols = layoutManager.getMetaNameListFor(LayoutType.SYMBOLS)[dialogView.symbolsLayoutSpinner.selectedItemPosition],
                    symbols2 = layoutManager.getMetaNameListFor(LayoutType.SYMBOLS2)[dialogView.symbols2LayoutSpinner.selectedItemPosition],
                    numeric = layoutManager.getMetaNameListFor(LayoutType.NUMERIC)[dialogView.numericLayoutSpinner.selectedItemPosition],
                    numericAdvanced = layoutManager.getMetaNameListFor(LayoutType.NUMERIC_ADVANCED)[dialogView.numericAdvancedLayoutSpinner.selectedItemPosition],
                    numericRow = layoutManager.getMetaNameListFor(LayoutType.NUMERIC_ROW)[dialogView.numericRowLayoutSpinner.selectedItemPosition],
                    phone = layoutManager.getMetaNameListFor(LayoutType.PHONE)[dialogView.phoneLayoutSpinner.selectedItemPosition],
                    phone2 = layoutManager.getMetaNameListFor(LayoutType.PHONE2)[dialogView.phone2LayoutSpinner.selectedItemPosition],
                )
                val success = subtypeManager.addSubtype(FlorisLocale.fromTag(languageCode), composerName, currencySetName, layoutMap)
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
        val dialogView = SettingsFragmentTypingSubtypeDialogBinding.inflate(layoutInflater)

        dialogView.languageSpinner.initItems(
            labels = subtypeManager.imeConfig.defaultSubtypesLanguageNames,
            keys = subtypeManager.imeConfig.defaultSubtypesLanguageCodes,
            defaultSelectedKey = subtype.locale.localeTag()
        )
        dialogView.composerSpinner.initItems(
            labels = subtypeManager.imeConfig.composerLabels,
            keys = subtypeManager.imeConfig.composerNames,
            defaultSelectedKey = subtype.composerName
        )
        dialogView.currencySetSpinner.initItems(
            labels = subtypeManager.imeConfig.currencySetLabels,
            keys = subtypeManager.imeConfig.currencySetNames,
            defaultSelectedKey = subtype.currencySetName
        )
        for (layoutType in LayoutType.values()) {
            getSpinnerForLayoutType(dialogView, layoutType)?.initItems(
                labels = layoutManager.getMetaLabelListFor(layoutType),
                keys = layoutManager.getMetaNameListFor(layoutType),
                defaultSelectedKey = subtype.layoutMap[layoutType] ?: ""
            )
        }

        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__localization__subtype_edit_title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(R.string.settings__localization__subtype_apply) { _, _ ->
                val languageCode = subtypeManager.imeConfig.defaultSubtypesLanguageCodes[dialogView.languageSpinner.selectedItemPosition]
                val composerName = subtypeManager.imeConfig.composerNames[dialogView.composerSpinner.selectedItemPosition]
                val currencySetName = subtypeManager.imeConfig.currencySetNames[dialogView.currencySetSpinner.selectedItemPosition]
                val layoutMap = SubtypeLayoutMap(
                    characters = layoutManager.getMetaNameListFor(LayoutType.CHARACTERS)[dialogView.charactersLayoutSpinner.selectedItemPosition],
                    symbols = layoutManager.getMetaNameListFor(LayoutType.SYMBOLS)[dialogView.symbolsLayoutSpinner.selectedItemPosition],
                    symbols2 = layoutManager.getMetaNameListFor(LayoutType.SYMBOLS2)[dialogView.symbols2LayoutSpinner.selectedItemPosition],
                    numeric = layoutManager.getMetaNameListFor(LayoutType.NUMERIC)[dialogView.numericLayoutSpinner.selectedItemPosition],
                    numericAdvanced = layoutManager.getMetaNameListFor(LayoutType.NUMERIC_ADVANCED)[dialogView.numericAdvancedLayoutSpinner.selectedItemPosition],
                    numericRow = layoutManager.getMetaNameListFor(LayoutType.NUMERIC_ROW)[dialogView.numericRowLayoutSpinner.selectedItemPosition],
                    phone = layoutManager.getMetaNameListFor(LayoutType.PHONE)[dialogView.phoneLayoutSpinner.selectedItemPosition],
                    phone2 = layoutManager.getMetaNameListFor(LayoutType.PHONE2)[dialogView.phone2LayoutSpinner.selectedItemPosition],
                )
                subtypeManager.modifySubtypeWithSameId(Subtype(
                    id = subtype.id,
                    locale = FlorisLocale.fromTag(languageCode),
                    composerName = composerName,
                    currencySetName = currencySetName,
                    layoutMap = layoutMap
                ))
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
                val itemView = ListItemBinding.inflate(layoutInflater)
                itemView.title.text = subtype.locale.displayName()
                val layoutMeta = layoutManager.getMetaFor(
                    LayoutType.CHARACTERS,
                    subtype.layoutMap.characters
                )
                if (layoutMeta != null) {
                    itemView.summary.text = layoutMeta.label
                } else {
                    itemView.summary.text = resources.getString(
                        R.string.settings__localization__subtype_error_layout_not_installed,
                        subtype.layoutMap.characters
                    )
                    itemView.summary.setTextColor(ColorStateList.valueOf(Color.RED))
                }
                itemView.root.setOnClickListener { showEditSubtypeDialog(subtype.id) }
                binding.subtypeListView.addView(itemView.root)
            }
        }
    }
}
