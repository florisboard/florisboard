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
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorGroup
import dev.patrickgold.florisboard.ime.text.composing.Composer
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("BlockingMethodInNonBlockingContext")
abstract class AbstractEditorInstance(context: Context) {
    companion object {
        private const val NumCharsBeforeCursor: Int = 256
        private const val NumCharsAfterCursor: Int = 128
        private const val NumCharsSafeMarginBeforeCursor: Int = 128
        //private const val NumCharsSafeMarginAfterCursor: Int = 0

        private const val CursorUpdateAll: Int =
            InputConnection.CURSOR_UPDATE_MONITOR or InputConnection.CURSOR_UPDATE_IMMEDIATE
        private const val CursorUpdateNone: Int = 0
    }

    private val subtypeManager by context.subtypeManager()
    private val scope = MainScope()
    protected val breakIterators = BreakIteratorGroup()

    private val _activeInfoFlow = MutableStateFlow(FlorisEditorInfo.Unspecified)
    val activeInfoFlow = _activeInfoFlow.asStateFlow()
    inline var activeInfo: FlorisEditorInfo
        get() = activeInfoFlow.value
        private set(v) {
            _activeInfoFlow.value = v
        }

    private val _activeCursorCapsModeFlow = MutableStateFlow(InputAttributes.CapsMode.NONE)
    val activeCursorCapsModeFlow = _activeCursorCapsModeFlow.asStateFlow()
    inline var activeCursorCapsMode: InputAttributes.CapsMode
        get() = activeCursorCapsModeFlow.value
        private set(v) {
            _activeCursorCapsModeFlow.value = v
        }

    private val _activeContentFlow = MutableStateFlow(EditorContent.Unspecified)
    val activeContentFlow = _activeContentFlow.asStateFlow()
    inline var activeContent: EditorContent
        get() = expectedContent() ?: activeContentFlow.value
        private set(v) {
            _activeContentFlow.value = v
        }
    private val expectedContentQueue = ExpectedContentQueue()

    fun expectedContent(): EditorContent? {
        return runBlocking { expectedContentQueue.peekNewestOrNull() }
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
        val textBeforeSelection = editorInfo.getInitialTextBeforeCursor(NumCharsBeforeCursor)
            ?: ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: ""
        val textAfterSelection = editorInfo.getInitialTextAfterCursor(NumCharsAfterCursor)
            ?: ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = editorInfo.getInitialSelectedText()
            ?: ic.getSelectedText(0) ?: ""

        scope.launch {
            activeContent = generateContent(
                editorInfo,
                selection,
                textBeforeSelection,
                textAfterSelection,
                selectedText,
            ).also { content ->
                ic.setComposingRegion(content.composing)
            }
        }
    }

    protected fun handleMassSelectionUpdate(newSelection: EditorRange, composing: EditorRange) {
        activeCursorCapsMode = InputAttributes.CapsMode.NONE
        activeContent = EditorContent.selectionOnly(newSelection)
        if (composing.isValid) {
            currentInputConnection()?.setComposingRegion(EditorRange.Unspecified)
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

        val expected = runBlocking {
            expectedContentQueue.popUntilOrNull {
                it.selection == newSelection && it.composing == composing &&
                    it.textBeforeSelection.length >= NumCharsSafeMarginBeforeCursor.coerceAtMost(it.selection.start)
            }
        }
        if (expected != null) {
            activeContent = expected
            return
        }

        // Get Text
        val textBeforeSelection =
            if (newSelection.start > 0) ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: "" else ""
        val textAfterSelection = ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = if (newSelection.isSelectionMode) ic.getSelectedText(0) ?: "" else ""

        scope.launch {
            activeContent = generateContent(
                editorInfo,
                newSelection,
                textBeforeSelection,
                textAfterSelection,
                selectedText,
            ).also { content ->
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
        currentInputConnection()?.requestCursorUpdates(CursorUpdateNone)
    }

    protected open fun reset() {
        activeInfo = FlorisEditorInfo.Unspecified
        activeCursorCapsMode = InputAttributes.CapsMode.NONE
        activeContent = EditorContent.Unspecified
        runBlocking { expectedContentQueue.clear() }
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

    abstract fun determineComposer(composerName: ExtensionComponentName): Composer

    protected open fun shouldDetermineComposingRegion(editorInfo: FlorisEditorInfo): Boolean {
        return editorInfo.isRichInputEditor
    }

    private suspend fun determineLocalComposing(textBeforeSelection: CharSequence): EditorRange {
        return breakIterators.word(subtypeManager.activeSubtype().primaryLocale) {
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
        val content = activeContent
        if (content.selection == selection) return true
        val ic = currentInputConnection() ?: return false
        ic.beginBatchEdit()
        runBlocking {
            val newContent = content
                .copy(localSelection = selection.translatedBy(-content.offset))
                .generateCopy(selection = selection)
            expectedContentQueue.push(newContent)
            ic.setSelection(selection.start, selection.end)
            ic.setComposingRegion(newContent.composing)
        }
        ic.endBatchEdit()
        return true
    }

    open fun commitChar(char: String): Boolean {
        val content = activeContent
        val selection = content.selection
        // TODO: length enforcement to 1 may be an issue for some Unicode chars which are 2 Java chars
        if (char.length != 1 || selection.isNotValid || selection.isSelectionMode || activeInfo.isRawInputEditor) {
            return commitText(char)
        }
        val ic = currentInputConnection() ?: return false
        val composer = determineComposer(subtypeManager.activeSubtype().composer)
        val previous = content.textBeforeSelection.takeLast(composer.toRead)
        val (rm, finalText) = composer.getActions(previous, char[0])
        if (rm <= 0) {
            commitText(finalText)
        } else runBlocking {
            ic.beginBatchEdit()
            val newSelection = EditorRange.cursor(selection.start - rm + finalText.length)
            val newContent = content.generateCopy(
                selection = newSelection,
                textBeforeSelection = buildString {
                    append(content.textBeforeSelection.dropLast(rm))
                    append(finalText)
                },
                selectedText = "",
            )
            expectedContentQueue.push(newContent)
            // Utilize composing region to replace previous chars without using delete. This avoids flickering in the
            // target editor and improves the UX
            ic.setComposingRegion(content.selection.start - rm, content.selection.start)
            ic.setComposingText(finalText, 1)
            // Now set the proper composing region we expect
            ic.setComposingRegion(newContent.composing)
            ic.endBatchEdit()
        }
        return true
    }

    open fun commitText(text: String): Boolean {
        val ic = currentInputConnection() ?: return false
        val content = activeContent
        val selection = content.selection
        ic.beginBatchEdit()
        ic.finishComposingText()
        if (activeInfo.isRawInputEditor) {
            ic.commitText(text, 1)
        } else runBlocking {
            val newSelection = EditorRange.cursor(selection.start + text.length)
            val newContent = content.generateCopy(
                selection = newSelection,
                textBeforeSelection = buildString {
                    append(content.textBeforeSelection)
                    append(text)
                },
                selectedText = "",
            )
            expectedContentQueue.push(newContent)
            ic.commitText(text, 1)
            ic.setComposingRegion(newContent.composing)
        }
        ic.endBatchEdit()
        return true
    }

    protected fun deleteBeforeCursor(type: TextType, n: Int): Boolean {
        val ic = currentInputConnection()
        if (ic == null || n < 1) return false
        val content = activeContent
        // Cannot perform below check due to editors which lie about their correct selection
        //if (content.selection.isValid && content.selection.start == 0) return true
        val oldTextBeforeSelection = content.textBeforeSelection
        return if (activeInfo.isRawInputEditor || oldTextBeforeSelection.isEmpty()) {
            // If editor is rich and text before selection is empty we seem to have an invalid state here, so we fall
            // back to emulating a hardware backspace.
            when (type) {
                TextType.CHARACTERS -> sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL, count = n)
                TextType.WORDS -> sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL, meta(ctrl = true), count = n)
            }
        } else {
            runBlocking {
                val locale = subtypeManager.activeSubtype().primaryLocale
                val length = when (type) {
                    TextType.CHARACTERS -> breakIterators.measureLastUChars(oldTextBeforeSelection, n, locale)
                    TextType.WORDS -> breakIterators.measureLastUWords(oldTextBeforeSelection, n, locale)
                }
                val newSelection = content.selection.translatedBy(-length)
                val newContent = content.generateCopy(
                    selection = newSelection,
                    textBeforeSelection = oldTextBeforeSelection.dropLast(length),
                )
                expectedContentQueue.push(newContent)
                ic.beginBatchEdit()
                ic.finishComposingText()
                ic.deleteSurroundingText(length, 0)
                ic.setComposingRegion(newContent.composing)
                ic.endBatchEdit()
                true
            }
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
    fun EditorContent.getTextBeforeCursor(n: Int): String {
        if (n < 1 || text.isEmpty()) return ""
        return runBlocking {
            val text = textBeforeSelection
            val length = breakIterators.measureLastUChars(text, n, subtypeManager.activeSubtype().primaryLocale)
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
    fun EditorContent.getTextAfterCursor(n: Int): String {
        if (n < 1 || text.isEmpty()) return ""
        return runBlocking {
            val text = textAfterSelection
            val length = breakIterators.measureUChars(text, n, subtypeManager.activeSubtype().primaryLocale)
            text.take(length)
        }
    }

    /**
     * Constructs a meta state integer flag which can be used for setting the `metaState` field when sending a KeyEvent
     * to the input connection. If this method is called without a meta modifier set to true, the default value `0` is
     * returned.
     *
     * @param ctrl Set to true to enable the CTRL meta modifier. Defaults to false.
     * @param alt Set to true to enable the ALT meta modifier. Defaults to false.
     * @param shift Set to true to enable the SHIFT meta modifier. Defaults to false.
     *
     * @return An integer containing all meta flags passed and formatted for use in a [KeyEvent].
     */
    fun meta(
        ctrl: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
    ): Int {
        var metaState = 0
        if (ctrl) {
            metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        }
        if (alt) {
            metaState = metaState or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        }
        if (shift) {
            metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }
        return metaState
    }

    private fun InputConnection.sendDownKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int, repeat: Int = 0): Boolean {
        return this.sendKeyEvent(
            KeyEvent(
                eventTime,
                eventTime,
                KeyEvent.ACTION_DOWN,
                keyEventCode,
                repeat,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD,
            )
        )
    }

    private fun InputConnection.sendUpKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int): Boolean {
        return this.sendKeyEvent(
            KeyEvent(
                eventTime,
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP,
                keyEventCode,
                0,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD,
            )
        )
    }

    /**
     * Same as [InputMethodService.sendDownUpKeyEvents] but also allows to set meta state.
     *
     * @param keyEventCode The key code to send, use a key code defined in Android's [KeyEvent].
     * @param metaState Flags indicating which meta keys are currently pressed.
     * @param count How often the key is pressed while the meta keys passed are down. Must be greater than or equal to
     *  `1`, else this method will immediately return false.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun sendDownUpKeyEvent(keyEventCode: Int, metaState: Int = meta(), count: Int = 1): Boolean {
        if (count < 1) return false
        val ic = currentInputConnection() ?: return false
        ic.beginBatchEdit()
        val eventTime = SystemClock.uptimeMillis()
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            ic.sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT, 0)
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            ic.sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            ic.sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT, 0)
        }
        for (n in 0 until count) {
            ic.sendDownKeyEvent(eventTime, keyEventCode, metaState, n)
        }
        ic.sendUpKeyEvent(eventTime, keyEventCode, metaState)
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            ic.sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            ic.sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            ic.sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT, 0)
        }
        ic.endBatchEdit()
        return true
    }

    protected enum class TextType {
        CHARACTERS,
        WORDS;
    }

    private class ExpectedContentQueue {
        private val list = guardedByLock { mutableListOf<EditorContent>() }

        suspend fun popUntilOrNull(predicate: (EditorContent) -> Boolean): EditorContent? {
            return list.withLock { list ->
                while (list.isNotEmpty()) {
                    val item = list.removeFirst()
                    if (predicate(item)) return@withLock item
                }
                return@withLock null
            }
        }

        suspend fun push(item: EditorContent) {
            list.withLock { list ->
                list.add(item)
            }
        }

        suspend fun peekNewestOrNull(): EditorContent? {
            return list.withLock { list ->
                list.lastOrNull()
            }
        }

        suspend fun clear() {
            list.withLock { list ->
                list.clear()
            }
        }
    }
}
