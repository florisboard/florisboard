/*
 * Copyright (C) 2020 Patrick Goldinger
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
import android.view.inputmethod.InputConnection
import androidx.core.text.isDigitsOnly
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import dev.patrickgold.florisboard.ime.clip.FlorisClipboardManager
import dev.patrickgold.florisboard.ime.clip.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clip.provider.ItemType
import dev.patrickgold.florisboard.ime.keyboard.ImeOptions
import dev.patrickgold.florisboard.ime.keyboard.InputAttributes
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.composing.Composer
import timber.log.Timber

/**
 * Class which holds information relevant to an editor instance like the [cachedInput],
 * [selection] etc. This class is thought to be an improved [EditorInfo]
 * object which also holds the state of the currently focused input editor.
 */
class EditorInstance private constructor(
    private val ims: InputMethodService?,
    val packageName: String,
    val state: KeyboardState,
    private val editorInfo: EditorInfo
) {
    val cachedInput: CachedInput = CachedInput(this)
    var contentMimeTypes: Array<out String?>? = null
    private val florisClipboardManager: FlorisClipboardManager = FlorisClipboardManager.getInstance()
    val cursorCapsMode: InputAttributes.CapsMode
        get() {
            val ic = inputConnection ?: return InputAttributes.CapsMode.NONE
            return InputAttributes.CapsMode.fromFlags(
                ic.getCursorCapsMode(state.inputAttributes.capsMode.toFlags())
            )
        }
    val inputConnection: InputConnection?
        get() = ims?.currentInputConnection
    var isComposingEnabled: Boolean = false
        set(v) {
            field = v
            cachedInput.reevaluate()
            if (v && !isRawInputEditor) {
                markComposingRegion(cachedInput.currentWord)
            } else {
                markComposingRegion(null)
            }
        }
    var shouldReevaluateComposingSuggestions: Boolean = false
    val isRawInputEditor: Boolean
        get() = state.inputAttributes.type == InputAttributes.Type.NULL
    var selection: Selection = Selection(this)
        private set
    var isPhantomSpaceActive: Boolean = false
        private set
    private var wasPhantomSpaceActiveLastUpdate: Boolean = false

    companion object {
        fun default(): EditorInstance {
            return EditorInstance(
                ims = null,
                packageName = "undefined",
                state = KeyboardState.new(),
                editorInfo = EditorInfo()
            )
        }

        fun from(editorInfo: EditorInfo?, ims: InputMethodService?, state: KeyboardState): EditorInstance {
            return if (editorInfo == null) {
                default()
            } else {
                EditorInstance(
                    ims = ims,
                    packageName = editorInfo.packageName,
                    state = state,
                    editorInfo = editorInfo
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        contentMimeTypes = editorInfo.contentMimeTypes
                    }
                    selection.update(editorInfo.initialSelStart, editorInfo.initialSelEnd)
                }
            }
        }
    }

    init {
        cachedInput.update()
    }

    /**
     * Event handler which reacts to selection updates coming from the target app's editor.
     */
    fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        if (newSelStart == oldSelStart && newSelEnd == oldSelEnd) {
            return
        }
        // The Android Framework allows that start can be greater than end in some cases. To prevent bugs in the Floris
        // input logic, we swap start and end here if this should really be the case.
        if (newSelEnd < newSelStart) {
            selection.update(newSelEnd, newSelStart)
        } else {
            selection.update(newSelStart, newSelEnd)
        }
        if (isPhantomSpaceActive && wasPhantomSpaceActiveLastUpdate) {
            isPhantomSpaceActive = false
        } else if (isPhantomSpaceActive && !wasPhantomSpaceActiveLastUpdate) {
            wasPhantomSpaceActiveLastUpdate = true
        }
        if (selection.isCursorMode) {
            cachedInput.update()
            if (isComposingEnabled) {
                if (candidatesStart >= 0 && candidatesEnd >= 0) {
                    shouldReevaluateComposingSuggestions = true
                }
                if (!isRawInputEditor && !isPhantomSpaceActive) {
                    markComposingRegion(cachedInput.currentWord)
                } else if (newSelStart >= 0) {
                    markComposingRegion(null)
                }
            }
        } else {
            if (candidatesStart >= 0 || candidatesEnd >= 0) {
                markComposingRegion(null)
            }
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
        return if (isRawInputEditor) {
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
        return if (isRawInputEditor || selection.isSelectionMode || !isComposingEnabled) {
            doCommitText(text).first
        } else {
            ic.beginBatchEdit()
            val isWordComponent = CachedInput.isWordComponent(text)
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
                    ic.setComposingRegion(cachedInput.currentWord.start, cachedInput.currentWord.end + finalText.length)
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
        return if (isRawInputEditor) {
            false
        } else {
            ic.beginBatchEdit()
            ic.finishComposingText()
            if (selection.start > 0) {
                val previous = getTextBeforeCursor(1)
                if (CachedInput.isWordComponent(previous) ||
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
                        editorInfo.packageName,
                        item.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                InputConnectionCompat.commitContent(ic, editorInfo, inputContentInfo, flags, null)
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
     * Deletes [n] words before the current cursor's position.
     * NOTE: this implementation does currently only delete currentWord. This is due to change in
     * future versions.
     *
     * @param n The number of words to delete before the cursor. Must be greater than 0 or this
     *  method will fail.
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun deleteWordsBeforeCursor(n: Int): Boolean {
        val ic = inputConnection ?: return false
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        return if (n < 1 || isRawInputEditor || !selection.isValid || !selection.isCursorMode) {
            false
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)

            try {
                getWordsInString(
                    cachedInput.rawText.substring(
                        0,
                        (selection.start - cachedInput.offset).coerceAtLeast(0)
                    )
                ).run {
                    get(size - n.coerceAtLeast(0)).range
                }.run {
                    ic.setSelection(first + cachedInput.offset, selection.start)
                }
            } catch (e: Exception) {
            }

            ic.commitText("", 1)

            cachedInput.update()
            ic.endBatchEdit()
            true
        }
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
        if (ic == null || !selection.isValid || n < 1 || isRawInputEditor) {
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
        if (ic == null || !selection.isValid || n < 1 || isRawInputEditor) {
            return ""
        }
        return ic.getTextBeforeCursor(n.coerceAtMost(selection.start), 0)?.toString() ?: ""
    }

    /**
     * Finds all words in the given string with the correct regex for current subtype.
     * TODO: currently only supports en-US
     *
     * @param string String to select words from
     * @return Words in [string] as a List of [MatchResult]
     */
    private fun getWordsInString(string: String): List<MatchResult> {
        val wordRegexPattern = "[\\p{L}]+".toRegex()
        return wordRegexPattern.findAll(
            string
        ).toList()
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
        val stringBeforeSelection =
            cachedInput.rawText.substring(0, (selection.start - cachedInput.offset).coerceAtLeast(0))
        getWordsInString(stringBeforeSelection).last().range.apply {
            selection.updateAndNotify(first + cachedInput.offset, selection.end)
        }
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
        val stringInsideSelection = cachedInput.rawText.substring(
            (selection.start - cachedInput.offset).coerceAtLeast(0),
            (selection.end - cachedInput.offset).coerceAtLeast(0)
        )
        getWordsInString(stringInsideSelection).first().range.apply {
            selection.updateAndNotify(selection.start + last + 1, selection.end)
        }
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
        val ic = inputConnection ?: return false
        return if (region == null || !region.isValid) {
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
        Timber.d("performClipboardCut")
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        florisClipboardManager.addNewPlaintext(selection.text)
        return sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL)
    }

    /**
     * Performs a copy command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCopy(): Boolean {
        Timber.d("performClipboardCopy")
        isPhantomSpaceActive = false
        wasPhantomSpaceActiveLastUpdate = false
        florisClipboardManager.addNewPlaintext(selection.text)
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
        Timber.d("Before commit clip data")
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
        if (isRawInputEditor) {
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
        return if (isRawInputEditor) {
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
}

/**
 * Class which marks a region of [CachedInput.rawText] and which provides length and
 * validation fields, as well as providing an easy way to get a [text] for this region.
 */
open class Region(
    private val editorInstance: EditorInstance,
    initStart: Int? = null,
    initEnd: Int? = null
) {
    /** The start marker for this region. */
    var start: Int = initStart ?: -1
        private set

    /** The end marker for this region. */
    var end: Int = initEnd ?: -1
        private set

    /** Returns true if the region's start and end markers are valid, false otherwise. */
    val isValid: Boolean
        get() = start >= 0 && end >= 0 && length >= 0

    /** The length of the region. */
    val length: Int
        get() = end - start

    /**
     * Dynamically returns a portion of the [CachedInput.rawText] marked by start and end.
     * Returns an empty string if the editorInstance is invalid or if the region is outside the
     * scope of the cached text.
     */
    val text: String
        get() {
            val eiText = editorInstance.cachedInput.rawText
            val eiStart = (start - editorInstance.cachedInput.offset).coerceIn(0, eiText.length)
            val eiEnd = (end - editorInstance.cachedInput.offset).coerceIn(0, eiText.length)
            return if (!isValid || eiEnd - eiStart <= 0) {
                ""
            } else {
                eiText.substring(eiStart, eiEnd)
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

    override operator fun equals(other: Any?): Boolean {
        return if (other is Region) {
            start == other.start && end == other.end
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        return result
    }

    override fun toString(): String {
        return "Region(start=$start,end=$end)"
    }
}

/**
 * Class which holds selection attributes and returns the correct text for set selection based on
 * the text in [editorInstance].
 */
class Selection(private val editorInstance: EditorInstance) : Region(editorInstance) {
    val isCursorMode: Boolean
        get() = length == 0 && isValid
    val isSelectionMode: Boolean
        get() = length != 0 && isValid

    /**
     * Same as [update], but also notifies the input connection linked by [editorInstance] of this
     * selection change.
     */
    fun updateAndNotify(newStart: Int, newEnd: Int): Boolean {
        return super.update(newStart, newEnd) && if (!editorInstance.isRawInputEditor) {
            editorInstance.inputConnection?.setSelection(newStart, newEnd) ?: false
        } else {
            false
        }
    }
}

/**
 * Class which holds the cached text as well as manages the parsing and evaluation of the current
 * word / words before&after the cursor.
 */
class CachedInput(private val editorInstance: EditorInstance) {
    private val wordsBeforeCurrent: MutableList<Region> = mutableListOf()
    val currentWord: Region = Region(editorInstance)
    private val wordsAfterCurrent: MutableList<Region> = mutableListOf()

    /**
     * The expected maximum length of the input text in the target app's editor. This is only the
     * safe-to-assume value, the actual maximum value may be higher.
     */
    var expectedMaxLength: Int = 0
        private set

    /**
     * The offset of the [rawText] from the selection root (index 0). This value is especially
     * relevant if the text before the cursor is longer than [CACHED_TEXT_N_CHARS_BEFORE_CURSOR].
     */
    var offset: Int = 0
        private set

    /**
     * The raw cached input text of the target app's editor. This cached value may be incomplete if
     * the target app's editor text is bigger than [CACHED_TEXT_N_CHARS_BEFORE_CURSOR] and
     * [CACHED_TEXT_N_CHARS_AFTER_CURSOR], but always caches the relevant text around the cursor.
     */
    var rawText: StringBuilder = StringBuilder()
        private set

    companion object {
        private const val CACHED_TEXT_N_CHARS_BEFORE_CURSOR: Int = 128
        private const val CACHED_TEXT_N_CHARS_AFTER_CURSOR: Int = 48

        private val WORD_EVAL_REGEX = """[^\p{L}\']""".toRegex()
        private val WORD_SPLIT_REGEX_EN = """((?<=$WORD_EVAL_REGEX)|(?=$WORD_EVAL_REGEX))""".toRegex()

        fun isWordComponent(string: String): Boolean {
            return !WORD_EVAL_REGEX.matches(string)
        }
    }

    /**
     * Returns a word region for a given index. If the given index does not point to a valid word,
     * a region with start&end values of -1 is returned.
     *
     * @param index The index of the word to get. 0 is equivalent to [currentWord], a negative
     *  index searches before the cursor and a positive index after it.
     * @return The region for the word at [index]. Be sure to check [Region.isValid] if the returned
     *  region is actually valid and can be used.
     */
    fun getWordForIndex(index: Int): Region {
        return when {
            index == 0 -> {
                currentWord
            }
            index > 0 -> {
                wordsAfterCurrent.getOrElse(index - 1) { Region(editorInstance) }
            }
            else -> {
                wordsBeforeCurrent.getOrElse(wordsBeforeCurrent.size - index.times(-1)) { Region(editorInstance) }
            }
        }
    }

    fun getWordHistory(maxCount: Int): List<String> {
        val retList = mutableListOf<String>()
        for ((n, region) in wordsBeforeCurrent.reversed().withIndex()) {
            if (n == maxCount) {
                break
            }
            retList.add(region.text)
        }
        return retList.toList()
    }

    /**
     * Updates the [rawText] and the [offset].
     */
    fun update() = editorInstance.run {
        val ic = inputConnection
        if (ic == null || selection.isSelectionMode) {
            offset = 0
            rawText.clear()
            expectedMaxLength = 0
        } else {
            val textBefore = getTextBeforeCursor(CACHED_TEXT_N_CHARS_BEFORE_CURSOR)
            val textSelected = ic.getSelectedText(0) ?: ""
            val textAfter = getTextAfterCursor(CACHED_TEXT_N_CHARS_AFTER_CURSOR)
            offset = (selection.start - textBefore.length).coerceAtLeast(0)
            rawText.apply {
                clear()
                append(textBefore)
                append(textSelected)
                append(textAfter)
            }
            expectedMaxLength = offset + rawText.length
        }
        reevaluate()
    }

    /**
     * Evaluates the current word as well as the words before&after the cursor in the linked editor
     * instance based on the current cursor position and given [splitRegex] and [wordRegex].
     *
     * @param splitRegex The delimiter regex which should be used to split up the content text and
     *  find words. May differ from locale to locale.
     * @param wordRegex The word validation regex used to verify that a given substring is really a
     *  word. May differ from locale to locale.
     * @return True on success, false otherwise.
     */
    private fun reevaluate(splitRegex: Regex, wordRegex: Regex): Boolean = editorInstance.run {
        wordsBeforeCurrent.clear()
        currentWord.update(-1, -1)
        wordsAfterCurrent.clear()

        if (selection.isValid && selection.isCursorMode) {
            val selStart = (selection.start - offset).coerceAtLeast(0)
            val words = rawText.split(splitRegex)
            var pos = 0
            for (word in words) {
                if (word.isNotEmpty() && !word.matches(wordRegex)) {
                    if (selStart >= pos && selStart <= (pos + word.length)) {
                        if (!editorInstance.isPhantomSpaceActive) {
                            currentWord.update(pos + offset, pos + offset + word.length)
                        } else {
                            wordsBeforeCurrent.add(Region(editorInstance, pos + offset, pos + offset + word.length))
                        }
                    } else if (pos < selStart) {
                        wordsBeforeCurrent.add(Region(editorInstance, pos + offset, pos + offset + word.length))
                    } else {
                        wordsAfterCurrent.add(Region(editorInstance, pos + offset, pos + offset + word.length))
                    }
                }
                pos += word.length
            }
            true
        } else {
            false
        }
    }

    /**
     * Evaluates the current word as well as the words before&after the cursor in the linked editor
     * instance based on the current cursor position with a separate regex for each subtype.
     * TODO: currently only supports en-US
     */
    fun reevaluate() {
        reevaluate(WORD_SPLIT_REGEX_EN, WORD_EVAL_REGEX)
    }

    @Suppress("unused")
    internal fun dump(): String {
        return StringBuilder().run {
            append("_\nwordsBeforeCursor = ")
            append(wordsBeforeCurrent.joinToString(","))
            append("\ncurrentWord = ")
            append(currentWord.toString())
            append("\nwordsAfterCursor = ")
            append(wordsAfterCurrent.joinToString(","))
            toString()
        }
    }
}
