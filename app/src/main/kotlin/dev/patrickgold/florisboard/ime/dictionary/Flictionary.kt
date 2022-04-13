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
import dev.patrickgold.florisboard.ime.nlp.SuggestionList
import dev.patrickgold.florisboard.ime.nlp.Word
import dev.patrickgold.florisboard.lib.io.FlorisRef
import java.io.InputStream

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
    private val languageModel: FlorisLanguageModel
) : Dictionary {
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
        fun load(context: Context, assetRef: FlorisRef): Result<Flictionary> {
            val buffer = ByteArray(5000) { 0 }
            val inputStream: InputStream
            if (assetRef.isAssets) {
                inputStream = context.assets.open(assetRef.relativePath)
            } else {
                return Result.failure(Exception("Only AssetSource.Assets is currently supported!"))
            }

            var headerStr: String? = null
            var date: Long = 0
            var version = 0
            val ngramTree = NgramTree()

            var pos = 0
            val ngramOrderStack = mutableListOf<Int>()
            val ngramTreeStack = mutableListOf<NgramNode>()

            while (true) {
                if (inputStream.readNext(buffer, 0, 1) <= 0) {
                    break
                }
                val cmd = buffer[0].toInt() and 0xFF
                when {
                    (cmd and MASK_BEGIN_PTREE_NODE) == CMDB_BEGIN_PTREE_NODE -> {
                        if (pos == 0) {
                            inputStream.close()
                            return Result.failure(
                                ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_PTREE_NODE,
                                    address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                                )
                            )
                        }
                        val order = ((cmd and ATTR_PTREE_NODE_ORDER) shr 4) + 1
                        val type = ((cmd and ATTR_PTREE_NODE_TYPE) shr 2)
                        val size = (cmd and ATTR_PTREE_NODE_SIZE) + 1
                        val freq: Int
                        val freqSize: Int
                        when (type) {
                            ATTR_PTREE_NODE_TYPE_CHAR -> {
                                freq = NgramNode.FREQ_CHARACTER
                                freqSize = 0
                            }
                            ATTR_PTREE_NODE_TYPE_WORD_FILLER -> {
                                freq = NgramNode.FREQ_WORD_FILLER
                                freqSize = 0
                            }
                            ATTR_PTREE_NODE_TYPE_WORD -> {
                                if (inputStream.readNext(buffer, 1, 1) > 0) {
                                    freq = buffer[1].toInt() and 0xFF
                                } else {
                                    inputStream.close()
                                    return Result.failure(
                                        ParseException(
                                            errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                            address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                                        )
                                    )
                                }
                                freqSize = 1
                            }
                            else -> return Result.failure(Exception("TODO: shortcut not supported"))
                        }
                        if (inputStream.readNext(buffer, freqSize + 1, size) > 0) {
                            val char = String(buffer, freqSize + 1, size, Charsets.UTF_8)[0]
                            val node = NgramNode(order, char, freq)
                            val lastOrder = ngramOrderStack.lastOrNull()
                            if (lastOrder == null) {
                                ngramTree.higherOrderChildren.add(node)
                            } else {
                                if (lastOrder == order) {
                                    ngramTreeStack.last().sameOrderChildren.add(node)
                                } else {
                                    ngramTreeStack.last().higherOrderChildren.add(node)
                                }
                            }
                            ngramOrderStack.add(order)
                            ngramTreeStack.add(node)
                            pos += (freqSize + 1 + size)
                        } else {
                            inputStream.close()
                            return Result.failure(
                                ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                    address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                                )
                            )
                        }
                    }

                    (cmd and MASK_BEGIN_HEADER) == CMDB_BEGIN_HEADER -> {
                        if (pos != 0) {
                            inputStream.close()
                            return Result.failure(
                                ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_HEADER,
                                    address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                                )
                            )
                        }
                        version = cmd and ATTR_HEADER_VERSION
                        if (version != VERSION_0) {
                            inputStream.close()
                            return Result.failure(
                                ParseException(
                                    errorType = ParseException.ErrorType.UNSUPPORTED_FLICTIONARY_VERSION,
                                    address = pos,
                                    cmdByte = cmd.toByte(),
                                    absoluteDepth = ngramTreeStack.size
                                )
                            )
                        }
                        if (inputStream.readNext(buffer, 1, 9) > 0) {
                            val size = (buffer[1].toInt() and 0xFF)
                            date =
                                ((buffer[2].toInt() and 0xFF).toLong() shl 56) +
                                ((buffer[3].toInt() and 0xFF).toLong() shl 48) +
                                ((buffer[4].toInt() and 0xFF).toLong() shl 40) +
                                ((buffer[5].toInt() and 0xFF).toLong() shl 32) +
                                ((buffer[6].toInt() and 0xFF).toLong() shl 24) +
                                ((buffer[7].toInt() and 0xFF).toLong() shl 16) +
                                ((buffer[8].toInt() and 0xFF).toLong() shl 8) +
                                ((buffer[9].toInt() and 0xFF).toLong() shl 0)
                            if (inputStream.readNext(buffer, 10, size) > 0) {
                                headerStr = String(buffer, 10, size, Charsets.UTF_8)
                                ngramOrderStack.add(-1)
                                ngramTreeStack.add(NgramTree())
                                pos += (10 + size)
                            } else {
                                inputStream.close()
                                return Result.failure(
                                    ParseException(
                                        errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                        address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                                    )
                                )
                            }
                        } else {
                            inputStream.close()
                            return Result.failure(
                                ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                    address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                                )
                            )
                        }
                    }

                    (cmd and MASK_END) == CMDB_END -> {
                        if (pos == 0) {
                            inputStream.close()
                            return Result.failure(
                                ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_END,
                                    address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                                )
                            )
                        }
                        val n = (cmd and ATTR_END_COUNT)
                        if (n > 0) {
                            if (n <= ngramTreeStack.size) {
                                for (c in 0 until n) {
                                    ngramOrderStack.removeLast()
                                    ngramTreeStack.removeLast()
                                }
                            } else {
                                inputStream.close()
                                return Result.failure(
                                    ParseException(
                                        errorType = ParseException.ErrorType.UNEXPECTED_ABSOLUTE_DEPTH_DECREASE_BELOW_ZERO,
                                        address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size - n
                                    )
                                )
                            }
                        } else {
                            inputStream.close()
                            return Result.failure(
                                ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_END_ZERO_VALUE,
                                    address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                                )
                            )
                        }
                        pos += 1
                    }
                    else -> {
                        inputStream.close()
                        return Result.failure(
                            ParseException(
                                errorType = ParseException.ErrorType.INVALID_CMD_BYTE_PROVIDED,
                                address = pos, cmdByte = cmd.toByte(), absoluteDepth = ngramTreeStack.size
                            )
                        )
                    }
                }
            }
            inputStream.close()

            if (ngramTreeStack.size != 0) {
                return Result.failure(
                    ParseException(
                        errorType = ParseException.ErrorType.UNEXPECTED_ABSOLUTE_DEPTH_NOT_ZERO_AT_EOF,
                        address = pos, cmdByte = 0x00.toByte(), absoluteDepth = ngramTreeStack.size
                    )
                )
            }

            return Result.success(
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

    // TODO: preceding tokens are currently ignored
    override fun getTokenPredictions(
        precedingTokens: List<Word>,
        currentToken: Word?,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        destSuggestionList: SuggestionList
    ) {
        currentToken ?: return

        if (currentToken.isNotBlank()) {
            val retList = languageModel.matchAllNgrams(
                ngram = Ngram(
                    _tokens = listOf(Token(currentToken.lowercase())),
                    _freq = -1
                ),
                maxEditDistance = 2,
                maxTokenCount = maxSuggestionCount,
                allowPossiblyOffensive = allowPossiblyOffensive
            )
            retList.forEach { destSuggestionList.add(it.data, 128) }
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
                append(
                    when (errorType) {
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
                    }
                )
                append(
                    String.format(
                        "\n at address 0x%08X where cmd_byte=0x%02X and section_depth=%d",
                        address,
                        cmdByte,
                        absoluteDepth
                    )
                )
                toString()
            }
        }
    }
}

/**
 * Reads the next [len] bytes from the input stream into the given byte array [b]. This method guarantees to either
 * read the full length requested or if an EOF file is encountered, -1 is returned. The first byte written is at
 * `b[off]`, the second byte at `b[off+1]` and so on.
 *
 * @param b The byte array to read the next [len] bytes into.
 * @param off The offset of the first byte written in the byte array [b]. Must be non-negative.
 * @param len The number of bytes to read. Must be non-negative.
 *
 * @return The number of bytes read, always matching [len] or -1 if EOF was encountered.
 *
 * @throws IndexOutOfBoundsException if either [off] or [len] is negative or the byte array has insufficient space to
 *  write the request [len] bytes into it.
 */
@Throws(IndexOutOfBoundsException::class)
fun InputStream.readNext(b: ByteArray, off: Int, len: Int): Int {
    if (off < 0 || len < 0 || len > b.size - off) {
        throw IndexOutOfBoundsException()
    } else if (len == 0) {
        return 0
    }

    var lenRead = 0
    while (lenRead < len) {
        val c = read()
        if (c == -1) {
            return -1
        } else {
            b[off + lenRead++] = c.toByte()
        }
    }
    return lenRead
}
