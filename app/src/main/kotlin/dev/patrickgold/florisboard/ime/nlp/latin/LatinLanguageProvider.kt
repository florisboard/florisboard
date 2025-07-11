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
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.florisboard.lib.android.readText
import org.florisboard.lib.kotlin.guardedByLock
import kotlin.math.min

class LatinLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        // Default user ID used for all subtypes, unless otherwise specified.
        // See `ime/core/Subtype.kt` Line 210 and 211 for the default usage
        const val ProviderId = "org.florisboard.nlp.providers.latin"
        
        // Parameters for suggestion algorithms
        private const val MIN_WORD_LENGTH_FOR_SUGGESTIONS = 2
        private const val MAX_EDIT_DISTANCE = 2
        private const val MIN_FREQUENCY_THRESHOLD = 10
        private const val PREFIX_MATCH_BONUS = 0.3
        private const val EXACT_MATCH_BONUS = 0.5
    }

    private val appContext by context.appContext()

    private val wordData = guardedByLock { mutableMapOf<String, Int>() }
    private val wordDataSerializer = MapSerializer(String.serializer(), Int.serializer())

    override val providerId = ProviderId

    override suspend fun create() {
        // Here we initialize our provider, set up all things which are not language dependent.
    }

    override suspend fun preload(subtype: Subtype) = withContext(Dispatchers.IO) {
        // Here we have the chance to preload dictionaries and prepare a neural network for a specific language.
        // Is kept in sync with the active keyboard subtype of the user, however a new preload does not necessary mean
        // the previous language is not needed anymore (e.g. if the user constantly switches between two subtypes)

        // To read a file from the APK assets the following methods can be used:
        // appContext.assets.open()
        // appContext.assets.reader()
        // appContext.assets.bufferedReader()
        // appContext.assets.readText()
        // To copy an APK file/dir to the file system cache (appContext.cacheDir), the following methods are available:
        // appContext.assets.copy()
        // appContext.assets.copyRecursively()

        // The subtype we get here contains a lot of data, however we are only interested in subtype.primaryLocale and
        // subtype.secondaryLocales.

        wordData.withLock { wordData ->
            if (wordData.isEmpty()) {
                // Here we use readText() because the test dictionary is a json dictionary
                val rawData = appContext.assets.readText("ime/dict/data.json")
                val jsonData = Json.decodeFromString(wordDataSerializer, rawData)
                wordData.putAll(jsonData)
                flogDebug { "Loaded ${wordData.size} words from dictionary" }
            }
        }
    }

    override suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): SpellingResult {
        if (word.isBlank()) {
            return SpellingResult.unspecified()
        }

        val normalizedWord = word.lowercase().trim()
        
        // Check if word exists in dictionary
        val isValidWord = wordData.withLock { dictionary ->
            dictionary.containsKey(normalizedWord) || 
            dictionary.containsKey(word) || // Check original case
            // Check common word variations
            dictionary.containsKey(normalizedWord.replaceFirstChar { it.uppercase() })
        }
        
        if (isValidWord) {
            return SpellingResult.validWord()
        }
        
        // Generate spelling suggestions for misspelled words
        val suggestions = generateSpellingSuggestions(normalizedWord, maxSuggestionCount)
        
        return if (suggestions.isNotEmpty()) {
            SpellingResult.typo(suggestions.toTypedArray(), isHighConfidenceResult = suggestions.size >= 2)
        } else {
            // Word not found and no suggestions available
            SpellingResult.typo(arrayOf())
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        val composingText = content.composingText.trim()
        
        if (composingText.length < MIN_WORD_LENGTH_FOR_SUGGESTIONS) {
            return emptyList()
        }
        
        return generateWordSuggestions(composingText, maxCandidateCount)
    }

    /**
     * Generate spelling suggestions for a misspelled word
     */
    private suspend fun generateSpellingSuggestions(word: String, maxCount: Int): List<String> {
        if (word.length < 2) return emptyList()
        
        return wordData.withLock { dictionary ->
            val suggestions = mutableListOf<Pair<String, Double>>()
            
            for ((dictWord, frequency) in dictionary) {
                if (dictWord.length <= word.length + MAX_EDIT_DISTANCE && 
                    dictWord.length >= word.length - MAX_EDIT_DISTANCE) {
                    
                    val distance = calculateEditDistance(word, dictWord)
                    if (distance <= MAX_EDIT_DISTANCE) {
                        val score = calculateSpellingScore(word, dictWord, frequency, distance)
                        suggestions.add(dictWord to score)
                    }
                }
            }
            
            suggestions.sortedByDescending { it.second }
                .take(maxCount)
                .map { it.first }
        }
    }

    /**
     * Generate word completion/prediction suggestions
     */
    private suspend fun generateWordSuggestions(prefix: String, maxCount: Int): List<SuggestionCandidate> {
        val lowerPrefix = prefix.lowercase()
        
        return wordData.withLock { dictionary ->
            val suggestions = mutableListOf<Triple<String, Int, Double>>()
            var hasExactMatch = false
            
            for ((word, frequency) in dictionary) {
                val lowerWord = word.lowercase()
                
                when {
                    // Exact match (different case) - highest priority
                    lowerWord == lowerPrefix && word != prefix -> {
                        val confidence = (frequency / 255.0) + EXACT_MATCH_BONUS
                        suggestions.add(Triple(word, frequency, confidence))
                        hasExactMatch = true
                    }
                    // Perfect case match - also exact
                    word == prefix -> {
                        val confidence = 1.0 // Maximum confidence
                        suggestions.add(Triple(word, frequency, confidence))
                        hasExactMatch = true
                    }
                    // Prefix match
                    lowerWord.startsWith(lowerPrefix) && lowerWord != lowerPrefix -> {
                        val confidence = (frequency / 255.0) + PREFIX_MATCH_BONUS
                        suggestions.add(Triple(word, frequency, confidence))
                    }
                    // Fuzzy match for short words
                    lowerPrefix.length >= 3 -> {
                        val distance = calculateEditDistance(lowerPrefix, lowerWord)
                        if (distance <= 1 && frequency >= MIN_FREQUENCY_THRESHOLD) {
                            val confidence = (frequency / 255.0) - (distance * 0.2)
                            if (confidence > 0.1) {
                                suggestions.add(Triple(word, frequency, confidence))
                            }
                        }
                    }
                }
            }
            
            suggestions.sortedWith(compareByDescending<Triple<String, Int, Double>> { it.third }
                .thenByDescending { it.second })
                .take(maxCount)
                .mapIndexed { index, (word, _, confidence) ->
                    val shouldAutoCommit = when {
                        // Only auto-commit if we have an exact match and it's the first suggestion
                        hasExactMatch && index == 0 && word.lowercase() == lowerPrefix -> true
                        // Or if it's a very short prefix (1-2 chars) and exact match
                        lowerPrefix.length <= 2 && word.lowercase() == lowerPrefix && index == 0 -> true
                        // For longer prefixes, only auto-commit exact matches with very high confidence
                        lowerPrefix.length >= 3 && word.lowercase() == lowerPrefix && confidence > 0.9 && index == 0 -> true
                        // Never auto-commit prefix matches (completions)
                        else -> false
                    }
                    
                    // Apply the user's capitalization pattern to the suggestion
                    val displayText = if (shouldAutoCommit && word.lowercase() == lowerPrefix) {
                        applyCasePattern(prefix, word)
                    } else {
                        word
                    }
                    
                    WordSuggestionCandidate(
                        text = displayText,
                        confidence = confidence.coerceIn(0.0, 1.0),
                        isEligibleForAutoCommit = shouldAutoCommit,
                        sourceProvider = this@LatinLanguageProvider
                    )
                }
        }
    }

    /**
     * Calculate edit distance (Levenshtein distance) between two strings
     */
    private fun calculateEditDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        if (len1 == 0) return len2
        if (len2 == 0) return len1
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }

    /**
     * Calculate spelling suggestion score based on edit distance and frequency
     */
    private fun calculateSpellingScore(input: String, candidate: String, frequency: Int, editDistance: Int): Double {
        val frequencyScore = frequency / 255.0
        val distanceScore = (MAX_EDIT_DISTANCE - editDistance).toDouble() / MAX_EDIT_DISTANCE
        val lengthScore = min(input.length, candidate.length).toDouble() / maxOf(input.length, candidate.length)
        
        // Bonus for common prefixes
        val prefixBonus = if (input.isNotEmpty() && candidate.lowercase().startsWith(input.lowercase())) {
            0.2
        } else {
            0.0
        }
        
        return (frequencyScore * 0.4 + distanceScore * 0.4 + lengthScore * 0.2 + prefixBonus)
    }

    /**
     * Apply the capitalization pattern from the input to the suggestion
     */
    private fun applyCasePattern(input: String, suggestion: String): String {
        if (input.isEmpty() || suggestion.isEmpty()) return suggestion
        
        return when {
            // All uppercase
            input.all { it.isUpperCase() } -> suggestion.uppercase()
            // First letter uppercase (title case)
            input.first().isUpperCase() && input.drop(1).all { it.isLowerCase() } -> {
                suggestion.lowercase().replaceFirstChar { it.uppercase() }
            }
            // Mixed case - preserve original suggestion
            input.any { it.isUpperCase() } && input.any { it.isLowerCase() } -> suggestion
            // All lowercase
            else -> suggestion.lowercase()
        }
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        // We can use flogDebug, flogInfo, flogWarning and flogError for debug logging, which is a wrapper for Logcat
        flogDebug { "Suggestion accepted: ${candidate.text}" }
        // TODO: In a real implementation, we could learn from accepted suggestions to improve future predictions
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { "Suggestion reverted: ${candidate.text}" }
        // TODO: Could be used to adjust confidence scores for similar suggestions
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        flogDebug { "Remove suggestion requested: ${candidate.text}" }
        // TODO: In a real implementation, we could maintain a blacklist of user-rejected words
        return false
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return wordData.withLock { it.keys.toList() }
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return wordData.withLock { 
            val frequency = it[word.lowercase()] ?: it[word] ?: 0
            frequency / 255.0
        }
    }

    override suspend fun destroy() {
        // Here we have the chance to de-allocate memory and finish our work. However this might never be called if
        // the app process is killed (which will most likely always be the case).
        wordData.withLock { it.clear() }
    }
}
