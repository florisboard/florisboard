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

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
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

class ThemeManagerActivity : AppCompatActivity() {
    private lateinit var binding: ThemeManagerActivityBinding
    private lateinit var layoutManager: LayoutManager
    private val mainScope = MainScope()
    private lateinit var prefs: PrefHelper
    private val themeManager: ThemeManager = ThemeManager.default()

    private var key: String = ""
    private var defaultValue: String = ""
    private var selectedTheme: Theme = Theme.empty()
    private var selectedRef: String = ""

    companion object {
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
        selectedRef = when (key) {
            PrefHelper.Theme.DAY_THEME_REF -> prefs.theme.dayThemeRef
            PrefHelper.Theme.NIGHT_THEME_REF -> prefs.theme.nightThemeRef
            else -> ""
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setTitle(when (key) {
            PrefHelper.Theme.DAY_THEME_REF -> R.string.settings__theme_manager__title_day
            PrefHelper.Theme.NIGHT_THEME_REF -> R.string.settings__theme_manager__title_night
            else -> R.string.settings__title
        })
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        when (key) {
            PrefHelper.Theme.DAY_THEME_REF -> prefs.theme.dayThemeRef = selectedRef
            PrefHelper.Theme.NIGHT_THEME_REF -> prefs.theme.nightThemeRef = selectedRef
        }
        super.finish()
    }

    private fun selectNullTheme() {
        val invalidString = resources.getString(R.string.assets__error__invalid)
        binding.themeNameValue.text = invalidString
        binding.themeSourceValue.text = invalidString
        binding.themeAuthorsLabel.text = resources.getQuantityText(R.plurals.assets__file__authors, 1)
        binding.themeAuthorsValue.text = invalidString
        binding.themeDeleteBtn.isEnabled = false
        binding.themeEditBtn.isEnabled = false
        selectedRef = "invalid:null"
        selectedTheme = Theme.empty()
    }

    private fun selectTheme(assetRef: AssetRef, themeMetaOnly: ThemeMetaOnly) {
        binding.themeNameValue.text = themeMetaOnly.label
        binding.themeSourceValue.text = assetRef.source.toString()
        binding.themeAuthorsLabel.text = resources.getQuantityText(R.plurals.assets__file__authors, themeMetaOnly.authors.size)
        binding.themeAuthorsValue.text = themeMetaOnly.authors.joinToString(", ")
        binding.themeDeleteBtn.isEnabled = assetRef.source == AssetSource.Internal
        binding.themeEditBtn.isEnabled = assetRef.source == AssetSource.Internal
        selectedRef = assetRef.toString()
        themeManager.loadTheme(assetRef).onSuccess {
            selectedTheme = it
            binding.keyboardPreview.onThemeUpdated(it)
        }
    }

    fun onFabActionClicked(view: View) {
        when (view.id) {
            R.id.fab_option_create_empty -> {
                Toast.makeText(this, "Create empty not yet implemented", Toast.LENGTH_SHORT).show()
            }
            R.id.fab_option_create_from_template -> {
                Toast.makeText(this, "Create from template not yet implemented", Toast.LENGTH_SHORT).show()
            }
            R.id.fab_option_import -> {
                Toast.makeText(this, "Import not yet implemented", Toast.LENGTH_SHORT).show()
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
                text = String.format(
                    resources.getString(R.string.settings__theme_manager__theme_summary),
                    themeMetaOnly.label, themeMetaOnly.authors.joinToString(", ")
                )
                setOnClickListener { view ->
                    selectTheme(assetRef, themeMetaOnly)
                    setCheckedRadioButton(view.id)
                }
                if (selectedRef == assetRef.toString()) {
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
                KeyboardMode.CHARACTERS, Subtype.DEFAULT, prefs).await()
            binding.keyboardPreview.onThemeUpdated(selectedTheme)
        }
    }
}
