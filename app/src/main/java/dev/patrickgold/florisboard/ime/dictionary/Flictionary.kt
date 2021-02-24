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
    private val date: Long,
    private val version: Int,
    private val headerStr: String,
    override val languageModel: LanguageModel<String, Int>
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
            val rawData: ByteArray
            if (assetRef.source == AssetSource.Assets) {
                val inputStream = context.assets.open(assetRef.path)
                rawData = inputStream.readBytes()
                inputStream.close()
            } else {
                return Err(Exception("Only AssetSource.Assets is currently supported!"))
            }

            var headerStr: String? = null
            var date: Long = 0
            var version = 0
            val ngramTree = NgramTree()

            var pos = 0
            val ngramOrderStack = mutableListOf(-1)
            val ngramTreeStack = mutableListOf<NgramNode>(ngramTree)

            while (pos < rawData.size) {
                val cmd = rawData[pos].toInt() and 0xFF
                when {
                    (cmd and MASK_BEGIN_PTREE_NODE) == CMDB_BEGIN_PTREE_NODE -> {
                        if (pos == 0) {
                            return Err(ParseException(
                                errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_PTREE_NODE,
                                address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1
                            ))
                        }
                        val order = ((cmd and ATTR_PTREE_NODE_ORDER) shr 4) + 1
                        val type = ((cmd and ATTR_PTREE_NODE_TYPE) shr 2)
                        val size = (cmd and ATTR_PTREE_NODE_SIZE) + 1
                        if (pos + 1 + size < rawData.size) {
                            val freq: Int
                            val offset: Int
                            when (type) {
                                ATTR_PTREE_NODE_TYPE_CHAR -> {
                                    freq = NgramNode.FREQ_CHARACTER
                                    offset = 1
                                }
                                ATTR_PTREE_NODE_TYPE_WORD_FILLER -> {
                                    freq = NgramNode.FREQ_WORD_FILLER
                                    offset = 1
                                }
                                ATTR_PTREE_NODE_TYPE_WORD -> {
                                    freq = rawData[pos + 1].toInt() and 0xFF
                                    offset = 2
                                }
                                else -> return Err(Exception("TODO: shortcut not supported"))
                            }
                            val bytes = ByteArray(size) { i -> rawData[pos + offset + i] }
                            val char = String(bytes, Charsets.UTF_8)[0]
                            val node = NgramNode(order, char, freq)
                            if (ngramOrderStack.last() == order) {
                                ngramTreeStack.last().sameOrderChildren[char] = node
                            } else {
                                ngramTreeStack.last().higherOrderChildren[char] = node
                            }
                            ngramOrderStack.add(order)
                            ngramTreeStack.add(node)
                            pos += (offset + size)
                        } else {
                            return Err(ParseException(
                                errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1
                            ))
                        }
                    }

                    (cmd and MASK_BEGIN_HEADER) == CMDB_BEGIN_HEADER -> {
                        if (pos != 0) {
                            return Err(ParseException(
                                errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_HEADER,
                                address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1
                            ))
                        }
                        version = cmd and ATTR_HEADER_VERSION
                        if (version != VERSION_0) {
                            return Err(ParseException(
                                errorType = ParseException.ErrorType.UNSUPPORTED_FLICTIONARY_VERSION,
                                address = pos,
                                cmdByte = cmd.toByte(),
                                absoluteDepth = ngramTreeStack.size - 1
                            ))
                        }
                        if (pos + 9 < rawData.size) {
                            val size = (rawData[pos + 1].toInt() and 0xFF)
                            date =
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
                                ngramOrderStack.add(-1)
                                ngramTreeStack.add(NgramTree())
                                pos += (10 + size)
                            } else {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                    address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1
                                ))
                            }
                        } else {
                            return Err(ParseException(
                                errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1
                            ))
                        }
                    }

                    (cmd and MASK_END) == CMDB_END -> {
                        if (pos == 0) {
                            return Err(ParseException(
                                errorType = ParseException.ErrorType.UNEXPECTED_CMD_END,
                                address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1
                            ))
                        }
                        val n = (cmd and ATTR_END_COUNT)
                        if (n > 0) {
                            if (n <= (ngramTreeStack.size - 1)) {
                                for (c in 0 until n) {
                                    ngramOrderStack.removeLast()
                                    ngramTreeStack.removeLast()
                                }
                            } else {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_ABSOLUTE_DEPTH_DECREASE_BELOW_ZERO,
                                    address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1 - n
                                ))
                            }
                        } else {
                            return Err(ParseException(
                                errorType = ParseException.ErrorType.UNEXPECTED_CMD_END_ZERO_VALUE,
                                address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1
                            ))
                        }
                        pos += 1
                    }
                    else -> return Err(ParseException(
                        errorType = ParseException.ErrorType.INVALID_CMD_BYTE_PROVIDED,
                        address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - 1
                    ))
                }
            }

            if ((ngramTreeStack.size - 1) != 0) {
                return Err(ParseException(
                    errorType = ParseException.ErrorType.UNEXPECTED_ABSOLUTE_DEPTH_NOT_ZERO_AT_EOF,
                    address = pos, cmdByte = 0x00.toByte(), absoluteDepth = ngramTreeStack.size - 1
                ))
            }

            return Ok(
                Flictionary(
                    name = "flict",
                    label = "flict",
                    authors = listOf(),
                    headerStr = headerStr ?: "",
                    date = date,
                    version = version,
                    languageModel = FlorisLanguageModel(ngramTree)
                )
            )
        }
    }

    override fun getDate(): Long = date

    override fun getVersion(): Int = version

    override fun getTokenPredictions(
        precedingTokens: List<Token<String>>,
        currentToken: Token<String>?,
        maxSuggestionCount: Int
    ): List<WeightedToken<String, Int>> {
        currentToken ?: return listOf()

        return if (currentToken.data.isNotEmpty()) {
            val retList = languageModel.matchAllNgrams(Ngram(listOf(Token(currentToken.data.toLowerCase())), -1))
            retList
        } else {
            listOf()
        }
    }

    /**
     * A parse exception to be used by [Flictionary] to indicate where the parsing of a binary file
     * failed, while also providing some additional information.
     */
    class ParseException(
        private val errorType: ErrorType,
        private val address: Int,
        private val cmdByte: Byte,
        private val absoluteDepth: Int
    ) : Exception() {
        enum class ErrorType {
            UNSUPPORTED_FLICTIONARY_VERSION,
            UNEXPECTED_CMD_BEGIN_HEADER,
            UNEXPECTED_CMD_BEGIN_PTREE_NODE,
            UNEXPECTED_CMD_DEFINE_SHORTCUT,
            UNEXPECTED_CMD_END,
            UNEXPECTED_CMD_END_ZERO_VALUE,
            UNEXPECTED_ABSOLUTE_DEPTH_DECREASE_BELOW_ZERO,
            UNEXPECTED_ABSOLUTE_DEPTH_NOT_ZERO_AT_EOF,
            UNEXPECTED_EOF,
            INVALID_CMD_BYTE_PROVIDED,
        }

        override val message: String
            get() = toString()
        override fun toString(): String {
            return StringBuilder().run {
                append(when (errorType) {
                    ErrorType.UNSUPPORTED_FLICTIONARY_VERSION -> {
                        "Unexpected Flictionary version: ${(cmdByte.toInt() and 0xFF) and ATTR_HEADER_VERSION}"
                    }
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
                    ErrorType.UNEXPECTED_ABSOLUTE_DEPTH_DECREASE_BELOW_ZERO -> {
                        "Unexpected decrease in absolute depth: cannot go below zero"
                    }
                    ErrorType.UNEXPECTED_ABSOLUTE_DEPTH_NOT_ZERO_AT_EOF -> {
                        "Unexpected non-zero value in absolute depth at end of file"
                    }
                    ErrorType.UNEXPECTED_EOF -> {
                        "Unexpected end of file while try to do look-ahead"
                    }
                    ErrorType.INVALID_CMD_BYTE_PROVIDED -> {
                        "Invalid command byte provided"
                    }
                })
                append(String.format("\n at address 0x%08X where cmd_byte=0x%02X and section_depth=%d", address, cmdByte, absoluteDepth))
                toString()
            }
        }
    }
}
