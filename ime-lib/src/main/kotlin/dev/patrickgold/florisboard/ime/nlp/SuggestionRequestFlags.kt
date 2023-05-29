/*
 * Copyright (C) 2023 Patrick Goldinger
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

import dev.patrickgold.florisboard.ime.input.InputShiftState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Class which allows to read 31-bit binary suggestion request flags. Note that the signed bit MUST always be 0, else
 * the behavior of this class is undefined.
 *
 * Layout of the binary flags:
 * | Byte 3 | Byte 2 | Byte 1 | Byte 0 |
 * |--------|--------|--------|--------|
 * |0       |        |        |11111111| Maximum suggestion count (1-255), 0 indicating no limit.
 * |0       |        |    1111|        | Maximum ngram level (2-15). Values 0 and 1 cause word history to be ignored.
 * |0       |        |  11    |        | Input shift state (0-3) at the start of the current word.
 * |0       |        |11      |        | Input shift state (0-3) at the current cursor position.
 * |0       |       1|        |        | Flag indicating if possibly offensive words should be suggested.
 * |0       |      1 |        |        | Flag indicating if user-hidden words should still be displayed.
 * |0       |     1  |        |        | Flag indicating if the current request is within a private session.
 * |01111111|11111   |        |        | Reserved for future use.
 *
 * Note: This class MUST be kept in sync with the C++ implementation:
 *  https://github.com/florisboard/nlp/blob/main/nlpcore/src/common/suggestion.cppm
 */
@Serializable(with = SuggestionRequestFlags.Serializer::class)
@JvmInline
value class SuggestionRequestFlags(val flags: Int) {
    companion object {
        const val M_MAX_SUGGESTION_COUNT = 0x000000FF
        val O_MAX_SUGGESTION_COUNT = M_MAX_SUGGESTION_COUNT.countTrailingZeroBits()
        const val M_MAX_NGRAM_LEVEL = 0x00000F00
        val O_MAX_NGRAM_LEVEL = M_MAX_NGRAM_LEVEL.countTrailingZeroBits()
        const val M_INPUT_SHIFT_STATE_START = 0x00003000
        val O_INPUT_SHIFT_STATE_START = M_INPUT_SHIFT_STATE_START.countTrailingZeroBits()
        const val M_INPUT_SHIFT_STATE_CURRENT = 0x0000C000
        val O_INPUT_SHIFT_STATE_CURRENT = M_INPUT_SHIFT_STATE_CURRENT.countTrailingZeroBits()
        const val F_ALLOW_POSSIBLY_OFFENSIVE = 0x00010000
        const val F_OVERRIDE_HIDDEN_FLAG = 0x00020000
        const val F_IS_PRIVATE_SESSION = 0x00040000

        fun new(
            maxSuggestionCount: Int,
            maxNgramLevel: Int,
            issStart: InputShiftState,
            issCurrent: InputShiftState,
            allowPossiblyOffensive: Boolean,
            overrideHiddenFlag: Boolean,
            isPrivateSession: Boolean,
        ): SuggestionRequestFlags {
            val flags = ((maxSuggestionCount shl O_MAX_SUGGESTION_COUNT) and M_MAX_SUGGESTION_COUNT) or
                ((maxNgramLevel shl O_MAX_NGRAM_LEVEL) and M_MAX_NGRAM_LEVEL) or
                ((issStart.toInt() shl O_INPUT_SHIFT_STATE_START) and M_INPUT_SHIFT_STATE_START) or
                ((issCurrent.toInt() shl O_INPUT_SHIFT_STATE_CURRENT) and M_INPUT_SHIFT_STATE_CURRENT) or
                (if (allowPossiblyOffensive) F_ALLOW_POSSIBLY_OFFENSIVE else 0) or
                (if (overrideHiddenFlag) F_OVERRIDE_HIDDEN_FLAG else 0) or
                (if (isPrivateSession) F_IS_PRIVATE_SESSION else 0)
            return SuggestionRequestFlags(flags)
        }
    }

    fun maxSuggestionCount(): Int {
        return (flags and M_MAX_SUGGESTION_COUNT) shr O_MAX_SUGGESTION_COUNT
    }

    fun maxNgramLevel(): Int {
        return (flags and M_MAX_NGRAM_LEVEL) shr O_MAX_NGRAM_LEVEL
    }

    fun inputShiftStateStart(): InputShiftState {
        return InputShiftState.values()[(flags and M_INPUT_SHIFT_STATE_START) shr O_INPUT_SHIFT_STATE_START]
    }

    fun inputShiftStateCurrent(): InputShiftState {
        return InputShiftState.values()[(flags and M_INPUT_SHIFT_STATE_CURRENT) shr O_INPUT_SHIFT_STATE_CURRENT]
    }

    fun allowPossiblyOffensive(): Boolean {
        return (flags and F_ALLOW_POSSIBLY_OFFENSIVE) != 0
    }

    fun overrideHiddenFlag(): Boolean {
        return (flags and F_OVERRIDE_HIDDEN_FLAG) != 0
    }

    fun isPrivateSession(): Boolean {
        return (flags and F_IS_PRIVATE_SESSION) != 0
    }

    override fun toString(): String {
        return "SuggestionRequestFlags { flags = 0x${flags.toString(16)} }"
    }

    fun toInt(): Int {
        return flags
    }

    object Serializer : KSerializer<SuggestionRequestFlags> {
        override val descriptor = PrimitiveSerialDescriptor("SuggestionRequestFlags", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SuggestionRequestFlags) {
            encoder.encodeInt(value.toInt())
        }

        override fun deserialize(decoder: Decoder): SuggestionRequestFlags {
            return SuggestionRequestFlags(decoder.decodeInt())
        }
    }
}
