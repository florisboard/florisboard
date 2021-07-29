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

package dev.patrickgold.florisboard

import android.service.textservice.SpellCheckerService
import android.util.LruCache
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.spelling.SpellingDict
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class FlorisSpellCheckerService : SpellCheckerService() {
    private val dictionaryManager get() = DictionaryManager.default()

    override fun onCreate() {
        flogInfo(LogTopic.SPELL_EVENTS)

        super.onCreate()
        dictionaryManager.loadUserDictionariesIfNecessary()
    }

    override fun createSession(): Session {
        flogInfo(LogTopic.SPELL_EVENTS)

        return FlorisSpellCheckerSession()
    }

    override fun onDestroy() {
        flogInfo(LogTopic.SPELL_EVENTS)

        super.onDestroy()
    }

    private class SuggestionsCache(size: Int) {
        val suggestionsInfoCache: LruCache<String, SuggestionsInfo> = LruCache(size)

        inline fun getOrGenerate(word: String, generator: (w: String) -> SuggestionsInfo): SuggestionsInfo {
            contract {
                callsInPlace(generator, InvocationKind.AT_MOST_ONCE)
            }
            val cachedSuggestionsInfo = suggestionsInfoCache.get(word)
            if (cachedSuggestionsInfo != null) {
                return cachedSuggestionsInfo
            }
            val newSuggestionsInfo = generator(word)
            suggestionsInfoCache.put(word, newSuggestionsInfo)
            return newSuggestionsInfo
        }
    }

    private class FlorisSpellCheckerSession : Session() {
        companion object {
            private const val USE_FLORIS_SUBTYPES_LOCALE: String = "zz"
            private const val SUGGESTIONS_MAX_SIZE = 50

            private val EMPTY_STRING_ARRAY: Array<out String> = arrayOf()
        }

        private val prefs get() = Preferences.default()
        private val dictionaryManager get() = DictionaryManager.default()
        private val spellingManager get() = SpellingManager.default()
        private val subtypeManager get() = SubtypeManager.default()

        private var spellingDict: SpellingDict? = null
        private lateinit var spellingLocale: FlorisLocale
        private val suggestionsCache = SuggestionsCache(SUGGESTIONS_MAX_SIZE)

        override fun onCreate() {
            flogInfo(LogTopic.SPELL_EVENTS) { "Session locale: $locale" }

            spellingLocale = when (locale) {
                null -> Subtype.DEFAULT.locale
                USE_FLORIS_SUBTYPES_LOCALE -> (subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT).locale
                else -> FlorisLocale.from(locale)
            }

            spellingDict = spellingManager.getSpellingDict(spellingLocale)
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            flogInfo(LogTopic.SPELL_EVENTS) { "text=${textInfo?.text}, limit=$suggestionsLimit"}

            val spellingDict = spellingDict ?: return SuggestionsInfo(0, EMPTY_STRING_ARRAY)
            val word = textInfo?.text ?: return SuggestionsInfo(0, EMPTY_STRING_ARRAY)

            return suggestionsCache.getOrGenerate(word) {
                var isWordOk = false
                if (prefs.spelling.useUdmEntries) {
                    isWordOk = dictionaryManager.spell(word, spellingLocale)
                }
                return@getOrGenerate if (isWordOk) {
                    SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, EMPTY_STRING_ARRAY)
                } else {
                    isWordOk = spellingDict.spell(word)
                    if (isWordOk) {
                        SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, EMPTY_STRING_ARRAY)
                    } else {
                        val suggestions = spellingDict.suggest(word, suggestionsLimit)
                        SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO, suggestions)
                    }
                }
            }
        }
    }
}
