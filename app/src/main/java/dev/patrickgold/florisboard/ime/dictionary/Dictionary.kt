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

import dev.patrickgold.florisboard.ime.extension.Asset
import dev.patrickgold.florisboard.ime.nlp.LanguageModel
import dev.patrickgold.florisboard.ime.nlp.MutableLanguageModel
import dev.patrickgold.florisboard.ime.nlp.Token
import dev.patrickgold.florisboard.ime.nlp.WeightedToken

/**
 * Standardized dictionary interface for interacting with dictionaries.
 */
interface Dictionary<T : Any, F : Comparable<F>> : Asset {
    val languageModel: LanguageModel<T, F>

    /**
     * Gets token predictions based on the given [precedingTokens] and the [currentToken]. The
     * length of the returned list is limited to [maxSuggestionCount]. Note that the returned list
     * may at any time give back less items than [maxSuggestionCount] indicates.
     */
    fun getTokenPredictions(
        precedingTokens: List<Token<T>>,
        currentToken: Token<T>?,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean
    ): List<WeightedToken<T, F>>

    fun getDate(): Long

    fun getVersion(): Int
}

interface MutableDictionary<T : Any, F : Comparable<F>> : Dictionary<T, F> {
    override val languageModel: MutableLanguageModel<T, F>

    fun trainTokenPredictions(
        precedingTokens: List<Token<T>>,
        lastToken: Token<T>
    )

    fun setDate(date: Int)

    fun setVersion(version: Int)
}
