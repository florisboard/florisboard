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
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.nlp.SuggestionList
import dev.patrickgold.florisboard.ime.nlp.Word
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.lang.ref.WeakReference
import java.util.*

/**
 * TODO: document
 */
class DictionaryManager private constructor(
    context: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val applicationContext: WeakReference<Context> = WeakReference(context.applicationContext ?: context)
    private val prefs get() = Preferences.default()
    private val florisboard get() = FlorisBoard.getInstance()

    private val dictionaryCache: MutableMap<String, Dictionary> = mutableMapOf()

    private var florisUserDictionaryDatabase: FlorisUserDictionaryDatabase? = null
    private var systemUserDictionaryDatabase: SystemUserDictionaryDatabase? = null

    companion object {
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
        queryUserDictionary(currentWord, subtype.locale, suggestions)
        block(suggestions)
        suggestions.dispose()
    }

    fun prepareDictionaries(subtype: Subtype) {
        // TODO: Implement this
    }

    fun queryUserDictionary(word: Word, locale: Locale, destSuggestionList: SuggestionList) {
        val florisDao = florisUserDictionaryDao()
        val systemDao = systemUserDictionaryDao()
        if (florisDao == null && systemDao == null) {
            return
        }
        if (prefs.dictionary.enableFlorisUserDictionary) {
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
        if (prefs.dictionary.enableSystemUserDictionary) {
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

    @Synchronized
    fun florisUserDictionaryDao(): UserDictionaryDao? {
        return if (prefs.suggestion.enabled && prefs.dictionary.enableFlorisUserDictionary) {
            florisUserDictionaryDatabase?.userDictionaryDao()
        } else {
            null
        }
    }

    @Synchronized
    fun florisUserDictionaryDatabase(): FlorisUserDictionaryDatabase? {
        return if (prefs.suggestion.enabled && prefs.dictionary.enableFlorisUserDictionary) {
            florisUserDictionaryDatabase
        } else {
            null
        }
    }

    @Synchronized
    fun systemUserDictionaryDao(): UserDictionaryDao? {
        return if (prefs.suggestion.enabled && prefs.dictionary.enableSystemUserDictionary) {
            systemUserDictionaryDatabase?.userDictionaryDao()
        } else {
            null
        }
    }

    @Synchronized
    fun systemUserDictionaryDatabase(): SystemUserDictionaryDatabase? {
        return if (prefs.suggestion.enabled && prefs.dictionary.enableSystemUserDictionary) {
            systemUserDictionaryDatabase
        } else {
            null
        }
    }

    @Synchronized
    fun loadUserDictionariesIfNecessary() {
        val context = applicationContext.get() ?: return

        if (prefs.suggestion.enabled) {
            if (florisUserDictionaryDatabase == null && prefs.dictionary.enableFlorisUserDictionary) {
                florisUserDictionaryDatabase = Room.databaseBuilder(
                    context,
                    FlorisUserDictionaryDatabase::class.java,
                    FlorisUserDictionaryDatabase.DB_FILE_NAME
                ).allowMainThreadQueries().build()
            }
            if (systemUserDictionaryDatabase == null && prefs.dictionary.enableSystemUserDictionary) {
                systemUserDictionaryDatabase = SystemUserDictionaryDatabase(context)
            }
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
