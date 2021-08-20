/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.core

import android.content.ClipDescription
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputBinding
import android.view.inputmethod.InputConnection
import androidx.core.text.isDigitsOnly
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.common.stringBuilder
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogDebug
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.debug.flogWarning
import dev.patrickgold.florisboard.ime.clip.FlorisClipboardManager
import dev.patrickgold.florisboard.ime.clip.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clip.provider.ItemType
import dev.patrickgold.florisboard.ime.keyboard.ImeOptions
import dev.patrickgold.florisboard.ime.keyboard.InputAttributes
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.composing.Composer
import dev.patrickgold.florisboard.util.debugSummarize

class EditorInstance(private val ims: InputMethodService, private val activeState: KeyboardState) {
    companion object {
        private const val CAPACITY_CHARS: Int = 1000
        private const val CAPACITY_LINES: Int = 10
        private const val CACHED_N_CHARS_BEFORE_CURSOR: Int = 320
        private const val CACHED_N_CHARS_AFTER_CURSOR: Int = 64

        private const val UNSET: Int = -1
        private const val CURSOR_UPDATE_DISABLED: Int = 0
    }

    val cachedInput: CachedInput = CachedInput()
    internal var extractedToken: Int = 0
    private var lastReportedComposingBounds: Bounds = Bounds(-1, -1)
    private var isInputBindingActive: Boolean = false
    private val florisClipboardManager get() = FlorisClipboardManager.getInstance()
    var contentMimeTypes: Array<out String?>? = null

    var shouldReevaluateComposingSuggestions: Boolean = false
    var isPhantomSpaceActive: Boolean = false
        private set
    private var wasPhantomSpaceActiveLastUpdate: Boolean = false
    var wordHistoryChangedListener: WordHistoryChangedListener? = null

    val inputBinding: InputBinding?
        get() = if (isInputBindingActive) ims.currentInputBinding else null
    val inputConnection: InputConnection?
        get() = if (isInputBindingActive) ims.currentInputConnection else null
    val editorInfo: EditorInfo?
        get() = if (isInputBindingActive && ims.currentInputStarted) ims.currentInputEditorInfo else null

    val cursorCapsMode: InputAttributes.CapsMode
        get() {
            val ic = inputConnection ?: return InputAttributes.CapsMode.NONE
            return InputAttributes.CapsMode.fromFlags(
                ic.getCursorCapsMode(activeState.inputAttributes.capsMode.toFlags())
            )
        }
    val packageName: String?
        get() = editorInfo?.packageName

    val selection: Region = Region(UNSET, UNSET)

    fun updateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        updateSelection(
            normalizeBounds(oldSelStart, oldSelEnd),
            normalizeBounds(newSelStart, newSelEnd),
            normalizeBounds(candidatesStart, candidatesEnd)
        )
    }

    fun updateSelection(oldSel: Bounds, newSel: Bounds, candidates: Bounds) {
        flogInfo(LogTopic.EDITOR_INSTANCE) { "oldSel=$oldSel newSel=$newSel candidates=$candidates" }
        if (oldSel == newSel) return
        selection.bounds = newSel
        lastReportedComposingBounds = candidates
        if (isPhantomSpaceActive && wasPhantomSpaceActiveLastUpdate) {
            isPhantomSpaceActive = false
        } else if (isPhantomSpaceActive && !wasPhantomSpaceActiveLastUpdate) {
            wasPhantomSpaceActiveLastUpdate = true
        }
        cachedInput.reevaluateWords()
        if (selection.isCursorMode) {
            if (activeState.isComposingEnabled) {
                if (candidates.start >= 0 && candidates.end >= 0) {
                    shouldReevaluateComposingSuggestions = true
                }
                if (activeState.isRichInputEditor && !isPhantomSpaceActive) {
                    markComposingRegion(cachedInput.currentWord)
                } else if (newSel.start >= 0) {
                    markComposingRegion(null)
                }
            }
        } else {
            if (candidates.start >= 0 || candidates.end >= 0) {
                markComposingRegion(null)
            }
        }
    }

    fun updateText(token: Int, exText: ExtractedText?) {
        flogInfo(LogTopic.EDITOR_INSTANCE) { "exText=${exText?.debugSummarize()}" }
        if (extractedToken != token) {
            flogWarning(LogTopic.EDITOR_INSTANCE) { "Received update text request with mismatching token, ignoring!" }
            return
        }
        cachedInput.updateText(exText)
        cachedInput.reevaluateWords()
        if (selection.isCursorMode) {
            markComposingRegion(cachedInput.currentWord)
        }
    }

    fun bindInput() {
        flogInfo(LogTopic.EDITOR_INSTANCE) { "(no args)" }
        isInputBindingActive = true
    }

    fun startInput(ei: EditorInfo?) {
        flogInfo(LogTopic.EDITOR_INSTANCE) { "info=${ei?.debugSummarize()}" }
        if (ei != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            contentMimeTypes = ei.contentMimeTypes
        }
        val ic = inputConnection ?: return
        ic.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
        val exText = ExtractedTextRequest().let { req ->
            req.token = ++extractedToken
            req.flags = 0
            req.hintMaxLines = CAPACITY_LINES
            req.hintMaxChars = CAPACITY_CHARS
            ic.getExtractedText(req, InputConnection.GET_EXTRACTED_TEXT_MONITOR)
        }
        if (exText != null) {
            updateText(extractedToken, exText)
        }
    }

    fun startInputView(ei: EditorInfo?) {
        flogInfo(LogTopic.EDITOR_INSTANCE) { "info=${ei?.debugSummarize()}" }
    }

    fun finishInputView() {
        flogInfo(LogTopic.EDITOR_INSTANCE) { "(no args)" }
    }

    fun finishInput() {
        flogInfo(LogTopic.EDITOR_INSTANCE) { "(no args)" }
        val ic = inputConnection ?: return
        ic.requestCursorUpdates(CURSOR_UPDATE_DISABLED)
    }

    fun unbindInput() {
        flogInfo(LogTopic.EDITOR_INSTANCE) { "(no args)" }
        isInputBindingActive = false
        reset()
    }

    fun composingEnabledChanged() {
        if (activeState.isComposingEnabled && activeState.isRichInputEditor) {
            markComposingRegion(cachedInput.currentWord)
        } else {
            markComposingRegion(null)
        }
    }

    /**
     * Completes the given [text] in the current composing region. Does nothing if the current
     * composing region is of zero length or null.
     *
     * @param text The text to complete in this editor's composing region.
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun commitCompletion(text: String): Boolean {
        val ic = inputConnection ?: return false
        return if (activeState.isRawInputEditor) {
            false
        } else {
            ic.beginBatchEdit()
            if (isPhantomSpaceActive && selection.start > 0 && getTextBeforeCursor(1) != " ") {
                ic.commitText(" ", 1)
            }
            ic.setComposingText(text, 1)
            isPhantomSpaceActive = true
            wasPhantomSpaceActiveLastUpdate = false
            markComposingRegion(null)
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Internal helper, replacing a call to inputConnection.commitText with text composition in mind.
     */
    fun doCommitText(text: String): Pair<Boolean, String> {
        val ic = inputConnection ?: return Pair(false, "")
        val composer: Composer = FlorisBoard.getInstance().composer
        return if (text.length != 1) {
            Pair(ic.commitText(text, 1), text)
        } else {
            ic.beginBatchEdit()
            ic.finishComposingText()
            val previous = getTextBeforeCursor(composer.toRead)
            val (rm, finalText) = composer.getActions(previous, text[0])
            if (rm != 0) ic.deleteSurroundingText(rm, 0)
            ic.commitText(finalText, 1)
            ic.endBatchEdit()
            Pair(true, finalText)
        }
    }

    /**
     * Commits the given [text] to this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * This method overwrites any selected text and replaces it with given [text]. If there is no
     * text selected (selection is in cursor mode), then this method will insert the [text] after
     * the cursor, then set the cursor position to the first character after the inserted text.
     *
     * @param text The text to commit.
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun commitText(text: String): Boolean {
        val ic = inputConnection ?: return false
        return if (activeState.isRawInputEditor || selection.isSelectionMode || !activeState.isComposingEnabled) {
            doCommitText(text).first
        } else {
            ic.beginBatchEdit()
            val isWordComponent = TextProcessor.isWord(text, FlorisLocale.ENGLISH)
            val isPhantomSpace = isPhantomSpaceActive && selection.start > 0 && getTextBeforeCursor(1) != " "
            when {
                isPhantomSpace && isWordComponent -> {
                    ic.finishComposingText()
                    ic.commitText(" ", 1)
                    ic.setComposingText(text, 1)
                }
                !isPhantomSpace && isWordComponent -> {
                    ic.finishComposingText()
                    val finalText = doCommitText(text).second
                    cachedInput.currentWord?.let {
                        ic.setComposingRegion(it.start, it.end + finalText.length)
                    }
                }
                else -> {
                    ic.finishComposingText()
                    ic.commitText(text, 1)
                }
            }
            isPhantomSpaceActive = false
            wasPhantomSpaceActiveLastUpdate = false
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Commit a word generated by a gesture.
     */
    fun commitGesture(text: String): Boolean {
        val ic = inputConnection ?: return false
        return if (activeState.isRawInputEditor) {
            false
        } else {
            ic.beginBatchEdit()
            ic.finishComposingText()
            if (selection.start > 0) {
                val previous = getTextBeforeCursor(1)
                if (TextProcessor.isWord(previous, FlorisLocale.ENGLISH) ||
                    previous.isDigitsOnly() ||
                    previous in TextInputManager.getInstance().symbolsWithSpaceAfter
                ) {
                    ic.commitText(" ", 1)
                }
            }
            ic.commitText(text, 1)
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Commits the given [ClipboardItem]. If the clip data is text (incl. HTML), it delegates to [commitText].
     * If the item has a content URI (and the EditText supports it), the item is committed as rich data.
     * This allows for committing (e.g) images.
     *
     * @param item The ClipboardItem to commit
     * @return True on success, false if something went wrong.
     */
    fun commitClipboardItem(item: ClipboardItem): Boolean {
        val mimeTypes = item.mimeTypes
        return when (item.type) {
            ItemType.IMAGE -> {
                val inputContentInfo = InputContentInfoCompat(
                    item.uri!!,
                    ClipDescription("clipboard image", mimeTypes),
                    null
                )
                val ic = inputConnection ?: return false
                ic.finishComposingText()
                var flags = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                } else {
                    FlorisBoard.getInstance().grantUriPermission(
                        editorInfo!!.packageName ?: "",
                        item.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                InputConnectionCompat.commitContent(ic, editorInfo!!, inputContentInfo, flags, null)
            }
            ItemType.TEXT -> {
                commitText(item.text.toString())
            }
        }
    }

    /**
     * Executes a backward delete on this editor's text. If a text selection is active, all
     * characters inside this selection will be removed, else only the left-most character from
     * the cursor's position.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun deleteBackwards(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL)
    }

    /**
     * Executes a backward delete on this editor's text. If a text selection is active, all
     * characters inside this selection will be removed, else only the left-most character from
     * the cursor's position.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun deleteWordBackwards(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL, meta(ctrl = true))
    }

    /**
     * Gets [n] characters after the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get after the cursor. Must be greater than 0 or this
     *  method will fail.
     * @return [n] or less characters after the cursor.
     */
    fun getTextAfterCursor(n: Int): String {
        val ic = inputConnection
        if (ic == null || !selection.isValid || n < 1 || activeState.isRawInputEditor) {
            return ""
        }
        return ic.getTextAfterCursor(n, 0)?.toString() ?: ""
    }

    /**
     * Gets [n] characters before the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get before the cursor. Must be greater than 0 or this
     *  method will fail.
     * @return [n] or less characters before the cursor.
     */
    fun getTextBeforeCursor(n: Int): String {
        val ic = inputConnection
        if (ic == null || !selection.isValid || n < 1 || activeState.isRawInputEditor) {
            return ""
        }
        return ic.getTextBeforeCursor(n.coerceAtMost(selection.start), 0)?.toString() ?: ""
    }

    /**
     * Adds one word on the left of selection to it.
     *
     * @return True on success, false if no new words are selected.
     */
    fun leftAppendWordToSelection(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        // no words left to select
        if (selection.start <= 0) {
            return false
        }
        val currentWord = cachedInput.currentWord
        val wordsBeforeCurrent = cachedInput.wordsBeforeCurrent
        if (currentWord != null && currentWord.isValid) {
            if (selection.start > currentWord.start) {
                selection.updateAndNotify(currentWord.start, selection.end)
                return true
            } else {
                for (word in wordsBeforeCurrent.reversed()) {
                    if (selection.start > word.start) {
                        selection.updateAndNotify(word.start, selection.end)
                        return true
                    }
                }
            }
        } else {
            for (word in wordsBeforeCurrent.reversed()) {
                if (selection.start > word.start) {
                    selection.updateAndNotify(word.start, selection.end)
                    return true
                }
            }
        }
        selection.updateAndNotify(0, selection.end)
        return true
    }

    /**
     * Removes one word on the left from the selection.
     *
     * @return True on success, false if no new words are deselected.
     */
    fun leftPopWordFromSelection(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        // no words left to pop
        if (selection.start >= selection.end) {
            return false
        }
        val currentWord = cachedInput.currentWord
        val wordsBeforeCurrent = cachedInput.wordsBeforeCurrent
        if (currentWord != null && currentWord.isValid) {
            for (word in wordsBeforeCurrent) {
                if (selection.start < word.start) {
                    selection.updateAndNotify(word.start, selection.end)
                    return true
                }
            }
            if (selection.start < currentWord.start) {
                selection.updateAndNotify(currentWord.start, selection.end)
                return true
            }
        } else {
            for (word in wordsBeforeCurrent) {
                if (selection.start < word.start) {
                    selection.updateAndNotify(word.start, selection.end)
                    return true
                }
            }
        }
        selection.updateAndNotify(selection.end, selection.end)
        return true
    }

    fun selectionSetNWordsLeft(n: Int): Boolean {
        flogDebug { "$n" }
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        if (selection.start < 0) return false
        if (n <= 0) {
            selection.updateAndNotify(selection.end, selection.end)
            return true
        }
        var wordsSelected = 0
        var selStart = selection.end
        val currentWord = cachedInput.currentWord
        val wordsBeforeCurrent = cachedInput.wordsBeforeCurrent
        if (currentWord != null && currentWord.isValid) {
            if (selStart > currentWord.start) {
                selStart = currentWord.start
                if (++wordsSelected == n) {
                    selection.updateAndNotify(selStart, selection.end)
                    return true
                }
            }
            for (word in wordsBeforeCurrent.reversed()) {
                if (selStart > word.start) {
                    selStart = word.start
                    if (++wordsSelected == n) {
                        selection.updateAndNotify(selStart, selection.end)
                        return true
                    }
                }
            }
        } else {
            for (word in wordsBeforeCurrent.reversed()) {
                if (selStart > word.start) {
                    selStart = word.start
                    if (++wordsSelected == n) {
                        selection.updateAndNotify(selStart, selection.end)
                        return true
                    }
                }
            }
        }
        selection.updateAndNotify(0, selection.end)
        return true
    }

    /**
     * Marks a given [region] as composing and notifies the input connection.
     *
     * @param region The region which should be marked as composing.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun markComposingRegion(region: Region?): Boolean {
        flogDebug(LogTopic.EDITOR_INSTANCE) { "Request to mark region: $region" }
        val ic = inputConnection ?: return false
        return if (region == null || !region.isValid || !activeState.isComposingEnabled) {
            flogDebug(LogTopic.EDITOR_INSTANCE) { " Clearing composing text." }
            ic.finishComposingText()
        } else {
            flogDebug(LogTopic.EDITOR_INSTANCE) { " Last reported composing region: $lastReportedComposingBounds" }
            return if (region.bounds == lastReportedComposingBounds) {
                flogDebug(LogTopic.EDITOR_INSTANCE) { " Should mark region but is already, skipping." }
                false
            } else {
                flogDebug(LogTopic.EDITOR_INSTANCE) { " Marking composing text region." }
                lastReportedComposingBounds = region.bounds
                ic.setComposingRegion(region.start, region.end)
            }
        }
    }

    /**
     * Performs a cut command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCut(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        florisClipboardManager.addNewPlaintext(selection.icText)
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL)
    }

    /**
     * Performs a copy command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCopy(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        florisClipboardManager.addNewPlaintext(selection.icText)
        return selection.updateAndNotify(selection.end, selection.end)
    }

    /**
     * Performs a paste command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardPaste(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        return commitClipboardItem(florisClipboardManager.primaryClip!!)
    }

    /**
     * Performs a select all on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardSelectAll(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        markComposingRegion(null)
        val ic = inputConnection ?: return false
        if (activeState.isRawInputEditor) {
            sendDownUpKeyEvent(KeyEvent.KEYCODE_A, meta(ctrl = true))
        } else {
            ic.performContextMenuAction(android.R.id.selectAll)
        }
        return true
    }

    /**
     * Performs an enter key press on the current input editor.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performEnter(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        return if (activeState.isRawInputEditor) {
            sendDownUpKeyEvent(KeyEvent.KEYCODE_ENTER)
        } else {
            commitText("\n")
        }
    }

    /**
     * Performs a given [action] on the current input editor.
     *
     * @param action The action to be performed on this editor instance.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performEnterAction(action: ImeOptions.EnterAction): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        val ic = inputConnection ?: return false
        return ic.performEditorAction(action.toInt())
    }

    /**
     * Undoes the last action.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performUndo(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true))
    }

    /**
     * Redoes the last Undo action.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performRedo(): Boolean {
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true, shift = true))
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
        shift: Boolean = false
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

    private fun sendDownKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int): Boolean {
        val ic = inputConnection ?: return false
        return ic.sendKeyEvent(
            KeyEvent(
                eventTime,
                eventTime,
                KeyEvent.ACTION_DOWN,
                keyEventCode,
                0,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD
            )
        )
    }

    private fun sendUpKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int): Boolean {
        val ic = inputConnection ?: return false
        return ic.sendKeyEvent(
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
                InputDevice.SOURCE_KEYBOARD
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
        val ic = inputConnection ?: return false
        ic.beginBatchEdit()
        val eventTime = SystemClock.uptimeMillis()
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT, 0)
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT, 0)
        }
        for (n in 0 until count) {
            sendDownKeyEvent(eventTime, keyEventCode, metaState)
            sendUpKeyEvent(eventTime, keyEventCode, metaState)
        }
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT, 0)
        }
        ic.endBatchEdit()
        return true
    }

    fun reset() {
        selection.apply { start = UNSET; end = UNSET }
        cachedInput.reset()
        lastReportedComposingBounds = Bounds(-1, -1)
    }

    internal fun normalizeBounds(start: Int, end: Int): Bounds {
        return if (start > end) {
            Bounds(end, start)
        } else {
            Bounds(start, end)
        }
    }

    internal fun ExtractedText.isPartialChange() = this.partialStartOffset > -1 && this.partialEndOffset > -1

    internal fun ExtractedText.getPartialChangeBounds(): Bounds {
        return normalizeBounds(this.partialStartOffset, this.partialEndOffset)
    }

    internal fun ExtractedText.getSelectionBounds(): Bounds {
        return normalizeBounds(this.selectionStart, this.selectionEnd)
    }

    internal fun ExtractedText.getTextStr() = (this.text ?: "").toString()

    private fun ExtractedText.debugSummarize(): String {
        return stringBuilder {
            append("ExtractedText:")
            appendLine()
            append("text=\"${this@debugSummarize.text}\"")
            appendLine()
            append("startOffset=${this@debugSummarize.startOffset}")
            appendLine()
            append("partialStartOffset=${this@debugSummarize.partialStartOffset}")
            appendLine()
            append("partialEndOffset=${this@debugSummarize.partialEndOffset}")
            appendLine()
            append("selectionStart=${this@debugSummarize.selectionStart}")
            appendLine()
            append("selectionEnd=${this@debugSummarize.selectionEnd}")
            appendLine()
        }
    }

    /**
     * Data class which specifies the bounds for a region between [start]
     * and [end].
     *
     * @property start The start marker of this bounds.
     * @property end The end marker of this bounds.
     */
    open class Bounds(var start: Int, var end: Int) {
        operator fun component1() = start
        operator fun component2() = end

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Bounds

            if (start != other.start) return false
            if (end != other.end) return false

            return true
        }

        override fun hashCode(): Int = 31 * start + end

        override fun toString(): String = "Bounds { start=$start, end=$end }"
    }

    /**
     * Class which marks a region of [CachedInput.rawText], which provides length, validation
     * and text access fields.
     */
    inner class Region(initStart: Int, initEnd: Int) : Bounds(initStart, initEnd) {
        /** Returns the bounds for this regions. */
        var bounds: Bounds
            get() = Bounds(start, end)
            set(v) { start = v.start; end = v.end }

        /** Returns true if the region's start and end markers are valid, false otherwise. */
        val isValid: Boolean
            get() = start >= 0 && end >= 0 && length >= 0

        /** The length of the region. */
        val length: Int
            get() = end - start

        val isCursorMode: Boolean
            get() = length == 0 && isValid
        val isSelectionMode: Boolean
            get() = length != 0 && isValid

        /**
         * Returns the text marked by [start] to [end] and returns it. On
         * failure or if the bounds are outside of the cached text, an empty
         * string is returned.
         */
        val text: String
            get() {
                val eiText = cachedInput.rawText
                val eiStart = (start - cachedInput.offset).coerceIn(0, eiText.length)
                val eiEnd = (end - cachedInput.offset).coerceIn(0, eiText.length)
                return if (!isValid || eiEnd - eiStart <= 0) { "" } else { eiText.substring(eiStart, eiEnd) }
            }

        val icText: String get() = when {
            !isValid -> ""
            else -> when (val ic = inputConnection) {
                null -> ""
                else -> ic.getSelectedText(0).toString()
            }
        }

        /**
         * Updates this region's [start] and [end] values.
         *
         * @param newStart The new start value for this region.
         * @param newEnd The new end value for this region.
         */
        fun update(newStart: Int, newEnd: Int): Boolean {
            start = newStart
            end = newEnd
            return true
        }

        /**
         * Same as [update], but also notifies the input connection linked by editor instance of this
         * selection change.
         */
        fun updateAndNotify(newStart: Int, newEnd: Int): Boolean {
            return update(newStart, newEnd) && if (activeState.isRichInputEditor) {
                inputConnection?.setSelection(newStart, newEnd) ?: false
            } else {
                false
            }
        }
    }

    inner class CachedInput {
        /**
         * The raw cached input text of the target app's editor. This cached value may be incomplete if
         * the target app's editor text is bigger than [CAPACITY_CHARS] or [CAPACITY_LINES], but always
         * caches the relevant text around the cursor.
         */
        var rawText: StringBuilder = StringBuilder(CAPACITY_CHARS)
            private set

        /**
         * The offset of the [rawText] from the selection root (index 0).
         */
        var offset: Int = 0
            private set

        var wordsBeforeCurrent: MutableList<Region> = mutableListOf()
            private set
        var wordsAfterCurrent: MutableList<Region> = mutableListOf()
            private set
        var currentWord: Region? = null
            private set

        fun updateText(exText: ExtractedText?) {
            if (exText == null) {
                reset()
                return
            }
            val sel = exText.getSelectionBounds()
            if (selection.bounds != sel) {
                flogWarning { "Selection from extracted text mismatches from selection state, fixing!" }
                selection.bounds = sel
            }
            if (exText.isPartialChange()) {
                val (partialStart, partialEnd) = exText.getPartialChangeBounds()
                rawText.replace(partialStart, partialEnd, exText.getTextStr())
            } else {
                rawText.replace(0, rawText.length, exText.getTextStr())
                offset = exText.startOffset.coerceAtLeast(0)
            }
        }

        fun reevaluateWords() {
            wordsBeforeCurrent.clear()
            wordsAfterCurrent.clear()
            currentWord = null

            if (selection.isValid) {
                val cursor = selection.end.coerceAtLeast(0)
                val detectStart = (cursor - CACHED_N_CHARS_BEFORE_CURSOR - offset).coerceAtLeast(0)
                val detectEnd = (cursor + CACHED_N_CHARS_AFTER_CURSOR - offset).coerceAtMost(rawText.length - 1)
                for (wordRange in TextProcessor.detectWords(rawText, detectStart, detectEnd, FlorisLocale.ENGLISH)) {
                    val wordStart = wordRange.first + offset + detectStart
                    val wordEnd = wordRange.last + 1 + offset + detectStart
                    if (cursor in wordStart..wordEnd) {
                        if (!isPhantomSpaceActive) {
                            currentWord = Region(wordStart, wordEnd)
                        } else {
                            wordsBeforeCurrent.add(Region(wordStart, wordEnd))
                        }
                    } else if (wordEnd < cursor) {
                        wordsBeforeCurrent.add(Region(wordStart, wordEnd))
                    } else {
                        wordsAfterCurrent.add(Region(wordStart, wordEnd))
                    }
                }
            }

            wordHistoryChangedListener?.onWordHistoryChanged(
                currentWord, wordsBeforeCurrent, wordsAfterCurrent
            )

            flogDebug(LogTopic.EDITOR_INSTANCE) {
                stringBuilder {
                    append("Words before current: ")
                    wordsBeforeCurrent.forEach {
                        append(it.toString())
                        append(' ')
                    }
                }
            }
            flogDebug(LogTopic.EDITOR_INSTANCE) {
                stringBuilder {
                    append("Current word: $currentWord")
                }
            }
            flogDebug(LogTopic.EDITOR_INSTANCE) {
                stringBuilder {
                    append("Words after current: ")
                    wordsAfterCurrent.forEach {
                        append(it.toString())
                        append(' ')
                    }
                }
            }
        }

        fun reset() {
            rawText.clear()
            offset = 0

            wordsBeforeCurrent.clear()
            wordsAfterCurrent.clear()
            currentWord = null
        }
    }

    interface WordHistoryChangedListener {
        fun onWordHistoryChanged(
            currentWord: Region?,
            wordsBeforeCurrent: List<Region>,
            wordsAfterCurrent: List<Region>
        )
    }
}
