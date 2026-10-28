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
import android.util.AtomicFile
import androidx.core.util.readText
import androidx.core.util.writeText
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorGroup
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.devtools.flogError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.florisboard.libnative.NlpBridge
import org.k3lp.runtime.K3Content
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class LatinLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        const val ProviderId = "org.florisboard.nlp.providers.latin"
        private const val MAX_PRECEDING_WORDS = 3
        private const val MAX_CONTEXT_BUFFER = 5
        private val SAVE_DELAY = 5.seconds
    }

    override val providerId = ProviderId

    private val appContext by context.appContext()
    private val breakIterators = BreakIteratorGroup()
    private val personalDictFile = AtomicFile(File(appContext.filesDir, "personal_dict.json"))
    private val contextMapFile = AtomicFile(File(appContext.filesDir, "context_map.json"))
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingSave: Job? = null

    private var latestEditorContent: K3Content? = null
    private var isPrivateSession: Boolean = false
    private val loadedLanguages = mutableSetOf<String>()
    private var activeLanguages = listOf<String>()
    @Volatile
    private var offensiveWords = emptySet<String>()
    
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
                try {
                    appContext.assets.open("ime/dict/$langCode.offensive.txt").use { inputStream ->
                        offensiveWords = offensiveWords + inputStream.bufferedReader().readLines()
                            .filter { it.isNotBlank() && !it.startsWith("#") }
                            .map { it.trim().lowercase() }
                    }
                } catch (_: Exception) { }

                loadedLanguages.add(langCode)
                updateActiveLanguages()
            }
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
            result.is_typo -> {
                val suggestions = result.suggestions.filterNot { isBlocked(it, allowPossiblyOffensive) }
                SpellingResult.typo(
                    suggestions.toTypedArray(),
                    isHighConfidenceResult = suggestions.size >= 2
                )
            }
            else -> SpellingResult.unspecified()
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: K3Content,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean
    ): List<SuggestionCandidate> {
        latestEditorContent = content
        this.isPrivateSession = isPrivateSession
        val composingText = content.compositionText.orEmpty()
        val prefix = composingText.trim()
        
        // Extract current context from text before cursor
        val extractedContext = extractContextWords(content, subtype)
        
        // Detect new input session: cursor at start with no/little text, or text field changed
        val currentSelectionEnd = content.selection.end
        val textLength = content.surroundingText.textBefore.length + composingText.length
        
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
            scheduleSave()

            // Update context buffer with the new word
            if (contextBuffer.lastOrNull() != lastWord) {
                contextBuffer.add(lastWord)
                if (contextBuffer.size > MAX_CONTEXT_BUFFER) {
                    contextBuffer.removeAt(0)
                }
            }
        }

        if (prefix.length < 2) {
            return predictNextWord(context, maxCandidateCount).filterNot { isBlocked(it.text, allowPossiblyOffensive) }
        }

        val suggestions = NlpBridge.suggest(prefix, context, maxCandidateCount - 1)

        val candidates = suggestions.filterNot { isBlocked(it.text, allowPossiblyOffensive) }.map { suggestion ->
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

    private fun isBlocked(word: CharSequence, allowPossiblyOffensive: Boolean): Boolean {
        return !allowPossiblyOffensive && word.toString().lowercase() in offensiveWords
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

    private suspend fun extractContextWords(content: K3Content, subtype: Subtype): List<String> {
        val text = content.surroundingText.textBefore
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
        scheduleSave()
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        if (isPrivateSession) return
        
        val word = candidate.text.toString().lowercase().trim()
        NlpBridge.penalizeWord(word)
        scheduleSave()
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        val word = candidate.text.toString().lowercase().trim()
        return NlpBridge.removeWord(word).also { if (it) scheduleSave() }
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> = emptyList()

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return NlpBridge.getFrequency(word.lowercase())
    }

    private fun loadPersistedData() {
        personalDictFile.readTextOrNull()?.takeIf { it.isNotBlank() }?.let {
            NlpBridge.importPersonalDict(it)
        }
        contextMapFile.readTextOrNull()?.takeIf { it.isNotBlank() }?.let {
            NlpBridge.importContextMap(it)
        }
    }

    private fun AtomicFile.readTextOrNull(): String? {
        return try {
            readText()
        } catch (_: FileNotFoundException) {
            null
        }
    }

    @Synchronized
    private fun scheduleSave() {
        pendingSave?.cancel()
        pendingSave = scope.launch {
            delay(SAVE_DELAY)
            try {
                savePersistedData()
            } catch (e: IOException) {
                flogError { "Failed to save learned words: $e" }
            }
        }
    }

    private suspend fun savePersistedData() = withContext(Dispatchers.IO) {
        NlpBridge.exportPersonalDict()?.let { personalDictFile.writeText(it) }
        NlpBridge.exportContextMap()?.let { contextMapFile.writeText(it) }
    }

    override suspend fun destroy() {
        synchronized(this) { pendingSave }?.cancelAndJoin()
        savePersistedData()
        NlpBridge.clear()
        loadedLanguages.clear()
        activeLanguages = emptyList()
        offensiveWords = emptySet()
        contextBuffer.clear()
        lastSelectionEnd = 0
    }
}
