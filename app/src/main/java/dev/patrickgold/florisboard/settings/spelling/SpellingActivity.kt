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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.common.FlorisActivity
import dev.patrickgold.florisboard.databinding.SpellingActivityBinding
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.ime.spelling.SpellingManager

class SpellingActivity : FlorisActivity<SpellingActivityBinding>() {
    private var activePage = Page.UNINITIALIZED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar.root)

        supportActionBar?.setTitle(R.string.settings__spelling__title_overview)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.fabOptionExtensionArchive.setOnClickListener {
            ImportDictionaryFragment(isArchive = true).apply {
                show(supportFragmentManager, null)
            }
        }
        binding.fabOptionAffixDictionary.setOnClickListener {
            ImportDictionaryFragment(isArchive = false).apply {
                show(supportFragmentManager, null)
            }
        }

        setActivePage(Page.OVERVIEW)
    }

    override fun onCreateBinding(): SpellingActivityBinding {
        return SpellingActivityBinding.inflate(layoutInflater)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.help_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.settings__help -> {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(resources.getString(R.string.florisboard__spell_checker_wiki_url))
                )
                startActivity(browserIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        when (activePage) {
            Page.MANAGE_DICTIONARIES -> setActivePage(Page.OVERVIEW)
            else -> super.onBackPressed()
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
                supportActionBar?.setTitle(R.string.settings__spelling__title_overview)
                OverviewFragment()
            }
            Page.MANAGE_DICTIONARIES -> {
                binding.fabAddImportDict.isVisible = true
                supportActionBar?.setTitle(R.string.settings__spelling__title_manage_dictionaries)
                ManageDictionariesFragment()
            }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, fragment::class.qualifiedName)
            .commit()
    }

    enum class Page {
        UNINITIALIZED,
        OVERVIEW,
        MANAGE_DICTIONARIES;
    }
}
