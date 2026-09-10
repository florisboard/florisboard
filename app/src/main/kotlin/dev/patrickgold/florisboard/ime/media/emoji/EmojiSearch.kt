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

package dev.patrickgold.florisboard.ime.media.emoji

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

suspend fun List<Emoji>.searchByInput(
    query: String,
    limit: Long,
): List<Emoji> = searchByInput(query, limit) { it }

suspend fun <R> List<Emoji>.searchByInput(
    query: String,
    limit: Long,
    transform: (Emoji) -> R,
): List<R> {
    return withContext(Dispatchers.Default) {
        parallelStream()
            .map { emoji ->
                val nameWeight = emoji.name.containsWeighted(query, ignoreCase = true)
                val keywordWeight = emoji.keywords
                    .any { it.contains(query, ignoreCase = true) }
                    .let { if (it) 1.0 else 0.0 }
                emoji to (nameWeight * 0.7 + keywordWeight * 0.3)
            }
            .sorted { (_, a), (_, b) -> b.compareTo(a) }
            .limit(limit)
            .filter { (_, a) -> a > 0 }
            .map { (emoji, _) -> transform(emoji) }
            .collect(Collectors.toList())
    }
}

private fun String.containsWeighted(other: String, ignoreCase: Boolean = false): Double = let { str ->
    if (str.contains(other, ignoreCase = ignoreCase)) {
        other.length.toDouble() / str.length.toDouble()
    } else {
        0.0
    }
}
