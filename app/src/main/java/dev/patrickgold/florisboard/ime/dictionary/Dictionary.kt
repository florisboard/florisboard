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
import dev.patrickgold.florisboard.ime.nlp.Token
import dev.patrickgold.florisboard.ime.nlp.WeightedToken

/**
 * Standardized dictionary interface for interacting with dictionaries.
 */
interface Dictionary<T: Any, F: Number> : Asset {
    /**
     * Can contain a language model which is associated with this dictionary. May return null if no
     * language model is attached to this dictionary.
     */
    val languageModel: LanguageModel<T, F>?

    @Throws(NullPointerException::class)
    fun getWeightedToken(token: Token<T>): WeightedToken<T, F>

    fun getWeightedTokenOrNull(token: Token<T>): WeightedToken<T, F>?

    /**
     * Returns a list of words which are relevant to the given [input] with maximum length of
     * [maxResultCount]. Note that the actual size of the list may me smaller or even zero, if the
     * query input does not provide good results.
     */
    fun getSuggestions(input: Token<T>, maxResultCount: Int): List<Token<T>>

    fun hasToken(token: Token<T>): Boolean
}

interface MutableDictionary<T: Any, F: Number> : Dictionary<T, F> {
    fun trainSuggestions(input: Token<T>)

    fun deleteToken(token: Token<T>)

    fun insertToken(token: Token<T>)

    fun updateToken(token: Token<T>)
}
