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

package dev.patrickgold.florisboard.ime.dictionary

import android.content.Context
import androidx.room.Room
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.nlp.SuggestionList
import dev.patrickgold.florisboard.ime.nlp.Word
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.io.FlorisRef
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.lang.ref.WeakReference

/**
 * TODO: document
 */
class DictionaryManager private constructor(
    context: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val applicationContext: WeakReference<Context> = WeakReference(context.applicationContext ?: context)
    private val prefs by florisPreferenceModel()

    private val dictionaryCache: MutableMap<String, Dictionary> = mutableMapOf()

    private var florisUserDictionaryDatabase: FlorisUserDictionaryDatabase? = null
    private var systemUserDictionaryDatabase: SystemUserDictionaryDatabase? = null

    companion object {
        val FLORIS_EN_REF = FlorisRef.assets("ime/dict/en.flict")

        private var defaultInstance: DictionaryManager? = null

        fun init(applicationContext: Context): DictionaryManager {
            val instance = DictionaryManager(applicationContext)
            defaultInstance = instance
            return instance
        }

        fun default(): DictionaryManager {
            val instance = defaultInstance
            if (instance != null) {
                return instance
            } else {
                throw UninitializedPropertyAccessException(
                    "${DictionaryManager::class.simpleName} has not been initialized previously. Make sure to call init(applicationContext) before using default()."
                )
            }
        }
    }

    inline fun suggest(
        currentWord: Word,
        preceidingWords: List<Word>,
        subtype: Subtype,
        allowPossiblyOffensive: Boolean,
        maxSuggestionCount: Int,
        block: (suggestions: SuggestionList) -> Unit
    ) {
        val suggestions = SuggestionList.new(maxSuggestionCount)
        queryUserDictionary(currentWord, subtype.primaryLocale, suggestions)
        loadDictionary(FLORIS_EN_REF).onSuccess {
            it.getTokenPredictions(preceidingWords, currentWord, maxSuggestionCount, allowPossiblyOffensive, suggestions)
        }
        block(suggestions)
        suggestions.dispose()
    }

    fun loadDictionary(ref: FlorisRef): Result<Dictionary> {
        dictionaryCache[ref.toString()]?.let {
            return Result.success(it)
        }
        if (ref.relativePath.endsWith(".flict")) {
            // Assume this is a Flictionary
            applicationContext.get()?.let {
                Flictionary.load(it, ref).onSuccess { flict ->
                    dictionaryCache[ref.toString()] = flict
                    return Result.success(flict)
                }.onFailure { err ->
                    flogError { err.toString() }
                    return Result.failure(err)
                }
            }
        } else {
            return Result.failure(Exception("Unable to determine supported type for given AssetRef!"))
        }
        return Result.failure(Exception("If this message is ever thrown, something is completely broken..."))
    }

    fun prepareDictionaries(subtype: Subtype) {
        loadDictionary(FLORIS_EN_REF)
    }

    fun queryUserDictionary(word: Word, locale: FlorisLocale, destSuggestionList: SuggestionList) {
        val florisDao = florisUserDictionaryDao()
        val systemDao = systemUserDictionaryDao()
        if (florisDao == null && systemDao == null) {
            return
        }
        if (prefs.dictionary.enableFlorisUserDictionary.get()) {
            florisDao?.query(word, locale)?.let {
                for (entry in it) {
                    destSuggestionList.add(entry.word, entry.freq)
                }
            }
            florisDao?.queryShortcut(word, locale)?.let {
                for (entry in it) {
                    destSuggestionList.add(entry.word, entry.freq)
                }
            }
        }
        if (prefs.dictionary.enableSystemUserDictionary.get()) {
            systemDao?.query(word, locale)?.let {
                for (entry in it) {
                    destSuggestionList.add(entry.word, entry.freq)
                }
            }
            systemDao?.queryShortcut(word, locale)?.let {
                for (entry in it) {
                    destSuggestionList.add(entry.word, entry.freq)
                }
            }
        }
    }

    fun spell(word: Word, locale: FlorisLocale): Boolean {
        val florisDao = florisUserDictionaryDao()
        val systemDao = systemUserDictionaryDao()
        if (florisDao == null && systemDao == null) {
            return false
        }
        var ret = false
        if (prefs.dictionary.enableFlorisUserDictionary.get()) {
            ret = ret || florisDao?.queryExactFuzzyLocale(word, locale)?.isNotEmpty() ?: false
            ret = ret || florisDao?.queryShortcut(word, locale)?.isNotEmpty() ?: false
        }
        if (prefs.dictionary.enableSystemUserDictionary.get()) {
            ret = ret || systemDao?.queryExactFuzzyLocale(word, locale)?.isNotEmpty() ?: false
            ret = ret || systemDao?.queryShortcut(word, locale)?.isNotEmpty() ?: false
        }
        return ret
    }

    @Synchronized
    fun florisUserDictionaryDao(): UserDictionaryDao? {
        return if (prefs.dictionary.enableFlorisUserDictionary.get()) {
            florisUserDictionaryDatabase?.userDictionaryDao()
        } else {
            null
        }
    }

    @Synchronized
    fun florisUserDictionaryDatabase(): FlorisUserDictionaryDatabase? {
        return if (prefs.dictionary.enableFlorisUserDictionary.get()) {
            florisUserDictionaryDatabase
        } else {
            null
        }
    }

    @Synchronized
    fun systemUserDictionaryDao(): UserDictionaryDao? {
        return if (prefs.dictionary.enableSystemUserDictionary.get()) {
            systemUserDictionaryDatabase?.userDictionaryDao()
        } else {
            null
        }
    }

    @Synchronized
    fun systemUserDictionaryDatabase(): SystemUserDictionaryDatabase? {
        return if (prefs.dictionary.enableSystemUserDictionary.get()) {
            systemUserDictionaryDatabase
        } else {
            null
        }
    }

    @Synchronized
    fun loadUserDictionariesIfNecessary() {
        val context = applicationContext.get() ?: return

        if (florisUserDictionaryDatabase == null && prefs.dictionary.enableFlorisUserDictionary.get()) {
            florisUserDictionaryDatabase = Room.databaseBuilder(
                context,
                FlorisUserDictionaryDatabase::class.java,
                FlorisUserDictionaryDatabase.DB_FILE_NAME
            ).allowMainThreadQueries().build()
        }
        if (systemUserDictionaryDatabase == null && prefs.dictionary.enableSystemUserDictionary.get()) {
            systemUserDictionaryDatabase = SystemUserDictionaryDatabase(context)
        }
    }

    @Synchronized
    fun unloadUserDictionariesIfNecessary() {
        if (florisUserDictionaryDatabase != null) {
            florisUserDictionaryDatabase?.close()
            florisUserDictionaryDatabase = null
        }
        if (systemUserDictionaryDatabase != null) {
            systemUserDictionaryDatabase = null
        }
    }
}
