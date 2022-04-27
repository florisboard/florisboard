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

@file:Suppress("NOTHING_TO_INLINE")

package dev.patrickgold.florisboard.lib.kotlin

import android.icu.text.BreakIterator
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorCache
import dev.patrickgold.florisboard.lib.FlorisLocale
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun String.lowercase(locale: FlorisLocale): String = this.lowercase(locale.base)

inline fun String.uppercase(locale: FlorisLocale): String = this.uppercase(locale.base)

private const val CURLY_ARG_OPEN = '{'
private const val CURLY_ARG_CLOSE = '}'

typealias CurlyArg = Pair<String, Any?>

fun String.curlyFormat(argValueFactory: (argName: String) -> String?): String {
    contract {
        callsInPlace(argValueFactory, InvocationKind.UNKNOWN)
    }
    val sb = StringBuilder(this)
    var curlyOpenIndex = sb.indexOf(CURLY_ARG_OPEN)
    while (curlyOpenIndex >= 0) {
        val nextCurlyOpenIndex = sb.indexOf(CURLY_ARG_OPEN, curlyOpenIndex + 1)
        val nextCurlyCloseIndex = sb.indexOf(CURLY_ARG_CLOSE, curlyOpenIndex + 1)
        if (nextCurlyCloseIndex < 0) break
        if (nextCurlyOpenIndex in 0 until nextCurlyCloseIndex) {
            curlyOpenIndex = nextCurlyOpenIndex
        }
        val argName = sb.substring(curlyOpenIndex + 1, nextCurlyCloseIndex)
        val argValue = argValueFactory(argName)
        if (argValue != null) {
            sb.replace(curlyOpenIndex, nextCurlyCloseIndex + 1, argValue)
        }
        curlyOpenIndex = sb.indexOf(CURLY_ARG_OPEN, curlyOpenIndex + 1)
    }
    return sb.toString()
}

fun String.curlyFormat(vararg args: CurlyArg): String {
    return this.curlyFormat(args.asList())
}

fun String.curlyFormat(args: List<CurlyArg>): String {
    if (args.isEmpty()) return this
    val sb = StringBuilder(this)
    for ((n, arg) in args.withIndex()) {
        val (argName, argValue) = arg
        sb.formatCurlyArg(n.toString(), argValue)
        sb.formatCurlyArg(argName, argValue)
    }
    return sb.toString()
}

private fun StringBuilder.formatCurlyArg(name: String, value: Any?) {
    if (name.isBlank()) return
    val spec = "$CURLY_ARG_OPEN$name$CURLY_ARG_CLOSE"
    var index = this.lastIndexOf(spec)
    while (index >= 0) {
        val start = index
        val end = index + spec.length
        this.replace(start, end, value.toString())
        index = this.lastIndexOf(spec)
    }
}

suspend fun String.measureUChars(
    numUnicodeChars: Int,
    locale: FlorisLocale = FlorisLocale.default(),
): Int {
    return BreakIteratorCache.character(locale) {
        it.setText(this)
        val start = it.first()
        var end: Int
        var n = 0
        do {
            end = it.next()
        } while (end != BreakIterator.DONE && ++n < numUnicodeChars)
        (if (end == BreakIterator.DONE) this.length else end) - start
    }.coerceIn(0, this.length)
}

suspend fun String.measureLastUChars(
    numUnicodeChars: Int,
    locale: FlorisLocale = FlorisLocale.default(),
): Int {
    return BreakIteratorCache.character(locale) {
        it.setText(this)
        val end = it.last()
        var start: Int
        var n = 0
        do {
            start = it.previous()
        } while (start != BreakIterator.DONE && ++n < numUnicodeChars)
        end - (if (start == BreakIterator.DONE) 0 else start)
    }.coerceIn(0, this.length)
}

suspend fun String.measureUWords(
    numUnicodeWords: Int,
    locale: FlorisLocale = FlorisLocale.default(),
): Int {
    return BreakIteratorCache.word(locale) {
        it.setText(this)
        val start = it.first()
        var end: Int
        var n = 0
        do {
            end = it.next()
        } while (end != BreakIterator.DONE && ++n < numUnicodeWords)
        (if (end == BreakIterator.DONE) this.length else end) - start
    }.coerceIn(0, this.length)
}

suspend fun String.measureLastUWords(
    numUnicodeWords: Int,
    locale: FlorisLocale = FlorisLocale.default(),
): Int {
    return BreakIteratorCache.word(locale) {
        it.setText(this)
        val end = it.last()
        var start: Int
        var n = 0
        do {
            start = it.previous()
        } while (start != BreakIterator.DONE && ++n < numUnicodeWords)
        end - (if (start == BreakIterator.DONE) 0 else start)
    }.coerceIn(0, this.length)
}
