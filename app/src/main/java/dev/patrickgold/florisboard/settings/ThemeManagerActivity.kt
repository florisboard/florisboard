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
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.ThemeManagerActivityBinding
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeMetaOnly
import dev.patrickgold.florisboard.util.ViewLayoutUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class ThemeManagerActivity : AppCompatActivity() {
    private lateinit var binding: ThemeManagerActivityBinding
    private lateinit var layoutManager: LayoutManager
    private val mainScope = MainScope()
    private lateinit var prefs: PrefHelper
    private val themeManager: ThemeManager = ThemeManager.default()

    private var key: String = ""
    private var defaultValue: String = ""
    private var selectedTheme: Theme = Theme.empty()
    private var selectedRef: AssetRef? = null

    companion object {
        private const val EDITOR_REQ_CODE: Int = 0xFB01

        const val EXTRA_KEY: String = "key"
        const val EXTRA_DEFAULT_VALUE: String = "default_value"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PrefHelper.getDefaultInstance(this)

        super.onCreate(savedInstanceState)
        binding = ThemeManagerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        key = intent.getStringExtra(EXTRA_KEY) ?: ""
        defaultValue = intent.getStringExtra(EXTRA_DEFAULT_VALUE) ?: ""
        selectedRef = evaluateSelectedRef()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setTitle(
            when (key) {
                PrefHelper.Theme.DAY_THEME_REF -> R.string.settings__theme_manager__title_day
                PrefHelper.Theme.NIGHT_THEME_REF -> R.string.settings__theme_manager__title_night
                else -> R.string.settings__title
            }
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.fabOptionCreateEmpty.setOnClickListener { onActionClicked(it) }
        binding.fabOptionCreateFromSelected.setOnClickListener { onActionClicked(it) }
        binding.themeDeleteBtn.setOnClickListener { onActionClicked(it) }
        binding.themeEditBtn.setOnClickListener { onActionClicked(it) }

        layoutManager = LayoutManager(this).apply {
            preloadComputedLayout(KeyboardMode.CHARACTERS, Subtype.DEFAULT, prefs)
        }

        buildUi()
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

    override fun finish() {
        // Normally the selection should already be applied to the prefs, but just to make sure we
        // apply it here again.
        setThemeRefInPrefs(selectedRef)
        super.finish()
    }

    private fun evaluateSelectedRef(ignorePrefs: Boolean = false): AssetRef? {
        return if (ignorePrefs) {
            when (key) {
                PrefHelper.Theme.DAY_THEME_REF -> themeManager.indexedDayThemeRefs.keys.firstOrNull()
                PrefHelper.Theme.NIGHT_THEME_REF -> themeManager.indexedNightThemeRefs.keys.firstOrNull()
                else -> null
            }
        } else {
            AssetRef.fromString(
                when (key) {
                    PrefHelper.Theme.DAY_THEME_REF -> prefs.theme.dayThemeRef
                    PrefHelper.Theme.NIGHT_THEME_REF -> prefs.theme.nightThemeRef
                    else -> ""
                }
            ).getOr(null)
        }
    }

    private fun setThemeRefInPrefs(ref: AssetRef?) {
        when (key) {
            PrefHelper.Theme.DAY_THEME_REF -> prefs.theme.dayThemeRef = ref.toString()
            PrefHelper.Theme.NIGHT_THEME_REF -> prefs.theme.nightThemeRef = ref.toString()
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

    private fun selectTheme(assetRef: AssetRef, themeMetaOnly: ThemeMetaOnly) {
        binding.themeNameValue.text = themeMetaOnly.label
        binding.themeSourceValue.text = resources.getString(when (assetRef.source) {
            is AssetSource.Assets -> R.string.pref__theme__source_assets
            is AssetSource.Internal -> R.string.pref__theme__source_internal
            is AssetSource.External -> R.string.pref__theme__source_external
        })
        binding.themeAuthorsLabel.text =
            resources.getQuantityText(R.plurals.assets__file__authors, themeMetaOnly.authors.size)
        binding.themeAuthorsValue.text = themeMetaOnly.authors.joinToString(", ")
        binding.themeDeleteBtn.isEnabled = assetRef.source == AssetSource.Internal
        binding.themeEditBtn.isEnabled = assetRef.source == AssetSource.Internal
        selectedRef = assetRef.copy()
        setThemeRefInPrefs(selectedRef)
        themeManager.loadTheme(assetRef).onSuccess {
            selectedTheme = it
            binding.keyboardPreview.onThemeUpdated(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EDITOR_REQ_CODE) {
            themeManager.update()
            buildUi()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
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
                    isNightTheme = key == PrefHelper.Theme.NIGHT_THEME_REF
                )
                val newAssetRef =
                    AssetRef(
                        AssetSource.Internal,
                        ThemeManager.THEME_PATH_REL + "/" + newTheme.name + ".json"
                    )
                themeManager.writeTheme(newAssetRef, newTheme).onSuccess {
                    startActivityForResult(Intent(this, ThemeEditorActivity::class.java).apply {
                        putExtra(ThemeEditorActivity.EXTRA_THEME_REF, newAssetRef.toString())
                    }, EDITOR_REQ_CODE)
                }.onFailure {
                    Timber.e(it.toString())
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
                val newAssetRef =
                    AssetRef(
                        AssetSource.Internal,
                        ThemeManager.THEME_PATH_REL + "/" + themeCopy.name + ".json"
                    )
                themeManager.writeTheme(newAssetRef, themeCopy).onSuccess {
                    startActivityForResult(Intent(this, ThemeEditorActivity::class.java).apply {
                        putExtra(ThemeEditorActivity.EXTRA_THEME_REF, newAssetRef.toString())
                    }, EDITOR_REQ_CODE)
                }.onFailure {
                    Timber.e(it.toString())
                }
            }
            /*R.id.fab_option_import -> {
                Toast.makeText(this, "Import not yet implemented", Toast.LENGTH_SHORT).show()
            }*/
            R.id.theme_delete_btn -> {
                val deleteRef = selectedRef?.copy()
                if (deleteRef?.source == AssetSource.Internal) {
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
                if (editRef?.source == AssetSource.Internal) {
                    startActivityForResult(Intent(this, ThemeEditorActivity::class.java).apply {
                        putExtra(ThemeEditorActivity.EXTRA_THEME_REF, editRef.toString())
                    }, EDITOR_REQ_CODE)
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
            PrefHelper.Theme.DAY_THEME_REF -> themeManager.indexedDayThemeRefs
            PrefHelper.Theme.NIGHT_THEME_REF -> themeManager.indexedNightThemeRefs
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
                    val marginV = ViewLayoutUtils.convertDpToPixel(8.0f, context).toInt()
                    val marginH = ViewLayoutUtils.convertDpToPixel(16.0f, context).toInt()
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
        mainScope.launch {
            binding.keyboardPreview.computedLayout = layoutManager.fetchComputedLayoutAsync(
                KeyboardMode.CHARACTERS, Subtype.DEFAULT, prefs
            ).await()
            binding.keyboardPreview.onThemeUpdated(selectedTheme)
        }
    }
}
