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

@Serializable
@SerialName("hangul-unicode")
class HangulUnicode : Composer {
    override val id: String = "hangul-unicode"
    override val label: String = "Hangul Unicode"
    override val toRead: Int = 1

    // Initial consonants, ordered for syllable creation
    private val initials = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    // Medial vowels, ordered for syllable creation
    private val medials = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
    // Final consonants (including none), ordered for syllable creation
    private val finals = "_ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"

    private val medialComp = mapOf(
        'ㅗ' to listOfNotNull("ㅏㅐㅣ", "ㅘㅙㅚ"),
        'ㅜ' to listOfNotNull("ㅓㅔㅣ", "ㅝㅞㅟ"),
        'ㅡ' to listOfNotNull("ㅣ", "ㅢ"),
    )

    private val finalComp = mapOf(
        'ㄱ' to listOfNotNull("ㅅ", "ㄳ"),
        'ㄴ' to listOfNotNull("ㅈㅎ", "ㄵㄶ"),
        'ㄹ' to listOfNotNull("ㄱㅁㅂㅅㅌㅍㅎ", "ㄺㄻㄼㄽㄾㄿㅀ"),
        'ㅂ' to listOfNotNull("ㅅ", "ㅄ"),
    )

    private fun reverseComp(map: Map<Char, List<String>>): Map<Char, List<Char>> {
        val ret = mutableMapOf<Char, List<Char>>()
        for ((first, v) in map) {
            val (seconds, comps) = v
            for (i in seconds.indices) {
                ret[comps[i]] = listOf(first, seconds[i])
            }
        }
        return ret
    }

    private val finalCompRev = reverseComp(finalComp)
    private val medialCompRev = reverseComp(medialComp)

    private fun syllable(ini: Int, med: Int, fin:Int): Char {
        return (ini*588 + med*28 + fin + 44032).toChar()
    }

    private fun syllableBlocks(syllOrd: Int): List<Int> {
        val initial = (syllOrd-44032)/588
        val medial = (syllOrd-44032-initial*588)/28
        val fin = (syllOrd-44032)%28
        return listOf(initial, medial, fin)
    }

    override fun getActions(precedingText: String, toInsert: String): Pair<Int, String> {
        val c = toInsert.firstOrNull()
        // precedingText is "at least the last 1 character of what's currently here"
        if (precedingText.isEmpty() || c == null) {
            return 0 to toInsert
        }
        val lastChar = precedingText.last()
        val lastOrd = lastChar.code

        if (lastChar in initials && c in medials) {
            return Pair(1, "${syllable(initials.indexOf(lastChar), medials.indexOf(c), 0)}")
        } else if (lastOrd in 44032..55203) { // syllable
            val (ini, med, fin) = syllableBlocks(lastOrd)

            // underscore is a sentinel in the "finals" string
            if (c == '_') {
                return 0 to toInsert
            }

            //  if there is no final and the new char is a final, merge
            if (fin == 0 && c in finals) {
                return 1 to "${syllable(ini, med, finals.indexOf(c))}"
            }

            // if there is already a final but it is mergeable with the new char into a composed final, merge
            if ((finals[fin] in finalComp) && c in finalComp[finals[fin]]!![0]) {
                val tple = finalComp[finals[fin]]
                return 1 to "${syllable(ini, med, finals.indexOf(tple!![1][tple[0].indexOf(c)]))}"
            }

            // if there is a simple final and the new char is a medial, split the old syllable
            if (fin != 0 && finals[fin] !in finalCompRev && c in medials)
                return 1 to "${syllable(ini, med, 0)}${syllable(initials.indexOf(finals[fin]), medials.indexOf(c), 0)}"

            // if there is a composed final and the new char is a medial, split the old final
            if (finals[fin] in finalCompRev && c in medials) {
                return 1 to "${syllable(ini, med, finals.indexOf(finalCompRev.getValue(finals[fin])[0]))}${syllable(initials.indexOf(finalCompRev.getValue(finals[fin])[1]), medials.indexOf(c), 0)}"
            }

            // if no final yet, and current medial can be composed with new char, merge
            if (medials[med] in medialComp && c in medialComp.getValue(medials[med])[0] && fin == 0) {
                val tple = medialComp[medials[med]]
                return 1 to "${syllable(ini, medials.indexOf(tple!![1][tple[0].indexOf(c)]), 0)}"
            }
        } else if (lastChar in medialComp.keys && medialComp[lastChar]?.get(0)?.contains(c) == true) { // medial+final
            return 1 to ""+ medialComp[lastChar]?.get(1)!![medialComp[lastChar]?.get(0)!!.indexOf(c)]
        } else if (lastChar in finalComp.keys && finalComp[lastChar]?.get(0)?.contains(c) == true) { // final+final
            return 1 to ""+ finalComp[lastChar]?.get(1)!![finalComp[lastChar]?.get(0)!!.indexOf(c)]
        }

        return 0 to toInsert
    }
}
