/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.editor

import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * Class which holds the same information as an [EditorInfo.inputType] int but more accessible and
 * readable.
 *
 * @see EditorInfo.inputType for mask table
 */
@JvmInline
value class InputAttributes private constructor(val raw: Int) {
    val type: Type
        get() = Type.fromInt(raw and InputType.TYPE_MASK_CLASS)

    val variation: Variation
        get() = when (type) {
            Type.DATETIME -> when (raw and InputType.TYPE_MASK_VARIATION) {
                InputType.TYPE_DATETIME_VARIATION_DATE -> Variation.DATE
                InputType.TYPE_DATETIME_VARIATION_NORMAL -> Variation.NORMAL
                InputType.TYPE_DATETIME_VARIATION_TIME -> Variation.TIME
                else -> Variation.NORMAL
            }
            Type.NUMBER -> when (raw and InputType.TYPE_MASK_VARIATION) {
                InputType.TYPE_NUMBER_VARIATION_NORMAL -> Variation.NORMAL
                InputType.TYPE_NUMBER_VARIATION_PASSWORD -> Variation.PASSWORD
                else -> Variation.NORMAL
            }
            Type.TEXT -> when (raw and InputType.TYPE_MASK_VARIATION) {
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
            else -> Variation.NORMAL
        }

    val capsMode: CapsMode
        get() = if (type == Type.TEXT) CapsMode.fromFlags(raw) else CapsMode.NONE

    val flagNumberDecimal: Boolean
        get() = type == Type.NUMBER && (raw and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0)

    val flagNumberSigned: Boolean
        get() = type == Type.NUMBER && (raw and InputType.TYPE_NUMBER_FLAG_SIGNED != 0)

    val flagTextAutoComplete: Boolean
        get() = type == Type.TEXT && (raw and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0)

    val flagTextAutoCorrect: Boolean
        get() = type == Type.TEXT && (raw and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT != 0)

    val flagTextImeMultiLine: Boolean
        get() = type == Type.TEXT && (raw and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE != 0)

    val flagTextMultiLine: Boolean
        get() = type == Type.TEXT && (raw and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0)

    val flagTextNoSuggestions: Boolean
        get() = type == Type.TEXT && (raw and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0)



    companion object {
        fun wrap(inputType: Int) = InputAttributes(inputType)
    }

    enum class Type(private val value: Int) {
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

    enum class Variation(private val value: Int) {
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

    enum class CapsMode(private val value: Int) {
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
