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
        list: StagedSuggestionList<String, Int>,
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
            if (list.canAdd(freq shr costSum)) {
                list.add(word.toString(), freq shr costSum)
            }
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

    fun listAllSameOrderWords(list: StagedSuggestionList<String, Int>, word: StringBuilder, allowPossiblyOffensive: Boolean) {
        word.append(char)
        if (isWord && ((isPossiblyOffensive && allowPossiblyOffensive) || !isPossiblyOffensive)) {
            if (list.canAdd(freq)) {
                list.add(word.toString(), freq)
            }
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
    ): List<WeightedToken<String, Int>> {
        val ngramList = mutableListOf<WeightedToken<String, Int>>()
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
                        val wordNodes = StagedSuggestionList<String, Int>(maxTokenCount)
                        val strBuilder = StringBuilder().append(word.substring(0, word.length - 1))
                        splitNode.listAllSameOrderWords(wordNodes, strBuilder, allowPossiblyOffensive)
                        ngramList.addAll(wordNodes)
                    }
                    if (ngramList.size < maxTokenCount) {
                        val wordNodes = StagedSuggestionList<String, Int>(maxTokenCount)
                        val strBuilder = StringBuilder()
                        currentNode.listSimilarWords(word, wordNodes, strBuilder, allowPossiblyOffensive, maxEditDistance)
                        ngramList.addAll(wordNodes)
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
