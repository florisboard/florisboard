/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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
 *       "symbolsPrecedingAutoSpace": ".,?‽!\"&%)]}»",
 *       "symbolsFollowingAutoSpace": "",
 *       "symbolsPrecedingPhantomSpace": ".,;:?‽!&%)]}»©®™",
 *       "symbolsFollowingPhantomSpace": "¿⸘¡([{",
 *       "symbolsTerminatingSentence": ".?‽!"
 *     }
 *   ]
 * ```
 *
 * For auto-spacing and phantom-space to consider inserting a space by itself, it must find the symbol in the
 * corresponding rule and may have additional conditions to meet before triggering an automatic space.
 *
 * @property id The ID of this punctuation rule. Can be any ID as long as it follows the ID syntax rule, conventionally
 *  though this is either "default" or the language tag of the locale this punctuation rule is meant for.
 * @property label The label of this punctuation rule, for showing this rule in the UI. Defaults to [id] if not set.
 * @property authors List of authors, not relevant for punctuation rules though. Can and should be omitted.
 * @property symbolsPrecedingAutoSpace List of characters considered to be valid symbols before an auto-space insertion.
 *  Each character is considered a separate symbol.
 * @property symbolsPrecedingAutoSpace List of characters considered to be valid symbols after an auto-space insertion.
 *  Each character is considered a separate symbol.
 * @property symbolsPrecedingPhantomSpace List of characters considered to be valid symbols before a phantom space.
 *  Each character is considered a separate symbol.
 * @property symbolsFollowingPhantomSpace List of characters considered to be valid symbols after a phantom space.
 *  Each character is considered a separate symbol.
 */
@Serializable
data class PunctuationRule(
    override val id: String,
    override val label: String = id,
    override val authors: List<String> = listOf("unspecified"),
    val symbolsPrecedingAutoSpace: String,
    val symbolsFollowingAutoSpace: String,
    val symbolsPrecedingPhantomSpace: String,
    val symbolsFollowingPhantomSpace: String,
    val symbolsTerminatingSentence: String,
) : ExtensionComponent {

    companion object {
        /** Fallback rule which does bare bone matching for spaces in case a proper punctuation rule is not found. */
        val Fallback = PunctuationRule(
            id = "fallback",
            label = "Fallback",
            symbolsPrecedingAutoSpace = ".,?!",
            symbolsFollowingAutoSpace = "",
            symbolsPrecedingPhantomSpace = ".,?!",
            symbolsFollowingPhantomSpace = "",
            symbolsTerminatingSentence = ".?!",
        )
    }
}
