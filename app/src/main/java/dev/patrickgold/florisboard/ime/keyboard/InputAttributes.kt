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

@file:Suppress("MemberVisibilityCanBePrivate")

package dev.patrickgold.florisboard.ime.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * Class which holds the same information as an [EditorInfo.inputType] int but more accessible and
 * readable.
 */
@JvmInline
value class InputAttributes(val state: KeyboardState) {
    companion object {
        const val M_INPUT_ATTRIBUTES: ULong =           0x0F_FF_FFu
        const val O_INPUT_ATTRIBUTES: Int =             44

        const val M_TYPE: ULong =                       0x07u
        const val O_TYPE: Int =                         44
        const val M_VARIATION: ULong =                  0x1Fu
        const val O_VARIATION: Int =                    47
        const val M_CAPS_MODE: ULong =                  0x03u
        const val O_CAPS_MODE: Int =                    52

        const val F_NUMBER_DECIMAL: ULong =             0x00_40_00_00_00_00_00_00u
        const val F_NUMBER_SIGNED: ULong =              0x00_80_00_00_00_00_00_00u
        const val F_TEXT_AUTO_COMPLETE: ULong =         0x01_00_00_00_00_00_00_00u
        const val F_TEXT_AUTO_CORRECT: ULong =          0x02_00_00_00_00_00_00_00u
        const val F_TEXT_IME_MULTILINE: ULong =         0x04_00_00_00_00_00_00_00u
        const val F_TEXT_MULTILINE: ULong =             0x08_00_00_00_00_00_00_00u
        const val F_TEXT_NO_SUGGESTIONS: ULong =        0x10_00_00_00_00_00_00_00u
    }

    var type: Type
        get() = Type.fromInt(state.getRegion(M_TYPE, O_TYPE))
        private set(v) = state.setRegion(M_TYPE, O_TYPE, v.toInt())

    var variation: Variation
        get() = Variation.fromInt(state.getRegion(M_VARIATION, O_VARIATION))
        private set(v) = state.setRegion(M_VARIATION, O_VARIATION, v.toInt())

    var capsMode: CapsMode
        get() = CapsMode.fromInt(state.getRegion(M_CAPS_MODE, O_CAPS_MODE))
        private set(v) = state.setRegion(M_CAPS_MODE, O_CAPS_MODE, v.toInt())

    var flagNumberDecimal: Boolean
        get() = state.getFlag(F_NUMBER_DECIMAL)
        private set(v) = state.setFlag(F_NUMBER_DECIMAL, v)
    var flagNumberSigned: Boolean
        get() = state.getFlag(F_NUMBER_SIGNED)
        private set(v) = state.setFlag(F_NUMBER_SIGNED, v)
    var flagTextAutoComplete: Boolean
        get() = state.getFlag(F_TEXT_AUTO_COMPLETE)
        private set(v) = state.setFlag(F_TEXT_AUTO_COMPLETE, v)
    var flagTextAutoCorrect: Boolean
        get() = state.getFlag(F_TEXT_AUTO_CORRECT)
        private set(v) = state.setFlag(F_TEXT_AUTO_CORRECT, v)
    var flagTextImeMultiLine: Boolean
        get() = state.getFlag(F_TEXT_IME_MULTILINE)
        private set(v) = state.setFlag(F_TEXT_IME_MULTILINE, v)
    var flagTextMultiLine: Boolean
        get() = state.getFlag(F_TEXT_MULTILINE)
        private set(v) = state.setFlag(F_TEXT_MULTILINE, v)
    var flagTextNoSuggestions: Boolean
        get() = state.getFlag(F_TEXT_NO_SUGGESTIONS)
        private set(v) = state.setFlag(F_TEXT_NO_SUGGESTIONS, v)

    fun update(editorInfo: EditorInfo) {
        val inputAttrsRaw = editorInfo.inputType
        state.setRegion(M_INPUT_ATTRIBUTES, O_INPUT_ATTRIBUTES, 0) // reset inputAttributes region
        when (inputAttrsRaw and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_NULL -> {
                type = Type.NULL
                variation = Variation.NORMAL
                capsMode = CapsMode.NONE
            }
            InputType.TYPE_CLASS_DATETIME -> {
                type = Type.DATETIME
                variation = when (inputAttrsRaw and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_DATETIME_VARIATION_DATE -> Variation.DATE
                    InputType.TYPE_DATETIME_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_DATETIME_VARIATION_TIME -> Variation.TIME
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.NONE
            }
            InputType.TYPE_CLASS_NUMBER -> {
                type = Type.NUMBER
                variation = when (inputAttrsRaw and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_NUMBER_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD -> Variation.PASSWORD
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.NONE
                flagNumberDecimal = inputAttrsRaw and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0
                flagNumberSigned = inputAttrsRaw and InputType.TYPE_NUMBER_FLAG_SIGNED != 0
            }
            InputType.TYPE_CLASS_PHONE -> {
                type = Type.PHONE
                variation = Variation.NORMAL
                capsMode = CapsMode.NONE
            }
            InputType.TYPE_CLASS_TEXT -> {
                type = Type.TEXT
                variation = when (inputAttrsRaw and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> Variation.EMAIL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> Variation.EMAIL_SUBJECT
                    InputType.TYPE_TEXT_VARIATION_FILTER -> Variation.FILTER
                    InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE -> Variation.LONG_MESSAGE
                    InputType.TYPE_TEXT_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_TEXT_VARIATION_PASSWORD -> Variation.PASSWORD
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> Variation.PERSON_NAME
                    InputType.TYPE_TEXT_VARIATION_PHONETIC -> Variation.PHONETIC
                    InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> Variation.POSTAL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> Variation.SHORT_MESSAGE
                    InputType.TYPE_TEXT_VARIATION_URI -> Variation.URI
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> Variation.VISIBLE_PASSWORD
                    InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> Variation.WEB_EDIT_TEXT
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> Variation.WEB_EMAIL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> Variation.WEB_PASSWORD
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.fromFlags(inputAttrsRaw)
                flagTextAutoComplete = inputAttrsRaw and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0
                flagTextAutoCorrect = inputAttrsRaw and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT != 0
                flagTextImeMultiLine = inputAttrsRaw and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE != 0
                flagTextMultiLine = inputAttrsRaw and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
                flagTextNoSuggestions = inputAttrsRaw and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0
            }
            else -> {
                type = Type.TEXT
                variation = Variation.NORMAL
                capsMode = CapsMode.NONE
            }
        }
    }

    enum class Type(val value: Int) {
        NULL(EditorInfo.TYPE_NULL),
        DATETIME(EditorInfo.TYPE_CLASS_DATETIME),
        NUMBER(EditorInfo.TYPE_CLASS_NUMBER),
        PHONE(EditorInfo.TYPE_CLASS_PHONE),
        TEXT(EditorInfo.TYPE_CLASS_TEXT);

        companion object {
            fun fromInt(int: Int) = values().firstOrNull { it.value == int } ?: NULL
        }

        fun toInt() = value
    }

    enum class Variation(val value: Int) {
        NORMAL(0),
        DATE(1),
        EMAIL_ADDRESS(2),
        EMAIL_SUBJECT(3),
        FILTER(4),
        LONG_MESSAGE(5),
        PASSWORD(6),
        PERSON_NAME(7),
        PHONETIC(8),
        POSTAL_ADDRESS(9),
        SHORT_MESSAGE(10),
        TIME(11),
        URI(12),
        VISIBLE_PASSWORD(13),
        WEB_EDIT_TEXT(14),
        WEB_EMAIL_ADDRESS(15),
        WEB_PASSWORD(16);

        companion object {
            fun fromInt(int: Int) = values().firstOrNull { it.value == int } ?: NORMAL
        }

        fun toInt() = value
    }

    enum class CapsMode(val value: Int) {
        NONE(0),
        ALL(1),
        SENTENCES(2),
        WORDS(3);

        companion object {
            fun fromFlags(flags: Int): CapsMode {
                return when {
                    flags and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS != 0 -> ALL
                    flags and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 -> SENTENCES
                    flags and InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0 -> WORDS
                    else -> NONE
                }
            }

            fun fromInt(int: Int) = values().firstOrNull { it.value == int } ?: NONE
        }

        fun toFlags(): Int {
            return when (this) {
                ALL -> InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                SENTENCES -> InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                WORDS -> InputType.TYPE_TEXT_FLAG_CAP_WORDS
                else -> 0
            }
        }

        fun toInt() = value
    }
}
