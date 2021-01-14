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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.michaelbull.result.onSuccess
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.ThemeEditorActivityBinding
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ThemeEditorActivity : AppCompatActivity() {
    private lateinit var binding: ThemeEditorActivityBinding
    private lateinit var layoutManager: LayoutManager
    private val mainScope = MainScope()
    private lateinit var prefs: PrefHelper
    private val themeManager: ThemeManager = ThemeManager.default()

    private var editedTheme: Theme = Theme.empty()
    private var editedThemeRef: AssetRef? = null
    private var isSaved: Boolean = false

    companion object {
        const val RESULT_CODE_THEME_EDIT_SAVED: Int =       0xFBADC1
        const val RESULT_CODE_THEME_EDIT_CANCELLED: Int =   0xFBADC2

        const val EXTRA_THEME_REF: String = "theme_ref"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PrefHelper.getDefaultInstance(this)

        super.onCreate(savedInstanceState)
        binding = ThemeEditorActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AssetRef.fromString(intent.getStringExtra(EXTRA_THEME_REF) ?: "").onSuccess { ref ->
            editedThemeRef = ref
            themeManager.loadTheme(ref).onSuccess { theme ->
                editedTheme = theme.copy()
            }
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = resources.getString(R.string.settings__theme_editor__title)
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

    fun onActionClicked(view: View) {
        when (view.id) {
            R.id.theme_cancel_btn -> onBackPressed()
            R.id.theme_save_btn -> {
                val ref = editedThemeRef
                if (ref != null) {
                    themeManager.writeTheme(ref, editedTheme.copy(
                        label = binding.themeNameValue.text.toString()
                    ))
                    isSaved = true
                    finish()
                }
            }
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.assets__action__cancel_confirm_title)
            setCancelable(true)
            setMessage(R.string.assets__action__cancel_confirm_message)
            setPositiveButton(R.string.assets__action__yes) { _, _ ->
                finish()
            }
            setNegativeButton(R.string.assets__action__no, null)
            create()
            show()
        }
    }

    override fun finish() {
        setResult(if (isSaved) {
            RESULT_CODE_THEME_EDIT_SAVED
        } else {
            RESULT_CODE_THEME_EDIT_CANCELLED
        })
        super.finish()
    }

    private fun buildUi() {
        binding.themeNameValue.setText(editedTheme.label)
        mainScope.launch {
            binding.keyboardPreview.computedLayout = layoutManager.fetchComputedLayoutAsync(
                KeyboardMode.CHARACTERS, Subtype.DEFAULT, prefs).await()
            binding.keyboardPreview.onThemeUpdated(editedTheme)
        }
    }
}
