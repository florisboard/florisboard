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
    inline var activeContent: EditorContent
        get() = activeContentFlow.value
        private set(v) { _activeContentFlow.value = v }

    private val _activeCursorCapsModeFlow = MutableStateFlow(InputAttributes.CapsMode.NONE)
    val activeCursorCapsModeFlow = _activeCursorCapsModeFlow.asStateFlow()
    inline var activeCursorCapsMode: InputAttributes.CapsMode
        get() = activeCursorCapsModeFlow.value
        private set(v) { _activeCursorCapsModeFlow.value = v }

    private val _activeInfoFlow = MutableStateFlow(FlorisEditorInfo.Unspecified)
    val activeInfoFlow = _activeInfoFlow.asStateFlow()
    inline var activeInfo: FlorisEditorInfo
        get() = activeInfoFlow.value
        private set(v) { _activeInfoFlow.value = v }

    private fun currentInputConnection() = FlorisImeService.currentInputConnection()

    open fun handleStartInput(editorInfo: FlorisEditorInfo) {
        activeInfo = editorInfo
        activeCursorCapsMode = editorInfo.initialCapsMode
        activeContent = EditorContent.Unspecified
        currentInputConnection()?.requestCursorUpdates(CursorUpdateAll)
    }

    open fun handleStartInputView(editorInfo: FlorisEditorInfo) {
        activeInfo = editorInfo
        val selection = editorInfo.initialSelection
        if (selection.isNotValid || editorInfo.isRawInputEditor) {
            activeCursorCapsMode = InputAttributes.CapsMode.NONE
            activeContent = EditorContent.Unspecified
            return
        }
        activeCursorCapsMode = editorInfo.initialCapsMode

        // Get Text
        val textBeforeSelection = editorInfo.getInitialTextBeforeCursor(NumCharsBeforeCursor) ?: ""
        val textAfterSelection = editorInfo.getInitialTextAfterCursor(NumCharsAfterCursor) ?: ""
        val selectedText = editorInfo.getInitialSelectedText() ?: ""

        scope.launch {
            initialize(editorInfo, selection, textBeforeSelection, textAfterSelection, selectedText).also { content ->
                setComposingRegion(content.composing)
            }
        }
    }

    open fun handleSelectionUpdate(oldSelection: EditorRange, newSelection: EditorRange, composing: EditorRange) {
        val editorInfo = activeInfo
        if (newSelection.isNotValid || editorInfo.isRawInputEditor) {
            activeCursorCapsMode = InputAttributes.CapsMode.NONE
            activeContent = EditorContent.Unspecified
            return
        }
        val ic = currentInputConnection() ?: return
        activeCursorCapsMode = InputAttributes.CapsMode.fromFlags(
            ic.getCursorCapsMode(editorInfo.inputAttributes.raw)
        )

        // Get Text
        val textBeforeSelection = if (newSelection.start > 0) ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: "" else ""
        val textAfterSelection = ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = if (newSelection.isSelectionMode) ic.getSelectedText(0) ?: "" else ""

        scope.launch {
            initialize(editorInfo, newSelection, textBeforeSelection, textAfterSelection, selectedText).also { content ->
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
        activeInfo = FlorisEditorInfo.Unspecified
        activeCursorCapsMode = InputAttributes.CapsMode.NONE
        activeContent = EditorContent.Unspecified
    }

    private suspend fun initialize(
        editorInfo: FlorisEditorInfo,
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
            activeContent = EditorContent.Unspecified
            return EditorContent.Unspecified
        }

        // Determine local composing word range, if any
        val localComposing = if (shouldDetermineComposingRegion(editorInfo) && localSelection.isCursorMode && textBeforeSelection.isNotEmpty()) {
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
        activeContent = content
        return content
    }

    protected open fun shouldDetermineComposingRegion(editorInfo: FlorisEditorInfo): Boolean {
        return editorInfo.isRichInputEditor
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

    private fun setComposingRegion(composing: EditorRange) {
        val ic = currentInputConnection() ?: return
        if (composing.isValid) {
            ic.setComposingRegion(composing.start, composing.end)
        } else {
            ic.finishComposingText()
        }
    }

    protected fun setSelection(selection: EditorRange): Boolean {
        if (activeInfo.isRawInputEditor) return false
        if (activeContent.selection == selection) return true
        val ic = currentInputConnection() ?: return false
        return ic.setSelection(selection.start, selection.end)
    }
}
