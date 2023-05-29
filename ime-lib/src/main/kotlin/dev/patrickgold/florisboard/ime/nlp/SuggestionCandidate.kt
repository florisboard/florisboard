/*
 * Copyright (C) 2023 Patrick Goldinger
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

import kotlinx.serialization.Serializable

/**
 * Interface for a candidate item, which is returned by a suggestion provider and used by the UI logic to render
 * the candidate row.
 */
interface SuggestionCandidate {
    /**
     * Required primary text of a candidate item, must be non-null and non-blank. The value of this property will
     * be committed to the target editor when the user clicks on this candidate item (either replacing the current
     * word or inserting after the cursor if there is no current word).
     *
     * In the UI it will be shown as the main label of a candidate item. Long texts that don't fit the maximum
     * candidate item width may be shortened and ellipsized.
     */
    val text: CharSequence

    /**
     * Optional secondary text of a candidate item, can be used to provide additional context, e.g. for translation
     * or transliteration. Regardless of this property's value it will never be committed to the target editor.
     *
     * In the UI it will be shown below the main label of a candidate item with a smaller font size, if the text
     * is a non-null and non-blank character sequence. Long texts that don't fit the maximum candidate item width
     * may be shortened and ellipsized.
     */
    val secondaryText: CharSequence?

    /**
     * The confidence of this suggestion to be what the user wanted to type. Must be a value between 0.0 and 1.0 (both
     * inclusive), where 0.0 means no confidence and 1.0 means highest confidence. The confidence rating may be used to
     * sort and filter candidates if multiple providers provide suggestions for a single input.
     */
    val confidence: Double

    /**
     * If true, it indicates that this candidate item should be automatically committed to the target editor once the
     * user inserts a non-letter character.
     *
     * In the UI, auto-insert candidates use a visual distinction such as bold font or a different color, depending
     * heavily on the theme the user has set.
     *
     * Only set this property to true if the algorithm has a high confidence that this suggestion is what the user
     * wanted to type.
     */
    val isEligibleForAutoCommit: Boolean

    /**
     * If true, it indicates that this candidate item should be user removable (by long-pressing). This flag should
     * only be set if it actually makes sense for this type of candidate to be removable and if the linked source
     * provider supports this action.
     */
    val isEligibleForUserRemoval: Boolean

    /**
     * Optional icon ID for showing an icon on the start of the candidate item. Mainly used for special suggestions
     * such as clipboard, word suggestions should not use this property. Do not provide an invalid drawable ID, any
     * non-null drawable ID that does not exist will result in an unhandled crash.
     *
     * In the UI, if the ID is non-null, it will be shown to the start of the main label and scaled accordingly.
     * The color of the icon is entirely decided by the theme of the user. Icons that are monochrome work best.
     */
    val iconId: Int?

    /**
     * The source provider of this candidate. Is used for several callbacks for training, blacklisting of candidates on
     * user-request, and so on. If null, it means that the source provider is unknown or does not want to receive
     * callbacks.
     */
    val sourceProvider: SuggestionProvider?
}

/**
 * Default implementation for a word candidate (autocorrect and next/current word suggestion).
 *
 * @see SuggestionCandidate
 */
@Serializable
data class WordSuggestionCandidate(
    override val text: CharSequence,
    override val secondaryText: CharSequence? = null,
    override val confidence: Double = 0.0,
    override val isEligibleForAutoCommit: Boolean = false,
    override val isEligibleForUserRemoval: Boolean = true,
) : SuggestionCandidate {
    override val iconId: Int? = null
    override var sourceProvider: SuggestionProvider? = null
}
