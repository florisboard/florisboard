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

import android.content.Context
import android.icu.text.BreakIterator
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorCache
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("BlockingMethodInNonBlockingContext")
class EditorInstance(context: Context) {
    companion object {
        private const val NumCharsBeforeCursor: Int = 256
        private const val NumCharsAfterCursor: Int = 128
        private const val NumCharsSafeMargin: Int = 128
    }

    private val subtypeManager by context.subtypeManager()
    private val scope = MainScope()

    private val _activeContent = MutableStateFlow(EditorContent.Unspecified)
    val activeContent = _activeContent.asStateFlow()
    fun activeContent(): EditorContent = activeContent.value

    private val _activeInfo = MutableStateFlow(FlorisEditorInfo.Unspecified)
    val activeInfo = _activeInfo.asStateFlow()
    fun activeInfo(): FlorisEditorInfo = activeInfo.value

    init {
        scope.launch {
            activeContent.collectLatest { content ->
                if (activeInfo().isRichInputEditor) {
                    val ic = currentInputConnection() ?: return@collectLatest
                    if (content.composing.isValid) {
                        ic.setComposingRegion(content.composing.start, content.composing.end)
                    } else {
                        ic.finishComposingText()
                    }
                }
            }
        }
    }

    private fun currentInputConnection() = FlorisImeService.currentInputConnection()

    fun initialize(editorInfo: FlorisEditorInfo) {
        _activeInfo.value = editorInfo
        val selection = editorInfo.initialSelection
        if (selection.isNotValid || editorInfo.isRawInputEditor) {
            _activeContent.value = EditorContent.Unspecified
            return
        }

        // Get Text
        val textBeforeSelection = editorInfo.getInitialTextBeforeCursor(NumCharsBeforeCursor) ?: ""
        val textAfterSelection = editorInfo.getInitialTextAfterCursor(NumCharsAfterCursor) ?: ""
        val selectedText = editorInfo.getInitialSelectedText() ?: ""

        scope.launch { initializeInternal(selection, textBeforeSelection, textAfterSelection, selectedText) }
    }

    fun update(selection: EditorRange) {
        val ic = currentInputConnection()
        if (ic == null || selection.isNotValid) {
            return reset()
        }

        // Get Text
        val textBeforeSelection = ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: ""
        val textAfterSelection = ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = ic.getSelectedText(0) ?: ""

        scope.launch { initializeInternal(selection, textBeforeSelection, textAfterSelection, selectedText) }
    }

    private suspend fun initializeInternal(
        selection: EditorRange,
        textBeforeSelection: CharSequence,
        textAfterSelection: CharSequence,
        selectedText: CharSequence,
    ) {
        // Calculate offset and local selection
        val offset = selection.start - textBeforeSelection.length
        val localSelection = EditorRange(
            start = textBeforeSelection.length,
            end = textBeforeSelection.length + selectedText.length,
        )

        // Check consistency and exit if necessary
        if (offset < 0 || selection.translatedBy(-offset) != localSelection) {
            return reset()
        }

        // Determine local composing word range, if any
        val localComposing = if (localSelection.isCursorMode && textBeforeSelection.isNotEmpty()) {
            determineLocalComposing(textBeforeSelection)
        } else {
            EditorRange.Unspecified
        }

        // Build and publish text and content
        val text = buildString {
            append(textBeforeSelection)
            append(selectedText)
            append(textAfterSelection)
            toString()
        }
        _activeContent.value = EditorContent(text, offset, localSelection, localComposing)
    }

    private suspend fun determineLocalComposing(textBeforeSelection: CharSequence): EditorRange {
        return BreakIteratorCache.word(subtypeManager.activeSubtype().primaryLocale) {
            it.setText(textBeforeSelection.toString())
            val end = it.last()
            val isWord = it.ruleStatus != BreakIterator.WORD_NONE
            if (isWord) {
                val start = it.previous()
                EditorRange(start, end)
            } else {
                EditorRange.Unspecified
            }
        }
    }

    fun reset() {
        _activeContent.value = EditorContent.Unspecified
        _activeInfo.value = FlorisEditorInfo.Unspecified
    }


}
