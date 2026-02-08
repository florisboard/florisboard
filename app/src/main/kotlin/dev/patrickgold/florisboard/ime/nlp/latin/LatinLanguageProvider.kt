/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.nlp.latin

import android.content.Context
import android.icu.text.BreakIterator
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorGroup
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.florisboard.lib.android.readText
import org.florisboard.libnative.NlpBridge
import java.io.File

class LatinLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        const val ProviderId = "org.florisboard.nlp.providers.latin"
        private const val MAX_PRECEDING_WORDS = 3
        private const val MAX_CONTEXT_BUFFER = 5
    }

    override val providerId = ProviderId

    private val appContext by context.appContext()
    private val breakIterators = BreakIteratorGroup()
    private val personalDictFile = File(appContext.filesDir, "personal_dict.json")
    private val contextMapFile = File(appContext.filesDir, "context_map.json")

    private var latestEditorContent: EditorContent? = null
    private var isPrivateSession: Boolean = false
    private val loadedLanguages = mutableSetOf<String>()
    private var activeLanguages = listOf<String>()
    
    // Context buffer to maintain word history reliably
    private val contextBuffer = mutableListOf<String>()
    private var lastSelectionEnd: Int = 0

    override suspend fun create() {
        loadPersistedData()
    }

    override suspend fun preload(subtype: Subtype) = withContext(Dispatchers.IO) {
        val langCode = getLanguageCode(subtype)
        
        if (langCode !in loadedLanguages) {
            val binaryLoaded = try {
                appContext.assets.open("ime/dict/$langCode.dict").use { inputStream ->
                    val data = inputStream.readBytes()
                    NlpBridge.loadDictionaryBinaryForLanguage(langCode, data)
                }
            } catch (e: Exception) {
                false
            }

            if (binaryLoaded) {
                try {
                    appContext.assets.open("ime/dict/$langCode.ngrams.json").use { inputStream ->
                        val jsonData = inputStream.bufferedReader().readText()
                        NlpBridge.loadNgramsForLanguage(langCode, jsonData)
                    }
                } catch (_: Exception) { }
                
                loadedLanguages.add(langCode)
                updateActiveLanguages()
            }
        }
    }

    fun preloadSubtypes(subtypes: List<Subtype>) {
        val langCodes = subtypes.mapNotNull { subtype ->
            val code = getLanguageCode(subtype)
            if (code in loadedLanguages) code else null
        }
        if (langCodes.isNotEmpty() && langCodes != activeLanguages) {
            activeLanguages = langCodes
            NlpBridge.setLanguages(langCodes.toTypedArray())
        }
    }

    private fun updateActiveLanguages() {
        if (loadedLanguages.isNotEmpty() && loadedLanguages.toList() != activeLanguages) {
            activeLanguages = loadedLanguages.toList()
            NlpBridge.setLanguages(activeLanguages.toTypedArray())
        }
    }

    private fun getLanguageCode(subtype: Subtype): String {
        val locale = subtype.primaryLocale
        val lang = locale.language
        val country = locale.country
        return if (country.isNotBlank()) "${lang}_$country" else lang
    }

    override suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean
    ): SpellingResult {
        val normalized = word.lowercase().trim()
        if (normalized.isBlank()) return SpellingResult.unspecified()

        val result = NlpBridge.spellCheck(normalized, precedingWords, maxSuggestionCount)
            ?: return SpellingResult.unspecified()

        return when {
            result.is_valid -> SpellingResult.validWord()
            result.is_typo -> SpellingResult.typo(
                result.suggestions.toTypedArray(),
                isHighConfidenceResult = result.suggestions.size >= 2
            )
            else -> SpellingResult.unspecified()
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean
    ): List<SuggestionCandidate> {
        latestEditorContent = content
        this.isPrivateSession = isPrivateSession
        val prefix = content.composingText.trim()
        
        // Extract current context from text before cursor
        val extractedContext = extractContextWords(content, subtype)
        
        // Detect new input session: cursor at start with no/little text, or text field changed
        val currentSelectionEnd = content.selection.end
        val textLength = content.textBeforeSelection.length + content.composingText.length
        
        if (textLength <= 1 && currentSelectionEnd <= 1) {
            if (contextBuffer.isNotEmpty()) {
                contextBuffer.clear()
            }
        } else if (currentSelectionEnd < lastSelectionEnd - 1) {
            // Cursor jumped backwards significantly - reset context buffer
            contextBuffer.clear()
            contextBuffer.addAll(extractedContext)
        }
        lastSelectionEnd = currentSelectionEnd
        
        val context = if (extractedContext.size >= 2) {
            extractedContext
        } else if (extractedContext.isNotEmpty() && contextBuffer.isNotEmpty()) {
            val lastExtracted = extractedContext.last()
            if (contextBuffer.contains(lastExtracted)) {
                val idx = contextBuffer.lastIndexOf(lastExtracted)
                contextBuffer.subList(0, idx).toList() + extractedContext
            } else {
                extractedContext
            }
        } else {
            extractedContext
        }.takeLast(MAX_CONTEXT_BUFFER)

        if (prefix.length < 2 && extractedContext.isNotEmpty() && !isPrivateSession) {
            val lastWord = extractedContext.last()
            val precedingContext = if (extractedContext.size >= 2) {
                extractedContext.dropLast(1)
            } else if (contextBuffer.isNotEmpty() && contextBuffer.last() != lastWord) {
                contextBuffer.takeLast(2)
            } else {
                emptyList()
            }
            
            // Learn word association (what word follows previous words)
            if (precedingContext.isNotEmpty()) {
                NlpBridge.learnWord(lastWord, precedingContext)
            }
            // Also boost the word itself in personal dictionary
            NlpBridge.learnWord(lastWord, emptyList())
            
            // Update context buffer with the new word
            if (contextBuffer.lastOrNull() != lastWord) {
                contextBuffer.add(lastWord)
                if (contextBuffer.size > MAX_CONTEXT_BUFFER) {
                    contextBuffer.removeAt(0)
                }
            }
        }

        if (prefix.length < 2) {
            return predictNextWord(context, maxCandidateCount)
        }

        val suggestions = NlpBridge.suggest(prefix, context, maxCandidateCount - 1)

        val candidates = suggestions.map { suggestion ->
            WordSuggestionCandidate(
                text = suggestion.text,
                confidence = suggestion.confidence,
                isEligibleForAutoCommit = suggestion.is_eligible_for_auto_commit,
                sourceProvider = this
            )
        }

        val typedWordCandidate = WordSuggestionCandidate(
            text = prefix,
            confidence = 0.5,
            isEligibleForAutoCommit = false,
            sourceProvider = this
        )

        return if (candidates.none { it.text.toString().equals(prefix, ignoreCase = true) }) {
            listOf(typedWordCandidate) + candidates
        } else {
            candidates
        }
    }

    private fun predictNextWord(context: List<String>, maxCount: Int): List<SuggestionCandidate> {
        if (context.isEmpty()) return emptyList()
        
        return NlpBridge.predictNextWord(context, maxCount).map { suggestion ->
            WordSuggestionCandidate(
                text = suggestion.text,
                confidence = suggestion.confidence,
                isEligibleForAutoCommit = false,
                sourceProvider = this
            )
        }
    }

    private suspend fun extractContextWords(content: EditorContent, subtype: Subtype): List<String> {
        val text = content.textBeforeSelection
        return breakIterators.word(subtype.primaryLocale) { iterator ->
            iterator.setText(text)
            buildList {
                var end = iterator.last()
                repeat(MAX_PRECEDING_WORDS) {
                    val start = iterator.previous()
                    if (start == BreakIterator.DONE) return@repeat
                    val word = text.substring(start, end).trim().lowercase()
                    if (word.length >= 2 && word.all { it.isLetter() }) add(word)
                    end = start
                }
            }.reversed()
        }
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        if (isPrivateSession) return
        
        val word = candidate.text.toString().lowercase().trim()
        if (word.length < 2) return

        val context = latestEditorContent?.let { extractContextWords(it, subtype) } ?: emptyList()
        NlpBridge.learnWord(word, context)
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        if (isPrivateSession) return
        
        val word = candidate.text.toString().lowercase().trim()
        NlpBridge.penalizeWord(word)
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        val word = candidate.text.toString().lowercase().trim()
        return NlpBridge.removeWord(word)
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> = emptyList()

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return NlpBridge.getFrequency(word.lowercase())
    }

    private fun loadPersistedData() {
        if (personalDictFile.exists()) {
            personalDictFile.readText().takeIf { it.isNotBlank() }?.let {
                NlpBridge.importPersonalDict(it)
            }
        }
        if (contextMapFile.exists()) {
            contextMapFile.readText().takeIf { it.isNotBlank() }?.let {
                NlpBridge.importContextMap(it)
            }
        }
    }

    private suspend fun savePersistedData() = withContext(Dispatchers.IO) {
        NlpBridge.exportPersonalDict()?.let { personalDictFile.writeText(it) }
        NlpBridge.exportContextMap()?.let { contextMapFile.writeText(it) }
    }

    override suspend fun destroy() {
        savePersistedData()
        NlpBridge.clear()
        loadedLanguages.clear()
        activeLanguages = emptyList()
        contextBuffer.clear()
        lastSelectionEnd = 0
    }
}
