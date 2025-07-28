/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.text.composing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Constants for list of consonants and vowels
object Letters {
    const val CONSONANTS =  "bcdđghklmnpqrstvx"
    const val VOWELS = mapOf(
        "a" to "aáàảãạ",
        "ă" to "ăắằẳẵặ",
        "â" to "âấầẩẫậ",
        "e" to "eéèẻẽẹ",
        "ê" to "êếềểễệ",
        "i" to "iíìỉĩị",
        "o" to "oóòỏõọ",
        "ô" to "ôốồổỗộ",
        "ơ" to "ơớờởỡợ",
        "u" to "uúùủũụ",
        "ư" to "ưứừửữự",
        "y" to "yýỳỷỹỵ",
    )
}

class InvalidVietnameseSyllableException: Exception()

/* Vietnamese syllable structure:
 * (Initial consonant) (u) (y) V1 tone (V2) (end consonant)
 * where
 * - u is a sonorant, can be:
 *   - /w/ represented by either o or u, depending on the onset and V1
 *   - ư
 * - y represented by either i or y, depending on the onset and V1
 * - V1 and V2 are vowels; V1 is the accented character
 *   - not all V1 and V2 are compatible; for example âi is not possible
 * - Some end consonants don't allow all tones.
 * Example:
 * tia ->
 *   - initial: t
 *   - V1: i
 *   - V2: a
 * quyền ->
 *   - initial: q
 *   - w: u
 *   - j: y
 *   - V1: ê
 *   - end: n
 * hoành ->
 *   - initial: h
 *   - w: o
 *   - V1: a
 *   - end: nh
 * thiên ->
 *   - initial: th
 *   - j: i
 *   - V1: ê
 *   - end: n
 * luôn ->
 *   - initial: l
 *   - w: u
 *   - V1: ô
 *   - end: n
 */

class Syllable {
    val initial: String? = null
    val u: Char? = null
    val y: Char? = null
    val v1: Char? = null
    val v2: Char? = null
    val end: String = ""
    val tone: Int = 0

    // Serialize the syllable to String
    fun toString(): String {
        val raw = ""
        if (this.initial) {
            raw += this.initial
        }
        if (this.u) {
            raw += this.u
        }
        if (this.y) {
            raw += this.y
        }
        if (this.v1) {
            raw += this.v1
        }
        if (this.v2) {
            raw += this.v2
        }
        if (this.end) {
            raw += this.end
        }
    }

    // Handle a new input to an existing syllable
    fun add(char: Char) {
        // Only allow for ASCII letters
        val code = char.toInt()
        if (code < 65 || code > 122) {
            throw InvalidVietnameseSyllableException()
        }
    }
}

@Serializable
@SerialName("telex-v2")
class Telex : Composer {
    override val id: String = "telex-v2"
    override val label: String = "Telex (v2)"
    override val toRead: Int = 1

    }
}
