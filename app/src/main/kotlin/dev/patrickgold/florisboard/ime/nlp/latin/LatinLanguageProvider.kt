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
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.nlp.*
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.apache.commons.codec.language.Metaphone
import org.florisboard.lib.android.readText
import org.florisboard.lib.kotlin.guardedByLock
import java.io.File
import kotlin.math.max
import kotlin.math.min

class LatinLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        // Default user ID used for all subtypes, unless otherwise specified.
        // See `ime/core/Subtype.kt` Line 210 and 211 for the default usage
        const val ProviderId = "org.florisboard.nlp.providers.latin"

        // Minimum word length to trigger suggestions
        private const val MIN_WORD_LENGTH = 2

        // Maximum allowed Levenshtein distance for spelling corrections
        private const val MAX_EDIT_DISTANCE = 2

        // Number of preceding words to consider for context-aware prediction
        private const val MAX_PRECEDING_WORDS = 3

        // Scoring bonuses
        private const val PERSONAL_BONUS = 0.25
        private const val CONTEXT_BONUS = 0.15
        private const val EXACT_MATCH_BONUS = 0.5

        // Learning constants
        private const val DECAY_FACTOR = 0.95
        private const val MAX_PERSONAL_FREQ = 255
    }

    override val providerId = ProviderId

    private val appContext by context.appContext()
    private val dictionaryManager = DictionaryManager.default()
    private val breakIterators = BreakIteratorGroup()
    private val metaphone = Metaphone()

    private val mainDict = guardedByLock { mutableMapOf<String, Int>() }
    private val personalDict = guardedByLock { mutableMapOf<String, Int>() }
    private val contextMap = guardedByLock { mutableMapOf<String, MutableMap<String, Int>>() }
    private val phoneticIndex = guardedByLock { mutableMapOf<String, MutableSet<String>>() }
    private val predictionCache = guardedByLock { mutableMapOf<String, List<WordSuggestionCandidate>>() }
    private val prefixIndex = guardedByLock { mutableMapOf<Char, MutableList<Pair<String, Int>>>() }

    private val contextFile = File(appContext.filesDir, "context_associations.json")
    private val contextSerializer = MapSerializer(String.serializer(), MapSerializer(String.serializer(), Int.serializer()))

    private var latestEditorContent: EditorContent? = null

    /**
     * Initializes personal dictionary, context associations, and phonetic fallback index.
     */
    override suspend fun create() {
        loadPersonalDictionary()
        loadContextAssociations()
        buildPhoneticIndex()
    }

    /**
     * Loads the dictionary data from assets when a new subtype is activated.
     */
    override suspend fun preload(subtype: Subtype) = withContext(Dispatchers.IO) {
        mainDict.withLock {
            if (it.isEmpty()) {
                val rawData = appContext.assets.readText("ime/dict/data.json")
                val json = Json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), rawData)
                it.putAll(json.mapKeys { it.key.trim() })
            }

            prefixIndex.withLock { index ->
                index.clear()
                for ((word, freq) in it) {
                    val firstChar = word.firstOrNull()?.lowercaseChar() ?: continue
                    index.getOrPut(firstChar) { mutableListOf() }.add(word to freq)
                }
            }
        }
    }

    /**
     * Validates whether a word is known, or provides correction suggestions otherwise.
     */
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
        if (isKnownWord(normalized, word)) return SpellingResult.validWord()

        val suggestions = suggestCorrections(normalized, precedingWords, maxSuggestionCount)
        return if (suggestions.isNotEmpty())
            SpellingResult.typo(suggestions.toTypedArray(), isHighConfidenceResult = suggestions.size >= 2)
        else
            SpellingResult.typo(arrayOf())
    }

    /**
     * Generates word suggestions based on user input and prior context.
     */
    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean
    ): List<SuggestionCandidate> {
        latestEditorContent = content
        val prefix = content.composingText.trim()
        if (prefix.length < MIN_WORD_LENGTH) return emptyList()

        val precedingWords = extractContextWords(content, subtype)
        return suggestWords(prefix, precedingWords, maxCandidateCount)
    }

    /**
     * Determines if a word exists in the main or personal dictionary.
     */
    private suspend fun isKnownWord(normalized: String, original: String): Boolean {
        val variants = listOf(normalized, original, normalized.replaceFirstChar { it.uppercase() }, normalized.uppercase())
        return mainDict.withLock { dict -> variants.any { dict.containsKey(it) } } ||
               personalDict.withLock { dict -> variants.any { dict.containsKey(it) } }
    }

    /**
     * Suggests spelling corrections based on edit distance and context.
     */
    private suspend fun suggestCorrections(word: String, context: List<String>, max: Int): List<String> {
        val candidates = mutableListOf<Pair<String, Double>>()
        val dicts = listOf(mainDict to 0.0, personalDict to PERSONAL_BONUS)

        for ((dict, bonus) in dicts) {
            dict.withLock {
                for ((candidate, freq) in it) {
                    val dist = editDistance(word, candidate)
                    if (dist <= MAX_EDIT_DISTANCE) {
                        val score = spellingScore(word, candidate, freq, dist) + bonus + contextScore(candidate, context)
                        candidates.add(candidate to score)
                    }
                }
            }
        }

        return candidates.sortedByDescending { it.second }.map { it.first }.distinct().take(max)
    }

    /**
     * Standard Levenshtein distance algorithm.
     */
    private fun editDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }

    /**
     * Computes a weighted spelling score using frequency, distance, and prefix matching.
     */
    private fun spellingScore(input: String, candidate: String, freq: Int, dist: Int): Double {
        val freqScore = when {
            freq >= 5000 -> 1.0
            freq >= 1000 -> 0.9
            freq >= 200  -> 0.6
            freq >= 10   -> 0.3
            else         -> 0.1
        }
        val distScore = (MAX_EDIT_DISTANCE - dist).toDouble() / MAX_EDIT_DISTANCE
        val prefixBonus = if (candidate.startsWith(input)) 0.2 else 0.0
        return freqScore * 0.4 + distScore * 0.4 + prefixBonus * 0.2
    }

    private fun getPrefixVariants(prefix: String): List<String> {
        val normalized = prefix.lowercase()
        return listOf(
            normalized,
            prefix,
            normalized.replaceFirstChar { it.uppercase() },
            normalized.uppercase()
        )
    }

    /**
     * Suggests word completions/predictions using prefix and context.
     */
    private suspend fun suggestWords(prefix: String, context: List<String>, maxCount: Int): List<SuggestionCandidate> {
        val prefixVariants = getPrefixVariants(prefix)
        val suggestions = mutableListOf<SuggestionCandidate>()

        personalDict.withLock {
            for ((word, freq) in it) {
                if (prefixVariants.none { variant -> word.lowercase().startsWith(variant.lowercase()) }) continue

                val displayWord = when {
                    prefix.all { it.isUpperCase() } -> word.uppercase()
                    prefix.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
                    else -> word.lowercase()
                }

                var conf = frequencyScore(freq) * 0.6 + contextScore(word, context) * 0.2
                if (word.equals(prefix, ignoreCase = true)) conf += EXACT_MATCH_BONUS

                suggestions.add(
                    WordSuggestionCandidate(
                        text = displayWord,
                        confidence = conf,
                        sourceProvider = this
                    )
                )
            }
        }

        prefixIndex.withLock { index ->
            val firstChar = prefix.firstOrNull()?.lowercaseChar()
            val indexedCandidates = index[firstChar] ?: emptyList()

            for ((word, freq) in indexedCandidates) {
                if (prefixVariants.none { variant -> word.lowercase().startsWith(variant.lowercase()) }) continue

                val displayWord = when {
                    prefix.all { it.isUpperCase() } -> word.uppercase()
                    prefix.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
                    else -> word.lowercase()
                }

                var conf = frequencyScore(freq) * 0.6 + contextScore(word, context) * 0.2
                if (word.equals(prefix, ignoreCase = true)) conf += EXACT_MATCH_BONUS

                suggestions.add(
                    WordSuggestionCandidate(
                        text = displayWord,
                        confidence = conf,
                        sourceProvider = this
                    )
                )
            }
        }

        return suggestions
            .sortedByDescending { it.confidence }
            .distinctBy { it.text.toString().lowercase() }
            .take(maxCount)
    }

    /**
     * Converts raw frequency into a normalized score.
     */
    private fun frequencyScore(freq: Int): Double = when {
        freq >= 5000 -> 1.0
        freq >= 1000 -> 0.9
        freq >= 500  -> 0.75
        freq >= 100  -> 0.5
        freq >= 10   -> 0.25
        else         -> 0.1
    }

    /**
     * Calculates context-based bonus for word prediction.
     */
    private suspend fun contextScore(word: String, context: List<String>): Double {
        var score = 0.0
        contextMap.withLock { map ->
            context.forEachIndexed { i, w ->
                val count = map[w]?.get(word) ?: return@forEachIndexed
                val weight = 1.0 - i.toDouble() / context.size
                score += count * weight
            }
        }
        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Extracts up to N previous words for context-aware suggestions.
     */
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

    /**
     * Builds reverse Metaphone index for fallback suggestions.
     */
    private suspend fun buildPhoneticIndex() {
        phoneticIndex.withLock { it.clear() }
        val allWords = mainDict.withLock { it.keys } + personalDict.withLock { it.keys }
        for (word in allWords) {
            val code = metaphone.encode(word)
            phoneticIndex.withLock { it.getOrPut(code) { mutableSetOf() }.add(word) }
        }
    }

    /**
     * Learns from accepted suggestions by boosting frequency and context.
     */
    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        val word = candidate.text.toString().lowercase().trim()
        if (word.length < 2) return

        personalDict.withLock {
            val current = it[word] ?: 0
            it[word] = min(current + 5, MAX_PERSONAL_FREQ)
        }

        val context = latestEditorContent?.let { extractContextWords(it, subtype) } ?: return
        for (prev in context) {
            contextMap.withLock {
                val map = it.getOrPut(prev) { mutableMapOf() }
                map[word] = (map[word] ?: 0) + 1
            }
        }
    }

    /**
     * Penalizes reverted suggestions by decaying learned frequency.
     */
    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        val word = candidate.text.toString().lowercase().trim()
        personalDict.withLock {
            val current = it[word] ?: return
            it[word] = max((current * DECAY_FACTOR).toInt(), 1)
        }
    }

    /**
     * Removes a word from the personal dictionary and phonetic index.
     */
    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        val word = candidate.text.toString().lowercase().trim()
        return personalDict.withLock {
            val removed = it.remove(word) != null
            if (removed) {
                val code = metaphone.encode(word)
                phoneticIndex.withLock { idx -> idx[code]?.remove(word) }
            }
            removed
        }
    }

    /**
     * Returns all known words from main and personal dictionaries.
     */
    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return mainDict.withLock { md ->
            personalDict.withLock { pd ->
                (md.keys + pd.keys).distinct()
            }
        }
    }

    /**
     * Returns normalized frequency score for a given word.
     */
    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        val lower = word.lowercase()
        return mainDict.withLock { md ->
            personalDict.withLock { pd ->
                val freq = max(md[lower] ?: 0, (pd[lower] ?: 0) * 2)
                frequencyScore(freq)
            }
        }
    }

    private fun loadPersonalDictionary() {
        // TODO: Implement persistent loading
    }

    private fun loadContextAssociations() {
        // TODO: Implement persistent loading
    }

    private fun savePersonalDictionary() {
        // TODO: Implement persistent saving
    }

    /**
     * Saves context associations to local storage in JSON format.
     */
    private suspend fun saveContextAssociations() {
        withContext(Dispatchers.IO) {
            contextMap.withLock {
                val json = Json.encodeToString(contextSerializer, it)
                contextFile.writeText(json)
            }
        }
    }

    /**
     * Clears all state and saves data on shutdown.
     */
    override suspend fun destroy() {
        savePersonalDictionary()
        saveContextAssociations()
        mainDict.withLock { it.clear() }
        personalDict.withLock { it.clear() }
        contextMap.withLock { it.clear() }
        phoneticIndex.withLock { it.clear() }
    }
}
