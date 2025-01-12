/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.nlp.SpellingLanguageMode
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.florisboard.lib.kotlin.map

class FlorisSpellCheckerService : SpellCheckerService() {
    private val prefs by florisPreferenceModel()
    private val dictionaryManager get() = DictionaryManager.default()
    private val nlpManager by nlpManager()
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
        private var cachedSpellingSubtype: Subtype? = null

        override fun onCreate() {
            flogInfo(LogTopic.SPELL_EVENTS) { "Session requested locale: $locale" }

            setupSpellingIfNecessary()
        }

        private fun setupSpellingIfNecessary() {
            val evaluatedSubtype = when (prefs.spelling.languageMode.get()) {
                SpellingLanguageMode.USE_KEYBOARD_SUBTYPES -> {
                    subtypeManager.activeSubtype
                }
                else -> {
                    Subtype.DEFAULT.copy(primaryLocale = FlorisLocale.default())
                }
            }

            if (evaluatedSubtype != cachedSpellingSubtype) {
                cachedSpellingSubtype = evaluatedSubtype
                nlpManager.preload(evaluatedSubtype)
            }
            flogInfo(LogTopic.SPELL_EVENTS) {
                "Session actual locale: ${cachedSpellingSubtype?.primaryLocale?.languageTag()}"
            }
        }

        private fun spellMultiple(
            spellingSubtype: Subtype,
            textInfos: Array<out TextInfo>,
            suggestionsLimit: Int,
        ): Array<SpellingResult> = runBlocking {
            val retInfos = Array(textInfos.size) { n ->
                val word = textInfos[n].text ?: ""
                async { nlpManager.spell(spellingSubtype, word, emptyList(), emptyList(), suggestionsLimit) }
            }
            Array(textInfos.size) { n ->
                retInfos[n].await().apply {
                    suggestionsInfo.setCookieAndSequence(textInfos[n].cookie, textInfos[n].sequence)
                }
            }
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            flogInfo(LogTopic.SPELL_EVENTS) { "text=${textInfo?.text}, limit=$suggestionsLimit" }

            textInfo?.text ?: return SpellingResult.unspecified().suggestionsInfo
            setupSpellingIfNecessary()
            val spellingSubtype = cachedSpellingSubtype ?: return SpellingResult.unspecified().suggestionsInfo

            return runBlocking {
                nlpManager
                    .spell(spellingSubtype, textInfo.text, emptyList(), emptyList(), suggestionsLimit)
                    .sendToDebugOverlayIfEnabled(textInfo)
                    .suggestionsInfo
            }
        }

        override fun onGetSuggestionsMultiple(
            textInfos: Array<out TextInfo>?,
            suggestionsLimit: Int,
            sequentialWords: Boolean,
        ): Array<SuggestionsInfo> {
            flogInfo(LogTopic.SPELL_EVENTS)

            textInfos ?: return emptyArray()
            setupSpellingIfNecessary()
            val spellingSubtype = cachedSpellingSubtype ?: return emptyArray()

            return spellMultiple(spellingSubtype, textInfos, suggestionsLimit)
                .sendToDebugOverlayIfEnabled(textInfos)
                .map { it.suggestionsInfo }
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
                nlpManager.clearDebugOverlay()
            }
        }

        override fun onClose() {
            flogInfo(LogTopic.SPELL_EVENTS)

            super.onClose()
            if (prefs.devtools.showSpellingOverlay.get()) {
                nlpManager.clearDebugOverlay()
            }
        }

        fun SpellingResult.sendToDebugOverlayIfEnabled(
            textInfo: TextInfo,
        ): SpellingResult {
            if (prefs.devtools.showSpellingOverlay.get()) {
                nlpManager.addToDebugOverlay(textInfo.text, this)
            }
            return this
        }

        fun Array<SpellingResult>.sendToDebugOverlayIfEnabled(
            textInfos: Array<out TextInfo>,
        ): Array<SpellingResult> {
            if (prefs.devtools.showSpellingOverlay.get()) {
                for ((n, info) in this.withIndex()) {
                    nlpManager.addToDebugOverlay(textInfos[n].text, info)
                }
            }
            return this
        }
    }
}
