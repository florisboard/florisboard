/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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
    val VOWELS: Map<Char, String> = mapOf(
        'a' to "aáàảãạ",
        'ă' to "ăắằẳẵặ",
        'â' to "âấầẩẫậ",
        'e' to "eéèẻẽẹ",
        'ê' to "êếềểễệ",
        'i' to "iíìỉĩị",
        'o' to "oóòỏõọ",
        'ô' to "ôốồổỗộ",
        'ơ' to "ơớờởỡợ",
        'u' to "uúùủũụ",
        'ư' to "ưứừửữự",
        'y' to "yýỳỷỹỵ",
    )
    val BARE_VOWELS: Map<Char, Char> = mapOf(
        'ă' to 'a',
        'â' to 'a',
        'ê' to 'e',
        'ô' to 'o',
        'ơ' to 'o',
        'ư' to 'u',
    )
}

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
 *   - start: t
 *   - V1: i
 *   - V2: a
 * quyền ->
 *   - start: q
 *   - w: u
 *   - j: y
 *   - V1: ê
 *   - end: n
 * hoành ->
 *   - start: h
 *   - w: o
 *   - V1: a
 *   - end: nh
 * thiên ->
 *   - start: th
 *   - j: i
 *   - V1: ê
 *   - end: n
 * luôn ->
 *   - start: l
 *   - w: u
 *   - V1: ô
 *   - end: n
 */

class Syllable {
    var start: String = ""
    var u: Char? = null
    var y: Char? = null
    var v1: Char? = null
    var v2: Char? = null
    var end: String = ""
    var tone: Int = 0

    // Serialize the syllable to String
    override fun toString(): String {
        val word = buildString {
            append(start)
            if (u != null) append(u)
            if (y != null) append(y)
            val lowerV1 = v1?.lowercaseChar()
            val isV1UpperCase = v1?.isUpperCase() ?: false
            if (lowerV1 != null) {
                var tonedLetter = Letters.VOWELS[lowerV1]!![tone]
                if (isV1UpperCase) {
                    tonedLetter = tonedLetter.uppercaseChar()
                }
                append(tonedLetter)
            }
            if (v2 != null) append(v2)
            append(end)
        }
        return word
    }

    companion object Factory {
        // Parse words from existing text
        // Return null if syllable is invalid
        fun parse(text: String): Syllable? {
            // Use a regex to quickly validate syllable
            // Initial: bcdđghklmnpqrstvx (all consonants), max length 3 (ngh)
            // Middle: (all vowels), max length 3
            // Final: cghmnpt (including ng, nh, ch), max length 2
            val initialLetters = Letters.CONSONANTS
            val middleLetters = Letters.VOWELS.values.joinToString("")
            val finalLetters = "cghmnpt"
            val quickRegex = Regex("([$initialLetters]*)([$middleLetters]*)([$finalLetters]*)", RegexOption.IGNORE_CASE)
            val matchResults = quickRegex.find(text)

            if (matchResults?.groupValues == null) {
                return null
            }
            val (_, initialGroup, middleGroup, finalGroup) = matchResults.groupValues

            val syllable = Syllable()
            // Parse the first group
            when (initialGroup.length) {
                0 -> syllable.start = ""
                1 -> syllable.start = initialGroup
                2 -> if (initialGroup.lowercase() in listOf("ch", "gh", "kh", "ng", "nh", "ph", "th", "tr")) syllable.start = initialGroup else return null
                3 -> if (initialGroup.lowercase() === "ngh") syllable.start = initialGroup else return null
                else -> return null
            }
            // Parse the second group
            // ensure q must be followed by u
            if (initialGroup == "q" && middleGroup.length > 0 && middleGroup[0] != 'u') {
                return null
            }
            // reverse tone map
            val reversed = mutableMapOf<Char, Char>()
            for ((vowel, toned) in Letters.VOWELS) {
                for (c in toned) {
                    reversed[c] = vowel
                }
            }

            when (middleGroup.length) {
                0 -> return syllable // unfinished word without vowels
                1 -> {
                    if (syllable.start == "q" && middleGroup[0].lowercaseChar() == 'u') {
                        syllable.u = middleGroup[0]
                    } else {
                        syllable.v1 = middleGroup[0]
                    }
                }
                2 -> {
                    val v1 = middleGroup[0]
                    val v2 = middleGroup[1]
                    val lowerV1 = v1.lowercaseChar()
                    val lowerV2 = v2.lowercaseChar()
                    val base1 = reversed[lowerV1]!!
                    val base2 = reversed[lowerV2]!!
                    val bare1 = Letters.BARE_VOWELS[base1]!!
                    val bare2 = Letters.BARE_VOWELS[base2]!!
                    val tone1 = Letters.VOWELS[base1]!!.indexOf(lowerV1)
                    val tone2 = Letters.VOWELS[base2]!!.indexOf(lowerV2)
                    // can't be more than 2 tones
                    if (tone1 > 0 && tone2 > 0) {
                        return null
                    }
                    if (tone1 > 0) {
                        syllable.tone = tone1
                    } else if (tone2 > 0) {
                        syllable.tone = tone2
                    }
                    when (bare1) {
                        'a', 'e' -> {
                            syllable.v1 = v1
                            syllable.v2 = v2
                        }
                        'i' -> {
                            // allow giặt gịa
                            if (syllable.start.lowercase() == "g" && middleGroup.lowercase() != "ịa" && base2 != 'ê') {
                                syllable.start += v1
                                syllable.v1 = v2
                            } else if (base2 == 'ê') {
                                syllable.y = v1
                                syllable.v1 = v2
                            } else {
                                syllable.v1 = v1
                                syllable.v2 = v2
                            }
                        }
                        'y' -> {
                            // allow giặt gỵa
                            if (syllable.start.lowercase() == "g") {
                                syllable.start += v1
                                syllable.v1 = v2
                            } else if (syllable.start == "") {
                                syllable.y = v1
                                syllable.v1 = v2
                            } else {
                                // y cannot goes in middle e.g. yên, yêt
                                return null
                            }
                        }
                        'o' -> {
                            if (bare1 == 'i') {
                                // e.g. hỏi
                                syllable.v1 = v1
                                syllable.v2 = v2
                            } else {
                                // e.g. hoá, toé (new tone placement)
                                // voọc, coóc-xê (rare oong/ooc)
                                syllable.u = v1
                                syllable.v1 = v2
                            }
                        }
                        'u' -> {
                            if (syllable.start.lowercase() == "q" || finalGroup != "") {
                                syllable.u = v1
                                syllable.v1 = v2
                            } else {
                                syllable.v1 = v1
                                syllable.v2 = v2
                            }
                        }
                        else -> return null
                    }
                }
                3 -> {
                    val v1 = middleGroup[0]
                    val v2 = middleGroup[1]
                    val v3 = middleGroup[2]
                    val lowerV1 = v1.lowercaseChar()
                    val lowerV2 = v2.lowercaseChar()
                    val lowerV3 = v3.lowercaseChar()
                    val base1 = reversed[lowerV1]!!
                    val base2 = reversed[lowerV2]!!
                    val base3 = reversed[lowerV3]!!
                    val bare1 = Letters.BARE_VOWELS[base1]!!
                    val bare2 = Letters.BARE_VOWELS[base2]!!
                    val bare3 = Letters.BARE_VOWELS[base3]!!
                    val tone1 = Letters.VOWELS[base1]!!.indexOf(lowerV1)
                    val tone2 = Letters.VOWELS[base2]!!.indexOf(lowerV2)
                    val tone3 = Letters.VOWELS[base3]!!.indexOf(lowerV3)
                    // tone must be on 2nd (oải) or 3rd (tuyết) vowel letter
                    if (tone1 > 0) {
                        return null
                    } else if (tone2 > 0 && tone3 == 0) {
                        syllable.tone = tone2
                    } else if (tone3 > 0 && tone2 == 0) {
                        syllable.tone = tone3
                    }
                    when (base1) {
                        'o' -> {
                            // allowed: oai, oay, oeo
                            syllable.u = middleGroup[0]
                            if ("$base2$base3" in listOf("oai", "oay", "oeo") && finalGroup.length == 0) {
                                syllable.v1 = middleGroup[1]
                                syllable.v2 = middleGroup[2]
                            } else {
                                return null
                            }
                        }
                        'u' -> {
                            // allowed: uai, uay, ueo, uây, uya, uyê, uyu, uôi
                            // if uyê, allows final
                            // first 3 only follows q
                            syllable.u = middleGroup[0]
                            when ("$bare2$bare3") {
                                "ai", "ay", "eo" -> {
                                    if (syllable.start != "q" || finalGroup.length > 0) {
                                        return null
                                    }
                                    syllable.v1 = middleGroup[1]
                                    syllable.v2 = middleGroup[2]
                                }
                                "ay", "ya", "yu", "oi", "ou" -> {
                                    if (finalGroup.length > 0) {
                                        return null
                                    }
                                    syllable.v1 = middleGroup[1]
                                    syllable.v2 = middleGroup[2]
                                }
                                "ye" -> {
                                    syllable.y = middleGroup[1]
                                    syllable.v1 = middleGroup[2]
                                }
                                else -> return null
                            }
                        }
                        'ư' -> {
                            // allows: ươi, ươu
                            if ("$bare2$bare3" in listOf("oi", "ou") && finalGroup.length == 0) {
                                syllable.u = middleGroup[0]
                                syllable.v1 = middleGroup[1]
                                syllable.v2 = middleGroup[2]
                            } else {
                                return null
                            }
                        }
                        // allowed: yêu, iêu
                        'i', 'y' -> {
                            if ("$bare2$bare3" != "eu" || finalGroup.length > 0) {
                                return null
                            }
                            if (initialGroup != "" && bare1 == 'y') {
                                return null
                            }
                            syllable.y = middleGroup[0]
                            syllable.v1 = middleGroup[1]
                            syllable.v2 = middleGroup[2]
                        }
                        else -> return null
                    }
                }
                else -> return null
            }
            // Parse the final group
            when (finalGroup) {
                "", "n", "m", "ng", "nh" -> syllable.end = finalGroup
                "t", "c", "ch", "p" -> if (syllable.tone in listOf(0, 1, 5)) {
                    syllable.end = finalGroup
                } else {
                    return null
                }
                else -> return null
            }
            return syllable
        }
    }
}

// Returns the number of characters to delete and the string to replace
fun compare(fromText: String, toText: String): Pair<Int, String> {
    val length = fromText.length
    for (i in 0..<length) {
        if (fromText[i] != toText[i]) {
            return Pair(length - i, toText.substring(i, length))
        }
    }
    return Pair(0, toText.substring(0, toText.length - length))
}

// If not same tone, change tone
// else, remove tone and append letter
fun handleTone(syllable: Syllable, letter: Char, tone: Int): String {
    if (syllable.tone != tone) {
        syllable.tone = tone
        return syllable.toString()
    } else {
        syllable.tone = 0
        return syllable.toString() + letter
    }
}

// Handle d + d -> đ
fun handleD(syllable: Syllable, newLetter: Char): String {
    if (syllable.start == "d") {
        syllable.start = "đ"
        return syllable.toString()
    } else if (syllable.start == "đ") {
        syllable.start = "d"
    }
    return syllable.toString() + newLetter
}

// Handle vowel diacritics
// letterPairs is a map from base letters to pair of fromLetters and toLetter
// e.g.
// newLetter = 'a'
// letterPairs = mapOf('a' to ("aă" to 'a'))
// => fromLetters = "aă"
//    toLetter = 'â'
// then
// a + a -> â
// ă + a -> â
// â + a -> aa
// i + a -> ia
fun handleVowel(syllable: Syllable, newLetter: Char, letterPairs: Map<Char, Pair<String, Char>>): String {
    val isV1UpperCase = syllable.v1?.isUpperCase() ?: false
    val lowerV1 = syllable.v1?.lowercaseChar()
    val bareLetter = Letters.BARE_VOWELS[lowerV1]
    val fromLetters = letterPairs[bareLetter]!!.first
    val toLetter = letterPairs[bareLetter]!!.second

    if (lowerV1 != null && lowerV1 in fromLetters) {
        if (isV1UpperCase) {
            syllable.v1 = toLetter.uppercaseChar()
        } else {
            syllable.v1 = toLetter
        }
    } else if (lowerV1 == toLetter) {
        if (isV1UpperCase) {
            syllable.v1 = bareLetter!!.uppercaseChar()
        } else {
            syllable.v1 = bareLetter
        }
    }
    // TODO: Handle lazy w -> ư

    // Special case for o and w:
    // these needs final consonant or v2 (to allows huơ)
    // uon + w -> ươn
    // uôn + w -> ươn
    // these doesn't need final consonant
    // ươ + w ->
    // ươn + w -> uôn
    val isUUpperCase = syllable.u?.isUpperCase() ?: false
    val mid = syllable.u?.lowercaseChar().toString() + bareLetter
    if (newLetter == 'w' && mid == "uo" && (syllable.v2 == 'i' || syllable.end !== "") && syllable.start != "q") {
        if (isUUpperCase) {
            syllable.u = 'Ư'
        } else {
            syllable.u = 'ư'
        }
    } else if (newLetter == 'o' && mid == "ưo") {
        if (isUUpperCase) {
            syllable.u = 'U'
        } else {
            syllable.u = 'u'
        }
    }
    return syllable.toString() + newLetter
}

@Serializable
@SerialName("telex-v2")
class Telex : Composer {
    override val id: String = "telex-v2"
    override val label: String = "Telex (v2)"
    // longest rhymes are 4 characters (uyên, uông/ương, oanh, uynh, uênh)
    // the only onset consonant that needs edit is đ, which is 1 character
    override val toRead: Int = 5

    // Customization options: to be implemented
    // Use w as a shorthand to type ư
    //val wForUw: Boolean = true
    // Use oà oè uỳ instead of òa òe ùy
    // Note that it'll be always quỳ instead of qùy
    //val newTonePlacement: Boolean = true

    override fun getActions(precedingText: String, toInsert: String): Pair<Int, String> {
        val char = toInsert.firstOrNull()
        // precedingText is "at least the last 1 character of what's currently here"
        if (precedingText.isEmpty() || char == null) {
            return 0 to toInsert
        }
        val code = char.code
        // if inserted letter isn't alphabetic, fallback to appender
        if (code < 65 || code > 122) {
            return 0 to toInsert
        }
        // If existing text is not valid syllable, fallback to appender
        val syllable = Syllable.parse(precedingText)
        if (syllable === null) {
            return 0 to toInsert
        }
        // logic for telex below
        val newText = when (char.lowercaseChar()) {
            'd' -> handleD(syllable, 'd')
            'a' -> handleVowel(syllable, 'a', mapOf('a' to ("aă" to 'â')))
            'e' -> handleVowel(syllable, 'e', mapOf('e' to ("e" to 'ê')))
            'o' -> handleVowel(syllable, 'o', mapOf('o' to ("oơ" to 'ô')))
            'w' -> handleVowel(syllable, 'w', mapOf(
                'a' to ("aâ" to 'ă'),
                'o' to ("oô" to 'ơ'),
                'u' to ("u" to 'ư'),
            ))
            'z' -> handleTone(syllable, 'z', 0)
            's' -> handleTone(syllable, 's', 1)
            'f' -> handleTone(syllable, 'f', 2)
            'r' -> handleTone(syllable, 'r', 3)
            'x' -> handleTone(syllable, 'x', 4)
            'j' -> handleTone(syllable, 'j', 5)
            // default to append
            else -> precedingText + char
        }
        return compare(precedingText, newText)
    }
}
