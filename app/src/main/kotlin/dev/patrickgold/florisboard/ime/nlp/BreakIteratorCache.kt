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

package dev.patrickgold.florisboard.ime.nlp

import android.icu.text.BreakIterator
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.kotlin.GuardedByLock
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import io.github.reactivecircus.cache4k.Cache

object BreakIteratorCache {
    @PublishedApi
    internal val charInstances = Cache.Builder().build<FlorisLocale, GuardedByLock<BreakIterator>>()

    @PublishedApi
    internal val wordInstances = Cache.Builder().build<FlorisLocale, GuardedByLock<BreakIterator>>()

    suspend inline fun <R> character(locale: FlorisLocale, crossinline action: (BreakIterator) -> R): R {
        val instance = charInstances.get(locale) { guardedByLock { BreakIterator.getCharacterInstance(locale.base) } }
        return instance.withLock { action(it) }
    }

    suspend inline fun <R> word(locale: FlorisLocale, crossinline action: (BreakIterator) -> R): R {
        val instance = wordInstances.get(locale) { guardedByLock { BreakIterator.getWordInstance(locale.base) } }
        return instance.withLock(null, action)
    }
}
