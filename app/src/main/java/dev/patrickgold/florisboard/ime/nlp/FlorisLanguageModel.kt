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

open class NgramTree(
    sameOrderChildren: MutableMap<Char, NgramNode> = mutableMapOf(),
    higherOrderChildren: MutableMap<Char, NgramNode> = mutableMapOf()
) : NgramNode(-1, '?', -1, sameOrderChildren, higherOrderChildren)

open class NgramNode(
    val order: Int,
    val char: Char,
    val freq: Int,
    val sameOrderChildren: MutableMap<Char, NgramNode> = mutableMapOf(),
    val higherOrderChildren: MutableMap<Char, NgramNode> = mutableMapOf()
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
                currentNode.higherOrderChildren[char]
            } else {
                currentNode.sameOrderChildren[char]
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

    fun listAllSameOrderWords(list: MutableList<WeightedToken<String, Int>>, prefix: String) {
        if (isWord) {
            list.add(WeightedToken(prefix, freq))
        }
        for ((char, childNode) in sameOrderChildren) {
            childNode.listAllSameOrderWords(list, prefix + char)
        }
    }
}

open class FlorisLanguageModel(
    initTreeObj: NgramTree? = null
) : LanguageModel<String, Int> {
    protected val ngramTree: NgramTree = initTreeObj ?: NgramTree()

    override fun getNgram(vararg tokens: String): Ngram<String, Int> {
        val ngram = getNgramOrNull(*tokens)
        if (ngram != null) {
            return ngram
        } else {
            throw NullPointerException("No n-gram found matching the given tokens: $tokens")
        }
    }

    override fun getNgram(ngram: Ngram<String, Int>): Ngram<String, Int> {
        val ngram = getNgramOrNull(ngram)
        if (ngram != null) {
            return ngram
        } else {
            throw NullPointerException("No n-gram found matching the given ngram: $ngram")
        }
    }

    override fun getNgramOrNull(vararg tokens: String): Ngram<String, Int>? {
        TODO("Not yet implemented")
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

    override fun matchAllNgrams(ngram: Ngram<String, Int>): List<WeightedToken<String, Int>> {
        val ngramList = mutableListOf<WeightedToken<String, Int>>()
        var currentNode: NgramNode = ngramTree
        for ((t, token) in ngram.tokens.withIndex()) {
            val word = token.data
            if (t + 1 >= ngram.tokens.size) {
                // The last word is not complete, so find all possible words and sort
                val splitWord = mutableListOf<Char>()
                var splitNode: NgramNode? = currentNode
                for ((pos, char) in word.withIndex()) {
                    val node = if (pos == 0) {
                        splitNode?.higherOrderChildren?.get(char)
                    } else {
                        splitNode?.sameOrderChildren?.get(char)
                    }
                    splitWord.add(char)
                    splitNode = node
                    if (node == null) {
                        break
                    }
                }
                if (splitNode != null) {
                    // Input thus far is valid
                    val words = mutableListOf<WeightedToken<String, Int>>()
                    splitNode.listAllSameOrderWords(words, splitWord.joinToString(""))
                    words.sortByDescending { it.freq }
                    ngramList.addAll(words)
                }
            } else {
                val node = currentNode.findWord(word)
                if (node == null) {
                    return listOf()
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
