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
import java.util.Locale
import kotlin.math.abs

class LatinLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        // Default user ID used for all subtypes, unless otherwise specified.
        // See `ime/core/Subtype.kt` Line 210 and 211 for the default usage
        const val ProviderId = "org.florisboard.nlp.providers.latin"
    }

    private val appContext by context.appContext()

    private val wordData = guardedByLock { mutableMapOf<String, Int>() }
    private val learnedWords = guardedByLock { mutableMapOf<String, Int>() }
    private val learnedBigrams = guardedByLock { mutableMapOf<String, Int>() }
    private val wordDataSerializer = MapSerializer(String.serializer(), Int.serializer())
    private val preferences = context.getSharedPreferences("latin_language_provider", Context.MODE_PRIVATE)
    private var previousWord: String? = null

    override val providerId = ProviderId

    override suspend fun create() {
        loadLearnedData()
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
        val normalized = word.lowercase(subtype.primaryLocale.base)
        val candidates = findCandidates(normalized, maxSuggestionCount)
        return when {
            isKnownWord(normalized) -> SpellingResult.validWord()
            candidates.isEmpty() -> SpellingResult.typo(emptyArray())
            else -> SpellingResult.typo(candidates.toTypedArray(), candidates.first().length > normalized.length / 2)
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        val currentWord = content.composingText.lowercase(subtype.primaryLocale.base)
        val candidates = if (currentWord.isBlank()) {
            nextWords(content.textBeforeSelection, maxCandidateCount)
        } else {
            findCandidates(currentWord, maxCandidateCount)
        }

        previousWord = content.textBeforeSelection
            .trim()
            .split(Regex("\\s+"))
            .lastOrNull { it.any(Char::isLetter) }
            ?.lowercase(subtype.primaryLocale.base)

        return candidates.mapIndexed { index, candidate ->
            val isCorrection = currentWord.isNotBlank() && candidate != currentWord && editDistance(currentWord, candidate) <= 1
            WordSuggestionCandidate(
                text = preserveCase(content.composingText, candidate),
                confidence = if (isCorrection) 0.95 else (0.9 - index * 0.05).coerceAtLeast(0.5),
                isEligibleForAutoCommit = isCorrection && index == 0 && currentWord.length > 2,
                isEligibleForUserRemoval = learnedWords.withLock { it.containsKey(candidate) },
                sourceProvider = this@LatinLanguageProvider,
            )
        }
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        val word = candidate.text.toString().lowercase(subtype.primaryLocale.base)
        if (word.any(Char::isLetter) && word.length > 1) {
            learn(word, previousWord)
        }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { candidate.toString() }
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        val word = candidate.text.toString().lowercase(subtype.primaryLocale.base)
        val removed = learnedWords.withLock { it.remove(word) != null }
        if (removed) saveLearnedData()
        return removed
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        val dictionaryWords = wordData.withLock { it.keys.toList() }
        val learnedWordList = learnedWords.withLock { it.keys.toList() }
        return dictionaryWords + learnedWordList
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        val normalized = word.lowercase(subtype.primaryLocale.base)
        return (learnedWords.withLock { it[normalized] } ?: wordData.withLock { it[normalized] } ?: 0) / 255.0
    }

    override suspend fun destroy() {
        saveLearnedData()
    }

    private suspend fun isKnownWord(word: String): Boolean =
        learnedWords.withLock { it.containsKey(word) } || wordData.withLock { it.containsKey(word) }

    private suspend fun findCandidates(input: String, limit: Int): List<String> {
        val words = wordData.withLock { it.toMap() } + learnedWords.withLock { it.toMap() }
        val prefix = words.filterKeys { it.startsWith(input) }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
        val fuzzy = if (prefix.size >= limit || input.length < 3) emptyList() else words.entries
            .asSequence()
            .filter { abs(it.key.length - input.length) <= 2 }
            .map { it.key to editDistance(input, it.key) }
            .filter { it.second <= 2 }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }.thenByDescending { words[it.first] ?: 0 })
            .map { it.first }
            .toList()
        return (prefix + fuzzy).distinct().take(limit)
    }

    private suspend fun nextWords(textBeforeSelection: String, limit: Int): List<String> {
        val lastWord = textBeforeSelection.trim().split(Regex("\\s+"))
            .lastOrNull { it.any(Char::isLetter) }
            ?.lowercase(Locale.getDefault()) ?: return emptyList()
        return learnedBigrams.withLock { bigrams ->
            bigrams.entries.asSequence()
                .filter { it.key.startsWith("$lastWord\\u0000") }
                .sortedByDescending { it.value }
                .map { it.key.substringAfter("\\u0000") }
                .distinct()
                .take(limit)
                .toList()
        }
    }

    private suspend fun learn(word: String, previous: String?) {
        val updated = learnedWords.withLock {
            val frequency = (it[word] ?: 160) + 1
            it[word] = frequency.coerceAtMost(255)
            frequency
        }
        if (!previous.isNullOrBlank()) {
            learnedBigrams.withLock {
                val key = "${previous.lowercase(Locale.getDefault())}\\u0000$word"
                it[key] = ((it[key] ?: 0) + 1).coerceAtMost(255)
            }
        }
        if (updated > 0) saveLearnedData()
    }

    private suspend fun loadLearnedData() {
        runCatching {
            preferences.getString("words", null)?.let { raw ->
                val values = Json.decodeFromString(wordDataSerializer, raw)
                learnedWords.withLock { it.putAll(values) }
            }
            preferences.getString("bigrams", null)?.let { raw ->
                val values = Json.decodeFromString(wordDataSerializer, raw)
                learnedBigrams.withLock { it.putAll(values) }
            }
        }
    }

    private suspend fun saveLearnedData() {
        runCatching {
            val words = learnedWords.withLock { it.toMap() }
            val bigrams = learnedBigrams.withLock { it.toMap() }
            preferences.edit()
                .putString("words", Json.encodeToString(wordDataSerializer, words))
                .putString("bigrams", Json.encodeToString(wordDataSerializer, bigrams))
                .apply()
        }
    }

    private fun preserveCase(original: String, candidate: String): String = when {
        original.all(Char::isUpperCase) -> candidate.uppercase()
        original.firstOrNull()?.isUpperCase() == true -> candidate.replaceFirstChar { it.uppercase() }
        else -> candidate
    }

    private fun editDistance(first: String, second: String): Int {
        var previous = IntArray(second.length + 1) { it }
        for (i in first.indices) {
            val current = IntArray(second.length + 1)
            current[0] = i + 1
            for (j in second.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (first[i] == second[j]) 0 else 1,
                )
            }
            previous = current
        }
        return previous.last()
    }
}
