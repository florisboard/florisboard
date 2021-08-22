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
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.spelling.SpellingService
import kotlinx.coroutines.runBlocking

class FlorisSpellCheckerService : SpellCheckerService() {
    companion object {
        private const val USE_FLORIS_SUBTYPES_LOCALE: String = "zz"
    }

    private val dictionaryManager get() = DictionaryManager.default()
    private val spellingService: SpellingService = SpellingService.globalInstance()
    private val subtypeManager get() = SubtypeManager.default()

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

    private inner class FlorisSpellCheckerSession : Session() {
        private var cachedSpellingLocale: FlorisLocale? = null

        override fun onCreate() {
            flogInfo(LogTopic.SPELL_EVENTS) { "Session locale: $locale" }

            setupSpellingIfNecessary()
        }

        private fun setupSpellingIfNecessary() {
            val evaluatedLocale = when (locale) {
                null -> Subtype.DEFAULT.locale
                USE_FLORIS_SUBTYPES_LOCALE -> (subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT).locale
                else -> FlorisLocale.from(locale)
            }

            if (evaluatedLocale != cachedSpellingLocale) {
                cachedSpellingLocale = evaluatedLocale
            }
        }

        private fun spellMultiple(
            spellingLocale: FlorisLocale,
            textInfos: Array<out TextInfo>,
            suggestionsLimit: Int
        ): Array<SuggestionsInfo> = runBlocking {
            val retInfos = Array(textInfos.size) { n ->
                val word = textInfos[n].text ?: ""
                spellingService.spellAsync(spellingLocale, word, suggestionsLimit)
            }
            Array(textInfos.size) { n ->
                retInfos[n].await().apply {
                    setCookieAndSequence(textInfos[n].cookie, textInfos[n].sequence)
                }
            }
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            flogInfo(LogTopic.SPELL_EVENTS) { "text=${textInfo?.text}, limit=$suggestionsLimit" }

            textInfo?.text ?: return SpellingService.emptySuggestionsInfo()
            setupSpellingIfNecessary()
            val spellingLocale = cachedSpellingLocale ?: return SpellingService.emptySuggestionsInfo()

            return spellingService.spell(spellingLocale, textInfo.text, suggestionsLimit)
        }

        override fun onGetSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int,
            sequentialWords: Boolean
        ): Array<SuggestionsInfo> {
            flogInfo(LogTopic.SPELL_EVENTS)

            textInfos ?: return emptyArray()
            setupSpellingIfNecessary()
            val spellingLocale = cachedSpellingLocale ?: return emptyArray()

            return spellMultiple(spellingLocale, textInfos, suggestionsLimit)
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int
        ): Array<SentenceSuggestionsInfo> {
            flogInfo(LogTopic.SPELL_EVENTS)

            // TODO: implement custom solution here instead of calling the default implementation
            return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit)
        }

        override fun onCancel() {
            flogInfo(LogTopic.SPELL_EVENTS)

            super.onCancel()
        }

        override fun onClose() {
            flogInfo(LogTopic.SPELL_EVENTS)

            super.onClose()
        }
    }
}
