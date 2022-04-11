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

/*
 * ====================== IMPORTANT ========================
 *
 * All code in this file is only temporary added back in so the stable track has suggestions again.
 * In 0.3.15 a renewed suggestion algorithm will be built and this mess will be removed!
 *
 * ==========================================================
 */

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
    ): List<Token<T>>
}

/**
 * Mutable version of [LanguageModel].
 */
interface MutableLanguageModel<T : Any, F : Comparable<F>> : LanguageModel<T, F> {
    fun deleteNgram(ngram: Ngram<T, F>)

    fun insertNgram(ngram: Ngram<T, F>)

    fun updateNgram(ngram: Ngram<T, F>)
}

/**
 * Represents the root node to a n-gram tree.
 */
open class NgramTree(
    sameOrderChildren: MutableList<NgramNode> = mutableListOf(),
    higherOrderChildren: MutableList<NgramNode> = mutableListOf()
) : NgramNode(0, '?', -1, sameOrderChildren, higherOrderChildren)

/**
 * A node of a n-gram tree, which holds the character it represents, the corresponding frequency,
 * a pre-computed string representing all parent characters and the current one as well as child
 * nodes, one for the same order n-gram nodes and one for the higher order n-gram nodes.
 */
open class NgramNode(
    val order: Int,
    val char: Char,
    val freq: Int,
    val sameOrderChildren: MutableList<NgramNode> = mutableListOf(),
    val higherOrderChildren: MutableList<NgramNode> = mutableListOf()
) {
    companion object {
        const val FREQ_CHARACTER = -1
        const val FREQ_WORD_MIN = 0
        const val FREQ_WORD_MAX = 255
        const val FREQ_WORD_FILLER = -2
        const val FREQ_IS_POSSIBLY_OFFENSIVE = 0
    }

    val isCharacter: Boolean
        get() = freq == FREQ_CHARACTER

    val isWord: Boolean
        get() = freq in FREQ_WORD_MIN..FREQ_WORD_MAX

    val isWordFiller: Boolean
        get() = freq == FREQ_WORD_FILLER

    val isPossiblyOffensive: Boolean
        get() = freq == FREQ_IS_POSSIBLY_OFFENSIVE

    fun findWord(word: String): NgramNode? {
        var currentNode = this
        for ((pos, char) in word.withIndex()) {
            val childNode = if (pos == 0) {
                currentNode.higherOrderChildren.find { it.char == char }
            } else {
                currentNode.sameOrderChildren.find { it.char == char }
            }
            if (childNode != null) {
                currentNode = childNode
            } else {
                return null
            }
        }
        return if (currentNode.isWord || currentNode.isWordFiller) {
            currentNode
        } else {
            null
        }
    }

    /**
     * This function allows to search for a given [input] word with a given [maxEditDistance] and
     * adds all matches in the trie to the [list].
     */
    fun listSimilarWords(
        input: String,
        list: SuggestionList,
        word: StringBuilder,
        allowPossiblyOffensive: Boolean,
        maxEditDistance: Int,
        deletionCost: Int = 0,
        insertionCost: Int = 0,
        substitutionCost: Int = 0,
        pos: Int = -1
    ) {
        if (pos > -1) {
            word.append(char)
        }
        val costSum = deletionCost + insertionCost + substitutionCost
        if (pos > -1 && (pos + 1 == input.length) && isWord && ((isPossiblyOffensive && allowPossiblyOffensive)
                || !isPossiblyOffensive)) {
            // Using shift right instead of divide by 2^(costSum) as it is mathematically the
            // same but faster.
            list.add(word.toString(), freq shr costSum)
        }
        if (pos <= -1) {
            for (childNode in higherOrderChildren) {
                childNode.listSimilarWords(
                    input, list, word, allowPossiblyOffensive, maxEditDistance, 0, 0, 0, 0
                )
            }
        } else if (maxEditDistance == costSum) {
            if (pos + 1 < input.length) {
                sameOrderChildren.find { it.char == input[pos + 1] }?.listSimilarWords(
                    input, list, word, allowPossiblyOffensive, maxEditDistance,
                    deletionCost, insertionCost, substitutionCost, pos + 1
                )
            }
        } else {
            // Delete
            if (pos + 2 < input.length) {
                sameOrderChildren.find { it.char == input[pos + 2] }?.listSimilarWords(
                    input, list, word, allowPossiblyOffensive, maxEditDistance,
                    deletionCost + 1, insertionCost, substitutionCost, pos + 2
                )
            }
            for (childNode in sameOrderChildren) {
                if (pos + 1 < input.length && childNode.char == input[pos + 1]) {
                    childNode.listSimilarWords(
                        input, list, word, allowPossiblyOffensive, maxEditDistance,
                        deletionCost, insertionCost, substitutionCost, pos + 1
                    )
                } else  {
                    // Insert
                    childNode.listSimilarWords(
                        input, list, word, allowPossiblyOffensive, maxEditDistance,
                        deletionCost, insertionCost + 1, substitutionCost, pos
                    )
                    if (pos + 1 < input.length) {
                        // Substitute
                        childNode.listSimilarWords(
                            input, list, word, allowPossiblyOffensive, maxEditDistance,
                            deletionCost, insertionCost, substitutionCost + 1, pos + 1
                        )
                    }
                }
            }
        }
        if (pos > -1) {
            word.deleteAt(word.lastIndex)
        }
    }

    fun listAllSameOrderWords(list: SuggestionList, word: StringBuilder, allowPossiblyOffensive: Boolean) {
        word.append(char)
        if (isWord && ((isPossiblyOffensive && allowPossiblyOffensive) || !isPossiblyOffensive)) {
            list.add(word.toString(), freq)
        }
        for (childNode in sameOrderChildren) {
            childNode.listAllSameOrderWords(list, word, allowPossiblyOffensive)
        }
        word.deleteAt(word.lastIndex)
    }
}

open class FlorisLanguageModel(
    initTreeObj: NgramTree? = null
) : LanguageModel<String, Int> {
    protected val ngramTree: NgramTree = initTreeObj ?: NgramTree()

    override fun getNgram(vararg tokens: String): Ngram<String, Int> {
        val ngramOut = getNgramOrNull(*tokens)
        if (ngramOut != null) {
            return ngramOut
        } else {
            throw NullPointerException("No n-gram found matching the given tokens: $tokens")
        }
    }

    override fun getNgram(ngram: Ngram<String, Int>): Ngram<String, Int> {
        val ngramOut = getNgramOrNull(ngram)
        if (ngramOut != null) {
            return ngramOut
        } else {
            throw NullPointerException("No n-gram found matching the given ngram: $ngram")
        }
    }

    override fun getNgramOrNull(vararg tokens: String): Ngram<String, Int>? {
        var currentNode: NgramNode = ngramTree
        for (token in tokens) {
            val childNode = currentNode.findWord(token)
            if (childNode != null) {
                currentNode = childNode
            } else {
                return null
            }
        }
        return Ngram(tokens.toList().map { Token(it) }, currentNode.freq)
    }

    override fun getNgramOrNull(ngram: Ngram<String, Int>): Ngram<String, Int>? {
        return getNgramOrNull(*ngram.tokens.toStringList().toTypedArray())
    }

    override fun hasNgram(ngram: Ngram<String, Int>, doMatchFreq: Boolean): Boolean {
        val result = getNgramOrNull(ngram)
        return if (result != null) {
            if (doMatchFreq) {
                ngram.freq == result.freq
            } else {
                true
            }
        } else {
            false
        }
    }

    override fun matchAllNgrams(
        ngram: Ngram<String, Int>,
        maxEditDistance: Int,
        maxTokenCount: Int,
        allowPossiblyOffensive: Boolean
    ): List<Token<String>> {
        val ngramList = mutableListOf<Token<String>>()
        var currentNode: NgramNode = ngramTree
        for ((t, token) in ngram.tokens.withIndex()) {
            val word = token.data
            if (t + 1 >= ngram.tokens.size) {
                if (word.isNotEmpty()) {
                    // The last word is not complete, so find all possible words and sort
                    val splitWord = mutableListOf<Char>()
                    var splitNode: NgramNode? = currentNode
                    for ((pos, char) in word.withIndex()) {
                        val node = if (pos == 0) {
                            splitNode?.higherOrderChildren?.find { it.char == char }
                        } else {
                            splitNode?.sameOrderChildren?.find { it.char == char }
                        }
                        splitWord.add(char)
                        splitNode = node
                        if (node == null) {
                            break
                        }
                    }
                    if (splitNode != null) {
                        // Input thus far is valid
                        val wordNodes = SuggestionList.new(maxTokenCount)
                        val strBuilder = StringBuilder().append(word.substring(0, word.length - 1))
                        splitNode.listAllSameOrderWords(wordNodes, strBuilder, allowPossiblyOffensive)
                        ngramList.addAll(wordNodes.map { Token(it) })
                    }
                    if (ngramList.size < maxTokenCount) {
                        val wordNodes = SuggestionList.new(maxTokenCount)
                        val strBuilder = StringBuilder()
                        currentNode.listSimilarWords(word, wordNodes, strBuilder, allowPossiblyOffensive, maxEditDistance)
                        ngramList.addAll(wordNodes.map { Token(it) })
                    }
                }
            } else {
                val node = currentNode.findWord(word)
                if (node == null) {
                    return ngramList
                } else {
                    currentNode = node
                }
            }
        }
        return ngramList
    }

    fun toFlorisMutableLanguageModel(): FlorisMutableLanguageModel = FlorisMutableLanguageModel(ngramTree)
}

open class FlorisMutableLanguageModel(
    initTreeObj: NgramTree? = null
) : MutableLanguageModel<String, Int>, FlorisLanguageModel(initTreeObj) {
    override fun deleteNgram(ngram: Ngram<String, Int>) {
        TODO("Not yet implemented")
    }

    override fun insertNgram(ngram: Ngram<String, Int>) {
        TODO("Not yet implemented")
    }

    override fun updateNgram(ngram: Ngram<String, Int>) {
        TODO("Not yet implemented")
    }

    fun toFlorisLanguageModel(): FlorisLanguageModel = FlorisLanguageModel(ngramTree)
}
