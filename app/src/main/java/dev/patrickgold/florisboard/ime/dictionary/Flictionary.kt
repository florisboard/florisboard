/*
 * Copyright (C) 2020 Patrick Goldinger
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
import dev.patrickgold.florisboard.ime.extension.Asset
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import java.lang.StringBuilder

typealias WordMap = LinkedHashMap<String, Int>

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
    val headerStr: String,
    val words: WordMap
) : Dictionary, Asset {
    companion object {
        private const val FLICT_BEGIN_SECTION_128 =     0x00
        private const val FLICT_BEGIN_SECTION_16384 =   0x80
        private const val FLICT_BEGIN_HEADER =          0xC0
        private const val FLICT_END =                   0xF0

        /**
         * Loads a Flictionary binary asset from given [assetRef] and returns a result containing
         * either the parsed dictionary or an exception giving information about the error which
         * occurred.
         */
        fun load(context: Context, assetRef: AssetRef): Result<Flictionary, Exception> {
            var headerStr: String? = null
            val words: WordMap = WordMap()
            if (assetRef.source == AssetSource.Assets) {
                val inputStream = context.assets.open(assetRef.path)
                val rawData = inputStream.readBytes()
                inputStream.close()
                var pos = 0
                var sectionDepth = 0
                val sectionWord = mutableListOf<String>()
                while (pos < rawData.size) {
                    val cmd = rawData[pos].toInt()
                    when {
                        (cmd and 0x80) == FLICT_BEGIN_SECTION_128 -> {
                            if (pos == 0) {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_SECTION,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                            val size = cmd and 0x7F
                            if (pos + 1 + size < rawData.size) {
                                val freq = rawData[pos + 1].toInt() and 0xFF
                                val word = ByteArray(size) { i -> rawData[pos + 2 + i] }
                                sectionWord.add(String(word))
                                if (freq > 0) {
                                    words[sectionWord.joinToString("")] = freq
                                }
                                sectionDepth++
                                pos += (2 + size)
                            } else {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_EOF,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                        }
                        (cmd and 0xC0) == FLICT_BEGIN_SECTION_16384 -> {
                            if (pos == 0) {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_SECTION,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                            if (pos + 1 < rawData.size) {
                                val size = ((cmd and 0x3F) shl 8) or (rawData[pos + 1].toInt() and 0xFF)
                                if (pos + 2 + size < rawData.size) {
                                    val freq = rawData[pos + 2].toInt() and 0xFF
                                    val word = ByteArray(size) { i -> rawData[pos + 3 + i] }
                                    sectionWord.add(String(word))
                                    if (freq > 0) {
                                        words[sectionWord.joinToString("")] = freq
                                    }
                                    sectionDepth++
                                    pos += (3 + size)
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
                        (cmd and 0xE0) == FLICT_BEGIN_HEADER -> {
                            if (pos != 0) {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_BEGIN_HEADER,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                            if (pos + 1 < rawData.size) {
                                val size = ((cmd and 0x3F) shl 8) + (rawData[pos + 1].toInt() and 0xFF)
                                if (pos + 1 + size < rawData.size) {
                                    val headerBytes = ByteArray(size) { i -> rawData[pos + 2 + i] }
                                    headerStr = String(headerBytes)
                                    sectionWord.add(headerStr)
                                    sectionDepth++
                                    pos += (2 + size)
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
                        (cmd and 0xF0) == FLICT_END -> {
                            if (pos == 0) {
                                return Err(ParseException(
                                    errorType = ParseException.ErrorType.UNEXPECTED_CMD_END,
                                    address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
                                ))
                            }
                            val n = (cmd and 0x0F)
                            if (n > 0) {
                                if (n <= sectionDepth) {
                                    sectionDepth -= n
                                    for (i in 0 until n) {
                                        sectionWord.removeLast()
                                    }
                                } else {
                                    return Err(ParseException(
                                        errorType = ParseException.ErrorType.UNEXPECTED_SECTION_DEPTH_DECREASE_BELOW_ZERO,
                                        address = pos, cmdByte = cmd.toByte(), sectionDepth = sectionDepth
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
                return Ok(Flictionary("flict", "flict", listOf(), headerStr ?: "", words))
            }
            return Err(Exception("Only AssetSource.Assets is currently supported!"))
        }
    }

    override fun getFreqForWord(word: String): Int {
        TODO("Not yet implemented")
    }

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
            UNEXPECTED_CMD_BEGIN_SECTION,
            UNEXPECTED_CMD_END,
            UNEXPECTED_CMD_END_ZERO_VALUE,
            UNEXPECTED_SECTION_DEPTH_DECREASE_BELOW_ZERO,
            UNEXPECTED_SECTION_DEPTH_NOT_ZERO_AT_EOF,
            UNEXPECTED_EOF,
            INVALID_CMD_BYTE_PROVIDED,
        }

        override val message: String?
            get() = toString()
        override fun toString(): String {
            return StringBuilder().run {
                append(when (errorType) {
                    ErrorType.UNEXPECTED_CMD_BEGIN_HEADER -> {
                        "Unexpected command: BEGIN_HEADER"
                    }
                    ErrorType.UNEXPECTED_CMD_BEGIN_SECTION -> {
                        "Unexpected command: BEGIN_SECTION"
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
