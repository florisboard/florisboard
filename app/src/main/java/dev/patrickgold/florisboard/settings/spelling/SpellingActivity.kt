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

package dev.patrickgold.florisboard.settings.spelling

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.common.FlorisActivity
import dev.patrickgold.florisboard.databinding.SpellingActivityBinding
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.spelling.SpellingManager

class SpellingActivity : FlorisActivity<SpellingActivityBinding>() {
    private val assetManager get() = AssetManager.default()
    private val spellingManager get() = SpellingManager.default()

    private var activePage = Page.UNINITIALIZED

    private val importDict = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // If uri is null it indicates that the selection activity was cancelled (mostly by pressing the back button),
        // so we don't display an error message here.
        if (uri == null) return@registerForActivityResult
        /*val toBeImportedTheme = themeManager.loadTheme(uri)
        if (toBeImportedTheme.isSuccess) {
            val newTheme = toBeImportedTheme.getOrNull()!!.copy(
                name = toBeImportedTheme.getOrNull()!!.name + "_imported",
                label = toBeImportedTheme.getOrNull()!!.label + " (Imported)"
            )
            val newAssetRef = AssetRef(
                AssetSource.Internal,
                ThemeManager.THEME_PATH_REL + "/" + newTheme.name + ".json"
            )
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
        }*/
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar.root)

        supportActionBar?.setTitle(R.string.settings__spelling__title_overview)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setActivePage(Page.OVERVIEW)
    }

    override fun onCreateBinding(): SpellingActivityBinding {
        return SpellingActivityBinding.inflate(layoutInflater)
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

    fun setActivePage(page: Page) {
        if (activePage == page) return
        activePage = page
        val fragment = when (page) {
            Page.UNINITIALIZED -> {
                binding.fabAddImportDict.isVisible = false
                return
            }
            Page.OVERVIEW -> {
                binding.fabAddImportDict.isVisible = false
                OverviewFragment()
            }
            Page.MANAGE_DICTIONARIES -> {
                binding.fabAddImportDict.isVisible = true
                OverviewFragment()
            }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    enum class Page {
        UNINITIALIZED,
        OVERVIEW,
        MANAGE_DICTIONARIES;
    }
}
