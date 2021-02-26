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
 * Abstract interface for a language model. Can house any n-grams with a minimum order of one.
 */
interface LanguageModel<T : Any, F : Comparable<F>> {
    /**
     * Tries to get the n-gram for the passed [tokens]. Throws a NPE if no match could be found.
     */
    @Throws(NullPointerException::class)
    fun getNgram(vararg tokens: T): Ngram<T, F>

    /**
     * Tries to get the n-gram for the passed [ngram], whereas the frequency is ignored while
     * searching. Throws a NPE if no match could be found.
     */
    @Throws(NullPointerException::class)
    fun getNgram(ngram: Ngram<T, F>): Ngram<T, F>

    /**
     * Tries to get the n-gram for the passed [tokens]. Returns null if no match could be found.
     */
    fun getNgramOrNull(vararg tokens: T): Ngram<T, F>?

    /**
     * Tries to get the n-gram for the passed [ngram], whereas the frequency is ignored while
     * searching. Returns null if no match could be found.
     */
    fun getNgramOrNull(ngram: Ngram<T, F>): Ngram<T, F>?

    /**
     * Checks if a given [ngram] exists within this model. If [doMatchFreq] is set to true, the
     * frequency is also matched.
     */
    fun hasNgram(ngram: Ngram<T, F>, doMatchFreq: Boolean = false): Boolean

    /**
     * Matches all n-grams which match the given [ngram], whereas the last item in the n-gram is
     * is used to search for predictions.
     */
    fun matchAllNgrams(
        ngram: Ngram<T, F>,
        maxEditDistance: Int,
        maxTokenCount: Int,
        allowPossiblyOffensive: Boolean
    ): List<WeightedToken<T, F>>
}

/**
 * Mutable version of [LanguageModel].
 */
interface MutableLanguageModel<T : Any, F : Comparable<F>> : LanguageModel<T, F> {
    fun deleteNgram(ngram: Ngram<T, F>)

    fun insertNgram(ngram: Ngram<T, F>)

    fun updateNgram(ngram: Ngram<T, F>)
}
