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
import android.view.inputmethod.InputConnection
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorCache
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("BlockingMethodInNonBlockingContext")
abstract class AbstractEditorInstance(context: Context) {
    companion object {
        private const val NumCharsBeforeCursor: Int = 256
        private const val NumCharsAfterCursor: Int = 128
        private const val NumCharsSafeMargin: Int = 128

        private const val CursorUpdateAll: Int =
            InputConnection.CURSOR_UPDATE_MONITOR or InputConnection.CURSOR_UPDATE_IMMEDIATE
        private const val CursorUpdateNone: Int = 0
    }

    private val subtypeManager by context.subtypeManager()
    private val scope = MainScope()

    private val _activeContentFlow = MutableStateFlow(EditorContent.Unspecified)
    val activeContentFlow = _activeContentFlow.asStateFlow()
    val activeContent: EditorContent = _activeContentFlow.value

    private val _activeCursorCapsModeFlow = MutableStateFlow(InputAttributes.CapsMode.NONE)
    val activeCursorCapsModeFlow = _activeCursorCapsModeFlow.asStateFlow()
    val activeCursorCapsMode: InputAttributes.CapsMode = _activeCursorCapsModeFlow.value

    private val _activeInfoFlow = MutableStateFlow(FlorisEditorInfo.Unspecified)
    val activeInfoFlow = _activeInfoFlow.asStateFlow()
    val activeInfo: FlorisEditorInfo = _activeInfoFlow.value

    init {
        scope.launch {
            activeContentFlow.collectLatest { content ->
                if (activeInfo.isRichInputEditor) {
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

    open fun handleStartInput(editorInfo: FlorisEditorInfo) {
        _activeInfoFlow.value = editorInfo
        _activeContentFlow.value = EditorContent.Unspecified
        _activeCursorCapsModeFlow.value = editorInfo.initialCapsMode
        currentInputConnection()?.requestCursorUpdates(CursorUpdateAll)
    }

    open fun handleStartInputView(editorInfo: FlorisEditorInfo) {
        _activeInfoFlow.value = editorInfo
        val selection = editorInfo.initialSelection
        if (selection.isNotValid || editorInfo.isRawInputEditor) {
            _activeContentFlow.value = EditorContent.Unspecified
            _activeCursorCapsModeFlow.value = InputAttributes.CapsMode.NONE
            return
        }
        _activeCursorCapsModeFlow.value = editorInfo.initialCapsMode

        // Get Text
        val textBeforeSelection = editorInfo.getInitialTextBeforeCursor(NumCharsBeforeCursor) ?: ""
        val textAfterSelection = editorInfo.getInitialTextAfterCursor(NumCharsAfterCursor) ?: ""
        val selectedText = editorInfo.getInitialSelectedText() ?: ""

        scope.launch {
            initialize(selection, textBeforeSelection, textAfterSelection, selectedText).also { content ->
                setComposingRegion(content.composing)
            }
        }
    }

    open fun handleSelectionUpdate(oldSelection: EditorRange, newSelection: EditorRange, composing: EditorRange) {
        val ic = currentInputConnection()
        if (ic == null || newSelection.isNotValid) {
            reset()
            return
        }
        _activeCursorCapsModeFlow.value = InputAttributes.CapsMode.fromFlags(
            ic.getCursorCapsMode(activeInfo.inputAttributes.raw)
        )

        // Get Text
        val textBeforeSelection = ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: ""
        val textAfterSelection = ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = ic.getSelectedText(0) ?: ""

        scope.launch {
            initialize(newSelection, textBeforeSelection, textAfterSelection, selectedText).also { content ->
                if (content.composing != composing) {
                    setComposingRegion(content.composing)
                }
            }
        }
    }

    open fun handleFinishInputView() {
        reset()
    }

    open fun handleFinishInput() {
        reset()
        currentInputConnection()?.requestCursorUpdates(CursorUpdateNone)
    }

    protected open fun reset() {
        _activeContentFlow.value = EditorContent.Unspecified
        _activeCursorCapsModeFlow.value = InputAttributes.CapsMode.NONE
        _activeInfoFlow.value = FlorisEditorInfo.Unspecified
    }

    private suspend fun initialize(
        selection: EditorRange,
        textBeforeSelection: CharSequence,
        textAfterSelection: CharSequence,
        selectedText: CharSequence,
    ): EditorContent {
        // Calculate offset and local selection
        val offset = selection.start - textBeforeSelection.length
        val localSelection = EditorRange(
            start = textBeforeSelection.length,
            end = textBeforeSelection.length + selectedText.length,
        )

        // Check consistency and exit if necessary
        if (offset < 0 || selection.translatedBy(-offset) != localSelection) {
            reset()
            return EditorContent.Unspecified
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
        val content = EditorContent(text, offset, localSelection, localComposing)
        _activeContentFlow.value = content
        return content
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

    protected open fun setComposingRegion(composing: EditorRange) {
        val ic = currentInputConnection() ?: return
        if (composing.isValid) {
            ic.setComposingRegion(composing.start, composing.end)
        } else {
            ic.finishComposingText()
        }
    }
}
