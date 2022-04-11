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
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.spelling.SpellingLanguageMode
import dev.patrickgold.florisboard.ime.spelling.SpellingService
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import kotlinx.coroutines.runBlocking

class FlorisSpellCheckerService : SpellCheckerService() {
    private val prefs by florisPreferenceModel()
    private val dictionaryManager get() = DictionaryManager.default()
    private val spellingManager by spellingManager()
    private val spellingService by spellingService()
    private val subtypeManager by subtypeManager()

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
            flogInfo(LogTopic.SPELL_EVENTS) { "Session requested locale: $locale" }

            setupSpellingIfNecessary()
        }

        private fun setupSpellingIfNecessary() {
            val evaluatedLocale = when (prefs.spelling.languageMode.get()) {
                SpellingLanguageMode.USE_KEYBOARD_SUBTYPES -> {
                    subtypeManager.activeSubtype().primaryLocale
                }
                else -> {
                    FlorisLocale.default()
                }
            }

            if (evaluatedLocale != cachedSpellingLocale) {
                cachedSpellingLocale = evaluatedLocale
            }
            flogInfo(LogTopic.SPELL_EVENTS) { "Session actual locale: ${cachedSpellingLocale?.languageTag()}" }
        }

        private fun spellMultiple(
            spellingLocale: FlorisLocale,
            textInfos: Array<out TextInfo>,
            suggestionsLimit: Int,
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

            return spellingService
                .spell(spellingLocale, textInfo.text, suggestionsLimit)
                .sendToDebugOverlayIfEnabled(textInfo)
        }

        override fun onGetSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int,
            sequentialWords: Boolean,
        ): Array<SuggestionsInfo> {
            flogInfo(LogTopic.SPELL_EVENTS)

            textInfos ?: return emptyArray()
            setupSpellingIfNecessary()
            val spellingLocale = cachedSpellingLocale ?: return emptyArray()

            return spellMultiple(spellingLocale, textInfos, suggestionsLimit).sendToDebugOverlayIfEnabled(textInfos)
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int,
        ): Array<SentenceSuggestionsInfo> {
            flogInfo(LogTopic.SPELL_EVENTS)

            // TODO: implement custom solution here instead of calling the default implementation
            return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit)
        }

        override fun onCancel() {
            flogInfo(LogTopic.SPELL_EVENTS)

            super.onCancel()
            if (prefs.devtools.showSpellingOverlay.get()) {
                spellingManager.clearDebugOverlay()
            }
        }

        override fun onClose() {
            flogInfo(LogTopic.SPELL_EVENTS)

            super.onClose()
            if (prefs.devtools.showSpellingOverlay.get()) {
                spellingManager.clearDebugOverlay()
            }
        }

        fun SuggestionsInfo.sendToDebugOverlayIfEnabled(
            textInfo: TextInfo,
        ): SuggestionsInfo {
            if (prefs.devtools.showSpellingOverlay.get()) {
                spellingManager.addToDebugOverlay(textInfo.text, this)
            }
            return this
        }

        fun Array<SuggestionsInfo>.sendToDebugOverlayIfEnabled(
            textInfos: Array<out TextInfo>,
        ): Array<SuggestionsInfo> {
            if (prefs.devtools.showSpellingOverlay.get()) {
                for ((n, info) in this.withIndex()) {
                    spellingManager.addToDebugOverlay(textInfos[n].text, info)
                }
            }
            return this
        }
    }
}
