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

package dev.patrickgold.florisboard.ime.spelling

import android.content.Context
import android.util.LruCache
import android.view.textservice.SuggestionsInfo
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.spellingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class SpellingService(context: Context) {
    companion object {
        private const val LRU_CACHE_MAX_LOCALE_ENTRIES = 4
        private const val LRU_CACHE_MAX_SUGGESTIONS_INFO_ENTRIES = 192

        private val EMPTY_STRING_ARRAY: Array<out String> = arrayOf()

        fun emptySuggestionsInfo() =
            SuggestionsInfo(0, EMPTY_STRING_ARRAY)

        fun dictMatchSuggestionsInfo() =
            SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, EMPTY_STRING_ARRAY)

        fun typoSuggestionsInfo(suggestions: Array<out String>) =
            SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO, suggestions)
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val prefs by florisPreferenceModel()
    private val dictionaryManager get() = DictionaryManager.default()
    private val spellingManager by context.spellingManager()

    private val globalCache = LruCache<FlorisLocale, SuggestionsCache>(LRU_CACHE_MAX_LOCALE_ENTRIES)
    private val globalCacheGuard = Mutex(locked = false)

    private suspend fun getSuggestionsCache(locale: FlorisLocale): SuggestionsCache? {
        return globalCacheGuard.withLock {
            val suCache = globalCache.get(locale)
            if (suCache != null) {
                suCache
            } else {
                val spellingDict = spellingManager.getSpellingDict(locale)
                if (spellingDict != null) {
                    val newCache = SuggestionsCache(LRU_CACHE_MAX_SUGGESTIONS_INFO_ENTRIES, spellingDict)
                    globalCache.put(locale, newCache)
                    newCache
                } else {
                    null
                }
            }
        }
    }

    fun spell(locale: FlorisLocale, word: String, suggestionsLimit: Int): SuggestionsInfo {
        return runBlocking { spellAsync(locale, word, suggestionsLimit).await() }
    }

    suspend fun spellAsync(locale: FlorisLocale, word: String, suggestionsLimit: Int): Deferred<SuggestionsInfo> {
        val suggestionsCache = getSuggestionsCache(locale) ?: return scope.async { emptySuggestionsInfo() }
        return suggestionsCache.getOrGenerateAsync(word) { spellingDict ->
            var isWordOk = false
            if (prefs.spelling.useUdmEntries.get()) {
                isWordOk = dictionaryManager.spell(word, locale)
            }
            return@getOrGenerateAsync if (isWordOk) {
                dictMatchSuggestionsInfo()
            } else {
                isWordOk = spellingDict.spell(word)
                if (isWordOk) {
                    dictMatchSuggestionsInfo()
                } else {
                    val suggestions = spellingDict.suggest(word, suggestionsLimit)
                    typoSuggestionsInfo(suggestions)
                }
            }
        }
    }

    private inner class SuggestionsCache(size: Int, private val spellingDict: SpellingDict) {
        private val localCache = LruCache<String, Deferred<SuggestionsInfo>>(size)
        private val localCacheGuard = Mutex(locked = false)

        suspend inline fun getOrGenerateAsync(
            word: String,
            crossinline generator: (dict: SpellingDict) -> SuggestionsInfo
        ): Deferred<SuggestionsInfo> {
            contract {
                callsInPlace(generator, InvocationKind.AT_MOST_ONCE)
            }
            return localCacheGuard.withLock {
                val cachedInfo = localCache.get(word)
                if (cachedInfo != null) {
                    cachedInfo
                } else {
                    val newInfo = scope.async {
                        generator(spellingDict)
                    }
                    localCache.put(word, newInfo)
                    newInfo
                }
            }
        }

        suspend fun clear() {
            localCacheGuard.withLock {
                localCache.evictAll()
            }
        }
    }
}
