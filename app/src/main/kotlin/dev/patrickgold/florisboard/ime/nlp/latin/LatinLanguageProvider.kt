/*
 * Copyright (C) 2023 Patrick Goldinger
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
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.keyboard.KeyProximityChecker
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.io.subFile
import dev.patrickgold.florisboard.lib.io.writeJson
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import dev.patrickgold.florisboard.native.NativeStr
import dev.patrickgold.florisboard.native.toNativeStr
import dev.patrickgold.florisboard.subtypeManager

private val DEFAULT_PREDICTION_WEIGHTS = LatinPredictionWeights(
    lookup = LatinPredictionLookupWeights(
        maxCostSum = 1.5,
        costIsEqual = 0.0,
        costIsEqualIgnoringCase = 0.25,
        costInsert = 0.5,
        costInsertStartOfStr = 1.0,
        costDelete = 0.5,
        costDeleteStartOfStr = 1.0,
        costSubstitute = 0.5,
        costSubstituteInProximity = 0.25,
        costSubstituteStartOfStr = 1.0,
        costTranspose = 0.0,
    ),
    training = LatinPredictionTrainingWeights(
        usageBonus = 128,
        usageReductionOthers = 1,
    ),
)

private val DEFAULT_KEY_PROXIMITY_CHECKER = KeyProximityChecker(
    enabled = false,
    mapping = mapOf(),
)

private data class LatinNlpSessionWrapper(
    var subtype: Subtype,
    var session: LatinNlpSession,
)

class LatinLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        const val ProviderId = "org.florisboard.nlp.providers.latin"
        const val NlpSessionConfigFileName = "nlp_session_config.json"
        const val UserDictionaryFileName = "user_dict.fldic"

        external fun nativeInitEmptyDictionary(dictPath: NativeStr)
    }

    private val subtypeManager by context.subtypeManager()
    private val cachedSessionWrappers = guardedByLock {
        mutableListOf<LatinNlpSessionWrapper>()
    }

    override suspend fun create() {
        // Do nothing
    }

    override suspend fun preload(subtype: Subtype) {
        if (subtype.isFallback()) return
        cachedSessionWrappers.withLock { sessionWrappers ->
            var sessionWrapper = sessionWrappers.find { it.subtype.id == subtype.id }
            if (sessionWrapper == null || sessionWrapper.subtype != subtype) {
                if (sessionWrapper == null) {
                    sessionWrapper = LatinNlpSessionWrapper(
                        subtype = subtype,
                        session = LatinNlpSession(),
                    )
                    sessionWrappers.add(sessionWrapper)
                } else {
                    sessionWrapper.subtype = subtype
                }
                val cacheDir = subtypeManager.cacheDirFor(subtype)
                val filesDir = subtypeManager.filesDirFor(subtype)
                val configFile = cacheDir.subFile(NlpSessionConfigFileName)
                val userDictFile = filesDir.subFile(UserDictionaryFileName)
                if (!userDictFile.exists()) {
                    nativeInitEmptyDictionary(userDictFile.absolutePath.toNativeStr())
                }
                val config = LatinNlpSessionConfig(
                    primaryLocale = subtype.primaryLocale,
                    secondaryLocales = subtype.secondaryLocales,
                    // TODO: dynamically find base dictionaries
                    baseDictionaryPaths = listOf(),
                    userDictionaryPath = userDictFile.absolutePath,
                    predictionWeights = DEFAULT_PREDICTION_WEIGHTS,
                    keyProximityChecker = DEFAULT_KEY_PROXIMITY_CHECKER,
                )
                configFile.writeJson(config)
                sessionWrapper.session.loadFromConfigFile(configFile)
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
        return when (word.lowercase()) {
            // Use typo for typing errors
            "typo" -> SpellingResult.typo(arrayOf("typo1", "typo2", "typo3"))
            // Use grammar error if the algorithm can detect this. On Android 11 and lower grammar errors are visually
            // marked as typos due to a lack of support
            "gerror" -> SpellingResult.grammarError(arrayOf("grammar1", "grammar2", "grammar3"))
            // Use valid word for valid input
            else -> SpellingResult.validWord()
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        val word = content.composingText.ifBlank { "next" }
        val suggestions = buildList {
            for (n in 0 until maxCandidateCount) {
                add(WordSuggestionCandidate(
                    text = "$word$n",
                    secondaryText = if (n % 2 == 1) "secondary" else null,
                    confidence = 0.5,
                    isEligibleForAutoCommit = false,//n == 0 && word.startsWith("auto"),
                    // We set ourselves as the source provider so we can get notify events for our candidate
                    sourceProvider = this@LatinLanguageProvider,
                ))
            }
        }
        return suggestions
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        // We can use flogDebug, flogInfo, flogWarning and flogError for debug logging, which is a wrapper for Logcat
        flogDebug { candidate.toString() }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { candidate.toString() }
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        flogDebug { candidate.toString() }
        return false
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return listOf()
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return 0.0
    }

    override suspend fun destroy() {
        // Here we have the chance to de-allocate memory and finish our work. However this might never be called if
        // the app process is killed (which will most likely always be the case).
    }

    override val providerId = ProviderId
}
