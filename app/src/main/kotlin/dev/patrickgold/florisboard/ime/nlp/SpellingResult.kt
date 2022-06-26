/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.nlp

import android.view.textservice.SuggestionsInfo
import dev.patrickgold.florisboard.lib.android.AndroidVersion

@JvmInline
value class SpellingResult(val suggestionsInfo: SuggestionsInfo) {
    val isValidWord: Boolean
        get() = suggestionsInfo.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0

    val isTypo: Boolean
        get() = suggestionsInfo.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0

    val isGrammarError: Boolean
        get() = AndroidVersion.ATLEAST_API31_S &&
            suggestionsInfo.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR != 0

    fun suggestions() = Array(suggestionsInfo.suggestionsCount) { n -> suggestionsInfo.getSuggestionAt(n) }

    companion object {
        private val EMPTY_STRING_ARRAY: Array<out String> = arrayOf()

        fun unspecified() = SpellingResult(SuggestionsInfo(0, EMPTY_STRING_ARRAY))

        fun validWord(): SpellingResult {
            return SpellingResult(SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, EMPTY_STRING_ARRAY))
        }

        fun typo(suggestions: Array<out String>, isHighConfidenceResult: Boolean = false): SpellingResult {
            val attributes = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
                (if (isHighConfidenceResult) SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS else 0)
            return SpellingResult(SuggestionsInfo(attributes, suggestions))
        }

        fun grammarError(suggestions: Array<out String>, isHighConfidenceResult: Boolean = false): SpellingResult {
            val attributes = if (AndroidVersion.ATLEAST_API31_S) {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
            } else {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
            } or (if (isHighConfidenceResult) SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS else 0)
            return SpellingResult(SuggestionsInfo(attributes, suggestions))
        }
    }
}
