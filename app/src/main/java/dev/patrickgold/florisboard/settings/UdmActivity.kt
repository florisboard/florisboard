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

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.UdmActivityBinding
import dev.patrickgold.florisboard.databinding.UdmEntryDialogBinding
import dev.patrickgold.florisboard.common.FlorisActivity
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.dictionary.FREQUENCY_DEFAULT
import dev.patrickgold.florisboard.ime.dictionary.FREQUENCY_MAX
import dev.patrickgold.florisboard.ime.dictionary.FREQUENCY_MIN
import dev.patrickgold.florisboard.ime.dictionary.UserDictionaryDao
import dev.patrickgold.florisboard.ime.dictionary.UserDictionaryDatabase
import dev.patrickgold.florisboard.ime.dictionary.UserDictionaryEntry
import dev.patrickgold.florisboard.ime.text.keyboard.*

interface OnListItemCLickListener {
    fun onListItemClick(pos: Int)
}

class LanguageEntryAdapter(
    private val data: List<String>,
    private val onListItemCLickListener: OnListItemCLickListener
) : RecyclerView.Adapter<LanguageEntryAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val onListItemCLickListener: OnListItemCLickListener) :
        RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(android.R.id.title)
        val summaryView: TextView = view.findViewById(android.R.id.summary)

        init {
            view.setOnClickListener {
                onListItemCLickListener.onListItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val listItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return ViewHolder(listItemView, onListItemCLickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleView.text = data[position]
        holder.summaryView.isVisible = false
    }

    override fun getItemCount(): Int {
        return data.size
    }
}

class UserDictionaryEntryAdapter(
    private val data: List<UserDictionaryEntry>,
    private val onListItemCLickListener: OnListItemCLickListener
) : RecyclerView.Adapter<UserDictionaryEntryAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val onListItemCLickListener: OnListItemCLickListener) :
        RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(android.R.id.title)
        val summaryView: TextView = view.findViewById(android.R.id.summary)

        init {
            view.setOnClickListener {
                onListItemCLickListener.onListItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val listItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return ViewHolder(listItemView, onListItemCLickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleView.text = data[position].word
        val shortcut = data[position].shortcut
        holder.summaryView.text = if (shortcut == null) {
            String.format(
                holder.summaryView.context.resources.getString(R.string.settings__udm__word_summary_freq),
                data[position].freq
            )
        } else {
            String.format(
                holder.summaryView.context.resources.getString(R.string.settings__udm__word_summary_freq_shortcut),
                data[position].freq,
                shortcut
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}

class UdmActivity : FlorisActivity<UdmActivityBinding>() {
    private val dictionaryManager: DictionaryManager get() = DictionaryManager.default()

    private var userDictionaryType: Int = -1
    private var currentLevel: Int = LEVEL_LANGUAGES
    private var currentLocale: FlorisLocale? = null
    private var activeDialogWindow: AlertDialog? = null

    private var languageList: List<FlorisLocale?> = listOf()
    private var wordList: List<UserDictionaryEntry> = listOf()

    private val languageListItemClickListener = object : OnListItemCLickListener {
        override fun onListItemClick(pos: Int) {
            if (currentLevel == LEVEL_LANGUAGES) {
                currentLocale = languageList[pos]
                currentLevel = LEVEL_WORDS
                buildUi()
            }
        }
    }

    private val wordListItemClickListener = object : OnListItemCLickListener {
        override fun onListItemClick(pos: Int) {
            if (currentLevel == LEVEL_WORDS) {
                val entry = wordList[pos]
                showEditWordDialog(entry)
            }
        }
    }

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
        private const val SYSTEM_USER_DICTIONARY_SETTINGS_INTENT_ACTION: String =
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

        binding.fabAddWord.setOnClickListener { showAddWordDialog() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        buildUi()
    }

    override fun onCreateBinding(): UdmActivityBinding {
        return UdmActivityBinding.inflate(layoutInflater)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.udm_extra_menu, menu)
        if (userDictionaryType == USER_DICTIONARY_TYPE_FLORIS) {
            menu?.findItem(R.id.udm__open_system_manager_ui)?.isVisible = false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        activeDialogWindow?.dismiss()
        activeDialogWindow = null
        currentLocale = null
    }

    override fun onResume() {
        super.onResume()
        buildUi()
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
            R.id.udm__open_system_manager_ui -> {
                startActivity(Intent(SYSTEM_USER_DICTIONARY_SETTINGS_INTENT_ACTION))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (currentLevel == LEVEL_WORDS) {
            currentLevel = LEVEL_LANGUAGES
            currentLocale = null
            buildUi()
        } else {
            super.onBackPressed()
        }
    }

    private fun userDictionaryDao(): UserDictionaryDao? {
        return when (userDictionaryType) {
            USER_DICTIONARY_TYPE_FLORIS -> dictionaryManager.florisUserDictionaryDao()
            USER_DICTIONARY_TYPE_SYSTEM -> dictionaryManager.systemUserDictionaryDao()
            else -> null
        }
    }

    private fun buildUi() {
        when (currentLevel) {
            LEVEL_LANGUAGES -> {
                languageList = userDictionaryDao()?.queryLanguageList()?.sortedBy { it?.displayLanguage() } ?: listOf()
                binding.recyclerView.adapter = LanguageEntryAdapter(
                    languageList.map { getDisplayNameForLocale(it) },
                    languageListItemClickListener
                )
                supportActionBar?.subtitle = null
            }
            LEVEL_WORDS -> {
                wordList = userDictionaryDao()?.queryAll(currentLocale) ?: listOf()
                binding.recyclerView.adapter = UserDictionaryEntryAdapter(
                    wordList,
                    wordListItemClickListener
                )
                supportActionBar?.subtitle = getDisplayNameForLocale(currentLocale)
            }
        }
    }

    private fun getDisplayNameForLocale(locale: FlorisLocale?): String {
        return locale?.displayName() ?: resources.getString(R.string.settings__udm__all_languages)
    }

    private fun showAddWordDialog() {
        val dialogBinding = UdmEntryDialogBinding.inflate(layoutInflater)
        dialogBinding.freq.setText(FREQUENCY_DEFAULT.toString())
        dialogBinding.freqLabel.hint = String.format(
            resources.getString(R.string.settings__udm__dialog__freq_label),
            FREQUENCY_MIN,
            FREQUENCY_MAX
        )
        if (currentLevel == LEVEL_WORDS) {
            currentLocale?.let {
                dialogBinding.locale.setText(it.localeTag())
            }
        }

        AlertDialog.Builder(this).apply {
            setTitle(R.string.settings__udm__dialog__title_add)
            setCancelable(true)
            setView(dialogBinding.root)
            setPositiveButton(R.string.assets__action__add, null)
            setNegativeButton(R.string.assets__action__cancel, null)
            setOnDismissListener { activeDialogWindow = null }
            create()
            activeDialogWindow = show()
            activeDialogWindow?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                if (processInput(dialogBinding, null)) {
                    activeDialogWindow?.dismiss()
                    activeDialogWindow = null
                    buildUi()
                }
            }
        }
    }

    private fun showEditWordDialog(entry: UserDictionaryEntry) {
        val dialogBinding = UdmEntryDialogBinding.inflate(layoutInflater)
        dialogBinding.word.setText(entry.word)
        dialogBinding.freq.setText(entry.freq.toString())
        dialogBinding.freqLabel.hint = String.format(
            resources.getString(R.string.settings__udm__dialog__freq_label),
            FREQUENCY_MIN,
            FREQUENCY_MAX
        )
        dialogBinding.shortcut.setText(entry.shortcut ?: "")
        dialogBinding.locale.setText(entry.locale ?: "")

        AlertDialog.Builder(this).apply {
            setTitle(R.string.settings__udm__dialog__title_edit)
            setCancelable(true)
            setView(dialogBinding.root)
            setPositiveButton(R.string.assets__action__apply, null)
            setNegativeButton(R.string.assets__action__cancel, null)
            setNeutralButton(R.string.assets__action__delete) { _, _ ->
                userDictionaryDao()?.delete(entry)
                buildUi()
            }
            setOnDismissListener { activeDialogWindow = null }
            create()
            activeDialogWindow = show()
            activeDialogWindow?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                if (processInput(dialogBinding, entry)) {
                    activeDialogWindow?.dismiss()
                    activeDialogWindow = null
                    buildUi()
                }
            }
        }
    }

    private fun processInput(dialogBinding: UdmEntryDialogBinding, entry: UserDictionaryEntry?): Boolean {
        val word = dialogBinding.word.text?.toString()?.ifBlank { null }
        val freq = dialogBinding.freq.text?.toString()?.ifBlank { null }
        val shortcut = dialogBinding.shortcut.text?.toString()?.ifBlank { null }
        val locale = dialogBinding.locale.text?.toString()?.ifBlank { null }

        var isValid = true
        if (word == null) {
            dialogBinding.word.error = resources.getString(R.string.settings__udm__dialog__word_error_empty)
            isValid = false
        } else if (word.contains(' ') || word.contains(';') || word.contains('=')) {
            dialogBinding.word.error = resources.getString(R.string.settings__udm__dialog__word_error_invalid)
            isValid = false
        }
        if (freq == null) {
            dialogBinding.freq.error = resources.getString(R.string.settings__udm__dialog__freq_error_empty)
            isValid = false
        } else if (runCatching { freq.toInt(10) }.getOrNull() !in FREQUENCY_MIN..FREQUENCY_MAX) {
            dialogBinding.freq.error = resources.getString(R.string.settings__udm__dialog__freq_error_invalid)
            isValid = false
        }
        if (shortcut != null && (shortcut.contains(' ') || shortcut.contains(';') || shortcut.contains('='))) {
            dialogBinding.shortcut.error = resources.getString(R.string.settings__udm__dialog__shortcut_error_invalid)
            isValid = false
        }
        if (locale != null && (runCatching { FlorisLocale.fromTag(locale).iso3Language.ifBlank { null } }.getOrNull() == null)) {
            dialogBinding.locale.error = resources.getString(R.string.settings__udm__dialog__locale_error_invalid)
            isValid = false
        }
        if (isValid) {
            val localeStr = if (locale == null) { null } else {
                FlorisLocale.fromTag(locale).localeTag()
            }
            if (entry != null) {
                userDictionaryDao()?.update(
                    UserDictionaryEntry(entry.id, word!!, freq!!.toInt(10), localeStr, shortcut)
                )
            } else {
                userDictionaryDao()?.insert(
                    UserDictionaryEntry(0, word!!, freq!!.toInt(10), localeStr, shortcut)
                )
            }
        }
        return isValid
    }
}
