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

package dev.patrickgold.florisboard.settings

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.UdmActivityBinding
import dev.patrickgold.florisboard.ime.core.FlorisActivity
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.dictionary.UserDictionaryDatabase
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.text.keyboard.*
import java.util.*

class UdmActivity : FlorisActivity<UdmActivityBinding>() {
    private val dictionaryManager: DictionaryManager get() = DictionaryManager.default()
    private val assetManager: AssetManager get() = AssetManager.default()

    private var userDictionaryType: Int = -1
    private var currentLevel: Int = LEVEL_LANGUAGES
    private var locale: Locale? = null

    private val importUserDictionary = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // If uri is null it indicates that the selection activity was cancelled (mostly by pressing the back button),
        // so we don't display an error message here.
        if (uri == null) return@registerForActivityResult
        val db: UserDictionaryDatabase? = when (userDictionaryType) {
            USER_DICTIONARY_TYPE_FLORIS -> {
                dictionaryManager.florisUserDictionaryDatabase()
            }
            USER_DICTIONARY_TYPE_SYSTEM -> {
                dictionaryManager.systemUserDictionaryDatabase()
            }
            else -> {
                showError(Exception("User dictionary type '$userDictionaryType' is not valid!"))
                return@registerForActivityResult
            }
        }
        if (db == null) {
            showError(NullPointerException("Database handle is null!"))
            return@registerForActivityResult
        }
        db.importCombinedList(this, uri).onSuccess {
            showMessage(R.string.settings__udm__dictionary_export_success)
        }.onFailure {
            showError(it)
        }
    }

    private val exportUserDictionary = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        // If uri is null it indicates that the selection activity was cancelled (mostly by pressing the back button,
        // so we don't display an error message here.
        if (uri == null) return@registerForActivityResult
        val db: UserDictionaryDatabase? = when (userDictionaryType) {
            USER_DICTIONARY_TYPE_FLORIS -> {
                dictionaryManager.florisUserDictionaryDatabase()
            }
            USER_DICTIONARY_TYPE_SYSTEM -> {
                dictionaryManager.systemUserDictionaryDatabase()
            }
            else -> {
                showError(Exception("User dictionary type '$userDictionaryType' is not valid!"))
                return@registerForActivityResult
            }
        }
        if (db == null) {
            showError(NullPointerException("Database handle is null!"))
            return@registerForActivityResult
        }
        db.exportCombinedList(this, uri).onSuccess {
            showMessage(R.string.settings__udm__dictionary_export_success)
        }.onFailure {
            showError(it)
        }
    }

    companion object {
        const val EXTRA_USER_DICTIONARY_TYPE: String = "key"
        const val USER_DICTIONARY_TYPE_SYSTEM: Int = 1
        const val USER_DICTIONARY_TYPE_FLORIS: Int = 2

        private const val LEVEL_LANGUAGES: Int = 1
        private const val LEVEL_WORDS: Int = 2
        private const val USER_DICTIONARY_SETTINGS_INTENT_ACTION: String =
            "android.settings.USER_DICTIONARY_SETTINGS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userDictionaryType = intent.getIntExtra(EXTRA_USER_DICTIONARY_TYPE, -1)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setTitle(
            when (userDictionaryType) {
                USER_DICTIONARY_TYPE_FLORIS -> R.string.settings__udm__title_floris
                USER_DICTIONARY_TYPE_SYSTEM -> R.string.settings__udm__title_system
                else -> R.string.settings__title
            }
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dictionaryManager.loadUserDictionariesIfNecessary()

        binding.fabAddWord.setOnClickListener {  }
    }

    override fun onCreateBinding(): UdmActivityBinding {
        return UdmActivityBinding.inflate(layoutInflater)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.udm_extra_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.udm__import -> {
                importUserDictionary.launch("*/*")
                true
            }
            R.id.udm__export -> {
                exportUserDictionary.launch("my-personal-dictionary.clb")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun buildUi() {

    }
}
