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

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.*
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ViewFlipper
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.*
import dev.patrickgold.florisboard.ime.text.editing.EditingKeyboardView
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.*
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.text.smartbar.SmartbarView
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import kotlin.math.roundToLong

/**
 * TextInputManager is responsible for managing everything which is related to text input. All of
 * the following count as text input: character, numeric (+advanced), phone and symbol layouts.
 *
 * All of the UI for the different keyboard layouts are kept under the same container element and
 * are separated from media-related UI. The core [FlorisBoard] will pass any event defined in
 * [FlorisBoard.EventListener] through to this class.
 *
 * TextInputManager is also the hub in the communication between the system, the active editor
 * instance and the Smartbar.
 */
class TextInputManager private constructor() : CoroutineScope by MainScope(),
    FlorisBoard.EventListener, SmartbarView.EventListener {

    private val florisboard = FlorisBoard.getInstance()
    private val activeEditorInstance: EditorInstance
        get() = florisboard.activeEditorInstance

    private var activeKeyboardMode: KeyboardMode? = null
    private var animator: ObjectAnimator? = null
    private val keyboardViews = EnumMap<KeyboardMode, KeyboardView>(KeyboardMode::class.java)
    private var editingKeyboardView: EditingKeyboardView? = null
    private var loadingPlaceholderKeyboard: KeyboardView? = null
    private val osHandler = Handler()
    private var textViewFlipper: ViewFlipper? = null
    private var textViewGroup: LinearLayout? = null

    var keyVariation: KeyVariation = KeyVariation.NORMAL
    val layoutManager = LayoutManager(florisboard)
    private var smartbarView: SmartbarView? = null

    // Caps/Space related properties
    var caps: Boolean = false
        private set
    var capsLock: Boolean = false
        private set
    private var hasCapsRecentlyChanged: Boolean = false
    private var hasSpaceRecentlyPressed: Boolean = false

    // Composing text related properties
    var isManualSelectionMode: Boolean = false
    private var isManualSelectionModeLeft: Boolean = false
    private var isManualSelectionModeRight: Boolean = false

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
        Timber.i("onCreate()")

        var subtypes = florisboard.subtypeManager.subtypes
        if (subtypes.isEmpty()) {
            subtypes = listOf(Subtype.DEFAULT)
        }
        for (subtype in subtypes) {
            for (mode in KeyboardMode.values()) {
                layoutManager.preloadComputedLayout(mode, subtype, florisboard.prefs)
            }
        }
    }

    override fun onCreateInputView() {
        keyboardViews.clear()
    }

    private suspend fun addKeyboardView(mode: KeyboardMode) {
        val keyboardView = KeyboardView(florisboard.context)
        keyboardView.id = View.generateViewId()
        keyboardView.computedLayout = layoutManager.fetchComputedLayoutAsync(mode, florisboard.activeSubtype, florisboard.prefs).await()
        keyboardViews[mode] = keyboardView
        textViewFlipper?.addView(keyboardView)
    }

    /**
     * Sets up the newly registered input view.
     */
    override fun onRegisterInputView(inputView: InputView) {
        Timber.i("onRegisterInputView(inputView)")

        textViewGroup = inputView.findViewById(R.id.text_input)
        textViewFlipper = inputView.findViewById(R.id.text_input_view_flipper)
        editingKeyboardView = inputView.findViewById(R.id.editing)
        loadingPlaceholderKeyboard = inputView.findViewById(R.id.keyboard_preview)

        launch(Dispatchers.Main) {
            textViewGroup?.let {
                animator = ObjectAnimator.ofFloat(it, "alpha", 0.9f, 1.0f).apply {
                    duration = 125
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    start()
                    launch {
                        delay(duration)
                        try {
                            duration = 500
                            setFloatValues(1.0f, 0.4f)
                        } catch (_: Exception) {}
                    }
                }
            }
            val activeKeyboardMode = getActiveKeyboardMode()
            addKeyboardView(activeKeyboardMode)
            setActiveKeyboardMode(activeKeyboardMode)
            animator?.cancel()
            textViewGroup?.let {
                animator = ObjectAnimator.ofFloat(it, "alpha", it.alpha, 1.0f).apply {
                    duration = (((1.0f - it.alpha) / 0.6f) * 125f).roundToLong()
                    repeatCount = 0
                    start()
                }
            }
            for (mode in KeyboardMode.values()) {
                if (mode != activeKeyboardMode && mode != KeyboardMode.SMARTBAR_NUMBER_ROW) {
                    addKeyboardView(mode)
                }
            }
        }
    }

    fun registerSmartbarView(view: SmartbarView) {
        smartbarView = view
        smartbarView?.setEventListener(this)
    }

    /**
     * Cancels all coroutines and cleans up.
     */
    override fun onDestroy() {
        Timber.i("onDestroy()")

        cancel()
        osHandler.removeCallbacksAndMessages(null)
        layoutManager.onDestroy()
        instance = null
    }

    /**
     * Evaluates the [activeKeyboardMode], [keyVariation] and [EditorInstance.isComposingEnabled]
     * property values when starting to interact with a input editor. Also resets the composing
     * texts and sets the initial caps mode accordingly.
     */
    override fun onStartInputView(instance: EditorInstance, restarting: Boolean) {
        val keyboardMode = when (instance.inputAttributes.type) {
            InputAttributes.Type.NUMBER -> {
                keyVariation = KeyVariation.NORMAL
                KeyboardMode.NUMERIC
            }
            InputAttributes.Type.PHONE -> {
                keyVariation = KeyVariation.NORMAL
                KeyboardMode.PHONE
            }
            InputAttributes.Type.TEXT -> {
                keyVariation = when (instance.inputAttributes.variation) {
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
                keyVariation = KeyVariation.NORMAL
                KeyboardMode.CHARACTERS
            }
        }
        instance.apply {
            isComposingEnabled = when (keyboardMode) {
                KeyboardMode.NUMERIC,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> false
                else -> keyVariation != KeyVariation.PASSWORD &&
                        florisboard.prefs.suggestion.enabled// &&
                //!instance.inputAttributes.flagTextAutoComplete &&
                //!instance.inputAttributes.flagTextNoSuggestions
            }
            isPrivateMode = florisboard.prefs.advanced.forcePrivateMode ||
                    imeOptions.flagNoPersonalizedLearning
        }
        if (!florisboard.prefs.correction.rememberCapsLockState) {
            capsLock = false
        }
        updateCapsState()
        setActiveKeyboardMode(keyboardMode)
        smartbarView?.updateSmartbarState()
    }

    /**
     * Handle stuff when finishing to interact with a input editor.
     */
    override fun onFinishInputView(finishingInput: Boolean) {
        smartbarView?.updateSmartbarState()
    }

    override fun onWindowShown() {
        keyboardViews[KeyboardMode.CHARACTERS]?.updateVisibility()
        smartbarView?.updateSmartbarState()
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
     * Sets [activeKeyboardMode] and updates the [SmartbarView.isQuickActionsVisible] state.
     */
    private fun setActiveKeyboardMode(mode: KeyboardMode) {
        textViewFlipper?.displayedChild = textViewFlipper?.indexOfChild(when (mode) {
            KeyboardMode.EDITING -> editingKeyboardView
            else -> keyboardViews[mode]
        })?.coerceAtLeast(0) ?: 0
        keyboardViews[mode]?.updateVisibility()
        keyboardViews[mode]?.requestLayout()
        keyboardViews[mode]?.requestLayoutAllKeys()
        activeKeyboardMode = mode
        isManualSelectionMode = false
        isManualSelectionModeLeft = false
        isManualSelectionModeRight = false
        smartbarView?.isQuickActionsVisible = false
        smartbarView?.updateSmartbarState()
    }

    override fun onSubtypeChanged(newSubtype: Subtype) {
        launch {
            val keyboardView = keyboardViews[KeyboardMode.CHARACTERS]
            keyboardView?.computedLayout = layoutManager.fetchComputedLayoutAsync(KeyboardMode.CHARACTERS, newSubtype, florisboard.prefs).await()
            keyboardView?.updateVisibility()
        }
    }

    /**
     * Main logic point for processing cursor updates as well as parsing the current composing word
     * and passing this info on to the [SmartbarView] to turn it into candidate suggestions.
     */
    override fun onUpdateSelection() {
        if (!activeEditorInstance.isNewSelectionInBoundsOfOld) {
            isManualSelectionMode = false
            isManualSelectionModeLeft = false
            isManualSelectionModeRight = false
        }
        updateCapsState()
        smartbarView?.updateSmartbarState()
    }

    override fun onPrimaryClipChanged() {
        smartbarView?.onPrimaryClipChanged()
    }

    /**
     * Updates the current caps state according to the [EditorInstance.cursorCapsMode], while
     * respecting [capsLock] property and the correction.autoCapitalization preference.
     */
    private fun updateCapsState() {
        if (!capsLock) {
            caps = florisboard.prefs.correction.autoCapitalization &&
                    activeEditorInstance.cursorCapsMode != InputAttributes.CapsMode.NONE
            launch(Dispatchers.Main) {
                keyboardViews[activeKeyboardMode]?.invalidateAllKeys()
            }
        }
    }

    /**
     * Executes a given [SwipeAction]. Ignores any [SwipeAction] but the ones relevant for this
     * class.
     */
    fun executeSwipeAction(swipeAction: SwipeAction) {
        when (swipeAction) {
            SwipeAction.DELETE_WORD -> handleDeleteWord()
            SwipeAction.INSERT_SPACE -> handleSpace()
            SwipeAction.MOVE_CURSOR_DOWN -> handleArrow(KeyCode.ARROW_DOWN)
            SwipeAction.MOVE_CURSOR_UP -> handleArrow(KeyCode.ARROW_UP)
            SwipeAction.MOVE_CURSOR_LEFT -> handleArrow(KeyCode.ARROW_LEFT)
            SwipeAction.MOVE_CURSOR_RIGHT -> handleArrow(KeyCode.ARROW_RIGHT)
            SwipeAction.MOVE_CURSOR_START_OF_LINE -> handleArrow(KeyCode.MOVE_HOME)
            SwipeAction.MOVE_CURSOR_END_OF_LINE -> handleArrow(KeyCode.MOVE_END)
            SwipeAction.SHIFT -> handleShift()
            SwipeAction.SHOW_INPUT_METHOD_PICKER -> sendKeyPress(
                KeyData(type = KeyType.FUNCTION, code = KeyCode.SHOW_INPUT_METHOD_PICKER)
            )
            else -> {}
        }
    }

    override fun onSmartbarBackButtonPressed() {
        setActiveKeyboardMode(KeyboardMode.CHARACTERS)
    }

    override fun onSmartbarPrivateModeButtonClicked() {
        Toast.makeText(florisboard.context, R.string.private_mode_dialog__title, Toast.LENGTH_LONG).show()
    }

    override fun onSmartbarQuickActionPressed(quickActionId: Int) {
        when (quickActionId) {
            R.id.quick_action_switch_to_editing_context -> {
                if (activeKeyboardMode == KeyboardMode.EDITING) {
                    setActiveKeyboardMode(KeyboardMode.CHARACTERS)
                } else {
                    setActiveKeyboardMode(KeyboardMode.EDITING)
                }
            }
            R.id.quick_action_switch_to_media_context -> florisboard.setActiveInput(R.id.media_input)
            R.id.quick_action_open_settings -> florisboard.launchSettings()
            R.id.quick_action_one_handed_toggle -> florisboard.toggleOneHandedMode()
            R.id.quick_action_undo -> {
                handleUndo()
                return
            }
            R.id.quick_action_redo -> {
                handleRedo()
                return
            }
        }
        smartbarView?.isQuickActionsVisible = false
        smartbarView?.updateSmartbarState()
    }

    private fun handleUndo(){
        activeEditorInstance.performUndo()
    }

    private fun handleRedo(){
        activeEditorInstance.performRedo()
    }

    /**
     * Handles a [KeyCode.DELETE] event.
     */
    private fun handleDelete() {
        hasCapsRecentlyChanged = false
        hasSpaceRecentlyPressed = false
        isManualSelectionMode = false
        isManualSelectionModeLeft = false
        isManualSelectionModeRight = false
        activeEditorInstance.deleteBackwards()
    }

    /**
     * Handles a [KeyCode.DELETE_WORD] event.
     */
    private fun handleDeleteWord() {
        hasCapsRecentlyChanged = false
        hasSpaceRecentlyPressed = false
        isManualSelectionMode = false
        isManualSelectionModeLeft = false
        isManualSelectionModeRight = false
        activeEditorInstance.deleteWordsBeforeCursor(1)
    }

    /**
     * Handles a [KeyCode.ENTER] event.
     */
    private fun handleEnter() {
        hasCapsRecentlyChanged = false
        hasSpaceRecentlyPressed = false
        if (activeEditorInstance.imeOptions.flagNoEnterAction) {
            activeEditorInstance.performEnter()
        } else {
            when (activeEditorInstance.imeOptions.action) {
                ImeOptions.Action.DONE,
                ImeOptions.Action.GO,
                ImeOptions.Action.NEXT,
                ImeOptions.Action.PREVIOUS,
                ImeOptions.Action.SEARCH,
                ImeOptions.Action.SEND -> {
                    activeEditorInstance.performEnterAction(activeEditorInstance.imeOptions.action)
                }
                else -> activeEditorInstance.performEnter()
            }
        }
    }

    /**
     * Handles a [KeyCode.LANGUAGE_SWITCH] event. Also handles if the language switch should cycle
     * FlorisBoard internal or system-wide.
     */
    private fun handleLanguageSwitch() {
        when (florisboard.prefs.keyboard.switchKeyMode) {
            SwitchKeyMode.DYNAMIC_LANGUAGE_EMOJI,
            SwitchKeyMode.ALWAYS_LANGUAGE_INTERNAL -> florisboard.switchToNextSubtype()
            else -> florisboard.switchToNextKeyboard()
        }
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
        if (florisboard.prefs.correction.doubleSpacePeriod) {
            if (hasSpaceRecentlyPressed) {
                osHandler.removeCallbacksAndMessages(null)
                val text = activeEditorInstance.getTextBeforeCursor(2)
                if (text.length == 2 && !text.matches("""[.!?‽\s][\s]""".toRegex())) {
                    activeEditorInstance.deleteBackwards()
                    activeEditorInstance.commitText(".")
                }
                hasSpaceRecentlyPressed = false
            } else {
                hasSpaceRecentlyPressed = true
                osHandler.postDelayed({
                    hasSpaceRecentlyPressed = false
                }, 300)
            }
        }
        activeEditorInstance.commitText(KeyCode.SPACE.toChar().toString())
    }

    /**
     * Handles [KeyCode] arrow and move events, behaves differently depending on text selection.
     */
    private fun handleArrow(code: Int) = activeEditorInstance.apply {
        val selectionStartMin = 0
        val selectionEndMax = cachedText.length
        if (selection.isSelectionMode && isManualSelectionMode) {
            // Text is selected and it is manual selection -> Expand selection depending on started
            //  direction.
            when (code) {
                KeyCode.ARROW_DOWN -> {}
                KeyCode.ARROW_LEFT -> {
                    if (isManualSelectionModeLeft) {
                        setSelection(
                            (selection.start - 1).coerceAtLeast(selectionStartMin),
                            selection.end
                        )
                    } else {
                        setSelection(selection.start, selection.end - 1)
                    }
                }
                KeyCode.ARROW_RIGHT -> {
                    if (isManualSelectionModeRight) {
                        setSelection(
                            selection.start,
                            (selection.end + 1).coerceAtMost(selectionEndMax)
                        )
                    } else {
                        setSelection(selection.start + 1, selection.end)
                    }
                }
                KeyCode.ARROW_UP -> {}
                KeyCode.MOVE_HOME -> {
                    if (isManualSelectionModeLeft) {
                        setSelection(selectionStartMin, selection.end)
                    } else {
                        setSelection(selectionStartMin, selection.start)
                    }
                }
                KeyCode.MOVE_END -> {
                    if (isManualSelectionModeRight) {
                        setSelection(selection.start, selectionEndMax)
                    } else {
                        setSelection(selection.end, selectionEndMax)
                    }
                }
            }
        } else if (selection.isSelectionMode && !isManualSelectionMode) {
            // Text is selected but no manual selection mode -> arrows behave as if selection was
            //  started in manual left mode
            when (code) {
                KeyCode.ARROW_DOWN -> {}
                KeyCode.ARROW_LEFT -> {
                    setSelection(selection.start, selection.end - 1)
                }
                KeyCode.ARROW_RIGHT -> {
                    setSelection(
                        selection.start,
                        (selection.end + 1).coerceAtMost(selectionEndMax)
                    )
                }
                KeyCode.ARROW_UP -> {}
                KeyCode.MOVE_HOME -> {
                    setSelection(selectionStartMin, selection.start)
                }
                KeyCode.MOVE_END -> {
                    setSelection(selection.start, selectionEndMax)
                }
            }
        } else if (!selection.isSelectionMode && isManualSelectionMode) {
            // No text is selected but manual selection mode is active, user wants to start a new
            //  selection. Must set manual selection direction.
            when (code) {
                KeyCode.ARROW_DOWN -> {}
                KeyCode.ARROW_LEFT -> {
                    setSelection(
                        (selection.start - 1).coerceAtLeast(selectionStartMin),
                        selection.start
                    )
                    isManualSelectionModeLeft = true
                    isManualSelectionModeRight = false
                }
                KeyCode.ARROW_RIGHT -> {
                    setSelection(
                        selection.end,
                        (selection.end + 1).coerceAtMost(selectionEndMax)
                    )
                    isManualSelectionModeLeft = false
                    isManualSelectionModeRight = true
                }
                KeyCode.ARROW_UP -> {}
                KeyCode.MOVE_HOME -> {
                    setSelection(selectionStartMin, selection.start)
                    isManualSelectionModeLeft = true
                    isManualSelectionModeRight = false
                }
                KeyCode.MOVE_END -> {
                    setSelection(selection.end, selectionEndMax)
                    isManualSelectionModeLeft = false
                    isManualSelectionModeRight = true
                }
            }
        } else {
            // No selection and no manual selection mode -> move cursor around
            when (code) {
                KeyCode.ARROW_DOWN -> activeEditorInstance.sendSystemKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN)
                KeyCode.ARROW_LEFT -> activeEditorInstance.sendSystemKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT)
                KeyCode.ARROW_RIGHT -> activeEditorInstance.sendSystemKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
                KeyCode.ARROW_UP -> activeEditorInstance.sendSystemKeyEvent(KeyEvent.KEYCODE_DPAD_UP)
                KeyCode.MOVE_HOME -> activeEditorInstance.sendSystemKeyEventAlt(KeyEvent.KEYCODE_DPAD_UP)
                KeyCode.MOVE_END -> activeEditorInstance.sendSystemKeyEventAlt(KeyEvent.KEYCODE_DPAD_DOWN)
            }
        }
    }

    /**
     * Handles a [KeyCode.CLIPBOARD_SELECT] event.
     */
    private fun handleClipboardSelect() = activeEditorInstance.apply {
        if (selection.isSelectionMode) {
            if (isManualSelectionMode && isManualSelectionModeLeft) {
                setSelection(selection.start, selection.start)
            } else {
                setSelection(selection.end, selection.end)
            }
            isManualSelectionMode = false
        } else {
            isManualSelectionMode = !isManualSelectionMode
            // Must call to update UI properly
            editingKeyboardView?.onUpdateSelection()
        }
    }

    /**
     * Handles a [KeyCode.CLIPBOARD_SELECT_ALL] event.
     */
    private fun handleClipboardSelectAll() {
        activeEditorInstance.setSelection(0, activeEditorInstance.cachedText.length)
    }

    /**
     * Main logic point for sending a key press. Different actions may occur depending on the given
     * [KeyData]. This method handles all key press send events, which are text based. For media
     * input send events see MediaInputManager.
     *
     * @param keyData The [KeyData] object which should be sent.
     */
    fun sendKeyPress(keyData: KeyData) {
        when (keyData.code) {
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.MOVE_HOME,
            KeyCode.MOVE_END -> handleArrow(keyData.code)
            KeyCode.CLIPBOARD_CUT -> activeEditorInstance.performClipboardCut()
            KeyCode.CLIPBOARD_COPY -> activeEditorInstance.performClipboardCopy()
            KeyCode.CLIPBOARD_PASTE -> {
                activeEditorInstance.performClipboardPaste()
                smartbarView?.resetClipboardSuggestion()
            }
            KeyCode.CLIPBOARD_SELECT -> handleClipboardSelect()
            KeyCode.CLIPBOARD_SELECT_ALL -> handleClipboardSelectAll()
            KeyCode.DELETE -> {
                handleDelete()
                smartbarView?.resetClipboardSuggestion()
            }
            KeyCode.ENTER -> {
                handleEnter()
                smartbarView?.resetClipboardSuggestion()
            }
            KeyCode.LANGUAGE_SWITCH -> handleLanguageSwitch()
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
                when (activeKeyboardMode) {
                    KeyboardMode.NUMERIC,
                    KeyboardMode.NUMERIC_ADVANCED,
                    KeyboardMode.PHONE,
                    KeyboardMode.PHONE2 -> when (keyData.type) {
                        KeyType.CHARACTER,
                        KeyType.NUMERIC -> {
                            val text = keyData.code.toChar().toString()
                            activeEditorInstance.commitText(text)
                        }
                        else -> when (keyData.code) {
                            KeyCode.PHONE_PAUSE,
                            KeyCode.PHONE_WAIT -> {
                                val text = keyData.code.toChar().toString()
                                activeEditorInstance.commitText(text)
                            }
                        }
                    }
                    else -> when (keyData.type) {
                        KeyType.CHARACTER, KeyType.NUMERIC -> when (keyData.code) {
                            KeyCode.SPACE -> handleSpace()
                            KeyCode.URI_COMPONENT_TLD -> {
                                val tld = when (caps) {
                                    true -> keyData.label.toUpperCase(Locale.getDefault())
                                    false -> keyData.label.toLowerCase(Locale.getDefault())
                                }
                                activeEditorInstance.commitText(tld)
                            }
                            else -> {
                                hasCapsRecentlyChanged = false
                                hasSpaceRecentlyPressed = false
                                var text = keyData.code.toChar().toString()
                                text = when (caps) {
                                    true -> text.toUpperCase(Locale.getDefault())
                                    false -> text.toLowerCase(Locale.getDefault())
                                }
                                activeEditorInstance.commitText(text)
                            }
                        }
                        else -> {
                            Timber.e("sendKeyPress(keyData): Received unknown key: $keyData")
                        }
                    }
                }
                smartbarView?.resetClipboardSuggestion()
            }
        }
        if (keyData.code != KeyCode.SHIFT && !capsLock) {
            updateCapsState()
        }
        smartbarView?.updateSmartbarState()
    }
}
