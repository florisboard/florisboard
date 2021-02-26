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
open class Ngram<T : Any, F : Comparable<F>>(_tokens: List<Token<T>>, _freq: F) {
    companion object {
        /** Constant order value for unigrams. */
        const val ORDER_UNIGRAM: Int = 1

        /** Constant order value for bigrams. */
        const val ORDER_BIGRAM: Int = 2

        /** Constant order value for trigrams. */
        const val ORDER_TRIGRAM: Int = 3
    }

    init {
        if (_tokens.size < ORDER_UNIGRAM) {
            throw Exception("A n-gram must contain at least 1 token!")
        }
    }

    /**
     * A list of tokens for this n-gram. The length of this list is guaranteed to be matching
     * [order].
     */
    val tokens: List<Token<T>> = _tokens

    /**
     * The frequency value of this n-gram.
     */
    val freq: F = _freq

    /**
     * The order of this n-gram (1, 2, 3, ...).
     */
    val order: Int
        get() = tokens.size
}

/**
 * Abstract interface representing a token used in [Ngram].
 */
open class Token<T : Any>(_data: T) {
    /**
     * The data of this token.
     */
    val data: T = _data

    override fun toString(): String {
        return "Token(\"$data\")"
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token<*>

        if (data != other.data) return false

        return true
    }
}

/**
 * Same as [Token] but allows to add a frequency value [freq].
 */
open class WeightedToken<T : Any, F : Comparable<F>>(_data: T, _freq: F) : Token<T>(_data) {
    /**
     * The frequency of this weighed token.
     */
    val freq: F = _freq

    override fun toString(): String {
        return "WeightedToken(\"$data\", $freq)"
    }

    override fun hashCode(): Int {
        return data.hashCode() + 31 * freq.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeightedToken<*, *>

        if (data != other.data || freq != other.freq) return false

        return true
    }
}

/**
 * Converts a list of tokens carrying [CharSequence] data to a list of [CharSequence].
 */
fun List<Token<CharSequence>>.toCharSequenceList(): List<CharSequence> {
    return this.map { it.data }
}

/**
 * Converts a list of tokens carrying [String] data to a list of [String].
 */
fun List<Token<String>>.toStringList(): List<String> {
    return this.map { it.data }
}
