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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.common.FlorisActivity
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.databinding.SpellingSheetImportDictionaryBinding
import dev.patrickgold.florisboard.ime.spelling.SpellingConfig
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.util.initItems
import dev.patrickgold.florisboard.util.setOnSelectedListener

class ImportDictionaryFragment(private val isArchive: Boolean) : BottomSheetDialogFragment() {
    private val assetManager get() = AssetManager.default()
    private val spellingManager get() = SpellingManager.default()
    private lateinit var binding: SpellingSheetImportDictionaryBinding
    private var selectedImportSource: SpellingConfig.ImportSource? = null
    private var selectedAffUri: Uri? = null
    private var selectedDicUri: Uri? = null

    private var importDict = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // If uri is null it indicates that the selection activity was cancelled (mostly by pressing the back button),
        // so we don't display an error message here.
        if (uri == null) return@registerForActivityResult
        binding.s2SelectedFilePath.text = uri.toString()
        val importSource = selectedImportSource ?: return@registerForActivityResult
        ViewUtils.setEnabled(binding.s3Group, true)
        spellingManager.prepareImport(importSource.id, uri).onSuccess { preprocessed ->
            with(preprocessed) {
                binding.s3VerifyLocale.text = resources.getString(
                    R.string.settings__spelling__import_dict_s3__verify_files_locale,
                    config.locale.languageTag()
                )
                binding.s3VerifyOriginal.text = resources.getString(
                    R.string.settings__spelling__import_dict_s3__verify_files_original,
                    config.originalSourceId
                )
                binding.s3VerifyAff.text = resources.getString(
                    R.string.settings__spelling__import_dict_s3__verify_files_any,
                    SpellingManager.AFF_EXT,
                    config.affFile
                )
                binding.s3VerifyDic.text = resources.getString(
                    R.string.settings__spelling__import_dict_s3__verify_files_any,
                    SpellingManager.DIC_EXT,
                    config.dicFile
                )
                binding.s3VerifyReadme.text = resources.getString(
                    R.string.settings__spelling__import_dict_s3__verify_files_readme,
                    config.readmeFile
                )
                binding.s3VerifyLicense.text = resources.getString(
                    R.string.settings__spelling__import_dict_s3__verify_files_license,
                    config.licenseFile
                )
            }
            binding.s3CompleteBtn.setOnClickListener {
                assetManager.writeExtension(preprocessed).onFailure { error ->
                    (activity as? FlorisActivity<*>)?.showErrorDialog(error)
                }
                dismiss()
                ((activity as? FlorisActivity<*>)?.supportFragmentManager?.findFragmentByTag(ManageDictionariesFragment::class.qualifiedName) as? ManageDictionariesFragment)?.buildUi()
            }
        }.onFailure { error ->
            (activity as? FlorisActivity<*>)?.showErrorDialog(error)
        }
    }

    private var importAff = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // If uri is null it indicates that the selection activity was cancelled (mostly by pressing the back button),
        // so we don't display an error message here.
        if (uri == null) return@registerForActivityResult
        selectedAffUri = uri
        binding.s2SelectedAffPath.text = uri.toString()
    }

    private var importDic = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // If uri is null it indicates that the selection activity was cancelled (mostly by pressing the back button),
        // so we don't display an error message here.
        if (uri == null) return@registerForActivityResult
        selectedDicUri = uri
        binding.s2SelectedDicPath.text = uri.toString()
        if (selectedAffUri != null) {
            ViewUtils.setEnabled(binding.s3Group, true)
            spellingManager.prepareImportRaw(selectedAffUri!!, selectedDicUri!!, binding.s11LanguageCode.text.toString()).onSuccess { preprocessed ->
                with(preprocessed) {
                    binding.s3VerifyLocale.text = resources.getString(
                        R.string.settings__spelling__import_dict_s3__verify_files_locale,
                        config.locale
                    )
                    binding.s3VerifyOriginal.text = resources.getString(
                        R.string.settings__spelling__import_dict_s3__verify_files_original,
                        config.originalSourceId
                    )
                    binding.s3VerifyAff.text = resources.getString(
                        R.string.settings__spelling__import_dict_s3__verify_files_any,
                        SpellingManager.AFF_EXT,
                        config.affFile
                    )
                    binding.s3VerifyDic.text = resources.getString(
                        R.string.settings__spelling__import_dict_s3__verify_files_any,
                        SpellingManager.DIC_EXT,
                        config.dicFile
                    )
                    binding.s3VerifyReadme.text = resources.getString(
                        R.string.settings__spelling__import_dict_s3__verify_files_readme,
                        config.readmeFile
                    )
                    binding.s3VerifyLicense.text = resources.getString(
                        R.string.settings__spelling__import_dict_s3__verify_files_license,
                        config.licenseFile
                    )
                }
                binding.s3CompleteBtn.setOnClickListener {
                    assetManager.writeExtension(preprocessed).onFailure { error ->
                        (activity as? FlorisActivity<*>)?.showErrorDialog(error)
                    }
                    dismiss()
                    ((activity as? FlorisActivity<*>)?.supportFragmentManager?.findFragmentByTag(ManageDictionariesFragment::class.qualifiedName) as? ManageDictionariesFragment)?.buildUi()
                }
            }.onFailure { error ->
                (activity as? FlorisActivity<*>)?.showErrorDialog(error)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SpellingSheetImportDictionaryBinding.inflate(inflater, container, false)

        ViewUtils.setEnabled(binding.s1Group, true)
        ViewUtils.setEnabled(binding.s2Group, true)
        ViewUtils.setEnabled(binding.s3Group, true)

        if (isArchive) {
            binding.s1SourceSpinner.initItems(spellingManager.importSourceLabels)
            binding.s1SourceSpinner.setOnSelectedListener { n ->
                if (n <= 0 || n > spellingManager.importSourceLabels.size) {
                    selectedImportSource = null
                    ViewUtils.setEnabled(binding.s2Group, false)
                    ViewUtils.setEnabled(binding.s3Group, false)
                } else {
                    selectedImportSource = spellingManager.config.importSources[n - 1]
                    ViewUtils.setEnabled(binding.s2Group, true)
                    ViewUtils.setEnabled(binding.s3Group, false)
                }
            }
            ViewUtils.setVisible(binding.s11Group, false)
            ViewUtils.setVisible(binding.s2SelectAffBtn, false)
            ViewUtils.setVisible(binding.s2SelectDicBtn, false)
        } else {
            ViewUtils.setVisible(binding.s1Group, false)
            ViewUtils.setVisible(binding.s2SelectFileBtn, false)
            ViewUtils.setEnabled(binding.s2Group, true)
            ViewUtils.setEnabled(binding.s3Group, false)
        }

        binding.s2SelectFileBtn.setOnClickListener {
            importDict.launch("*/*")
        }

        binding.s2SelectAffBtn.setOnClickListener {
            importAff.launch("*/*")
        }

        binding.s2SelectDicBtn.setOnClickListener {
            importDic.launch("*/*")
        }

        return binding.root
    }
}
