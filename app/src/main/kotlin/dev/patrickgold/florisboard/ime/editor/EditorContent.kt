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

package dev.patrickgold.florisboard.ime.editor

import org.florisboard.lib.kotlin.safeSubstring

/**
 * A snapshot window of an input editor content around the selection/cursor.
 *
 * @property text The raw text of the editor content. May be the full text or only a partial view.
 * @property offset The offset of the whole editor content snapshot. `-1` indicates the value is unknown.
 * @property localSelection The selection reported by the editor, without [offset] included.
 * @property localComposing The composing region for the editor, without [offset] included.
 * @property localCurrentWord The current word for the editor (typically the same as [localComposing]), without [offset]
 *     included.
 */
data class EditorContent(
    val text: String,
    val offset: Int,
    val localSelection: EditorRange,
    val localComposing: EditorRange,
    val localCurrentWord: EditorRange,
) {
    /**
     * The text before the selection as a new string. This may be the whole text before the selection or only a subset,
     * depending on the cache and app configuration. Can also be empty for raw editors or if there is no text before
     * the selection.
     */
    val textBeforeSelection: String
        get() = if (localSelection.isValid) text.safeSubstring(0, localSelection.start) else ""

    /**
     * The selected text as a new string. This is always either the entire selected text or an empty string.
     */
    val selectedText: String
        get() = if (localSelection.isValid) text.safeSubstring(localSelection.start, localSelection.end) else ""

    /**
     * The text after the selection as a new string. This may be the whole text after the selection or only a subset,
     * depending on the cache and app configuration. Can also be empty for raw editors or if there is no text after the
     * selection.
     */
    val textAfterSelection: String
        get() = if (localSelection.isValid) text.safeSubstring(localSelection.end) else ""

    /**
     * The selection reported by the editor, with [offset] included.
     */
    val selection: EditorRange
        get() = if (offset > 0) localSelection.translatedBy(offset) else localSelection

    /**
     * The composing region for the editor, with [offset] included. May be intentionally invalid even if there is a
     * current word if the user has requested to disable the composing region.
     */
    val composing: EditorRange
        get() = if (offset > 0) localComposing.translatedBy(offset) else localComposing

    /**
     * The composing region text as a new string. May be intentionally empty even if there is a current word if the
     * user has requested to disable the composing region. This is always either the entire composing text or an empty
     * string.
     */
    val composingText: String
        get() = if (localComposing.isValid) text.safeSubstring(localComposing.start, localComposing.end) else ""

    /**
     * The current word for the editor (typically the same as [localComposing]), with [offset] included.
     */
    val currentWord: EditorRange
        get() = if (offset > 0) localCurrentWord.translatedBy(offset) else localCurrentWord

    /**
     * The current word for the editor (typically the same as [localComposing]) as a new string. This is always either
     * the current word or an empty string.
     */
    val currentWordText: String
        get() = if (localCurrentWord.isValid) text.safeSubstring(localCurrentWord.start, localCurrentWord.end) else ""

    companion object {
        /**
         * Default editor content which indicates an unspecified content. This is used for raw editors or if there is
         * an error in the communication between the keyboard and the app.
         */
        val Unspecified =
            EditorContent("", -1, EditorRange.Unspecified, EditorRange.Unspecified, EditorRange.Unspecified)

        /**
         * Allows to instantiate a selection-only content, primarily used for mass-selection event handling.
         */
        fun selectionOnly(selection: EditorRange) =
            EditorContent("", -1, selection, EditorRange.Unspecified, EditorRange.Unspecified)
    }
}
