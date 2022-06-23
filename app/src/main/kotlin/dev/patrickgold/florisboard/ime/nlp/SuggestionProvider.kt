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

import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.lib.util.NetworkUtils

interface SuggestionProvider {
    /**
     * Interface for a candidate item, which is returned by a suggestion provider and used by the UI logic to render
     * the candidate row.
     */
    interface Candidate {
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
         * If true, it indicates that this candidate item will be automatically committed to the target editor once the
         * user inserts a non-letter character.
         *
         * In the UI, auto-insert candidates use a visual distinction such as bold font or a different color, depending
         * heavily on the theme the user has set.
         *
         * Only set this property to true if the algorithm has a high confidence that this suggestion is what the user
         * wanted to type.
         */
        val isAutoCommit: Boolean

        /**
         * Optional icon ID for showing an icon on the start of the candidate item. Mainly used for special suggestions
         * such as clipboard, word suggestions should not use this property. Do not provide an invalid drawable ID, any
         * non-null drawable ID that does not exist will result in an unhandled crash.
         *
         * In the UI, if the ID is non-null, it will be shown to the start of the main label and scaled accordingly.
         * The color of the icon is entirely decided by the theme of the user. Icons that are monochrome work best.
         */
        val iconId: Int?
    }

    /**
     * Default implementation for a word candidate (autocorrect and next/current word suggestion).
     *
     * @see Candidate
     */
    class WordCandidate(
        override val text: CharSequence,
        override val secondaryText: CharSequence? = null,
        override val isAutoCommit: Boolean = false,
    ) : Candidate {
        override val iconId: Int? = null
    }

    /**
     * Default implementation for a clipboard candidate. Should generally not be used by a suggestion provider.
     *
     * @see Candidate
     */
    class ClipboardCandidate(val clipboardItem: ClipboardItem) : Candidate {
        override val text: CharSequence = clipboardItem.stringRepresentation()

        override val secondaryText: CharSequence? = null

        override val isAutoCommit: Boolean = false

        override val iconId: Int = when (clipboardItem.type) {
            ItemType.TEXT -> when {
                NetworkUtils.isUrl(text) -> R.drawable.ic_link
                NetworkUtils.isEmailAddress(text) -> R.drawable.ic_email
                else -> R.drawable.ic_assignment
            }
            ItemType.IMAGE -> R.drawable.ic_image
            ItemType.VIDEO -> R.drawable.ic_videocam
        }
    }
}
