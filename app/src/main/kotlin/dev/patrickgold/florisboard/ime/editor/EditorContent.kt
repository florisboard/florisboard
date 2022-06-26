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

package dev.patrickgold.florisboard.ime.editor

import dev.patrickgold.florisboard.lib.kotlin.safeSubstring

/**
 * A snapshot window of an input editor content around the selection/cursor.
 *
 * @property text The raw text of the editor content. May be the full text or only a partial view.
 * @property offset The offset of the whole editor content snapshot. `-1` indicates the value is unknown.
 * @property localSelection The selection reported by the editor, without [offset] included.
 * @property localComposing The composing region for the editor, without [offset] included.
 * @property localCurrentWord The current word for the editor (typically the same as [localComposing]), without
 *  [offset] included.
 */
data class EditorContent(
    val text: String,
    val offset: Int,
    val localSelection: EditorRange,
    val localComposing: EditorRange,
    val localCurrentWord: EditorRange,
) {
    val textBeforeSelection: String
        get() = if (localSelection.isValid) text.safeSubstring(0, localSelection.start) else ""

    val selectedText: String
        get() = if (localSelection.isValid) text.safeSubstring(localSelection.start, localSelection.end) else ""

    val textAfterSelection: String
        get() = if (localSelection.isValid) text.safeSubstring(localSelection.end) else ""

    val selection: EditorRange
        get() = if (offset > 0) localSelection.translatedBy(offset) else localSelection

    val composing: EditorRange
        get() = if (offset > 0) localComposing.translatedBy(offset) else localComposing

    val composingText: String
        get() = if (localComposing.isValid) text.safeSubstring(localComposing.start, localComposing.end) else ""

    val currentWord: EditorRange
        get() = if (offset > 0) localCurrentWord.translatedBy(offset) else localCurrentWord

    val currentWordText: String
        get() = if (localCurrentWord.isValid) text.safeSubstring(localCurrentWord.start, localCurrentWord.end) else ""

    companion object {
        val Unspecified =
            EditorContent("", -1, EditorRange.Unspecified, EditorRange.Unspecified, EditorRange.Unspecified)

        fun selectionOnly(selection: EditorRange) =
            EditorContent("", -1, selection, EditorRange.Unspecified, EditorRange.Unspecified)
    }
}
