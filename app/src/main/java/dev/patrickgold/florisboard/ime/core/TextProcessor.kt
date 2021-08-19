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

package dev.patrickgold.florisboard.ime.core

import dev.patrickgold.florisboard.common.FlorisLocale

@Suppress("RegExpRedundantEscape")
object TextProcessor {
    private val LATIN_BASIC_WORD_REGEX = """[_]*(([\p{L}\d\']+[_-]*[\p{L}\d\']+)|[\p{L}\d\']+)[_]*""".toRegex()

    private fun wordRegexFor(locale: FlorisLocale): Regex {
        return when (locale) {
            else -> LATIN_BASIC_WORD_REGEX
        }
    }

    fun detectWords(text: CharSequence, locale: FlorisLocale): Sequence<IntRange> {
        val regex = wordRegexFor(locale)
        return regex.findAll(text).map { it.range }
    }

    fun detectWords(text: CharSequence, start: Int, end: Int, locale: FlorisLocale): Sequence<IntRange> {
        val regex = wordRegexFor(locale)
        val tStart = start.coerceAtLeast(0)
        val tEnd = end.coerceAtMost(text.length)
        return regex.findAll(text.slice(tStart..tEnd)).map { it.range }
    }

    fun isWord(text: CharSequence, locale: FlorisLocale): Boolean {
        val regex = wordRegexFor(locale)
        return regex.matches(text)
    }
}
