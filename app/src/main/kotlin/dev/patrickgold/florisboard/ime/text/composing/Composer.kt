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
import kotlinx.serialization.Transient

/**
 * Base interface for a text composer. A composer allows to dynamically transform text, which is especially useful for
 * languages with complicated scripts, where providing one key for each letter is not really possible.
 */
interface Composer {
    val id: String
    val label: String
    val toRead: Int

    /**
     * Requests the composer to provide an action for given [precedingText] and the character [toInsert].
     *
     * @param precedingText The preceding text which is already committed to the target editor field. The string is
     *  typically [toRead] characters long, however it can also be empty or have more characters than [toRead].
     * @param toInsert The character (either single char or single char + diacritics) which are about to be inserted.
     *
     * @return A pair of an integer specifying how many chars from the end to delete from [precedingText] and a string
     *  representing the new transformed character to insert. If the composer does not have an action, `0 to toInsert`
     *  should be returned.
     */
    fun getActions(precedingText: String, toInsert: String): Pair<Int, String>
}

/**
 * Default composer which simply appends any text to insert to the preceding text. Is also used as a fallback.
 */
@Serializable
@SerialName("appender")
object Appender : Composer {
    override val id = "appender"
    override val label = "Appender"
    override val toRead = 0

    override fun getActions(precedingText: String, toInsert: String): Pair<Int, String> {
        return 0 to toInsert
    }
}

@Serializable
@SerialName("with-rules")
class WithRules(
    override val id: String,
    override val label: String,
    val rules: Map<String, String>,
) : Composer {
    override val toRead = (rules.keys.maxOf { it.length } - 1).coerceAtLeast(0)

    @Transient val ruleOrder = rules.keys.toList().sortedBy { it.length }.reversed()

    override fun getActions(precedingText: String, toInsert: String): Pair<Int, String> {
        val str = precedingText + toInsert
        for (key in ruleOrder) {
            if (str.lowercase().endsWith(key)) {
                val value = rules.getValue(key)
                val firstOfKey = str.takeLast(key.length).take(1)
                return (key.length - 1) to (if (firstOfKey.uppercase() == firstOfKey) value.uppercase() else value)
            }
        }
        return 0 to toInsert
    }
}
