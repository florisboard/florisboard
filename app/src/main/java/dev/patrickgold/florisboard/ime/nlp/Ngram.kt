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

package dev.patrickgold.florisboard.ime.nlp

/**
 * Abstract interface representing a n-gram of tokens. Each n-gram instance can be assigned a
 * unique frequency [freq].
 */
interface Ngram<T : Any, F : Number> {
    companion object {
        /** Constant order value for unigrams. */
        const val ORDER_UNIGRAM: Int = 1

        /** Constant order value for bigrams. */
        const val ORDER_BIGRAM: Int = 2

        /** Constant order value for trigrams. */
        const val ORDER_TRIGRAM: Int = 3
    }

    /**
     * A list of tokens for this n-gram. The length of this list is guaranteed to be matching
     * [order].
     */
    val tokens: List<Token<T>>

    /**
     * The frequency value of this n-gram.
     */
    val freq: F

    /**
     * The order of this n-gram (1, 2, 3, ...).
     */
    val order: Int
        get() = tokens.size
}

/**
 * Abstract interface representing a token used in [Ngram]. Can also be set to a wildcard token for
 * fuzzy searching.
 */
interface Token<T : Any> {
    /**
     * Retrieves the data of this token and returns it. The data is only valid if [isWildcardToken]
     * returns true, else the data has to be considered invalid.
     */
    fun getData(): T

    /**
     * Returns true if this token represents a wildcard, false otherwise.
     */
    fun isWildcardToken(): Boolean
}

/**
 * Same as [Token] but allows to add a frequency value and retrieve it via [getFreq].
 */
interface WeightedToken<T : Any, F : Number> : Token<T> {
    /**
     * Returns the frequency of this weighed token.
     */
    fun getFreq(): F
}

/**
 * Same as [Token] but can be modified by calling [setData].
 */
interface MutableToken<T : Any> : Token<T> {
    /**
     * Allows to set the data of this token.
     */
    fun setData(data: T)
}

/**
 * Converts a list of tokens carrying [CharSequence] data to a list of [CharSequence].
 */
fun List<Token<CharSequence>>.toCharSequenceList(): List<CharSequence> {
    return this.map { it.getData() }
}

/**
 * Converts a list of tokens carrying [String] data to a list of [String].
 */
fun List<Token<String>>.toStringList(): List<String> {
    return this.map { it.getData() }
}
