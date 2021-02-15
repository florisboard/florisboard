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

class FlorisNgram(
    override val tokens: List<Token<String>>,
    override val freq: Int
) : Ngram<String, Int>

open class FlorisToken(
    private val word: String,
    private val isWildcard: Boolean = false
) : Token<String> {
    override fun getData(): String = word

    override fun isWildcardToken(): Boolean = isWildcard
}

class FlorisWeightedToken(
    word: String,
    private val freq: Int
) : FlorisToken(word, false), WeightedToken<String, Int> {
    override fun getFreq(): Int = freq
}
