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

package dev.patrickgold.florisboard.common

import android.icu.lang.UCharacter
import android.icu.lang.UCharacterCategory
import dev.patrickgold.florisboard.common.android.AndroidVersion

/**
 * Character codes and comments source:
 *  https://www.w3.org/International/questions/qa-bidi-unicode-controls#basedirection
 */
@Suppress("unused")
object UnicodeCtrlChar {
    /** Sets base direction to LTR and isolates the embedded content from the surrounding text */
    const val LeftToRightIsolate = "\u2066"

    /** Sets base direction to RTL and isolates the embedded content from the surrounding text */
    const val RightToLeftIsolate = "\u2067"

    /** Isolates the content and sets the direction according to the first strongly typed directional character */
    const val FirstStrongIsolate = "\u2068"

    /** Closes a previously opened isolated text block */
    const val PopDirectionalIsolate = "\u2069"

    val Matcher = """[$LeftToRightIsolate$RightToLeftIsolate$FirstStrongIsolate$PopDirectionalIsolate]""".toRegex()
}

fun String.stripUnicodeCtrlChars(): String {
    return this.replace(UnicodeCtrlChar.Matcher, "")
}

object Unicode {
    fun isNonSpacingMark(code: Int): Boolean {
        return if (AndroidVersion.ATLEAST_API24_N) {
            UCharacter.getType(code).toByte() == UCharacterCategory.NON_SPACING_MARK
        } else {
            // See: https://en.wikipedia.org/wiki/Combining_character
            //      https://unicode-table.com/en/blocks/arabic/
            return when (code) {
                in 0x0300..0x036F, // Combining Diacritical Marks
                in 0x1AB0..0x1AFF, // Combining Diacritical Marks Extended
                in 0x1DC0..0x1DFF, // Combining Diacritical Marks Supplement
                in 0x20D0..0x20FF, // Combining Diacritical Marks for Symbols
                in 0xFE20..0xFE2F, // Combining Half Marks
                0x0E31, // Thai
                in 0x0E34..0x0E3A, // Thai
                in 0x0E47..0x0E4E, // Thai
                in 0x0610..0x0614, // Honorifics
                in 0x064B..0x065F, // Tashkil, Combining maddah, hamza and other
                0x0670, // Tashkil (single char)
                in 0x06D6..0x06ED, // Quranic annotation signs
                -> true
                else -> false
            }
        }
    }
}
