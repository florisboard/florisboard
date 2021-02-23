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

import android.content.Context
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.ime.nlp.*
import java.lang.StringBuilder
import java.util.regex.Pattern

/**
 * Class Flictionary which takes care of loading the binary asset as well as providing words for
 * queries.
 *
 * This class accepts binary dictionary files of the type "flict" as defined in here:
 * https://github.com/florisboard/dictionary-tools/blob/main/flictionary.md
 */
class Flictionary private constructor(
    override val name: String,
    override val label: String,
    override val authors: List<String>,
    private val headerStr: String,
    private val words: NgramNode
) : Dictionary<String, Int> {
    companion object {
        private const val VERSION_0 =                           0x0

        private const val MASK_BEGIN_PTREE_NODE =               0x80
        private const val CMDB_BEGIN_PTREE_NODE =               0x00
        private const val ATTR_PTREE_NODE_ORDER =               0x70
        private const val ATTR_PTREE_NODE_TYPE =                0x0C
        private const val ATTR_PTREE_NODE_TYPE_CHAR =           0
        private const val ATTR_PTREE_NODE_TYPE_WORD_FILLER =    1
        private const val ATTR_PTREE_NODE_TYPE_WORD =           2
        private const val ATTR_PTREE_NODE_TYPE_SHORTCUT =       3
        private const val ATTR_PTREE_NODE_SIZE =                0x03

        private const val MASK_END =                            0xC0
        private const val CMDB_END =                            0x80
        private const val ATTR_END_COUNT =                      0x3F

        private const val MASK_BEGIN_HEADER =                   0xE0
        private const val CMDB_BEGIN_HEADER =                   0xC0
        private const val ATTR_HEADER_VERSION =                 0x1F

        private const val MASK_DEFINE_SHORTCUT =                0xF0
        private const val CMDB_DEFINE_SHORTCUT =                0xE0

        /**
         * Loads a Flictionary binary asset from given [assetRef] and returns a result containing
         * either the parsed dictionary or an exception giving information about the error which
         * occurred.
         */
        fun load(context: Context, assetRef: AssetRef): Result<Flictionary, Exception> {
            var headerStr: String? = null
            val words = NgramNode(0, "", -1, mutableListOf())
            if (assetRef.source == AssetSource.Assets) {
                val inputStream = context.assets.open(assetRef.path)
                val rawData = inputStream.readBytes()
                inputStream.close()
                var pos = 0
                var sectionDepth = 0
                val sectionWord = mutableListOf<MutableList<String>>(
                    mutableListOf(),
                    mutableListOf(),
                    mutableListOf(),
                    mutableListOf(),
                    mutableListOf(),
                    mutableListOf(),
                    mutableListOf(),
                    mutableListOf()
                )
                val currentNode = mutableMapOf(Pair(0, words))
                while (pos < rawData.size) {
                    val cmd = rawData[pos].toInt() and 0xFF
                    when {
                        (cmd and MASK_BEGIN_PTREE_NODE) == CMDB_BEGIN_PTREE_NODE -> {
                            if (pos == 0) {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_PTREE_NODE,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                            val order = ((cmd and ATTR_PTREE_NODE_ORDER) shr 4) + 1
                            val type = ((cmd and ATTR_PTREE_NODE_TYPE) shr 2)
                            val size = (cmd and ATTR_PTREE_NODE_SIZE) + 1
                            if (pos + 1 + size < rawData.size) {
                                when (type) {
                                    ATTR_PTREE_NODE_TYPE_CHAR -> {
                                        val word = ByteArray(size) { i -> rawData[pos + 1 + i] }
                                        sectionWord[order-1].add(String(word, Charsets.UTF_8))
                                        pos += (1 + size)
                                    }
                                    ATTR_PTREE_NODE_TYPE_WORD_FILLER -> {
                                        val word = ByteArray(size) { i -> rawData[pos + 1 + i] }
                                        sectionWord[order-1].add(String(word, Charsets.UTF_8))
                                        val node = NgramNode(order, sectionWord[order-1].joinToString(""), -1)
                                        currentNode[order-1]!!.children.add(node)
                                        currentNode[order] = node
                                        pos += (2 + size)
                                    }
                                    ATTR_PTREE_NODE_TYPE_WORD -> {
                                        val freq = rawData[pos + 1].toInt() and 0xFF
                                        val word = ByteArray(size) { i -> rawData[pos + 2 + i] }
                                        sectionWord[order-1].add(String(word, Charsets.UTF_8))
                                        val node = NgramNode(order, sectionWord[order-1].joinToString(""), freq)
                                        currentNode[order-1]!!.children.add(node)
                                        currentNode[order] = node
                                        pos += (2 + size)
                                    }
                                }
                                sectionDepth++
                            } else {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                        }
                        (cmd and MASK_BEGIN_HEADER) == CMDB_BEGIN_HEADER -> {
                            if (pos != 0) {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_HEADER,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                            val version = cmd and ATTR_HEADER_VERSION
                            if (pos + 9 < rawData.size) {
                                val size = (rawData[pos + 1].toInt() and 0xFF)
                                val date: Long =
                                    ((rawData[pos + 2].toInt() and 0xFF).toLong() shl 56) +
                                    ((rawData[pos + 3].toInt() and 0xFF).toLong() shl 48) +
                                    ((rawData[pos + 4].toInt() and 0xFF).toLong() shl 40) +
                                    ((rawData[pos + 5].toInt() and 0xFF).toLong() shl 32) +
                                    ((rawData[pos + 6].toInt() and 0xFF).toLong() shl 24) +
                                    ((rawData[pos + 7].toInt() and 0xFF).toLong() shl 16) +
                                    ((rawData[pos + 8].toInt() and 0xFF).toLong() shl 8) +
                                    ((rawData[pos + 9].toInt() and 0xFF).toLong() shl 0)
                                if (pos + 9 + size < rawData.size) {
                                    val headerBytes = ByteArray(size) { i -> rawData[pos + 10 + i] }
                                    headerStr = String(headerBytes, Charsets.UTF_8)
                                    sectionWord[0].add(headerStr)
                                    sectionDepth++
                                    pos += (10 + size)
                                } else {
                                    return Err(ParseException(
                                        errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                        address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                    ))
                                }
                            } else {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                        }
                        (cmd and MASK_END) == CMDB_END -> {
                            if (pos == 0) {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_END,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                            val n = (cmd and ATTR_END_COUNT)
                            if (n > 0) {
                                if (n <= sectionDepth) {
                                    var nDesired = n
                                    sectionLoop@ for (i in (0..7).reversed()) {
                                        if (sectionWord[i].size >= nDesired) {
                                            for (j in 0 until nDesired) {
                                                sectionWord[i].removeLast()
                                            }
                                            break@sectionLoop
                                        } else {
                                            nDesired -= sectionWord[i].size
                                            sectionWord[i].clear()
                                        }
                                    }
                                    sectionDepth -= n
                                } else {
                                    return Err(ParseException(
                                        errorType = ParseException.ErrorType.UNEXPECTED_SECTION_DEPTH_DECREASE_BELOW_ZERO,
                                        address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth - n
                                    ))
                                }
                            } else {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_END_ZERO_VALUE,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                            pos += 1
                        }
                        else -> return Err(ParseException(
                            errorType = ParseException.ErrorType.INVALID_CMD_BYTE_PROVIDED,
                            address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                        ))
                    }
                }
                if (sectionDepth != 0) {
                    return Err(ParseException(
                        errorType = ParseException.ErrorType.UNEXPECTED_SECTION_DEPTH_NOT_ZERO_AT_EOF,
                        address = pos, cmdByte = 0x00.toByte(), sectionDepth = sectionDepth
                    ))
                }
                //words.sortByDescending { weightedToken -> weightedToken.freq }
                return Ok(Flictionary("flict", "flict", listOf(), headerStr ?: "", words))
            }
            return Err(Exception("Only AssetSource.Assets is currently supported!"))
        }
    }

    override fun getDate(): Long {
        TODO("Not yet implemented")
    }

    override fun getVersion(): Int {
        TODO("Not yet implemented")
    }

    override fun getTokenPredictions(
        precedingTokens: List<Token<String>>,
        currentToken: Token<String>?,
        maxSuggestionCount: Int
    ): List<WeightedToken<String, Int>> {
        currentToken ?: return listOf()
        val idata = currentToken.data
        val pattern = Pattern.compile("$idata\\p{L}*")
        return if (idata.isNotEmpty()) {
            val retList = mutableListOf<WeightedToken<String, Int>>()
            for (entry in words.children) {
                if (pattern.matcher(entry.word).matches()){// || NlpUtils.editDistance(entry.getData(), idata) <= 1) {
                    if (entry.freq > 0) {
                        retList.add(WeightedToken(entry.word, entry.freq))
                        if (retList.size >= maxSuggestionCount) {
                            break
                        }
                    }
                }
            }
            retList.sortByDescending { it.freq }
            retList
        } else {
            listOf()
        }
    }

    override fun getNgram(ngram: Ngram<String, Int>): Ngram<String, Int> {
        TODO("Not yet implemented")
    }

    override fun getNgram(vararg tokens: String): Ngram<String, Int> {
        TODO("Not yet implemented")
    }

    override fun getNgramOrNull(ngram: Ngram<String, Int>): Ngram<String, Int>? {
        TODO("Not yet implemented")
    }

    override fun getNgramOrNull(vararg tokens: String): Ngram<String, Int>? {
        TODO("Not yet implemented")
    }

    override fun hasNgram(ngram: Ngram<String, Int>, doMatchFreq: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun matchAllNgrams(ngram: Ngram<String, Int>): List<Ngram<String, Int>> {
        TODO("Not yet implemented")
    }

    private class NgramNode(val order: Int, val word: String, val freq: Int, val children: MutableList<NgramNode> = mutableListOf())

    /**
     * A parse exception to be used by [Flictionary] to indicate where the parsing of a binary file
     * failed, while also providing some additional information.
     */
    class ParseException(
        private val errorType: ErrorType,
        private val address: Int,
        private val cmdByte: Byte,
        private val sectionDepth: Int
    ) : Exception() {
        enum class ErrorType {
            UNEXPECTED_CMD_BEGIN_HEADER,
            UNEXPECTED_CMD_BEGIN_PTREE_NODE,
            UNEXPECTED_CMD_DEFINE_SHORTCUT,
            UNEXPECTED_CMD_END,
            UNEXPECTED_CMD_END_ZERO_VALUE,
            UNEXPECTED_SECTION_DEPTH_DECREASE_BELOW_ZERO,
            UNEXPECTED_SECTION_DEPTH_NOT_ZERO_AT_EOF,
            UNEXPECTED_EOF,
            INVALID_CMD_BYTE_PROVIDED,
        }

        override val message: String
            get() = toString()
        override fun toString(): String {
            return StringBuilder().run {
                append(when (errorType) {
                    ErrorType.UNEXPECTED_CMD_BEGIN_HEADER -> {
                        "Unexpected command: BEGIN_HEADER"
                    }
                    ErrorType.UNEXPECTED_CMD_BEGIN_PTREE_NODE -> {
                        "Unexpected command: BEGIN_PTREE_NODE"
                    }
                    ErrorType.UNEXPECTED_CMD_DEFINE_SHORTCUT -> {
                        "Unexpected command: DEFINE_SHORTCUT"
                    }
                    ErrorType.UNEXPECTED_CMD_END -> {
                        "Unexpected command: END"
                    }
                    ErrorType.UNEXPECTED_CMD_END_ZERO_VALUE -> {
                        "Unexpected zero value provided for command END"
                    }
                    ErrorType.UNEXPECTED_SECTION_DEPTH_DECREASE_BELOW_ZERO -> {
                        "Unexpected decrease in section depth: cannot go below zero"
                    }
                    ErrorType.UNEXPECTED_SECTION_DEPTH_NOT_ZERO_AT_EOF -> {
                        "Unexpected non-zero value in section depth at end of file"
                    }
                    ErrorType.UNEXPECTED_EOF -> {
                        "Unexpected end of file while try to do look-ahead"
                    }
                    ErrorType.INVALID_CMD_BYTE_PROVIDED -> {
                        "Invalid command byte provided"
                    }
                })
                append(String.format("\n at address 0x%08X where cmd_byte=0x%02X and section_depth=%d", address, cmdByte, sectionDepth))
                toString()
            }
        }
    }
}
