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

package dev.patrickgold.florisboard.ime.dictionary

import dev.patrickgold.florisboard.ime.nlp.SuggestionList
import dev.patrickgold.florisboard.ime.nlp.Word
import dev.patrickgold.florisboard.lib.io.Asset

/**
 * Standardized dictionary interface for interacting with dictionaries.
 */
interface Dictionary : Asset {
    /**
     * Gets token predictions based on the given [precedingTokens] and the [currentToken]. The
     * length of the returned list is limited to [maxSuggestionCount]. Note that the returned list
     * may at any time give back less items than [maxSuggestionCount] indicates.
     */
    fun getTokenPredictions(
        precedingTokens: List<Word>,
        currentToken: Word?,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        destSuggestionList: SuggestionList
    )

    fun getDate(): Long

    fun getVersion(): Int
}

interface MutableDictionary : Dictionary {
    fun trainTokenPredictions(
        precedingTokens: List<Word>,
        lastToken: Word
    )

    fun setDate(date: Int)

    fun setVersion(version: Int)
}
