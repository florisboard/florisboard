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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.common.FlorisActivity
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.databinding.ThemeManagerActivityBinding
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.DefaultComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.key.CurrencySet
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.*
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeMetaOnly
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.res.FlorisRef
import kotlinx.coroutines.launch

class ThemeManagerActivity : FlorisActivity<ThemeManagerActivityBinding>() {
    private lateinit var layoutManager: LayoutManager
    private val themeManager: ThemeManager get() = ThemeManager.default()
    private val assetManager: AssetManager get() = AssetManager.default()

    private lateinit var textKeyboardIconSet: TextKeyboardIconSet
    private val textComputingEvaluator = object : ComputingEvaluator by DefaultComputingEvaluator {
        override fun evaluateVisible(data: KeyData): Boolean {
            return data.code != KeyCode.SWITCH_TO_MEDIA_CONTEXT
        }

        override fun isSlot(data: KeyData): Boolean {
            return CurrencySet.isCurrencySlot(data.code)
        }

        override fun getSlotData(data: KeyData): KeyData {
            return TextKeyData(label = "$")
        }
    }

    private var key: String = ""
    private var defaultValue: String = ""
    private var selectedTheme: Theme = Theme.empty()
    private var selectedRef: FlorisRef? = null

    private val themeEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
        if (result?.resultCode == ThemeEditorActivity.RESULT_CODE_THEME_EDIT_SAVED) {
            themeManager.update()
            buildUi()
        }
    }

    private val importTheme = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // If uri is null it indicates that the selection activity was cancelled (mostly by pressing the back button),
        // so we don't display an error message here.
        if (uri == null) return@registerForActivityResult
        val toBeImportedTheme = themeManager.loadTheme(uri)
        if (toBeImportedTheme.isSuccess) {
            val newTheme = toBeImportedTheme.getOrNull()!!.copy(
                name = toBeImportedTheme.getOrNull()!!.name + "_imported",
                label = toBeImportedTheme.getOrNull()!!.label + " (Imported)"
            )
            val newAssetRef = FlorisRef.internal(ThemeManager.THEME_PATH_REL + "/" + newTheme.name + ".json")
            themeManager.writeTheme(newAssetRef, newTheme).onSuccess {
                themeManager.update()
                selectedTheme = newTheme
                selectedRef = newAssetRef
                setThemeRefInPrefs(newAssetRef)
                buildUi()
                showMessage(R.string.settings__theme_manager__theme_import_success)
            }.onFailure {
                showError(it)
            }
        } else {
            showError(toBeImportedTheme.exceptionOrNull()!!)
        }
    }

    private val exportTheme = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        // If uri is null it indicates that the selection activity was cancelled (mostly by pressing the back button,
        // so we don't display an error message here.
        if (uri == null) return@registerForActivityResult
        val selectedRef = selectedRef
        if (selectedRef != null) {
            assetManager.loadTextAsset(selectedRef).fold(
                onSuccess = { text -> assetManager.writeTextAsset(uri, text).fold(
                    onSuccess = { showMessage(R.string.settings__theme_manager__theme_export_success) },
                    onFailure = { showError(it) }
                ) },
                onFailure = { showError(it) }
            )
        } else {
            showError(NullPointerException("selectedRef is null!"))
        }
    }

    companion object {
        const val EXTRA_KEY: String = "key"
        const val EXTRA_DEFAULT_VALUE: String = "default_value"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layoutManager = LayoutManager()

        key = intent.getStringExtra(EXTRA_KEY) ?: ""
        defaultValue = intent.getStringExtra(EXTRA_DEFAULT_VALUE) ?: ""
        selectedRef = evaluateSelectedRef()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setTitle(
            when (key) {
                Preferences.Theme.DAY_THEME_REF -> R.string.settings__theme_manager__title_day
                Preferences.Theme.NIGHT_THEME_REF -> R.string.settings__theme_manager__title_night
                else -> R.string.settings__title
            }
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.fabOptionCreateEmpty.setOnClickListener { onActionClicked(it) }
        binding.fabOptionCreateFromSelected.setOnClickListener { onActionClicked(it) }
        binding.fabOptionImport.setOnClickListener { onActionClicked(it) }
        binding.themeDeleteBtn.setOnClickListener { onActionClicked(it) }
        binding.themeEditBtn.setOnClickListener { onActionClicked(it) }
        binding.themeExportBtn.setOnClickListener { onActionClicked(it) }

        textKeyboardIconSet = TextKeyboardIconSet.new(this)
        binding.keyboardPreview.setIconSet(textKeyboardIconSet)
        binding.keyboardPreview.setComputingEvaluator(textComputingEvaluator)
        binding.keyboardPreview.sync()

        buildUi()
    }

    override fun onCreateBinding(): ThemeManagerActivityBinding {
        return ThemeManagerActivityBinding.inflate(layoutInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        // Normally the selection should already be applied to the prefs, but just to make sure we
        // apply it here again.
        setThemeRefInPrefs(selectedRef)
    }

    override fun finish() {
        super.finish()
        // Normally the selection should already be applied to the prefs, but just to make sure we
        // apply it here again.
        setThemeRefInPrefs(selectedRef)
    }

    private fun evaluateSelectedRef(ignorePrefs: Boolean = false): FlorisRef? {
        return if (ignorePrefs) {
            when (key) {
                Preferences.Theme.DAY_THEME_REF -> themeManager.indexedDayThemeRefs.keys.firstOrNull()
                Preferences.Theme.NIGHT_THEME_REF -> themeManager.indexedNightThemeRefs.keys.firstOrNull()
                else -> null
            }
        } else {
            FlorisRef.from(when (key) {
                Preferences.Theme.DAY_THEME_REF -> prefs.theme.dayThemeRef
                Preferences.Theme.NIGHT_THEME_REF -> prefs.theme.nightThemeRef
                else -> ""
            }).takeIf { it.isValid }
        }
    }

    private fun setThemeRefInPrefs(ref: FlorisRef?) {
        when (key) {
            Preferences.Theme.DAY_THEME_REF -> prefs.theme.dayThemeRef = ref.toString()
            Preferences.Theme.NIGHT_THEME_REF -> prefs.theme.nightThemeRef = ref.toString()
        }
    }

    private fun selectNullTheme() {
        val invalidString = resources.getString(R.string.assets__error__invalid)
        binding.themeNameValue.text = invalidString
        binding.themeSourceValue.text = invalidString
        binding.themeAuthorsLabel.text =
            resources.getQuantityText(R.plurals.assets__file__authors, 1)
        binding.themeAuthorsValue.text = invalidString
        binding.themeDeleteBtn.isEnabled = false
        binding.themeEditBtn.isEnabled = false
        selectedRef = null
        setThemeRefInPrefs(selectedRef)
        selectedTheme = Theme.empty()
    }

    private fun selectTheme(assetRef: FlorisRef, themeMetaOnly: ThemeMetaOnly) {
        binding.themeNameValue.text = themeMetaOnly.label
        binding.themeSourceValue.text = resources.getString(when {
            assetRef.isAssets -> R.string.pref__theme__source_assets
            assetRef.isInternal -> R.string.pref__theme__source_internal
            assetRef.isExternal -> R.string.pref__theme__source_external
            else -> R.string.assets__error__invalid
        })
        binding.themeAuthorsLabel.text =
            resources.getQuantityText(R.plurals.assets__file__authors, themeMetaOnly.authors.size)
        binding.themeAuthorsValue.text = themeMetaOnly.authors.joinToString(", ")
        binding.themeDeleteBtn.isEnabled = assetRef.isInternal
        binding.themeEditBtn.isEnabled = assetRef.isInternal
        selectedRef = assetRef
        setThemeRefInPrefs(selectedRef)
        themeManager.loadTheme(assetRef).onSuccess {
            selectedTheme = it
            binding.keyboardPreview.onThemeUpdated(it)
        }
    }

    fun onActionClicked(view: View) {
        when (view.id) {
            R.id.fab_option_create_empty -> {
                val timestamp = System.currentTimeMillis()
                val newTheme = Theme.baseTheme(
                    name = "theme-$timestamp",
                    label = resources.getString(R.string.settings__theme_manager__theme_new_title),
                    authors = listOf("@me"),
                    isNightTheme = key == Preferences.Theme.NIGHT_THEME_REF
                )
                val newAssetRef = FlorisRef.internal(ThemeManager.THEME_PATH_REL + "/" + newTheme.name + ".json")
                themeManager.writeTheme(newAssetRef, newTheme).onSuccess {
                    themeEditor.launch(
                        Intent(this, ThemeEditorActivity::class.java).apply {
                            putExtra(ThemeEditorActivity.EXTRA_THEME_REF, newAssetRef.toString())
                        }
                    )
                }.onFailure {
                    flogError(LogTopic.THEME_MANAGER) { it.toString() }
                }
            }
            R.id.fab_option_create_from_selected -> {
                val timestamp = System.currentTimeMillis()
                val authorsMut = selectedTheme.authors.toMutableList()
                authorsMut.add("@me")
                val themeCopy = selectedTheme.copy(
                    name = "theme-$timestamp",
                    label = String.format(
                        resources.getString(R.string.settings__theme_manager__theme_custom_title),
                        selectedTheme.label
                    ),
                    authors = authorsMut.toList()
                )
                val newAssetRef = FlorisRef.internal(ThemeManager.THEME_PATH_REL + "/" + themeCopy.name + ".json")
                themeManager.writeTheme(newAssetRef, themeCopy).onSuccess {
                    themeEditor.launch(
                        Intent(this, ThemeEditorActivity::class.java).apply {
                            putExtra(ThemeEditorActivity.EXTRA_THEME_REF, newAssetRef.toString())
                        }
                    )
                }.onFailure {
                    flogError(LogTopic.THEME_MANAGER) { it.toString() }
                }
            }
            R.id.fab_option_import -> {
                importTheme.launch("*/*")
            }
            R.id.theme_delete_btn -> {
                val deleteRef = selectedRef
                if (deleteRef?.isInternal == true) {
                    val msg = String.format(
                        resources.getString(R.string.assets__action__delete_confirm_message),
                        selectedTheme.label
                    )
                    AlertDialog.Builder(this).apply {
                        setTitle(R.string.assets__action__delete_confirm_title)
                        setCancelable(true)
                        setMessage(msg)
                        setPositiveButton(android.R.string.ok) { _, _ ->
                            themeManager.deleteTheme(deleteRef)
                            selectedRef = evaluateSelectedRef(ignorePrefs = true)
                            setThemeRefInPrefs(selectedRef)
                            themeManager.update()
                            buildUi()
                        }
                        setNegativeButton(android.R.string.cancel, null)
                        create()
                        show()
                    }
                } else {
                    // This toast normally should never show, though if the edit button is enabled
                    // even if it shouldn't, just show a toast so the user knows the app is
                    // responding.
                    Toast.makeText(
                        this,
                        "Cannot delete themes included with this app or provided by external sources",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            R.id.theme_edit_btn -> {
                val editRef = selectedRef
                if (editRef?.isInternal == true) {
                    themeEditor.launch(
                        Intent(this, ThemeEditorActivity::class.java).apply {
                            putExtra(ThemeEditorActivity.EXTRA_THEME_REF, editRef.toString())
                        }
                    )
                } else {
                    // This toast normally should never show, though if the edit button is enabled
                    // even if it shouldn't, just show a toast so the user knows the app is
                    // responding.
                    Toast.makeText(
                        this,
                        "Cannot edit themes included with this app or provided by external sources",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            R.id.theme_export_btn -> {
                exportTheme.launch("${selectedTheme.name}.json")
            }
        }
    }

    private fun setCheckedRadioButton(@IdRes id: Int) {
        binding.themeList.forEach { view ->
            if (view is RadioButton) {
                view.isChecked = view.id == id
            }
        }
    }

    private fun buildUi() {
        val metaIndex = when (key) {
            Preferences.Theme.DAY_THEME_REF -> themeManager.indexedDayThemeRefs
            Preferences.Theme.NIGHT_THEME_REF -> themeManager.indexedNightThemeRefs
            else -> mutableMapOf()
        }
        binding.themeList.removeAllViews()
        var selectId: Int? = null
        for ((assetRef, themeMetaOnly) in metaIndex) {
            val radioButton = RadioButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val marginV = ViewUtils.dp2px(8.0f).toInt()
                    val marginH = ViewUtils.dp2px(16.0f).toInt()
                    setMargins(marginH, marginV, marginH, marginV)
                    setPadding(marginV, 0, 0, 0)
                }
                id = View.generateViewId()
                text = themeMetaOnly.label
                setOnClickListener { view ->
                    selectTheme(assetRef, themeMetaOnly)
                    setCheckedRadioButton(view.id)
                }
                if (selectedRef == assetRef) {
                    selectId = id
                    selectTheme(assetRef, themeMetaOnly)
                }
            }
            binding.themeList.addView(radioButton)
        }
        if (selectId == null) {
            selectNullTheme()
        } else {
            setCheckedRadioButton(selectId!!)
        }
        launch {
            binding.keyboardPreview.setComputedKeyboard(layoutManager.computeKeyboardAsync(
                KeyboardMode.CHARACTERS, Subtype.DEFAULT
            ).await())
            binding.keyboardPreview.onThemeUpdated(selectedTheme)
        }
    }
}
