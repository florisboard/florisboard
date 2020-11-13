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

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import androidx.annotation.RequiresApi
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import java.lang.StringBuilder

// Constants for detectLastUnicodeCharacterLengthBeforeCursor method
private const val LIGHT_SKIN_TONE           = 0x1F3FB
private const val MEDIUM_LIGHT_SKIN_TONE    = 0x1F3FC
private const val MEDIUM_SKIN_TONE          = 0x1F3FD
private const val MEDIUM_DARK_SKIN_TONE     = 0x1F3FE
private const val DARK_SKIN_TONE            = 0x1F3FF
private const val RED_HAIR                  = 0x1F9B0
private const val CURLY_HAIR                = 0x1F9B1
private const val WHITE_HAIR                = 0x1F9B2
private const val BALD                      = 0x1F9B3
private const val ZERO_WIDTH_JOINER         =  0x200D
private const val VARIATION_SELECTOR        =  0xFE0F

// Array which holds all variations for easier checking (convenience only)
private val emojiVariationArray: Array<Int> = arrayOf(
    LIGHT_SKIN_TONE,
    MEDIUM_LIGHT_SKIN_TONE,
    MEDIUM_SKIN_TONE,
    MEDIUM_DARK_SKIN_TONE,
    DARK_SKIN_TONE,
    RED_HAIR,
    CURLY_HAIR,
    WHITE_HAIR,
    BALD
)

/**
 * Class which holds information relevant to an editor instance like the input [cachedText], [selection],
 * [inputAttributes], [imeOptions], etc. This class is thought to be an improved [EditorInfo]
 * object which also holds the state of the currently focused input editor.
 */
class EditorInstance private constructor(private val ims: InputMethodService?) {
    val cursorCapsMode: InputAttributes.CapsMode
        get() {
            val ic = ims?.currentInputConnection ?: return InputAttributes.CapsMode.NONE
            return InputAttributes.CapsMode.fromFlags(
                ic.getCursorCapsMode(inputAttributes.capsMode.toFlags())
            )
        }
    var currentWord: Region = Region(this)
        private set
    var imeOptions: ImeOptions = ImeOptions.fromImeOptionsInt(EditorInfo.IME_NULL)
        private set
    var inputAttributes: InputAttributes = InputAttributes.fromInputTypeInt(InputType.TYPE_NULL)
        private set
    var isComposingEnabled: Boolean = false
        set(v) {
            field = v
            reevaluateCurrentWord()
            if (v) {
                markComposingRegion(currentWord)
            } else {
                markComposingRegion(null)
            }
        }
    var isNewSelectionInBoundsOfOld: Boolean = false
        private set
    var isRawInputEditor: Boolean = true
        private set
    var packageName: String = "undefined"
        private set
    var selection: Selection = Selection(this)
        private set
    val cachedText: String
        get() = cachedTextInternal.toString()
    private var cachedTextInternal: StringBuilder = StringBuilder("")

    companion object {
        fun default(): EditorInstance {
            return EditorInstance(null)
        }

        fun from(editorInfo: EditorInfo?, ims: InputMethodService?): EditorInstance {
            return if (editorInfo == null) { default() } else {
                EditorInstance(ims).apply {
                    imeOptions = ImeOptions.fromImeOptionsInt(editorInfo.imeOptions)
                    inputAttributes = InputAttributes.fromInputTypeInt(editorInfo.inputType)
                    packageName = editorInfo.packageName
                    /*selection = Selection(this).apply {
                        start = editorInfo.initialSelStart
                        end = editorInfo.initialSelEnd
                    }*/
                }
            }
        }
    }

    init {
        updateEditorState()
        reevaluateCurrentWord()
    }

    /**
     * Event handler which reacts to selection updates coming from the target app's editor.
     */
    fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int
    ) {
        updateEditorState()
        isNewSelectionInBoundsOfOld =
            newSelStart >= (oldSelStart - 1) &&
            newSelStart <= (oldSelStart + 1) &&
            newSelEnd >= (oldSelEnd - 1) &&
            newSelEnd <= (oldSelEnd + 1)
        selection.apply {
            start = newSelStart
            end = newSelEnd
        }
        reevaluateCurrentWord()
        if (selection.isCursorMode && isComposingEnabled && !isRawInputEditor) {
            markComposingRegion(currentWord)
        } else {
            markComposingRegion(null)
        }
    }

    /**
     * Completes the given [text] in the current composing region. Does nothing if the current
     * composing region is of zero length or null.
     *
     * @param text The text to complete in this editor's composing region.
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun commitCompletion(text: String): Boolean {
        return if (isRawInputEditor) {
            false
        } else {
            false // TODO: complete text here
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
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun commitText(text: String): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        if (isRawInputEditor) {
            return ic.commitText(text, 1)
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)
            if (selection.isCursorMode) {
                cachedTextInternal.insert(selection.start, text)
            } else if (selection.isSelectionMode) {
                cachedTextInternal.replace(selection.start, selection.end, text)
            }
            selection.apply {
                start += text.length
                end = start
            }
            reevaluateCurrentWord()
            ic.commitText(text, 1)
            if (isComposingEnabled) {
                markComposingRegion(currentWord)
            }
            ic.setSelection(selection.start, selection.end)
            ic.endBatchEdit()
            return true
        }
    }

    /**
     * Executes a backward delete on this editor's text. If a text selection is active, all
     * characters inside this selection will be removed, else only the left-most character from
     * the cursor's position.
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun deleteBackwards(): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        if (isRawInputEditor) {
            return sendSystemKeyEvent(KeyEvent.KEYCODE_DEL)
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)
            if (selection.isCursorMode && selection.start > 0) {
                val length = detectLastUnicodeCharacterLengthBeforeCursor()
                cachedTextInternal.replace(selection.start - length, selection.start, "")
                selection.apply {
                    start -= length
                    end = start
                }
                ic.deleteSurroundingText(length, 0)
            } else if (selection.isSelectionMode) {
                cachedTextInternal.replace(selection.start, selection.end, "")
                selection.apply {
                    end = start
                }
                ic.commitText("", 1)
            }
            reevaluateCurrentWord()
            if (isComposingEnabled) {
                markComposingRegion(currentWord)
            }
            ic.setSelection(selection.start, selection.end)
            ic.endBatchEdit()
            return true
        }
    }

    /**
     * Deletes [n] words before the current cursor's position.
     * NOTE: this implementation does currently only delete currentWord. This is due to change in
     * future versions.
     *
     * @param n The number of words to delete before the cursor. Must be greater than 0 or this
     *  method will fail.
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun deleteWordsBeforeCursor(n: Int): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        if (n < 1 || isRawInputEditor) {
            return false
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)
            if (currentWord.isValid) {
                cachedTextInternal.replace(currentWord.start, currentWord.end, "")
                selection.apply {
                    start = currentWord.start
                    end = start
                }
                ic.setSelection(currentWord.start, currentWord.end)
                ic.commitText("", 1)
            }
            reevaluateCurrentWord()
            ic.setSelection(selection.start, selection.end)
            ic.endBatchEdit()
            return true
        }
    }

    /**
     * Gets [n] characters after the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get after the cursor. Must be greater than 0 or this
     *  method will fail.
     *
     * @returns [n] or less characters after the cursor.
     */
    fun getTextAfterCursor(n: Int): String {
        if (!selection.isValid || n < 1 || isRawInputEditor) {
            return ""
        }
        val from = selection.end
        val to = (selection.end + n).coerceAtMost(cachedTextInternal.length)
        return cachedTextInternal.substring(from, to)
    }

    /**
     * Gets [n] characters before the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get before the cursor. Must be greater than 0 or this
     *  method will fail.
     *
     * @returns [n] or less characters after the cursor.
     */
    fun getTextBeforeCursor(n: Int): String {
        if (!selection.isValid || n < 1 || isRawInputEditor) {
            return ""
        }
        val from = (selection.start - n).coerceAtLeast(0)
        val to = selection.start
        return cachedTextInternal.substring(from, to)
    }

    /**
     * Performs an enter key press on the current input editor.
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun performEnter(): Boolean {
        return if (isRawInputEditor) {
            sendSystemKeyEvent(KeyEvent.KEYCODE_ENTER)
        } else {
            commitText("\n")
        }
    }

    /**
     * Performs a given [action] on the current input editor.
     *
     * @param action The action to be performed on this editor instance.
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun performEnterAction(action: ImeOptions.Action): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return ic.performEditorAction(action.toInt())
    }

    /**
     * Sends a given [keyCode] as a [KeyEvent.ACTION_DOWN].
     *
     * @param keyCode The key code to send, use a key code defined in Android's [KeyEvent], not in
     *  [KeyCode] or this call may send a weird character, as this key codes do not match!!
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun sendSystemKeyEvent(keyCode: Int): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }

    /**
     * Sends a given [keyCode] as a [KeyEvent.ACTION_DOWN] with ALT pressed.
     *
     * @param keyCode The key code to send, use a key code defined in Android's [KeyEvent], not in
     *  [KeyCode] or this call may send a weird character, as this key codes do not match!!
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun sendSystemKeyEventAlt(keyCode: Int): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return ic.sendKeyEvent(
            KeyEvent(
                0,
                1,
                KeyEvent.ACTION_DOWN, keyCode,
                0,
                KeyEvent.META_ALT_LEFT_ON
            )
        )
    }

    /**
     * Sets the selection region of this instance and notifies the input connection.
     *
     * @param from The start index of the selection in characters (inclusive).
     * @param to The end index of the selection in characters (exclusive).
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    fun setSelection(from: Int, to: Int): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return if (isRawInputEditor) {
            selection.apply {
                start = -1
                end = -1
            }
            false
        } else {
            selection.apply {
                start = from
                end = to
            }
            ic.setSelection(from, to)
        }
    }

    /**
     * Detects the length of the character before the cursor, as many Unicode characters nowadays
     * are longer than 1 Java char and thus the length has to be calculated in order to avoid
     * deleting only half of an emoji...
     * Is used primarily in [deleteBackwards].
     *
     * @returns The length of the last Unicode character, in Java characters or 0 if the current
     *  selection is invalid.
     */
    private fun detectLastUnicodeCharacterLengthBeforeCursor(): Int {
        if (!selection.isValid) {
            return 0
        }
        var charIndex = 0
        var charLength = 0
        var charShouldGlue = false
        val textToSearch = cachedTextInternal.substring(0, selection.start.coerceAtMost(cachedTextInternal.length))
        var i = 0
        while (i < textToSearch.length) {
            val cp = textToSearch.codePointAt(i)
            val cpLength = Character.charCount(cp)
            when {
                charShouldGlue || cp == VARIATION_SELECTOR || emojiVariationArray.contains(cp) -> {
                    charLength += cpLength
                    charShouldGlue = false
                }
                cp == ZERO_WIDTH_JOINER -> {
                    charLength += cpLength
                    charShouldGlue = true
                }
                else -> {
                    charIndex = i
                    charLength = 0
                    charShouldGlue = false
                }
            }
            i += cpLength
        }
        return textToSearch.length - charIndex
    }

    /**
     * Marks a given [region] as composing and notifies the input connection.
     *
     * @param region The region which should be marked as composing.
     *
     * @returns True on success, false if an error occurred or the input connection is invalid.
     */
    private fun markComposingRegion(region: Region?): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return when (region) {
            null -> ic.finishComposingText()
            else -> if (region.isValid) {
                ic.setComposingRegion(region.start, region.end)
            } else {
                ic.finishComposingText()
            }
        }
    }

    /**
     * Evaluates the current word in this editor instance based on the current cursor position and
     * given delimiter [regex].
     *
     * @param regex The delimiter regex which should be used to split up the content text and find
     *  words. May differ from locale to locale.
     *
     * @returns True on success, false if no current word could be found.
     */
    private fun reevaluateCurrentWord(regex: Regex): Boolean {
        var foundValidWord = false
        if (selection.isValid && selection.isCursorMode) {
            val words = cachedText.split("((?<=$regex)|(?=$regex))".toRegex())
            var pos = 0
            for (word in words) {
                if (selection.start >= pos && selection.start <= pos + word.length &&
                    word.isNotEmpty() && !word.matches(regex)) {
                    currentWord.apply {
                        start = pos
                        end = pos + word.length
                    }
                    foundValidWord = true
                    break
                } else {
                    pos += word.length
                }
            }
        }
        if (!foundValidWord) {
            currentWord.apply {
                start = -1
                end = -1
            }
        }
        return foundValidWord
    }

    /**
     * Evaluates the current word with the correct delimiter regex for current subtype.
     * TODO: currently only supports en-US
     */
    private fun reevaluateCurrentWord() {
        val regex = "[^\\p{L}]".toRegex()
        reevaluateCurrentWord(regex)
    }

    /**
     * Gets the current text from the app's editor view.
     *
     * @returns The target editor's content string.
     */
    private fun updateEditorState() {
        val ic = ims?.currentInputConnection
        val et = ic?.getExtractedText(
            ExtractedTextRequest(), 0
        )
        val text = et?.text
        cachedTextInternal.setLength(0)
        if (ic == null || et == null || text == null) {
            isRawInputEditor = true
            selection.apply {
                start = -1
                end = -1
            }
        } else {
            isRawInputEditor = false
            cachedTextInternal.append(text)
            selection.apply {
                start = et.selectionStart.coerceAtMost(cachedTextInternal.length)
                end = et.selectionEnd.coerceAtMost(cachedTextInternal.length)
            }
        }
        reevaluateCurrentWord()
    }
}

/**
 * Class which holds the same information as an [EditorInfo.imeOptions] int but more accessible and
 * readable.
 */
class ImeOptions private constructor(imeOptions: Int) {
    val action: Action = Action.fromInt(imeOptions)
    val flagForceAscii: Boolean = imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII > 0
    val flagNavigateNext: Boolean = imeOptions and EditorInfo.IME_FLAG_NAVIGATE_NEXT > 0
    val flagNavigatePrevious: Boolean = imeOptions and EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS > 0
    val flagNoAccessoryAction: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION > 0
    val flagNoEnterAction: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0
    val flagNoExtractUi: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_EXTRACT_UI > 0
    val flagNoFullscreen: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_FULLSCREEN > 0
    @RequiresApi(Build.VERSION_CODES.O)
    val flagNoPersonalizedLearning: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING > 0

    companion object {
        fun fromImeOptionsInt(imeOptions: Int): ImeOptions {
            return ImeOptions(imeOptions)
        }
    }

    enum class Action {
        DONE,
        GO,
        NEXT,
        NONE,
        PREVIOUS,
        SEARCH,
        SEND,
        UNSPECIFIED;

        companion object {
            fun fromInt(raw: Int): Action {
                return when (raw and EditorInfo.IME_MASK_ACTION) {
                    EditorInfo.IME_ACTION_DONE -> DONE
                    EditorInfo.IME_ACTION_GO -> GO
                    EditorInfo.IME_ACTION_NEXT -> NEXT
                    EditorInfo.IME_ACTION_NONE -> NONE
                    EditorInfo.IME_ACTION_PREVIOUS -> PREVIOUS
                    EditorInfo.IME_ACTION_SEARCH -> SEARCH
                    EditorInfo.IME_ACTION_SEND -> SEND
                    EditorInfo.IME_ACTION_UNSPECIFIED -> UNSPECIFIED
                    else -> NONE
                }
            }
        }

        fun toInt(): Int {
            return when (this) {
                DONE -> EditorInfo.IME_ACTION_DONE
                GO -> EditorInfo.IME_ACTION_GO
                NEXT -> EditorInfo.IME_ACTION_NEXT
                NONE -> EditorInfo.IME_ACTION_NONE
                PREVIOUS -> EditorInfo.IME_ACTION_PREVIOUS
                SEARCH -> EditorInfo.IME_ACTION_SEARCH
                SEND -> EditorInfo.IME_ACTION_SEND
                UNSPECIFIED-> EditorInfo.IME_ACTION_UNSPECIFIED
            }
        }
    }
}

/**
 * Class which holds the same information as an [EditorInfo.inputType] int but more accessible and
 * readable.
 */
class InputAttributes private constructor(inputType: Int) {
    val type: Type
    val variation: Variation
    val capsMode: CapsMode
    var flagNumberDecimal: Boolean = false
        private set
    var flagNumberSigned: Boolean = false
        private set
    var flagTextAutoComplete: Boolean = false
        private set
    var flagTextAutoCorrect: Boolean = false
        private set
    var flagTextImeMultiLine: Boolean = false
        private set
    var flagTextMultiLine: Boolean = false
        private set
    var flagTextNoSuggestions: Boolean = false
        private set

    init {
        when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_DATETIME -> {
                type = Type.DATETIME
                variation = when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_DATETIME_VARIATION_DATE -> Variation.DATE
                    InputType.TYPE_DATETIME_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_DATETIME_VARIATION_TIME -> Variation.TIME
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.NONE
            }
            InputType.TYPE_CLASS_NUMBER -> {
                type = Type.NUMBER
                variation = when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_NUMBER_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD -> Variation.PASSWORD
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.NONE
                flagNumberDecimal = inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL > 0
                flagNumberSigned = inputType and InputType.TYPE_NUMBER_FLAG_SIGNED > 0
            }
            InputType.TYPE_CLASS_PHONE -> {
                type = Type.PHONE
                variation = Variation.NORMAL
                capsMode = CapsMode.NONE
            }
            InputType.TYPE_CLASS_TEXT -> {
                type = Type.TEXT
                variation = when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> Variation.EMAIL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> Variation.EMAIL_SUBJECT
                    InputType.TYPE_TEXT_VARIATION_FILTER -> Variation.FILTER
                    InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE -> Variation.LONG_MESSAGE
                    InputType.TYPE_TEXT_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_TEXT_VARIATION_PASSWORD -> Variation.PASSWORD
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> Variation.PERSON_NAME
                    InputType.TYPE_TEXT_VARIATION_PHONETIC -> Variation.PHONETIC
                    InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> Variation.POSTAL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> Variation.SHORT_MESSAGE
                    InputType.TYPE_TEXT_VARIATION_URI -> Variation.URI
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> Variation.VISIBLE_PASSWORD
                    InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> Variation.WEB_EDIT_TEXT
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> Variation.WEB_EMAIL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> Variation.WEB_PASSWORD
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.fromFlags(inputType)
                flagTextAutoComplete = inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE > 0
                flagTextAutoCorrect = inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT > 0
                flagTextImeMultiLine = inputType and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE > 0
                flagTextMultiLine = inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE > 0
                flagTextNoSuggestions = inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS > 0
            }
            else -> {
                type = Type.TEXT
                variation = Variation.NORMAL
                capsMode = CapsMode.NONE
            }
        }
    }

    companion object {
        fun fromInputTypeInt(inputType: Int): InputAttributes {
            return InputAttributes(inputType)
        }
    }

    enum class Type {
        DATETIME,
        NUMBER,
        PHONE,
        TEXT;
    }

    enum class Variation {
        DATE,
        EMAIL_ADDRESS,
        EMAIL_SUBJECT,
        FILTER,
        LONG_MESSAGE,
        NORMAL,
        PASSWORD,
        PERSON_NAME,
        PHONETIC,
        POSTAL_ADDRESS,
        SHORT_MESSAGE,
        TIME,
        URI,
        VISIBLE_PASSWORD,
        WEB_EDIT_TEXT,
        WEB_EMAIL_ADDRESS,
        WEB_PASSWORD;
    }

    enum class CapsMode {
        ALL,
        NONE,
        SENTENCES,
        WORDS;

        companion object {
            fun fromFlags(flags: Int): CapsMode {
                return when {
                    flags and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS > 0 -> ALL
                    flags and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES > 0 -> SENTENCES
                    flags and InputType.TYPE_TEXT_FLAG_CAP_WORDS > 0 -> WORDS
                    else -> NONE
                }
            }
        }

        fun toFlags(): Int {
            return when (this) {
                ALL -> InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                SENTENCES -> InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                WORDS -> InputType.TYPE_TEXT_FLAG_CAP_WORDS
                else -> 0
            }
        }
    }
}

/**
 * Class which marks a region of the [text] in [editorInstance].
 */
open class Region(private val editorInstance: EditorInstance) {
    var start: Int = -1
    var end: Int = -1
    val isValid: Boolean
        get() = start >= 0 && end >= 0 && length >= 0
    val length: Int
        get() = end - start
    val text: String
        get() {
            val eiText = editorInstance.cachedText
            return if (!isValid || start >= eiText.length) {
                ""
            } else {
                val end = if (end >= eiText.length) { eiText.length } else { end }
                editorInstance.cachedText.substring(start, end)
            }
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
}
