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
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorGroup
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
import kotlin.math.ln
import kotlin.math.max
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
        private const val BASE_PREFIX_MATCH_BONUS = 0.2
        private const val EXACT_MATCH_BONUS = 0.5
        private const val CONTEXT_MATCH_BONUS = 0.15
        private const val PERSONAL_DICTIONARY_BONUS = 0.25
        
        // Context analysis parameters
        private const val MAX_PRECEDING_WORDS = 3
        private const val MIN_CONTEXT_WORD_LENGTH = 2
        
        // Frequency scaling parameters
        private const val FREQUENCY_SCALE_FACTOR = 255.0
        private const val FREQUENCY_LOG_BASE = 2.0
        private const val MIN_LOG_FREQUENCY = 1.0
        
        // Personal dictionary parameters
        private const val LEARNING_FREQUENCY_INCREMENT = 5
        private const val MAX_PERSONAL_FREQUENCY = 255
        private const val LEARNING_DECAY_FACTOR = 0.95
    }

    private val appContext by context.appContext()
    private val dictionaryManager = DictionaryManager.default()
    private val breakIterators = BreakIteratorGroup()

    private val wordData = guardedByLock { mutableMapOf<String, Int>() }
    private val wordDataSerializer = MapSerializer(String.serializer(), Int.serializer())
    
    // Personal dictionary for on-the-fly learning
    private val personalDictionary = guardedByLock { mutableMapOf<String, Int>() }
    
    // Context-based word associations (word -> list of following words with frequencies)
    private val contextAssociations = guardedByLock { mutableMapOf<String, MutableMap<String, Int>>() }

    override val providerId = ProviderId

    override suspend fun create() {
        // Initialize personal dictionary from user dictionary
        loadPersonalDictionary()
    }

    override suspend fun preload(subtype: Subtype) = withContext(Dispatchers.IO) {
        wordData.withLock { wordData ->
            if (wordData.isEmpty()) {
                // Load main dictionary
                val rawData = appContext.assets.readText("ime/dict/data.json")
                val jsonData = Json.decodeFromString(wordDataSerializer, rawData)
                wordData.putAll(jsonData)
                flogDebug { "Loaded ${wordData.size} words from dictionary" }
            }
        }
        
        // Load personal dictionary and context associations
        loadPersonalDictionary()
        loadContextAssociations()
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
        
        // Check main dictionary and personal dictionary
        val isValidWord = isWordValid(normalizedWord, word)
        
        if (isValidWord) {
            return SpellingResult.validWord()
        }
        
        // Generate context-aware spelling suggestions
        val suggestions = generateSpellingSuggestions(normalizedWord, precedingWords, maxSuggestionCount)
        
        return if (suggestions.isNotEmpty()) {
            SpellingResult.typo(suggestions.toTypedArray(), isHighConfidenceResult = suggestions.size >= 2)
        } else {
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
        
        // Extract preceding words for context-aware predictions
        val precedingWords = extractPrecedingWords(content, subtype)
        
        return generateContextAwareSuggestions(composingText, precedingWords, maxCandidateCount, !isPrivateSession, subtype)
    }

    /**
     * Extract preceding words from editor content for context analysis
     */
    private suspend fun extractPrecedingWords(content: EditorContent, subtype: Subtype): List<String> {
        val textBefore = content.textBeforeSelection
        if (textBefore.isEmpty()) return emptyList()
        
        return breakIterators.word(subtype.primaryLocale) { iterator ->
            iterator.setText(textBefore)
            val words = mutableListOf<String>()
            var end = iterator.last()
            
            // Extract words working backwards from the cursor
            for (i in 0 until MAX_PRECEDING_WORDS) {
                if (end == BreakIterator.DONE) break
                
                val start = iterator.previous()
                if (start == BreakIterator.DONE) break
                
                if (iterator.ruleStatus != BreakIterator.WORD_NONE) {
                    val word = textBefore.substring(start, end).trim()
                    if (word.length >= MIN_CONTEXT_WORD_LENGTH && word.all { it.isLetter() }) {
                        words.add(word.lowercase())
                    }
                }
                end = start
            }
            
            words.reversed() // Return in chronological order
        }
    }

    /**
     * Check if a word is valid in any dictionary
     */
    private suspend fun isWordValid(normalizedWord: String, originalWord: String): Boolean {
        return wordData.withLock { it.containsKey(normalizedWord) || it.containsKey(originalWord) } ||
               personalDictionary.withLock { it.containsKey(normalizedWord) } ||
               wordData.withLock { it.containsKey(normalizedWord.replaceFirstChar { it.uppercase() }) }
    }

    /**
     * Generate context-aware spelling suggestions
     */
    private suspend fun generateSpellingSuggestions(
        word: String, 
        precedingWords: List<String>, 
        maxCount: Int
    ): List<String> {
        if (word.length < 2) return emptyList()
        
        return wordData.withLock { dictionary ->
            val suggestions = mutableListOf<Pair<String, Double>>()
            
            // Get base dictionary suggestions
            for ((dictWord, frequency) in dictionary) {
                if (dictWord.length <= word.length + MAX_EDIT_DISTANCE && 
                    dictWord.length >= word.length - MAX_EDIT_DISTANCE) {
                    
                    val distance = calculateEditDistance(word, dictWord)
                    if (distance <= MAX_EDIT_DISTANCE) {
                        val baseScore = calculateSpellingScore(word, dictWord, frequency, distance)
                        val contextScore = calculateContextScore(dictWord, precedingWords)
                        val finalScore = baseScore + contextScore
                        suggestions.add(dictWord to finalScore)
                    }
                }
            }
            
            // Add personal dictionary suggestions
            personalDictionary.withLock { personalDict ->
                for ((dictWord, frequency) in personalDict) {
                    val distance = calculateEditDistance(word, dictWord)
                    if (distance <= MAX_EDIT_DISTANCE) {
                        val baseScore = calculateSpellingScore(word, dictWord, frequency, distance)
                        val contextScore = calculateContextScore(dictWord, precedingWords)
                        val personalBonus = PERSONAL_DICTIONARY_BONUS
                        val finalScore = baseScore + contextScore + personalBonus
                        suggestions.add(dictWord to finalScore)
                    }
                }
            }
            
            suggestions.sortedByDescending { it.second }
                .distinctBy { it.first }
                .take(maxCount)
                .map { it.first }
        }
    }

    /**
     * Generate context-aware word suggestions with improved frequency scaling and prefix matching
     */
    private suspend fun generateContextAwareSuggestions(
        prefix: String, 
        precedingWords: List<String>, 
        maxCount: Int,
        allowLearning: Boolean,
        subtype: Subtype
    ): List<SuggestionCandidate> {
        val lowerPrefix = prefix.lowercase()
        
        return withContext(Dispatchers.Default) {
            val suggestions = mutableListOf<SuggestionScore>()
            var hasExactMatch = false
            
            // Process main dictionary
            wordData.withLock { dictionary ->
                for ((word, frequency) in dictionary) {
                    val lowerWord = word.lowercase()
                    val suggestion = processWordForSuggestion(
                        word, lowerWord, lowerPrefix, frequency, precedingWords, false
                    )
                    suggestion?.let { 
                        suggestions.add(it)
                        if (it.isExactMatch) hasExactMatch = true
                    }
                }
            }
            
            // Process personal dictionary with higher confidence
            personalDictionary.withLock { personalDict ->
                for ((word, frequency) in personalDict) {
                    val lowerWord = word.lowercase()
                    val suggestion = processWordForSuggestion(
                        word, lowerWord, lowerPrefix, frequency, precedingWords, true
                    )
                    suggestion?.let { 
                        suggestions.add(it)
                        if (it.isExactMatch) hasExactMatch = true
                    }
                }
            }
            
            // Add user dictionary suggestions
            try {
                val userDictSuggestions = dictionaryManager.queryUserDictionary(lowerPrefix, subtype.primaryLocale)
                for (candidate in userDictSuggestions) {
                    if (candidate is WordSuggestionCandidate) {
                        val contextScore = calculateContextScore(candidate.text.toString(), precedingWords)
                        val adjustedConfidence = candidate.confidence + contextScore + PERSONAL_DICTIONARY_BONUS
                        suggestions.add(SuggestionScore(
                            word = candidate.text.toString(),
                            rawFrequency = (adjustedConfidence * FREQUENCY_SCALE_FACTOR).toInt(),
                            confidence = adjustedConfidence,
                            isExactMatch = candidate.text.toString().lowercase() == lowerPrefix,
                            isPrefixMatch = candidate.text.toString().lowercase().startsWith(lowerPrefix),
                            isPersonal = true
                        ))
                    }
                }
            } catch (e: Exception) {
                // If DictionaryManager is not available, continue without user dictionary suggestions
                flogDebug { "User dictionary not available: ${e.message}" }
            }
            
            // Sort and convert to suggestion candidates
            suggestions.sortedWith(
                compareByDescending<SuggestionScore> { it.confidence }
                    .thenByDescending { it.rawFrequency }
                    .thenBy { it.word.length }
            )
            .distinctBy { it.word.lowercase() }
            .take(maxCount)
            .mapIndexed { index, score ->
                val shouldAutoCommit = shouldAutoCommitSuggestion(score, lowerPrefix, index, hasExactMatch)
                val displayText = if (shouldAutoCommit && score.isExactMatch) {
                    applyCasePattern(prefix, score.word)
                } else {
                    score.word
                }
                
                WordSuggestionCandidate(
                    text = displayText,
                    confidence = score.confidence.coerceIn(0.0, 1.0),
                    isEligibleForAutoCommit = shouldAutoCommit,
                    sourceProvider = this@LatinLanguageProvider
                )
            }
        }
    }

    /**
     * Process a single word for suggestion generation
     */
    private suspend fun processWordForSuggestion(
        word: String,
        lowerWord: String,
        lowerPrefix: String,
        rawFrequency: Int,
        precedingWords: List<String>,
        isPersonal: Boolean
    ): SuggestionScore? {
        val baseFrequencyScore = calculateImprovedFrequencyScore(rawFrequency)
        val contextScore = calculateContextScore(word, precedingWords)
        val personalBonus = if (isPersonal) PERSONAL_DICTIONARY_BONUS else 0.0
        
        return when {
            // Exact match (different case)
            lowerWord == lowerPrefix && word != lowerPrefix -> {
                val confidence = baseFrequencyScore + EXACT_MATCH_BONUS + contextScore + personalBonus
                SuggestionScore(word, rawFrequency, confidence, true, true, isPersonal)
            }
            // Perfect case match
            word == lowerPrefix -> {
                val confidence = 1.0 + contextScore + personalBonus
                SuggestionScore(word, rawFrequency, confidence, true, true, isPersonal)
            }
            // Prefix match with improved weighting
            lowerWord.startsWith(lowerPrefix) && lowerWord != lowerPrefix -> {
                val prefixBonus = calculateImprovedPrefixBonus(lowerPrefix, lowerWord)
                val confidence = baseFrequencyScore + prefixBonus + contextScore + personalBonus
                SuggestionScore(word, rawFrequency, confidence, false, true, isPersonal)
            }
            // Fuzzy match for longer prefixes
            lowerPrefix.length >= 3 -> {
                val distance = calculateEditDistance(lowerPrefix, lowerWord)
                if (distance <= 1 && rawFrequency >= MIN_FREQUENCY_THRESHOLD) {
                    val confidence = baseFrequencyScore - (distance * 0.2) + contextScore + personalBonus
                    if (confidence > 0.1) {
                        SuggestionScore(word, rawFrequency, confidence, false, false, isPersonal)
                    } else null
                } else null
            }
            else -> null
        }
    }

    /**
     * Improved frequency scaling that handles varying frequency ranges
     */
    private fun calculateImprovedFrequencyScore(frequency: Int): Double {
        return when {
            frequency <= 0 -> 0.0
            frequency >= FREQUENCY_SCALE_FACTOR -> 1.0
            else -> {
                // Use logarithmic scaling for better distribution
                val logFreq = ln(frequency.toDouble() + 1) / ln(FREQUENCY_LOG_BASE)
                val maxLogFreq = ln(FREQUENCY_SCALE_FACTOR + 1) / ln(FREQUENCY_LOG_BASE)
                (logFreq / maxLogFreq).coerceIn(0.0, 1.0)
            }
        }
    }

    /**
     * Calculate improved prefix match bonus based on match length
     */
    private fun calculateImprovedPrefixBonus(prefix: String, word: String): Double {
        val matchLength = prefix.length
        val wordLength = word.length
        
        // Longer prefix matches get higher bonus
        val lengthRatio = matchLength.toDouble() / wordLength
        val lengthBonus = lengthRatio * BASE_PREFIX_MATCH_BONUS
        
        // Additional bonus for longer absolute matches
        val absoluteLengthBonus = when {
            matchLength >= 6 -> 0.15
            matchLength >= 4 -> 0.10
            matchLength >= 3 -> 0.05
            else -> 0.0
        }
        
        return BASE_PREFIX_MATCH_BONUS + lengthBonus + absoluteLengthBonus
    }

    /**
     * Calculate context score based on preceding words
     */
    private suspend fun calculateContextScore(candidate: String, precedingWords: List<String>): Double {
        if (precedingWords.isEmpty()) return 0.0
        
        return contextAssociations.withLock { associations ->
            var maxScore = 0.0
            
            for (precedingWord in precedingWords) {
                val followingWords = associations[precedingWord]
                if (followingWords != null) {
                    val candidateLower = candidate.lowercase()
                    val frequency = followingWords[candidateLower] ?: 0
                    if (frequency > 0) {
                        val totalCount = followingWords.values.sum()
                        val contextProbability = frequency.toDouble() / totalCount
                        maxScore = max(maxScore, contextProbability * CONTEXT_MATCH_BONUS)
                    }
                }
            }
            
            maxScore
        }
    }

    /**
     * Determine if suggestion should be auto-committed
     */
    private fun shouldAutoCommitSuggestion(
        score: SuggestionScore,
        lowerPrefix: String,
        index: Int,
        hasExactMatch: Boolean
    ): Boolean {
        return when {
            // Only auto-commit if we have an exact match and it's the first suggestion
            hasExactMatch && index == 0 && score.isExactMatch -> true
            // For very short prefixes, be more liberal
            lowerPrefix.length <= 2 && score.isExactMatch && index == 0 -> true
            // For longer prefixes, require high confidence
            lowerPrefix.length >= 3 && score.isExactMatch && score.confidence > 0.9 && index == 0 -> true
            // Personal dictionary words get preference
            score.isPersonal && score.isExactMatch && index == 0 -> true
            else -> false
        }
    }

    /**
     * Load personal dictionary from user preferences or storage
     */
    private suspend fun loadPersonalDictionary() {
        // This would typically load from a file or database
        // For now, we'll start with an empty personal dictionary
        personalDictionary.withLock { it.clear() }
    }

    /**
     * Load context associations from storage
     */
    private suspend fun loadContextAssociations() {
        // This would typically load from a file or database
        // For now, we'll start with empty associations
        contextAssociations.withLock { it.clear() }
    }

    /**
     * Save personal dictionary to storage
     */
    private suspend fun savePersonalDictionary() {
        // Implementation would save to persistent storage
        // This could use SharedPreferences, Room database, or file storage
    }

    /**
     * Save context associations to storage
     */
    private suspend fun saveContextAssociations() {
        // Implementation would save to persistent storage
    }

    /**
     * Data class to hold suggestion scoring information
     */
    private data class SuggestionScore(
        val word: String,
        val rawFrequency: Int,
        val confidence: Double,
        val isExactMatch: Boolean,
        val isPrefixMatch: Boolean,
        val isPersonal: Boolean
    )

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
        val frequencyScore = calculateImprovedFrequencyScore(frequency)
        val distanceScore = (MAX_EDIT_DISTANCE - editDistance).toDouble() / MAX_EDIT_DISTANCE
        val lengthScore = min(input.length, candidate.length).toDouble() / maxOf(input.length, candidate.length)
        
        // Bonus for common prefixes
        val prefixBonus = if (input.isNotEmpty() && candidate.lowercase().startsWith(input.lowercase())) {
            calculateImprovedPrefixBonus(input, candidate)
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
        flogDebug { "Suggestion accepted: ${candidate.text}" }
        
        // Learn from accepted suggestions
        val word = candidate.text.toString().lowercase().trim()
        if (word.length >= MIN_CONTEXT_WORD_LENGTH) {
            personalDictionary.withLock { personalDict ->
                val currentFreq = personalDict[word] ?: 0
                val newFreq = min(currentFreq + LEARNING_FREQUENCY_INCREMENT, MAX_PERSONAL_FREQUENCY)
                personalDict[word] = newFreq
            }
            
            // TODO: Update context associations based on preceding words
            // This would require passing context information through the notification
            
            // Persist the learning
            savePersonalDictionary()
        }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { "Suggestion reverted: ${candidate.text}" }
        
        // Reduce confidence for reverted suggestions
        val word = candidate.text.toString().lowercase().trim()
        personalDictionary.withLock { personalDict ->
            val currentFreq = personalDict[word] ?: 0
            if (currentFreq > 0) {
                val newFreq = max((currentFreq * LEARNING_DECAY_FACTOR).toInt(), 1)
                personalDict[word] = newFreq
            }
        }
        
        savePersonalDictionary()
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        flogDebug { "Remove suggestion requested: ${candidate.text}" }
        
        // Remove from personal dictionary
        val word = candidate.text.toString().lowercase().trim()
        val removed = personalDictionary.withLock { personalDict ->
            personalDict.remove(word) != null
        }
        
        if (removed) {
            savePersonalDictionary()
        }
        
        return removed
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return wordData.withLock { mainDict ->
            personalDictionary.withLock { personalDict ->
                (mainDict.keys + personalDict.keys).distinct()
            }
        }
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return wordData.withLock { mainDict ->
            personalDictionary.withLock { personalDict ->
                val mainFreq = mainDict[word.lowercase()] ?: mainDict[word] ?: 0
                val personalFreq = personalDict[word.lowercase()] ?: 0
                
                // Combine frequencies, giving preference to personal dictionary
                val combinedFreq = max(mainFreq, personalFreq * 2) // Personal words get 2x weight
                calculateImprovedFrequencyScore(combinedFreq)
            }
        }
    }

    override suspend fun destroy() {
        // Save any pending data before destruction
        savePersonalDictionary()
        saveContextAssociations()
        
        // Clear memory
        wordData.withLock { it.clear() }
        personalDictionary.withLock { it.clear() }
        contextAssociations.withLock { it.clear() }
    }
}
