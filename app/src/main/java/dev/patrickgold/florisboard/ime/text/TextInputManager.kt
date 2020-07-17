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

import android.content.Context
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ViewFlipper
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputView
import dev.patrickgold.florisboard.ime.core.Subtype
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
    private var isTextSelected: Boolean = false

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
        keyboardView.florisboard = florisboard
        keyboardView.prefs = florisboard.prefs
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
            else -> keyVariation != KeyVariation.PASSWORD
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
     * Sets [activeKeyboardMode] and updates the [SmartbarManager.activeContainerId].
     */
    private fun setActiveKeyboardMode(mode: KeyboardMode) {
        textViewFlipper?.displayedChild =
            textViewFlipper?.indexOfChild(keyboardViews[mode]) ?: 0
        keyboardViews[mode]?.updateVisibility()
        keyboardViews[mode]?.requestLayout()
        keyboardViews[mode]?.requestLayoutAllKeys()
        activeKeyboardMode = mode
        smartbarManager.activeContainerId = smartbarManager.getPreferredContainerId()
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

        val ic = florisboard.currentInputConnection

        if (isComposingEnabled) {
            if (cursorAnchorInfo.selectionEnd - cursorAnchorInfo.selectionStart == 0) {
                val newCursorPos = cursorAnchorInfo.selectionStart
                val prevComposingText = (cursorAnchorInfo.composingText ?: "").toString()
                val inputText =
                    (ic?.getExtractedText(ExtractedTextRequest(), 0)?.text ?: "").toString()
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
        isTextSelected = cursorAnchorInfo.selectionEnd - cursorAnchorInfo.selectionStart != 0
        updateCapsState()
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
     * Handles a [KeyCode.DELETE] event.
     */
    private fun handleDelete() {
        val ic = florisboard.currentInputConnection
        ic?.beginBatchEdit()
        resetComposingText()
        ic?.sendKeyEvent(
            KeyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL
            )
        )
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
            ic?.sendKeyEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER
                )
            )
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
                else -> {
                    ic?.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_ENTER
                        )
                    )
                }
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
     * Main logic point for sending a key press. Different actions may occur depending on the given
     * [KeyData]. This method handles all key press send events, which are text based. For media
     * input send events see MediaInputManager.
     *
     * @param keyData The [KeyData] object which should be sent.
     */
    fun sendKeyPress(keyData: KeyData) {
        val ic = florisboard.currentInputConnection

        when (keyData.code) {
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
