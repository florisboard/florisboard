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
import dev.patrickgold.florisboard.lib.kotlin.measureLastUChars
import dev.patrickgold.florisboard.lib.kotlin.measureUChars
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("BlockingMethodInNonBlockingContext")
abstract class AbstractEditorInstance(context: Context) {
    companion object {
        private const val NumCharsBeforeCursor: Int = 256
        private const val NumCharsAfterCursor: Int = 128
        private const val NumCharsSafeMarginBeforeCursor: Int = 128
        private const val NumCharsSafeMarginAfterCursor: Int = 0

        private const val CursorUpdateAll: Int =
            InputConnection.CURSOR_UPDATE_MONITOR or InputConnection.CURSOR_UPDATE_IMMEDIATE
        private const val CursorUpdateNone: Int = 0
    }

    private val subtypeManager by context.subtypeManager()
    private val scope = MainScope()

    private val batchEditState = AtomicBoolean(false)
    var expectedContent: EditorContent? = null

    private val _activeContentFlow = MutableStateFlow(EditorContent.Unspecified)
    val activeContentFlow = _activeContentFlow.asStateFlow()
    inline var activeContent: EditorContent
        get() = activeContentFlow.value
        private set(v) {
            _activeContentFlow.value = v
        }

    private val _activeCursorCapsModeFlow = MutableStateFlow(InputAttributes.CapsMode.NONE)
    val activeCursorCapsModeFlow = _activeCursorCapsModeFlow.asStateFlow()
    inline var activeCursorCapsMode: InputAttributes.CapsMode
        get() = activeCursorCapsModeFlow.value
        private set(v) {
            _activeCursorCapsModeFlow.value = v
        }

    private val _activeInfoFlow = MutableStateFlow(FlorisEditorInfo.Unspecified)
    val activeInfoFlow = _activeInfoFlow.asStateFlow()
    inline var activeInfo: FlorisEditorInfo
        get() = activeInfoFlow.value
        private set(v) {
            _activeInfoFlow.value = v
        }

    private fun currentInputConnection() = FlorisImeService.currentInputConnection()

    open fun handleStartInput(editorInfo: FlorisEditorInfo) {
        activeInfo = editorInfo
        activeCursorCapsMode = editorInfo.initialCapsMode
        activeContent = EditorContent.Unspecified
        currentInputConnection()?.requestCursorUpdates(CursorUpdateAll)
    }

    open fun handleStartInputView(editorInfo: FlorisEditorInfo) {
        val ic = currentInputConnection()
        activeInfo = editorInfo
        val selection = editorInfo.initialSelection
        if (ic == null || selection.isNotValid || editorInfo.isRawInputEditor) {
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
            activeContent = generateContent(editorInfo,
                selection,
                textBeforeSelection,
                textAfterSelection,
                selectedText).also { content ->
                ic.setComposingRegion(content.composing)
            }
        }
    }

    open fun handleSelectionUpdate(oldSelection: EditorRange, newSelection: EditorRange, composing: EditorRange) {
        val ic = currentInputConnection()
        val editorInfo = activeInfo
        if (ic == null || newSelection.isNotValid || editorInfo.isRawInputEditor) {
            activeCursorCapsMode = InputAttributes.CapsMode.NONE
            activeContent = EditorContent.Unspecified
            return
        }
        activeCursorCapsMode = InputAttributes.CapsMode.fromFlags(
            ic.getCursorCapsMode(editorInfo.inputAttributes.raw)
        )

        val expected = expectedContent
        if (expected != null) {
            expectedContent = null
            if (activeContent.selection == oldSelection && expected.selection == newSelection &&
                expected.composing == composing && expected.textBeforeSelection.length >= NumCharsSafeMarginBeforeCursor.coerceAtMost(
                    expected.selection.start)
            ) {
                activeContent = expected
                return
            }
        }

        // Get Text
        val textBeforeSelection =
            if (newSelection.start > 0) ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: "" else ""
        val textAfterSelection = ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = if (newSelection.isSelectionMode) ic.getSelectedText(0) ?: "" else ""

        scope.launch {
            activeContent = generateContent(editorInfo,
                newSelection,
                textBeforeSelection,
                textAfterSelection,
                selectedText).also { content ->
                if (content.composing != composing) {
                    ic.setComposingRegion(content.composing)
                }
            }
        }
    }

    open fun handleFinishInputView() {
        reset()
    }

    open fun handleFinishInput() {
        reset()
        batchEditState.set(false)
        currentInputConnection()?.requestCursorUpdates(CursorUpdateNone)
    }

    protected open fun reset() {
        activeInfo = FlorisEditorInfo.Unspecified
        activeCursorCapsMode = InputAttributes.CapsMode.NONE
        activeContent = EditorContent.Unspecified
    }

    private suspend fun generateContent(
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
            return EditorContent.Unspecified
        }

        // Determine local composing word range, if any
        val localComposing =
            if (shouldDetermineComposingRegion(editorInfo) && localSelection.isCursorMode && textBeforeSelection.isNotEmpty()) {
                determineLocalComposing(textBeforeSelection)
            } else {
                EditorRange.Unspecified
            }

        // Build and publish text and content
        val text = buildString {
            append(textBeforeSelection)
            append(selectedText)
            append(textAfterSelection)
        }
        return EditorContent(text, offset, localSelection, localComposing)
    }

    private suspend fun EditorContent.generateCopy(
        editorInfo: FlorisEditorInfo = activeInfo,
        selection: EditorRange = this.selection,
        textBeforeSelection: CharSequence = this.textBeforeSelection,
        textAfterSelection: CharSequence = this.textAfterSelection,
        selectedText: CharSequence = this.selectedText,
    ): EditorContent {
        return generateContent(editorInfo, selection, textBeforeSelection, textAfterSelection, selectedText)
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

    private fun InputConnection.setComposingRegion(composing: EditorRange) {
        if (composing.isValid) {
            this.setComposingRegion(composing.start, composing.end)
        } else {
            this.finishComposingText()
        }
    }

    protected fun setSelection(selection: EditorRange): Boolean {
        if (activeInfo.isRawInputEditor) return false
        if (activeContent.selection == selection) return true
        val ic = currentInputConnection() ?: return false
        return ic.setSelection(selection.start, selection.end)
    }

    open fun commitText(text: String): Boolean {
        if (text.isEmpty()) return false
        val ic = currentInputConnection() ?: return false
        val content = expectedContent ?: activeContent
        val selection = content.selection
        ic.beginBatchEdit()
        ic.finishComposingText()
        if (activeInfo.isRawInputEditor) {
            ic.commitText(text, 1)
        } else runBlocking {
            val newSelection = EditorRange(selection.start + text.length, selection.start + text.length)
            val newContent = content.generateCopy(
                editorInfo = activeInfo,
                selection = newSelection,
                textBeforeSelection =
                    if (selection.isSelectionMode) {
                        content.textBeforeSelection
                    } else buildString {
                        append(content.textBeforeSelection)
                        append(text)
                    },
                selectedText = "",
            )
            expectedContent = newContent
            ic.commitText(text, 1)
            if (newContent.composing.isValid) {
                ic.setComposingRegion(newContent.composing)
            }
        }
        ic.endBatchEdit()
        return true
    }

    protected fun deleteUCharsBeforeCursor(n: Int): Boolean {
        val ic = currentInputConnection()
        if (ic == null || n < 1) return false
        val content = expectedContent ?: activeContent
        if (content.selection.start == 0) return true
        val oldTextBeforeSelection = content.textBeforeSelection
        return if (oldTextBeforeSelection.isNotEmpty()) {
            runBlocking {
                val length = oldTextBeforeSelection.measureLastUChars(n, subtypeManager.activeSubtype().primaryLocale)
                val newSelection = content.selection.translatedBy(-length)
                val newContent = content.generateCopy(
                    editorInfo = activeInfo,
                    selection = newSelection,
                    textBeforeSelection = oldTextBeforeSelection.dropLast(length),
                )
                expectedContent = newContent
                ic.beginBatchEdit()
                ic.finishComposingText()
                ic.deleteSurroundingText(length, 0)
                ic.setComposingRegion(newContent.composing)
                ic.endBatchEdit()
                true
            }
        } else {
            ic.deleteSurroundingText(n, 0)
        }
    }

    /**
     * Gets [n] characters before the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get before the cursor. Must be greater than 0 or this
     *  method will fail. This number indicates the number of Unicode chars, so the returned string
     *  length may be greater than n, due to Java char encoding.
     *
     * @return [n] or less characters before the cursor.
     */
    fun getTextBeforeCursor(n: Int): String {
        if (n < 1) return ""
        return runBlocking {
            val text = activeContent.textBeforeSelection
            val length = text.measureLastUChars(n, subtypeManager.activeSubtype().primaryLocale)
            text.takeLast(length)
        }
    }

    /**
     * Gets [n] characters after the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get after the cursor. Must be greater than 0 or this
     *  method will fail. This number indicates the number of Unicode chars, so the returned string
     *  length may be greater than n, due to Java char encoding.
     *
     * @return [n] or less characters after the cursor.
     */
    fun getTextAfterCursor(n: Int): String {
        if (n < 1) return ""
        return runBlocking {
            val text = activeContent.textAfterSelection
            val length = text.measureUChars(n, subtypeManager.activeSubtype().primaryLocale)
            text.take(length)
        }
    }
}
