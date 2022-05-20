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

import dev.patrickgold.florisboard.lib.ext.ExtensionComponent
import dev.patrickgold.florisboard.lib.kotlin.RegexSerializer
import kotlinx.serialization.Serializable

/**
 * Data class which describes a punctuation rule for auto-spacing and phantom-space determination. Punctuation rules
 * are defined in keyboard extension's manifest file, there can be multiple ones defined, if necessary.
 *
 * Here's an example of a properly configured punctuation rule in a keyboard extension:
 *
 * ```json
 *   "punctuationRules": [
 *     {
 *       "id": "default",
 *       "label": "Default",
 *       "symbolsPrecedingSpace": ".*[.,;:!?‽&%)\\]}»©®™\\p{L}0-9]",
 *       "symbolsFollowingSpace": "[\\p{L}0-9].*"
 *     }
 *   ]
 * ```
 *
 * For auto-spacing and phantom-space to consider inserting a space by itself, both [symbolsPrecedingSpace] (which gets
 * a small subset of the text before the cursor) and [symbolsFollowingSpace] (which gets the character or word to
 * insert) must match the entire string they get.
 *
 * @property id The ID of this punctuation rule. Can be any ID as long as it follows the ID syntax rule, conventionally
 *  though this is either "default" or the language tag of the locale this punctuation rule is meant for.
 * @property label The label of this punctuation rule, for showing this rule in the UI. Defaults to [id] if not set.
 * @property authors List of authors, not relevant for punctuation rules though. Can and should be omitted.
 * @property symbolsPrecedingSpace Regex which checks if the text before the cursor indicates that a space should
 *  follow. This can be a simple regex, which matches a list of single symbols, or a more complex one, which matches
 *  symbols based on previous characters. This regex must match the entire text, partial matches are not allowed.
 *  This means that the regex should begin with a match all expression (`.*`), to match preceding unrelated symbols and
 *  characters.
 * @property symbolsFollowingSpace Regex which checks if the text to insert after the cursor indicates that a space
 *  should precede. This can be a simple regex, which matches a list of single symbols, or a more complex one, which
 *  matches symbols based on next characters. This regex must match the entire text, partial matches are not allowed.
 *  This means that the regex should end with a match all expression (`.*`), to match following unrelated symbols and
 *  characters.
 */
@Serializable
data class PunctuationRule(
    override val id: String,
    override val label: String = id,
    override val authors: List<String> = listOf("unspecified"),
    val symbolsPrecedingSpace: @Serializable(with = RegexSerializer::class) Regex,
    val symbolsFollowingSpace: @Serializable(with = RegexSerializer::class) Regex,
) : ExtensionComponent {
    companion object {
        /** Fallback rule which does bare bone matching for spaces in case a proper punctuation rule is not found. */
        val Fallback = PunctuationRule(
            id = "fallback",
            label = "Fallback",
            symbolsPrecedingSpace = """[^\s]""".toRegex(),
            symbolsFollowingSpace = """\p{L}[0-9]""".toRegex(),
        )
    }
}
