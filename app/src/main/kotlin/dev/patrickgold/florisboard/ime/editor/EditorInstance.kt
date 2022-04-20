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

package dev.patrickgold.florisboard.ime.editor

import android.content.ClipDescription
import android.content.ContentUris
import android.content.Intent
import android.icu.text.BreakIterator
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardFileStorage
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.nlp.BreakIterators
import dev.patrickgold.florisboard.ime.nlp.TextProcessor
import dev.patrickgold.florisboard.ime.text.key.InputMode
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.android.AndroidVersion
import dev.patrickgold.florisboard.lib.android.showShortToast
import dev.patrickgold.florisboard.lib.kotlin.tryOrNull
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class EditorInstance(private val ims: InputMethodService) {
    private val prefs by florisPreferenceModel()
    private val clipboardManager by ims.clipboardManager()
    private val keyboardManager by ims.keyboardManager()
    private val subtypeManager by ims.subtypeManager()

    private val activeEditorInfo get() = keyboardManager.activeEditorInfo
    private val activeState get() = keyboardManager.activeState
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var activeSelection by mutableStateOf(EditorRegion.Unspecified)
        private set
    val inputCache = InputCache()
    val phantomSpace = PhantomSpaceState()

    private val inputConnection: InputConnection?
        get() = tryOrNull { ims.currentInputConnection }

    val cursorCapsMode: InputAttributes.CapsMode
        get() {
            val ic = inputConnection ?: return InputAttributes.CapsMode.NONE
            return InputAttributes.CapsMode.fromFlags(
                ic.getCursorCapsMode(activeEditorInfo.inputAttributes.capsMode.toFlags())
            )
        }

    fun updateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        val oldSel = EditorRegion.normalized(oldSelStart, oldSelEnd)
        val newSel = EditorRegion.normalized(newSelStart, newSelEnd)
        val candidates = EditorRegion.normalized(candidatesStart, candidatesEnd)

        if (oldSel == newSel && newSel == activeSelection && candidates == inputCache.editorContent().composingRegion) {
            // Nothing has changed, ignore call
            return
        }
        activeSelection = newSel
        inputCache.update { editorContent ->
            if (editorContent.composingRegion != candidates) {
                markComposingRegion(editorContent.composingRegion)
            }
        }
    }

    fun updateSelectionAndNotify(start: Int, end: Int): Boolean {
        val ic = inputConnection ?: return false
        return if (activeEditorInfo.isRichInputEditor) {
            phantomSpace.setInactive()
            ic.setSelection(start, end)
        } else {
            false
        }
    }

    fun startInput(editorInfo: FlorisEditorInfo) {
        activeSelection = editorInfo.initialSelection
        val ic = inputConnection ?: return
        ic.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
    }

    fun startInputView(editorInfo: FlorisEditorInfo) {
        activeSelection = editorInfo.initialSelection
        val keyboardMode = when (editorInfo.inputAttributes.type) {
            InputAttributes.Type.NUMBER -> {
                activeState.keyVariation = KeyVariation.NORMAL
                KeyboardMode.NUMERIC
            }
            InputAttributes.Type.PHONE -> {
                activeState.keyVariation = KeyVariation.NORMAL
                KeyboardMode.PHONE
            }
            InputAttributes.Type.TEXT -> {
                activeState.keyVariation = when (editorInfo.inputAttributes.variation) {
                    InputAttributes.Variation.EMAIL_ADDRESS,
                    InputAttributes.Variation.WEB_EMAIL_ADDRESS -> {
                        KeyVariation.EMAIL_ADDRESS
                    }
                    InputAttributes.Variation.PASSWORD,
                    InputAttributes.Variation.VISIBLE_PASSWORD,
                    InputAttributes.Variation.WEB_PASSWORD -> {
                        KeyVariation.PASSWORD
                    }
                    InputAttributes.Variation.URI -> {
                        KeyVariation.URI
                    }
                    else -> {
                        KeyVariation.NORMAL
                    }
                }
                KeyboardMode.CHARACTERS
            }
            else -> {
                activeState.keyVariation = KeyVariation.NORMAL
                KeyboardMode.CHARACTERS
            }
        }
        activeState.keyboardMode = keyboardMode
        activeState.isComposingEnabled = when (keyboardMode) {
            KeyboardMode.NUMERIC,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> false
            else -> activeState.keyVariation != KeyVariation.PASSWORD &&
                prefs.suggestion.enabled.get()// &&
            //!instance.inputAttributes.flagTextAutoComplete &&
            //!instance.inputAttributes.flagTextNoSuggestions
        }
        if (!prefs.correction.rememberCapsLockState.get()) {
            activeState.inputMode = InputMode.NORMAL
        }
        inputCache.update { markComposingRegion(it.composingRegion) }
    }

    fun finishInputView() {
        reset()
    }

    fun finishInput() {
        reset()
        val ic = inputConnection ?: return
        ic.requestCursorUpdates(CURSOR_UPDATE_DISABLED)
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
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun commitText(text: String): Boolean {
        val ic = inputConnection ?: return false
        ic.beginBatchEdit()

        val isWordComponent = TextProcessor.isWord(text)
        val isPhantomSpaceActive = phantomSpace.isActive && activeSelection.isValid &&
            getTextBeforeCursor(1) != SPACE && isWordComponent

        when {
            activeEditorInfo.isRawInputEditor || activeSelection.isSelectionMode -> {
                ic.finishComposingText()
                if (isPhantomSpaceActive) {
                    ic.commitText("$SPACE$text", 1)
                } else {
                    ic.commitText(text, 1)
                }
            }
            else -> when {
                isPhantomSpaceActive -> {
                    ic.finishComposingText()
                    ic.commitText("$SPACE$text", 1)
                }
                isWordComponent -> {
                    val composingText = inputCache.editorContent().composingText
                    ic.setComposingText("$composingText$text", 1)
                }
                else -> {
                    ic.finishComposingText()
                    ic.commitText(text, 1)
                }
            }
        }

        phantomSpace.setInactive()
        ic.endBatchEdit()
        return true
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
        return if (activeEditorInfo.isRawInputEditor) {
            false
        } else {
            ic.beginBatchEdit()
            if (phantomSpace.isActive && activeSelection.start > 0 && getTextBeforeCursor(1) != " ") {
                ic.commitText(" ", 1)
            }
            ic.setComposingText(text, 1)
            phantomSpace.setActive(showComposingRegion = false)
            markComposingRegion(null)
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Internal helper, replacing a call to inputConnection.commitText with text composition in mind.
     */
    //private fun doCommitText(text: String): Pair<String, Boolean> {
    //    val ic = inputConnection ?: return "" to false
    //    val composer = keyboardManager.resources.composers.value?.get(subtypeManager.activeSubtype().composer) ?: Appender()
    //    return if (text.length != 1) {
    //        ic.commitText(text, 1)
    //    } else {
    //        ic.beginBatchEdit()
    //        ic.finishComposingText()
    //        val previous = getTextBeforeCursor(composer.toRead)
    //        val (rm, finalText) = composer.getActions(previous, text[0])
    //        if (rm == 0) {
    //            ic.commitText(finalText, 1)
    //        } else {
    //            val et = ic.getExtractedText(ExtractedTextRequest(), 0)
    //            ic.setComposingRegion(et.selectionStart-rm, et.selectionStart)
    //            ic.setComposingText(finalText, 1)
    //        }
    //        ic.endBatchEdit()
    //        Pair(true, finalText)
    //    }
    //}

    /**
     * Commit a word generated by a gesture.
     */
    fun commitGesture(text: String): Boolean {
        val ic = inputConnection ?: return false
        return if (activeEditorInfo.isRawInputEditor) {
            false
        } else {
            ic.beginBatchEdit()
            ic.finishComposingText()
            if (activeSelection.start > 0) {
                val previous = getTextBeforeCursor(1)
                if (TextProcessor.isWord(previous) || previous.isDigitsOnly()) {
                    ic.commitText(" ", 1)
                }
            }
            ic.commitText(text, 1)
            phantomSpace.setActive(showComposingRegion = false)
            markComposingRegion(null)
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
     *
     * @return True on success, false if something went wrong.
     */
    fun commitClipboardItem(item: ClipboardItem?): Boolean {
        if (item == null) return false
        val mimeTypes = item.mimeTypes
        return when (item.type) {
            ItemType.TEXT -> {
                commitText(item.text.toString())
                true
            }
            ItemType.IMAGE, ItemType.VIDEO -> {
                item.uri ?: return false
                val id = ContentUris.parseId(item.uri)
                val file = ClipboardFileStorage.getFileForId(ims, id)
                if (!file.exists()) return false
                val inputContentInfo = InputContentInfoCompat(
                    item.uri,
                    ClipDescription("clipboard media file", mimeTypes),
                    null,
                )
                val ic = inputConnection ?: return false
                ic.finishComposingText()
                var flags = 0
                if (AndroidVersion.ATLEAST_API25_N_MR1) {
                    flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                } else {
                    ims.grantUriPermission(
                        activeEditorInfo.packageName,
                        item.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                InputConnectionCompat.commitContent(ic, activeEditorInfo.base, inputContentInfo, flags, null)
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
        phantomSpace.setInactive()
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
        phantomSpace.setInactive()
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL, meta(ctrl = true))
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
        if (n < 1) return ""
        return inputCache.editorContent().beforeSelected.takeLast(n)
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
        if (n < 1) return ""
        return inputCache.editorContent().afterSelected.take(n)
    }

    fun selectionSetNWordsLeft(n: Int): Boolean {
        phantomSpace.setInactive()
        val selection = activeSelection
        if (selection.isNotValid) return false
        if (n <= 0) {
            return updateSelectionAndNotify(selection.end, selection.end)
        }
        //var wordsSelected = 0
        //var selStart = selection.end
        ////val currentWord = cachedInput.currentWord
        ////val wordsBeforeCurrent = cachedInput.wordsBeforeCurrent
        //if (currentWord != null && currentWord.isValid) {
        //    if (selStart > currentWord.start) {
        //        selStart = currentWord.start
        //        if (++wordsSelected == n) {
        //            updateSelectionAndNotify(selStart, selection.end)
        //            return true
        //        }
        //    }
        //    for (word in wordsBeforeCurrent.reversed()) {
        //        if (selStart > word.start) {
        //            selStart = word.start
        //            if (++wordsSelected == n) {
        //                updateSelectionAndNotify(selStart, selection.end)
        //                return true
        //            }
        //        }
        //    }
        //} else {
        //    for (word in wordsBeforeCurrent.reversed()) {
        //        if (selStart > word.start) {
        //            selStart = word.start
        //            if (++wordsSelected == n) {
        //                updateSelectionAndNotify(selStart, selection.end)
        //                return true
        //            }
        //        }
        //    }
        //}
        //updateSelectionAndNotify(0, selection.end)
        return true
    }

    /**
     * Marks a given [region] as composing and notifies the input connection.
     *
     * @param region The region which should be marked as composing.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun markComposingRegion(region: EditorRegion?): Boolean {
        val ic = inputConnection ?: return false
        return if (region == null || !region.isValid || phantomSpace.isActive && !phantomSpace.showComposingRegion) {
            ic.finishComposingText()
        } else {
            ic.setComposingRegion(region.start, region.end)
        }
    }

    /**
     * Performs a cut command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCut(): Boolean {
        phantomSpace.setInactive()
        val text = inputConnection?.getSelectedText(0)
        if (text != null) {
            clipboardManager.addNewPlaintext(text.toString())
        } else {
            ims.showShortToast("Failed to retrieve selected text requested to cut: Eiter selection state is invalid or an error occurred within the input connection.")
        }
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL)
    }

    /**
     * Performs a copy command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCopy(): Boolean {
        phantomSpace.setInactive()
        val text = inputConnection?.getSelectedText(0)
        if (text != null) {
            clipboardManager.addNewPlaintext(text.toString())
        } else {
            ims.showShortToast("Failed to retrieve selected text requested to copy: Eiter selection state is invalid or an error occurred within the input connection.")
        }
        return updateSelectionAndNotify(activeSelection.end, activeSelection.end)
    }

    /**
     * Performs a paste command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardPaste(): Boolean {
        phantomSpace.setInactive()
        return commitClipboardItem(clipboardManager.primaryClip.value).also { result ->
            if (!result) {
                ims.showShortToast("Failed to paste item.")
            }
        }
    }

    /**
     * Performs a select all on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardSelectAll(): Boolean {
        phantomSpace.setInactive()
        markComposingRegion(null)
        val ic = inputConnection ?: return false
        if (activeEditorInfo.isRawInputEditor) {
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
        phantomSpace.setInactive()
        return if (activeEditorInfo.isRawInputEditor) {
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
    fun performEnterAction(action: ImeOptions.Action): Boolean {
        phantomSpace.setInactive()
        val ic = inputConnection ?: return false
        return ic.performEditorAction(action.toInt())
    }

    /**
     * Undoes the last action.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performUndo(): Boolean {
        phantomSpace.setInactive()
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true))
    }

    /**
     * Redoes the last Undo action.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performRedo(): Boolean {
        phantomSpace.setInactive()
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
        activeSelection = EditorRegion.Unspecified
        inputCache.reset()
        phantomSpace.setInactive()
    }

    companion object {
        private const val CACHED_N_CHARS_BEFORE_CURSOR: Int = 128
        private const val CACHED_N_CHARS_AFTER_CURSOR: Int = 64

        private const val CURSOR_UPDATE_DISABLED: Int = 0

        private const val SPACE = " "
    }

    inner class InputCache {
        private val _editorContent = MutableLiveData(EditorContent.Unspecified)
        val editorContent: LiveData<EditorContent> get() = _editorContent

        fun editorContent(): EditorContent = editorContent.value!!

        fun update(action: (EditorContent) -> Unit) {
            val ic = inputConnection
            val selection = activeSelection
            if (ic == null || selection.isNotValid || activeEditorInfo.isRawInputEditor) {
                return reset()
            }
            scope.launch {
                val beforeSelectedAsync = async {
                    when {
                        selection.start > 0 -> {
                            (ic.getTextBeforeCursor(CACHED_N_CHARS_BEFORE_CURSOR, 0) ?: "").toString()
                        }
                        else -> ""
                    }
                }
                val selectedAsync = async {
                    when {
                        selection.isSelectionMode -> {
                            (ic.getSelectedText(0) ?: "").toString()
                        }
                        else -> ""
                    }.toString()
                }
                val afterSelectedAsync = async {
                    (ic.getTextAfterCursor(CACHED_N_CHARS_AFTER_CURSOR, 0) ?: "").toString()
                }
                val beforeSelected = beforeSelectedAsync.await()
                val offset = selection.start - beforeSelected.length
                val composingRegion = if (selection.isCursorMode && beforeSelected.isNotEmpty()) {
                    BreakIterators.withWordInstance(subtypeManager.activeSubtype().primaryLocale) {
                        it.setText(beforeSelected)
                        val end = it.last()
                        val isWord = it.ruleStatus != BreakIterator.WORD_NONE
                        if (isWord) {
                            val start = it.previous()
                            EditorRegion(offset + start, offset + end)
                        } else {
                            EditorRegion.Unspecified
                        }
                    }
                } else {
                    EditorRegion.Unspecified
                }
                val selected = selectedAsync.await()
                val afterSelected = afterSelectedAsync.await()
                val content = EditorContent(offset, beforeSelected, selected, afterSelected, composingRegion)
                action(content)
                _editorContent.postValue(content)
            }
        }

        fun reset() {
            _editorContent.postValue(EditorContent.Unspecified)
        }
    }

    data class EditorContent(
        val offset: Int,
        val beforeSelected: String,
        val selected: String,
        val afterSelected: String,
        val composingRegion: EditorRegion,
    ) {
        val composingText: String
            get() = if (composingRegion.isValid) beforeSelected.takeLast(composingRegion.length) else ""

        companion object {
            val Unspecified = EditorContent(0, "", "", "", EditorRegion.Unspecified)
        }
    }

    class PhantomSpaceState {
        private val state = AtomicInteger(0)

        val isActive: Boolean
            get() = state.get() and F_IS_ACTIVE != 0

        val showComposingRegion: Boolean
            get() = state.get() and F_SHOW_COMPOSING_REGION != 0

        fun setActive(showComposingRegion: Boolean) {
            state.set(F_IS_ACTIVE or (if (showComposingRegion) F_SHOW_COMPOSING_REGION else 0))
        }

        fun setInactive() {
            state.set(0)
        }

        companion object {
            private const val F_IS_ACTIVE = 0x1
            private const val F_SHOW_COMPOSING_REGION = 0x2
        }
    }
}
