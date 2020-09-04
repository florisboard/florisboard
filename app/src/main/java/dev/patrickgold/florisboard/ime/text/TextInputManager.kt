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

package dev.patrickgold.florisboard.ime.text

import android.content.ClipData
import android.content.Context
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.*
import android.widget.LinearLayout
import android.widget.ViewFlipper
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputView
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editing.EditingKeyboardView
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.text.smartbar.SmartbarManager
import kotlinx.coroutines.*
import java.util.*

/**
 * TextInputManager is responsible for managing everything which is related to text input. All of
 * the following count as text input: character, numeric (+advanced), phone and symbol layouts.
 *
 * All of the UI for the different keyboard layouts are kept under the same container element and
 * are separated from media-related UI. The core [FlorisBoard] will pass any event defined in
 * [FlorisBoard.EventListener] through to this class.
 *
 * TextInputManager also keeps track of the current composing word and syncs this value with the
 * Smartbar, which, depending on the mode and variation, may create candidates.
 * @see SmartbarManager.generateCandidatesFromComposing for more information.
 */
class TextInputManager private constructor() : CoroutineScope by MainScope(),
    FlorisBoard.EventListener {

    private val florisboard = FlorisBoard.getInstance()

    private var activeKeyboardMode: KeyboardMode? = null
    private val keyboardViews = EnumMap<KeyboardMode, KeyboardView>(KeyboardMode::class.java)
    private var editingKeyboardView: EditingKeyboardView? = null
    private val osHandler = Handler()
    private var textViewFlipper: ViewFlipper? = null
    var textViewGroup: LinearLayout? = null

    var keyVariation: KeyVariation = KeyVariation.NORMAL
    private val layoutManager = LayoutManager(florisboard)
    lateinit var smartbarManager: SmartbarManager

    // Caps/Space related properties
    var caps: Boolean = false
        private set
    var capsLock: Boolean = false
        private set
    private var cursorCapsMode: CapsMode = CapsMode.NONE
    private var editorCapsMode: CapsMode = CapsMode.NONE
    private var hasCapsRecentlyChanged: Boolean = false
    private var hasSpaceRecentlyPressed: Boolean = false

    // Composing text related properties
    private var composingText: String? = null
    private var composingTextStart: Int? = null
    private var cursorPos: Int = 0
    private var isComposingEnabled: Boolean = false
    var isManualSelectionMode: Boolean = false
    private var isManualSelectionModeLeft: Boolean = false
    private var isManualSelectionModeRight: Boolean = false
    val isTextSelected: Boolean
        get() = selectionEnd - selectionStart != 0
    private var lastCursorAnchorInfo: CursorAnchorInfo? = null
    private var selectionStart: Int = 0
    private val selectionStartMin: Int = 0
    private var selectionEnd: Int = 0
    private var selectionEndMax: Int = 0

    companion object {
        private var instance: TextInputManager? = null

        @Synchronized
        fun getInstance(): TextInputManager {
            if (instance == null) {
                instance = TextInputManager()
            }
            return instance!!
        }
    }

    init {
        florisboard.addEventListener(this)
    }

    /**
     * Non-UI-related setup + preloading of all required computed layouts (asynchronous in the
     * background).
     */
    override fun onCreate() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onCreate()")

        for (mode in KeyboardMode.values()) {
            if (mode == KeyboardMode.CHARACTERS) {
                var subtypes = florisboard.subtypeManager.subtypes
                if (subtypes.isEmpty()) {
                    subtypes = listOf(Subtype.DEFAULT)
                }
                for (subtype in subtypes) {
                    layoutManager.preloadComputedLayout(mode, subtype)
                }
            } else {
                layoutManager.preloadComputedLayout(mode, florisboard.activeSubtype)
            }
        }
        smartbarManager = SmartbarManager.getInstance()
    }

    private suspend fun addKeyboardView(mode: KeyboardMode) {
        val keyboardView = KeyboardView(florisboard.context)
        keyboardView.computedLayout = layoutManager.fetchComputedLayoutAsync(mode, florisboard.activeSubtype).await()
        keyboardViews[mode] = keyboardView
        withContext(Dispatchers.Main) {
            textViewFlipper?.addView(keyboardView)
        }
    }

    /**
     * Sets up the newly registered input view.
     */
    override fun onRegisterInputView(inputView: InputView) {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onRegisterInputView(inputView)")

        launch(Dispatchers.Default) {
            textViewGroup = inputView.findViewById(R.id.text_input)
            textViewFlipper = inputView.findViewById(R.id.text_input_view_flipper)
            editingKeyboardView = inputView.findViewById(R.id.editing)

            val activeKeyboardMode = getActiveKeyboardMode()
            addKeyboardView(activeKeyboardMode)
            withContext(Dispatchers.Main) {
                setActiveKeyboardMode(activeKeyboardMode)
            }
            for (mode in KeyboardMode.values()) {
                if (mode != activeKeyboardMode) {
                    addKeyboardView(mode)
                }
            }
        }
    }

    /**
     * Cancels all coroutines and cleans up.
     */
    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

        cancel()
        osHandler.removeCallbacksAndMessages(null)
        layoutManager.onDestroy()
        smartbarManager.onDestroy()
        instance = null
    }

    /**
     * Evaluates the [activeKeyboardMode], [keyVariation] and [isComposingEnabled] property values
     * when starting to interact with a input editor. Also resets the composing texts and sets the
     * initial caps mode accordingly.
     */
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        val keyboardMode = when (info) {
            null -> KeyboardMode.CHARACTERS
            else -> when (info.inputType and InputType.TYPE_MASK_CLASS) {
                InputType.TYPE_CLASS_NUMBER -> {
                    keyVariation = KeyVariation.NORMAL
                    KeyboardMode.NUMERIC
                }
                InputType.TYPE_CLASS_PHONE -> {
                    keyVariation = KeyVariation.NORMAL
                    KeyboardMode.PHONE
                }
                InputType.TYPE_CLASS_TEXT -> {
                    keyVariation = when (info.inputType and InputType.TYPE_MASK_VARIATION) {
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> {
                            KeyVariation.EMAIL_ADDRESS
                        }
                        InputType.TYPE_TEXT_VARIATION_PASSWORD,
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> {
                            KeyVariation.PASSWORD
                        }
                        InputType.TYPE_TEXT_VARIATION_URI -> {
                            KeyVariation.URI
                        }
                        else -> {
                            KeyVariation.NORMAL
                        }
                    }
                    KeyboardMode.CHARACTERS
                }
                else -> {
                    keyVariation = KeyVariation.NORMAL
                    KeyboardMode.CHARACTERS
                }
            }
        }
        isComposingEnabled = when (keyboardMode) {
            KeyboardMode.NUMERIC,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> false
            else -> keyVariation != KeyVariation.PASSWORD && florisboard.prefs.suggestion.enabled
        }
        updateCapsState()
        resetComposingText()
        setActiveKeyboardMode(keyboardMode)
        smartbarManager.onStartInputView(keyboardMode, isComposingEnabled)
    }

    /**
     * Handle stuff when finishing to interact with a input editor.
     */
    override fun onFinishInputView(finishingInput: Boolean) {
        smartbarManager.onFinishInputView()
    }

    override fun onWindowShown() {
        keyboardViews[KeyboardMode.CHARACTERS]?.updateVisibility()
        smartbarManager.onWindowShown()
    }

    /**
     * Gets [activeKeyboardMode].
     *
     * @return If null [KeyboardMode.CHARACTERS], else [activeKeyboardMode].
     */
    fun getActiveKeyboardMode(): KeyboardMode {
        return activeKeyboardMode ?: KeyboardMode.CHARACTERS
    }

    /**
     * Sets [activeKeyboardMode] and updates the [SmartbarManager.isQuickActionsVisible].
     */
    fun setActiveKeyboardMode(mode: KeyboardMode) {
        textViewFlipper?.displayedChild = textViewFlipper?.indexOfChild(when (mode) {
            KeyboardMode.EDITING -> editingKeyboardView
            else -> keyboardViews[mode]
        }) ?: 0
        keyboardViews[mode]?.updateVisibility()
        keyboardViews[mode]?.requestLayout()
        keyboardViews[mode]?.requestLayoutAllKeys()
        activeKeyboardMode = mode
        smartbarManager.isQuickActionsVisible = false
        isManualSelectionMode = false
        isManualSelectionModeLeft = false
        isManualSelectionModeRight = false
    }

    override fun onSubtypeChanged(newSubtype: Subtype) {
        launch {
            val keyboardView = keyboardViews[KeyboardMode.CHARACTERS]
            keyboardView?.computedLayout = layoutManager.fetchComputedLayoutAsync(KeyboardMode.CHARACTERS, newSubtype).await()
            keyboardView?.updateVisibility()
        }
    }

    /**
     * Main logic point for processing cursor updates as well as parsing the current composing word
     * and passing this info on to the [SmartbarManager] to turn it into candidate suggestions.
     */
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        cursorAnchorInfo ?: return
        lastCursorAnchorInfo = cursorAnchorInfo

        val ic = florisboard.currentInputConnection

        val isNewSelectionInBoundsOfOld =
            cursorAnchorInfo.selectionStart >= (selectionStart - 1) &&
            cursorAnchorInfo.selectionStart <= (selectionStart + 1) &&
            cursorAnchorInfo.selectionEnd >= (selectionEnd - 1) &&
            cursorAnchorInfo.selectionEnd <= (selectionEnd + 1)
        selectionStart = cursorAnchorInfo.selectionStart
        selectionEnd = cursorAnchorInfo.selectionEnd
        val inputText =
            (ic?.getExtractedText(ExtractedTextRequest(), 0)?.text ?: "").toString()
        selectionEndMax = inputText.length
        if (isComposingEnabled) {
            if (!isTextSelected) {
                val newCursorPos = cursorAnchorInfo.selectionStart
                val prevComposingText = (cursorAnchorInfo.composingText ?: "").toString()
                setComposingTextBasedOnInput(inputText, newCursorPos)
                if ((newCursorPos == cursorPos) && (composingText == prevComposingText)) {
                    // Ignore this, as nothing has changed
                } else {
                    cursorPos = newCursorPos
                    if (composingText != null && composingTextStart != null) {
                        ic?.setComposingRegion(
                            composingTextStart!!,
                            composingTextStart!! + composingText!!.length
                        )
                    } else {
                        resetComposingText()
                    }
                }
            } else {
                resetComposingText()
            }
            smartbarManager.generateCandidatesFromComposing(composingText)
        }
        if (!isNewSelectionInBoundsOfOld) {
            isManualSelectionMode = false
            isManualSelectionModeLeft = false
            isManualSelectionModeRight = false
        }
        updateCapsState()
        smartbarManager.onUpdateCursorAnchorInfo(cursorAnchorInfo)
    }

    /**
     * Resets the [composingText] and [composingTextStart] properties. Does NOT sync with
     * [SmartbarManager]!
     *
     * @param notifyInputConnection If the current input connection should be notified.
     */
    private fun resetComposingText(notifyInputConnection: Boolean = true) {
        if (notifyInputConnection) {
            val ic = florisboard.currentInputConnection
            ic?.finishComposingText()
        }
        composingText = null
        composingTextStart = null
    }

    /**
     * Tries to parse the [composingText] from a given [inputCursorPos] within [inputText].
     * Sets both [composingText] and [composingTextStart] to null if it fails, else to its
     * parsed values.
     *
     * @param inputText The input text to search in.
     * @param inputCursorPos The position where to search in [inputText].
     */
    private fun setComposingTextBasedOnInput(inputText: String, inputCursorPos: Int) {
        val words = inputText.split("[^\\p{L}]".toRegex())
        var pos = 0
        resetComposingText(false)
        for (word in words) {
            if (inputCursorPos >= pos && inputCursorPos <= pos + word.length && word.isNotEmpty()) {
                composingText = word
                composingTextStart = pos
                break
            } else {
                pos += word.length + 1
            }
        }
    }

    /**
     * Should primarily pe used by [SmartbarManager.candidateViewOnClickListener] to commit
     * a candidate if a user has pressed on it.
     */
    fun commitCandidate(candidateText: String) {
        val ic = florisboard.currentInputConnection
        ic?.setComposingText(candidateText, 1)
        ic?.finishComposingText()
    }

    /**
     * Parses the [CapsMode] out of the given [flags].
     *
     * @param flags The input flags.
     * @return A [CapsMode] value.
     */
    private fun parseCapsModeFromFlags(flags: Int): CapsMode {
        return when {
            flags and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS > 0 -> {
                CapsMode.ALL
            }
            flags and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES > 0 -> {
                CapsMode.SENTENCES
            }
            flags and InputType.TYPE_TEXT_FLAG_CAP_WORDS > 0 -> {
                CapsMode.WORDS
            }
            else -> {
                CapsMode.NONE
            }
        }
    }

    /**
     * Fetches the current cursor caps mode from the current input connection.
     *
     * @return The [CapsMode] according to the returned flags by the current input connection.
     */
    private fun fetchCurrentCursorCapsMode(): CapsMode {
        val ic = florisboard.currentInputConnection
        val info = florisboard.currentInputEditorInfo
        val capsFlags = ic?.getCursorCapsMode(info.inputType) ?: 0
        return parseCapsModeFromFlags(capsFlags)
    }

    /**
     * Updates the current caps state according to the [cursorCapsMode], while respecting
     * [capsLock] property.
     */
    private fun updateCapsState() {
        cursorCapsMode = fetchCurrentCursorCapsMode()
        editorCapsMode = parseCapsModeFromFlags(florisboard.currentInputEditorInfo.inputType)
        if (!capsLock) {
            caps = cursorCapsMode != CapsMode.NONE
            keyboardViews[activeKeyboardMode]?.invalidateAllKeys()
        }
    }

    /**
     * Sends a given [keyCode] as a [KeyEvent.ACTION_DOWN].
     *
     * @param ic The input connection on which this operation should be performed.
     * @param keyCode The key code to send, use a key code defined in Android's [KeyEvent], not in
     *  [KeyCode] or this call may send a weird character, as this key codes do not match!!
     */
    private fun sendSystemKeyEvent(ic: InputConnection?, keyCode: Int) {
        ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }

    /**
     * Sends a given [keyCode] as a [KeyEvent.ACTION_DOWN] with ALT pressed.
     *
     * @param ic The input connection on which this operation should be performed.
     * @param keyCode The key code to send, use a key code defined in Android's [KeyEvent], not in
     *  [KeyCode] or this call may send a weird character, as this key codes do not match!!
     */
    private fun sendSystemKeyEventAlt(ic: InputConnection?, keyCode: Int) {
        ic?.sendKeyEvent(
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
     * Handles a [KeyCode.DELETE] event.
     */
    private fun handleDelete() {
        val ic = florisboard.currentInputConnection
        ic?.beginBatchEdit()
        resetComposingText()
        isManualSelectionMode = false
        isManualSelectionModeLeft = false
        isManualSelectionModeRight = false
        sendSystemKeyEvent(ic, KeyEvent.KEYCODE_DEL)
        ic?.endBatchEdit()
    }

    /**
     * Handles a [KeyCode.ENTER] event.
     */
    private fun handleEnter() {
        val ic = florisboard.currentInputConnection
        ic?.beginBatchEdit()
        resetComposingText()
        val action = florisboard.currentInputEditorInfo?.imeOptions ?: 0
        if (action and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0) {
            sendSystemKeyEvent(ic, KeyEvent.KEYCODE_ENTER)
        } else {
            when (action and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_GO,
                EditorInfo.IME_ACTION_NEXT,
                EditorInfo.IME_ACTION_PREVIOUS,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND -> {
                    ic?.performEditorAction(action)
                }
                else -> sendSystemKeyEvent(ic, KeyEvent.KEYCODE_ENTER)
            }
        }
        ic?.endBatchEdit()
    }

    /**
     * Handles a [KeyCode.SHIFT] event.
     */
    private fun handleShift() {
        if (hasCapsRecentlyChanged) {
            osHandler.removeCallbacksAndMessages(null)
            caps = true
            capsLock = true
            hasCapsRecentlyChanged = false
        } else {
            caps = !caps
            capsLock = false
            hasCapsRecentlyChanged = true
            osHandler.postDelayed({
                hasCapsRecentlyChanged = false
            }, 300)
        }
        keyboardViews[activeKeyboardMode]?.invalidateAllKeys()
    }

    /**
     * Handles a [KeyCode.SPACE] event. Also handles the auto-correction of two space taps if
     * enabled by the user.
     */
    private fun handleSpace() {
        val ic = florisboard.currentInputConnection
        if (florisboard.prefs.correction.doubleSpacePeriod) {
            if (hasSpaceRecentlyPressed) {
                osHandler.removeCallbacksAndMessages(null)
                val text = ic?.getTextBeforeCursor(2, 0) ?: ""
                if (text.length == 2 && !text.matches("""[.!?‽\s][\s]""".toRegex())) {
                    ic?.deleteSurroundingText(1, 0)
                    ic?.commitText(".", 1)
                }
                hasSpaceRecentlyPressed = false
            } else {
                hasSpaceRecentlyPressed = true
                osHandler.postDelayed({
                    hasSpaceRecentlyPressed = false
                }, 300)
            }
        }
        ic?.commitText(KeyCode.SPACE.toChar().toString(), 1)
    }

    /**
     * Handles [KeyCode] arrow and move events, behaves differently depending on text selection.
     */
    private fun handleArrow(code: Int) {
        val ic = florisboard.currentInputConnection
        resetComposingText()
        if (isTextSelected && isManualSelectionMode) {
            // Text is selected and it is manual selection -> Expand selection depending on started
            //  direction.
            when (code) {
                KeyCode.ARROW_DOWN -> {}
                KeyCode.ARROW_LEFT -> {
                    if (isManualSelectionModeLeft) {
                        ic?.setSelection(
                            (selectionStart - 1).coerceAtLeast(selectionStartMin),
                            selectionEnd
                        )
                    } else {
                        ic?.setSelection(selectionStart, selectionEnd - 1)
                    }
                }
                KeyCode.ARROW_RIGHT -> {
                    if (isManualSelectionModeRight) {
                        ic?.setSelection(
                            selectionStart,
                            (selectionEnd + 1).coerceAtMost(selectionEndMax)
                        )
                    } else {
                        ic?.setSelection(selectionStart + 1, selectionEnd)
                    }
                }
                KeyCode.ARROW_UP -> {}
                KeyCode.MOVE_HOME -> {
                    if (isManualSelectionModeLeft) {
                        ic?.setSelection(selectionStartMin, selectionEnd)
                    } else {
                        ic?.setSelection(selectionStartMin, selectionStart)
                    }
                }
                KeyCode.MOVE_END -> {
                    if (isManualSelectionModeRight) {
                        ic?.setSelection(selectionStart, selectionEndMax)
                    } else {
                        ic?.setSelection(selectionEnd, selectionEndMax)
                    }
                }
            }
        } else if (isTextSelected && !isManualSelectionMode) {
            // Text is selected but no manual selection mode -> arrows behave as if selection was
            //  started in manual left mode
            when (code) {
                KeyCode.ARROW_DOWN -> {}
                KeyCode.ARROW_LEFT -> {
                    ic?.setSelection(selectionStart, selectionEnd - 1)
                }
                KeyCode.ARROW_RIGHT -> {
                    ic?.setSelection(
                        selectionStart,
                        (selectionEnd + 1).coerceAtMost(selectionEndMax)
                    )
                }
                KeyCode.ARROW_UP -> {}
                KeyCode.MOVE_HOME -> {
                    ic?.setSelection(selectionStartMin, selectionStart)
                }
                KeyCode.MOVE_END -> {
                    ic?.setSelection(selectionStart, selectionEndMax)
                }
            }
        } else if (!isTextSelected && isManualSelectionMode) {
            // No text is selected but manual selection mode is active, user wants to start a new
            //  selection. Must set manual selection direction.
            when (code) {
                KeyCode.ARROW_DOWN -> {}
                KeyCode.ARROW_LEFT -> {
                    ic?.setSelection(
                        (selectionStart - 1).coerceAtLeast(selectionStartMin),
                        selectionStart
                    )
                    isManualSelectionModeLeft = true
                    isManualSelectionModeRight = false
                }
                KeyCode.ARROW_RIGHT -> {
                    ic?.setSelection(
                        selectionEnd,
                        (selectionEnd + 1).coerceAtMost(selectionEndMax)
                    )
                    isManualSelectionModeLeft = false
                    isManualSelectionModeRight = true
                }
                KeyCode.ARROW_UP -> {}
                KeyCode.MOVE_HOME -> {
                    ic?.setSelection(selectionStartMin, selectionStart)
                    isManualSelectionModeLeft = true
                    isManualSelectionModeRight = false
                }
                KeyCode.MOVE_END -> {
                    ic?.setSelection(selectionEnd, selectionEndMax)
                    isManualSelectionModeLeft = false
                    isManualSelectionModeRight = true
                }
            }
        } else {
            // No selection and no manual selection mode -> move cursor around
            when (code) {
                KeyCode.ARROW_DOWN -> sendSystemKeyEvent(ic, KeyEvent.KEYCODE_DPAD_DOWN)
                KeyCode.ARROW_LEFT -> sendSystemKeyEvent(ic, KeyEvent.KEYCODE_DPAD_LEFT)
                KeyCode.ARROW_RIGHT -> sendSystemKeyEvent(ic, KeyEvent.KEYCODE_DPAD_RIGHT)
                KeyCode.ARROW_UP -> sendSystemKeyEvent(ic, KeyEvent.KEYCODE_DPAD_UP)
                KeyCode.MOVE_HOME -> sendSystemKeyEventAlt(ic, KeyEvent.KEYCODE_DPAD_UP)
                KeyCode.MOVE_END -> sendSystemKeyEventAlt(ic, KeyEvent.KEYCODE_DPAD_DOWN)
            }
        }
    }

    /**
     * Handles a [KeyCode.CLIPBOARD_CUT] event.
     * TODO: handle other data than text too, e.g. Uri, Intent, ...
     */
    private fun handleClipboardCut() {
        val ic = florisboard.currentInputConnection
        val selectedText = ic?.getSelectedText(0)
        if (selectedText != null) {
            florisboard.clipboardManager
                ?.setPrimaryClip(ClipData.newPlainText(selectedText, selectedText))
        }
        resetComposingText()
        ic?.commitText("", 1)
    }

    /**
     * Handles a [KeyCode.CLIPBOARD_COPY] event.
     * TODO: handle other data than text too, e.g. Uri, Intent, ...
     */
    private fun handleClipboardCopy() {
        val ic = florisboard.currentInputConnection
        val selectedText = ic?.getSelectedText(0)
        if (selectedText != null) {
            florisboard.clipboardManager
                ?.setPrimaryClip(ClipData.newPlainText(selectedText, selectedText))
        }
        resetComposingText()
        ic?.setSelection(selectionEnd, selectionEnd)
    }

    /**
     * Handles a [KeyCode.CLIPBOARD_PASTE] event.
     * TODO: handle other data than text too, e.g. Uri, Intent, ...
     */
    private fun handleClipboardPaste() {
        val ic = florisboard.currentInputConnection
        val item = florisboard.clipboardManager?.primaryClip?.getItemAt(0)
        val pasteText = item?.text
        if (pasteText != null) {
            resetComposingText()
            ic?.commitText(pasteText, 1)
        }
    }

    /**
     * Handles a [KeyCode.CLIPBOARD_SELECT] event.
     */
    private fun handleClipboardSelect() {
        val ic = florisboard.currentInputConnection
        resetComposingText()
        if (isTextSelected) {
            if (isManualSelectionMode && isManualSelectionModeLeft) {
                ic?.setSelection(selectionStart, selectionStart)
            } else {
                ic?.setSelection(selectionEnd, selectionEnd)
            }
            isManualSelectionMode = false
        } else {
            isManualSelectionMode = !isManualSelectionMode
            // Must recall to update UI properly
            florisboard.onUpdateCursorAnchorInfo(lastCursorAnchorInfo)
        }
}

    /**
     * Handles a [KeyCode.CLIPBOARD_SELECT_ALL] event.
     */
    private fun handleClipboardSelectAll() {
        val ic = florisboard.currentInputConnection
        resetComposingText()
        ic?.setSelection(selectionStartMin, selectionEndMax)
    }

    /**
     * Main logic point for sending a key press. Different actions may occur depending on the given
     * [KeyData]. This method handles all key press send events, which are text based. For media
     * input send events see MediaInputManager.
     *
     * @param keyData The [KeyData] object which should be sent.
     */
    fun sendKeyPress(keyData: KeyData) {
        val ic = florisboard.currentInputConnection

        when (keyData.code) {
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.MOVE_HOME,
            KeyCode.MOVE_END -> handleArrow(keyData.code)
            KeyCode.CLIPBOARD_CUT -> handleClipboardCut()
            KeyCode.CLIPBOARD_COPY -> handleClipboardCopy()
            KeyCode.CLIPBOARD_PASTE -> handleClipboardPaste()
            KeyCode.CLIPBOARD_SELECT -> handleClipboardSelect()
            KeyCode.CLIPBOARD_SELECT_ALL -> handleClipboardSelectAll()
            KeyCode.DELETE -> handleDelete()
            KeyCode.ENTER -> handleEnter()
            KeyCode.LANGUAGE_SWITCH -> florisboard.switchToNextSubtype()
            KeyCode.SETTINGS -> florisboard.launchSettings()
            KeyCode.SHIFT -> handleShift()
            KeyCode.SHOW_INPUT_METHOD_PICKER -> {
                val im =
                    florisboard.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                im.showInputMethodPicker()
            }
            KeyCode.SWITCH_TO_MEDIA_CONTEXT -> florisboard.setActiveInput(R.id.media_input)
            KeyCode.SWITCH_TO_TEXT_CONTEXT -> florisboard.setActiveInput(R.id.text_input)
            KeyCode.TOGGLE_ONE_HANDED_MODE -> florisboard.toggleOneHandedMode()
            KeyCode.VIEW_CHARACTERS -> setActiveKeyboardMode(KeyboardMode.CHARACTERS)
            KeyCode.VIEW_NUMERIC -> setActiveKeyboardMode(KeyboardMode.NUMERIC)
            KeyCode.VIEW_NUMERIC_ADVANCED -> setActiveKeyboardMode(KeyboardMode.NUMERIC_ADVANCED)
            KeyCode.VIEW_PHONE -> setActiveKeyboardMode(KeyboardMode.PHONE)
            KeyCode.VIEW_PHONE2 -> setActiveKeyboardMode(KeyboardMode.PHONE2)
            KeyCode.VIEW_SYMBOLS -> setActiveKeyboardMode(KeyboardMode.SYMBOLS)
            KeyCode.VIEW_SYMBOLS2 -> setActiveKeyboardMode(KeyboardMode.SYMBOLS2)
            else -> {
                ic?.beginBatchEdit()
                resetComposingText()
                when (activeKeyboardMode) {
                    KeyboardMode.NUMERIC,
                    KeyboardMode.NUMERIC_ADVANCED,
                    KeyboardMode.PHONE,
                    KeyboardMode.PHONE2 -> when (keyData.type) {
                        KeyType.CHARACTER,
                        KeyType.NUMERIC -> {
                            val text = keyData.code.toChar().toString()
                            ic?.commitText(text, 1)
                        }
                        else -> when (keyData.code) {
                            KeyCode.PHONE_PAUSE,
                            KeyCode.PHONE_WAIT -> {
                                val text = keyData.code.toChar().toString()
                                ic?.commitText(text, 1)
                            }
                        }
                    }
                    else -> when (keyData.type) {
                        KeyType.CHARACTER -> when (keyData.code) {
                            KeyCode.SPACE -> handleSpace()
                            KeyCode.URI_COMPONENT_TLD -> {
                                val tld = when (caps) {
                                    true -> keyData.label.toUpperCase(Locale.getDefault())
                                    false -> keyData.label.toLowerCase(Locale.getDefault())
                                }
                                ic?.commitText(tld, 1)
                            }
                            else -> {
                                var text = keyData.code.toChar().toString()
                                text = when (caps) {
                                    true -> text.toUpperCase(Locale.getDefault())
                                    false -> text.toLowerCase(Locale.getDefault())
                                }
                                ic?.commitText(text, 1)
                            }
                        }
                        else -> {
                            Log.e(
                                this::class.simpleName,
                                "sendKeyPress(keyData): Received unknown key: $keyData"
                            )
                        }
                    }
                }
                ic?.endBatchEdit()
            }
        }
    }

    enum class CapsMode {
        ALL,
        NONE,
        SENTENCES,
        WORDS;
    }
}
