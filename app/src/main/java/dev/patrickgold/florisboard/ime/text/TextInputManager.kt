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
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.debug.*
import dev.patrickgold.florisboard.ime.clip.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.core.*
import dev.patrickgold.florisboard.ime.dictionary.Dictionary
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.ime.nlp.Token
import dev.patrickgold.florisboard.ime.nlp.toStringList
import dev.patrickgold.florisboard.ime.text.gestures.GlideTypingManager
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.*
import dev.patrickgold.florisboard.ime.text.keyboard.*
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.text.smartbar.SmartbarView
import kotlinx.coroutines.*
import org.json.JSONArray
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
class TextInputManager private constructor() : CoroutineScope by MainScope(), InputKeyEventReceiver,
    FlorisBoard.EventListener, SmartbarView.EventListener {

    var isGlidePostEffect: Boolean = false
    private val florisboard = FlorisBoard.getInstance()
    val symbolsWithSpaceAfter: List<String>
    private val activeEditorInstance: EditorInstance
        get() = florisboard.activeEditorInstance

    lateinit var layoutManager: LayoutManager
        private set
    private var activeKeyboardMode: KeyboardMode? = null
    private val keyboards = TextKeyboardCache()
    private var textInputKeyboardView: TextKeyboardView? = null
    private var textViewGroup: LinearLayout? = null
    private val dictionaryManager: DictionaryManager = DictionaryManager.default()
    private var activeDictionary: Dictionary<String, Int>? = null
    val inputEventDispatcher: InputEventDispatcher = InputEventDispatcher.new(
        repeatableKeyCodes = intArrayOf(
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.DELETE,
            KeyCode.FORWARD_DELETE
        )
    )

    var keyVariation: KeyVariation = KeyVariation.NORMAL
    internal var smartbarView: SmartbarView? = null

    // Caps/Shift related properties
    var caps: Boolean = false
        private set
    var capsLock: Boolean = false
        private set
    private var newCapsState: Boolean = false

    // Composing text related properties
    var isManualSelectionMode: Boolean = false
    private var isManualSelectionModeStart: Boolean = false
    private var isManualSelectionModeEnd: Boolean = false

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
        val data =
            AssetManager.default().loadTextAsset(AssetRef(AssetSource.Assets, "ime/text/symbols-with-space.json")).getOrThrow()
        val json = JSONArray(data)
        this.symbolsWithSpaceAfter = List(json.length()){ json.getString(it) }
    }

    val evaluator = object : TextComputingEvaluator {
        override fun evaluateCaps(): Boolean {
            return caps || capsLock
        }

        override fun evaluateCaps(data: TextKeyData): Boolean {
            return evaluateCaps() && data.code >= KeyCode.SPACE
        }

        override fun evaluateEnabled(data: TextKeyData): Boolean {
            return when (data.code) {
                KeyCode.CLIPBOARD_COPY,
                KeyCode.CLIPBOARD_CUT -> {
                    florisboard.activeEditorInstance.selection.isSelectionMode &&
                        !florisboard.activeEditorInstance.isRawInputEditor
                }
                KeyCode.CLIPBOARD_PASTE -> {
                    // such gore. checks
                    // 1. has a clipboard item
                    // 2. the clipboard item has any of the supported mime types of the editor OR is plain text.
                    florisboard.florisClipboardManager?.canBePasted(
                        florisboard.florisClipboardManager?.primaryClip
                    ) == true
                }
                KeyCode.CLIPBOARD_SELECT_ALL -> {
                    !florisboard.activeEditorInstance.isRawInputEditor
                }
                KeyCode.SWITCH_TO_CLIPBOARD_CONTEXT -> {
                    florisboard.prefs.clipboard.enableHistory
                }
                else -> true
            }
        }

        override fun evaluateVisible(data: TextKeyData): Boolean {
            return when (data.code) {
                KeyCode.SWITCH_TO_CLIPBOARD_CONTEXT -> florisboard.prefs.clipboard.enableHistory
                else -> true
            }
        }

        override fun getActiveSubtype(): Subtype {
            return florisboard.activeSubtype
        }

        override fun getKeyVariation(): KeyVariation {
            return keyVariation
        }

        override fun getKeyboardMode(): KeyboardMode {
            return KeyboardMode.CHARACTERS // Correct value will be inserted by the TextKeyboardView
        }

        override fun isSlot(data: TextKeyData): Boolean {
            return CurrencySet.isCurrencySlot(data.code)
        }

        override fun getSlotData(data: TextKeyData): TextKeyData? {
            return florisboard.subtypeManager.getCurrencySet(getActiveSubtype()).getSlot(data.code)
        }
    }

    /**
     * Non-UI-related setup + preloading of all required computed layouts (asynchronous in the
     * background).
     */
    override fun onCreate() {
        flogInfo(LogTopic.IMS_EVENTS)

        layoutManager = LayoutManager()
        inputEventDispatcher.keyEventReceiver = this
        var subtypes = florisboard.subtypeManager.subtypes
        if (subtypes.isEmpty()) {
            subtypes = listOf(Subtype.DEFAULT)
        }
        for (subtype in subtypes) {
            for (mode in KeyboardMode.values()) {
                launch(Dispatchers.Default) {
                    val keyboard = layoutManager.computeKeyboardAsync(mode, subtype, florisboard.prefs)
                    keyboards.set(mode, subtype, keyboard)
                }
            }
        }
    }

    override fun onCreateInputView() {
        flogInfo(LogTopic.IMS_EVENTS)

        keyboards.clear()
    }

    /**
     * Sets up the newly registered input view.
     */
    override fun onRegisterInputView(inputView: InputView) {
        flogInfo(LogTopic.IMS_EVENTS)

        textViewGroup = inputView.findViewById(R.id.text_input)
        textInputKeyboardView = inputView.findViewById(R.id.text_input_keyboard_view)
        textInputKeyboardView?.setComputingEvaluator(evaluator)

        launch(Dispatchers.Main) {
            val animator1 = textViewGroup?.let {
                ObjectAnimator.ofFloat(it, "alpha", 0.9f, 1.0f).apply {
                    duration = 125
                    repeatCount = 0
                    start()
                }
            }
            val animator2 = textViewGroup?.let {
                ObjectAnimator.ofFloat(it, "alpha", 1.0f, 0.4f).apply {
                    startDelay = 125
                    duration = 500
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    start()
                }
            }
            setActiveKeyboardMode(getActiveKeyboardMode())
            animator1?.cancel()
            animator2?.cancel()
            val animator3 = textViewGroup?.let {
                ObjectAnimator.ofFloat(it, "alpha", it.alpha, 1.0f).apply {
                    duration = (((1.0f - it.alpha) / 0.6f) * 125f).roundToLong()
                    repeatCount = 0
                    start()
                }
            }
            delay(animator3?.duration ?: 1)
            animator3?.end()
        }
    }

    fun registerSmartbarView(view: SmartbarView) {
        smartbarView = view
        smartbarView?.setEventListener(this)
    }

    fun unregisterSmartbarView(view: SmartbarView) {
        if (smartbarView == view) {
            smartbarView = null
        }
    }

    /**
     * Cancels all coroutines and cleans up.
     */
    override fun onDestroy() {
        flogInfo(LogTopic.IMS_EVENTS)

        textInputKeyboardView?.setComputingEvaluator(null)
        inputEventDispatcher.keyEventReceiver = null
        inputEventDispatcher.close()
        cancel()
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
        isGlidePostEffect = false
        updateCapsState()
        setActiveKeyboardMode(keyboardMode)
        smartbarView?.setCandidateSuggestionWords(System.nanoTime(), null)
        smartbarView?.updateSmartbarState()
    }

    /**
     * Handle stuff when finishing to interact with a input editor.
     */
    override fun onFinishInputView(finishingInput: Boolean) {
        smartbarView?.updateSmartbarState()
    }

    override fun onWindowShown() {
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
    private fun setActiveKeyboardMode(mode: KeyboardMode) = launch {
        setActiveKeyboard(mode, florisboard.activeSubtype)
        activeKeyboardMode = mode
        isManualSelectionMode = false
        isManualSelectionModeStart = false
        isManualSelectionModeEnd = false
        smartbarView?.isQuickActionsVisible = false
        smartbarView?.updateSmartbarState()
    }

    private fun setActiveKeyboard(mode: KeyboardMode, subtype: Subtype) = launch {
        val activeKeyboard = keyboards.getOrElse(mode, subtype) {
            layoutManager.computeKeyboardAsync(
                keyboardMode = mode,
                subtype = subtype,
                prefs = florisboard.prefs
            )
        }
        textInputKeyboardView?.setComputedKeyboard(activeKeyboard.await())
    }

    override fun onSubtypeChanged(newSubtype: Subtype) {
        launch {
            if (activeEditorInstance.isComposingEnabled) {
                withContext(Dispatchers.IO) {
                    dictionaryManager.loadDictionary(AssetRef(AssetSource.Assets, "ime/dict/en.flict")).let {
                        activeDictionary = it.getOrDefault(null)
                    }
                }
            }
            if (PrefHelper.getDefaultInstance(florisboard).glide.enabled) {
                GlideTypingManager.getInstance().setWordData(newSubtype)
            }
            setActiveKeyboard(getActiveKeyboardMode(), newSubtype)
        }
        isGlidePostEffect = false
    }

    /**
     * Main logic point for processing cursor updates as well as parsing the current composing word
     * and passing this info on to the [SmartbarView] to turn it into candidate suggestions.
     */
    override fun onUpdateSelection() {
        if (!inputEventDispatcher.isPressed(KeyCode.SHIFT)) {
            updateCapsState()
        }
        smartbarView?.updateSmartbarState()
        flogInfo(LogTopic.IMS_EVENTS) { "current word: ${activeEditorInstance.cachedInput.currentWord.text}" }
        if (activeEditorInstance.isComposingEnabled && !inputEventDispatcher.isPressed(KeyCode.DELETE)) {
            if (activeEditorInstance.shouldReevaluateComposingSuggestions) {
                activeEditorInstance.shouldReevaluateComposingSuggestions = false
                activeDictionary?.let {
                    launch(Dispatchers.Default) {
                        val startTime = System.nanoTime()
                        val suggestions = it.getTokenPredictions(
                            precedingTokens = listOf(),
                            currentToken = Token(activeEditorInstance.cachedInput.currentWord.text),
                            maxSuggestionCount = 16,
                            allowPossiblyOffensive = !florisboard.prefs.suggestion.blockPossiblyOffensive
                        ).toStringList()
                        if (BuildConfig.DEBUG) {
                            val elapsed = (System.nanoTime() - startTime) / 1000.0
                            flogInfo { "sugg fetch time: $elapsed us" }
                        }
                        withContext(Dispatchers.Main) {
                            smartbarView?.setCandidateSuggestionWords(startTime, suggestions)
                            smartbarView?.updateCandidateSuggestionCapsState()
                        }
                    }
                }
            } else {
                smartbarView?.setCandidateSuggestionWords(System.nanoTime(), null)
            }
        }
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
            textInputKeyboardView?.notifyStateChanged()
        }
    }

    /**
     * Executes a given [SwipeAction]. Ignores any [SwipeAction] but the ones relevant for this
     * class.
     */
    fun executeSwipeAction(swipeAction: SwipeAction) {
        val keyData = when (swipeAction) {
            SwipeAction.CYCLE_TO_PREVIOUS_KEYBOARD_MODE -> when (getActiveKeyboardMode()) {
                KeyboardMode.CHARACTERS -> TextKeyData.VIEW_NUMERIC_ADVANCED
                KeyboardMode.NUMERIC_ADVANCED -> TextKeyData.VIEW_SYMBOLS2
                KeyboardMode.SYMBOLS2 -> TextKeyData.VIEW_SYMBOLS
                else -> TextKeyData.VIEW_CHARACTERS
            }
            SwipeAction.CYCLE_TO_NEXT_KEYBOARD_MODE -> when (getActiveKeyboardMode()) {
                KeyboardMode.CHARACTERS -> TextKeyData.VIEW_SYMBOLS
                KeyboardMode.SYMBOLS -> TextKeyData.VIEW_SYMBOLS2
                KeyboardMode.SYMBOLS2 -> TextKeyData.VIEW_NUMERIC_ADVANCED
                else -> TextKeyData.VIEW_CHARACTERS
            }
            SwipeAction.DELETE_WORD -> TextKeyData.DELETE_WORD
            SwipeAction.INSERT_SPACE -> TextKeyData.SPACE
            SwipeAction.MOVE_CURSOR_DOWN -> TextKeyData.ARROW_DOWN
            SwipeAction.MOVE_CURSOR_UP -> TextKeyData.ARROW_UP
            SwipeAction.MOVE_CURSOR_LEFT -> TextKeyData.ARROW_LEFT
            SwipeAction.MOVE_CURSOR_RIGHT -> TextKeyData.ARROW_RIGHT
            SwipeAction.MOVE_CURSOR_START_OF_LINE -> TextKeyData.MOVE_START_OF_LINE
            SwipeAction.MOVE_CURSOR_END_OF_LINE -> TextKeyData.MOVE_END_OF_LINE
            SwipeAction.MOVE_CURSOR_START_OF_PAGE -> TextKeyData.MOVE_START_OF_PAGE
            SwipeAction.MOVE_CURSOR_END_OF_PAGE -> TextKeyData.MOVE_END_OF_PAGE
            SwipeAction.SHIFT -> TextKeyData.SHIFT
            SwipeAction.REDO -> TextKeyData.REDO
            SwipeAction.UNDO -> TextKeyData.UNDO
            SwipeAction.SWITCH_TO_CLIPBOARD_CONTEXT -> TextKeyData.SWITCH_TO_CLIPBOARD_CONTEXT
            SwipeAction.SHOW_INPUT_METHOD_PICKER -> TextKeyData.SHOW_INPUT_METHOD_PICKER
            else -> null
        }
        if (keyData != null) {
            inputEventDispatcher.send(InputKeyEvent.downUp(keyData))
        }
    }

    override fun onSmartbarBackButtonPressed() {
        setActiveKeyboardMode(KeyboardMode.CHARACTERS)
    }

    override fun onSmartbarCandidatePressed(word: String) {
        isGlidePostEffect = false
        activeEditorInstance.commitCompletion(word)
    }

    override fun onSmartbarClipboardCandidatePressed(clipboardItem: ClipboardItem) {
        isGlidePostEffect = false
        activeEditorInstance.commitClipboardItem(clipboardItem)
    }

    override fun onSmartbarPrivateModeButtonClicked() {
        Toast.makeText(florisboard, R.string.private_mode_dialog__title, Toast.LENGTH_LONG).show()
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
            R.id.quick_action_one_handed_toggle -> florisboard.toggleOneHandedMode(isRight = true)
            R.id.quick_action_undo -> {
                inputEventDispatcher.send(InputKeyEvent.downUp(TextKeyData.UNDO))
                return
            }
            R.id.quick_action_redo -> {
                inputEventDispatcher.send(InputKeyEvent.downUp(TextKeyData.REDO))
                return
            }
        }
        smartbarView?.isQuickActionsVisible = false
        smartbarView?.updateSmartbarState()
    }

    /**
     * Handles a [KeyCode.DELETE] event.
     */
    private fun handleDelete() {
        if (isGlidePostEffect){
            handleDeleteWord()
            isGlidePostEffect = false
            smartbarView?.setCandidateSuggestionWords(System.nanoTime(), null)
        } else {
            isManualSelectionMode = false
            isManualSelectionModeStart = false
            isManualSelectionModeEnd = false
            activeEditorInstance.deleteBackwards()
        }
    }

    /**
     * Handles a [KeyCode.DELETE_WORD] event.
     */
    private fun handleDeleteWord() {
        isManualSelectionMode = false
        isManualSelectionModeStart = false
        isManualSelectionModeEnd = false
        isGlidePostEffect = false
        activeEditorInstance.deleteWordsBeforeCursor(1)
    }

    /**
     * Handles a [KeyCode.ENTER] event.
     */
    private fun handleEnter() {
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
        isGlidePostEffect = false
    }

    /**
     * Handles a [KeyCode.LANGUAGE_SWITCH] event. Also handles if the language switch should cycle
     * FlorisBoard internal or system-wide.
     */
    private fun handleLanguageSwitch() {
        when (florisboard.prefs.keyboard.utilityKeyAction) {
            UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS,
            UtilityKeyAction.SWITCH_LANGUAGE -> florisboard.switchToNextSubtype()
            else -> florisboard.switchToNextKeyboard()
        }
    }

    /**
     * Handles a [KeyCode.SHIFT] down event.
     */
    private fun handleShiftDown(ev: InputKeyEvent) {
        if (ev.isConsecutiveEventOf(inputEventDispatcher.lastKeyEventDown, florisboard.prefs.keyboard.longPressDelay.toLong())) {
            newCapsState = true
            caps = true
            capsLock = true
        } else {
            newCapsState = !caps
            caps = true
            capsLock = false
        }
        textInputKeyboardView?.notifyStateChanged()
        smartbarView?.updateCandidateSuggestionCapsState()
    }

    /**
     * Handles a [KeyCode.SHIFT] up event.
     */
    private fun handleShiftUp() {
        caps = newCapsState
        textInputKeyboardView?.notifyStateChanged()
        smartbarView?.updateCandidateSuggestionCapsState()
    }

    /**
     * Handles a [KeyCode.SHIFT] cancel event.
     */
    private fun handleShiftCancel() {
        caps = false
        capsLock = false
        textInputKeyboardView?.notifyStateChanged()
        smartbarView?.updateCandidateSuggestionCapsState()
    }

    /**
     * Handles a [KeyCode.SHIFT] up event.
     */
    private fun handleShiftLock() {
        val lastKeyEvent = inputEventDispatcher.lastKeyEventDown ?: return
        if (lastKeyEvent.data.code == KeyCode.SHIFT && lastKeyEvent.action == InputKeyEvent.Action.DOWN) {
            newCapsState = true
            caps = true
            capsLock = true
            textInputKeyboardView?.notifyStateChanged()
            smartbarView?.updateCandidateSuggestionCapsState()
        }
    }

    /**
     * Handles a [KeyCode.SPACE] event. Also handles the auto-correction of two space taps if
     * enabled by the user.
     */
    private fun handleSpace(ev: InputKeyEvent) {
        if (florisboard.prefs.keyboard.spaceBarSwitchesToCharacters && getActiveKeyboardMode() != KeyboardMode.CHARACTERS) {
            setActiveKeyboardMode(KeyboardMode.CHARACTERS)
        }
        if (florisboard.prefs.correction.doubleSpacePeriod) {
            if (ev.isConsecutiveEventOf(inputEventDispatcher.lastKeyEventUp, florisboard.prefs.keyboard.longPressDelay.toLong())) {
                val text = activeEditorInstance.getTextBeforeCursor(2)
                if (text.length == 2 && !text.matches("""[.!?‽\s][\s]""".toRegex())) {
                    activeEditorInstance.deleteBackwards()
                    activeEditorInstance.commitText(".")
                }
            }
        }
        isGlidePostEffect = false
        activeEditorInstance.commitText(KeyCode.SPACE.toChar().toString())
    }

    /**
     * Handles [KeyCode] arrow and move events, behaves differently depending on text selection.
     */
    private fun handleArrow(code: Int, count: Int) = activeEditorInstance.apply {
        val isShiftPressed = isManualSelectionMode || inputEventDispatcher.isPressed(KeyCode.SHIFT)
        when (code) {
            KeyCode.ARROW_DOWN -> {
                if (!selection.isSelectionMode && isManualSelectionMode) {
                    isManualSelectionModeStart = false
                    isManualSelectionModeEnd = true
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, meta(shift = isShiftPressed), count)
            }
            KeyCode.ARROW_LEFT -> {
                if (!selection.isSelectionMode && isManualSelectionMode) {
                    isManualSelectionModeStart = true
                    isManualSelectionModeEnd = false
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, meta(shift = isShiftPressed), count)
            }
            KeyCode.ARROW_RIGHT -> {
                if (!selection.isSelectionMode && isManualSelectionMode) {
                    isManualSelectionModeStart = false
                    isManualSelectionModeEnd = true
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, meta(shift = isShiftPressed), count)
            }
            KeyCode.ARROW_UP -> {
                if (!selection.isSelectionMode && isManualSelectionMode) {
                    isManualSelectionModeStart = true
                    isManualSelectionModeEnd = false
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_UP, meta(shift = isShiftPressed), count)
            }
            KeyCode.MOVE_START_OF_PAGE -> {
                if (!selection.isSelectionMode && isManualSelectionMode) {
                    isManualSelectionModeStart = true
                    isManualSelectionModeEnd = false
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_UP, meta(alt = true, shift = isShiftPressed), count)
            }
            KeyCode.MOVE_END_OF_PAGE -> {
                if (!selection.isSelectionMode && isManualSelectionMode) {
                    isManualSelectionModeStart = false
                    isManualSelectionModeEnd = true
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, meta(alt = true, shift = isShiftPressed), count)
            }
            KeyCode.MOVE_START_OF_LINE -> {
                if (!selection.isSelectionMode && isManualSelectionMode) {
                    isManualSelectionModeStart = true
                    isManualSelectionModeEnd = false
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, meta(alt = true, shift = isShiftPressed), count)
            }
            KeyCode.MOVE_END_OF_LINE -> {
                if (!selection.isSelectionMode && isManualSelectionMode) {
                    isManualSelectionModeStart = false
                    isManualSelectionModeEnd = true
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, meta(alt = true, shift = isShiftPressed), count)
            }
        }
        isGlidePostEffect = false
    }

    /**
     * Handles a [KeyCode.CLIPBOARD_SELECT] event.
     */
    private fun handleClipboardSelect() = activeEditorInstance.apply {
        if (selection.isSelectionMode) {
            if (isManualSelectionMode && isManualSelectionModeStart) {
                selection.updateAndNotify(selection.start, selection.start)
            } else {
                selection.updateAndNotify(selection.end, selection.end)
            }
            isManualSelectionMode = false
        } else {
            isManualSelectionMode = !isManualSelectionMode
            // Must call to update UI properly
            //editingKeyboardView?.onUpdateSelection()
        }
        isGlidePostEffect = false
    }

    override fun onInputKeyDown(ev: InputKeyEvent) {
        val data = ev.data
        when (data.code) {
            KeyCode.INTERNAL_BATCH_EDIT -> {
                florisboard.beginInternalBatchEdit()
                return
            }
            KeyCode.SHIFT -> {
                handleShiftDown(ev)
            }
        }
    }

    override fun onInputKeyUp(ev: InputKeyEvent) {
        val data = ev.data
        when (data.code) {
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.MOVE_START_OF_PAGE,
            KeyCode.MOVE_END_OF_PAGE,
            KeyCode.MOVE_START_OF_LINE,
            KeyCode.MOVE_END_OF_LINE -> if (ev.action == InputKeyEvent.Action.DOWN_UP || ev.action == InputKeyEvent.Action.REPEAT) {
                handleArrow(data.code, ev.count)
            } else {
                handleArrow(data.code, 1)
            }
            KeyCode.CLEAR_CLIPBOARD_HISTORY -> florisboard.florisClipboardManager?.clearHistoryWithAnimation()
            KeyCode.CLIPBOARD_CUT -> activeEditorInstance.performClipboardCut()
            KeyCode.CLIPBOARD_COPY -> activeEditorInstance.performClipboardCopy()
            KeyCode.CLIPBOARD_PASTE -> activeEditorInstance.performClipboardPaste()
            KeyCode.CLIPBOARD_SELECT -> handleClipboardSelect()
            KeyCode.CLIPBOARD_SELECT_ALL -> activeEditorInstance.performClipboardSelectAll()
            KeyCode.DELETE -> handleDelete()
            KeyCode.DELETE_WORD -> handleDeleteWord()
            KeyCode.ENTER -> handleEnter()
            KeyCode.INTERNAL_BATCH_EDIT -> {
                florisboard.endInternalBatchEdit()
                return
            }
            KeyCode.LANGUAGE_SWITCH -> handleLanguageSwitch()
            KeyCode.REDO -> activeEditorInstance.performRedo()
            KeyCode.SETTINGS -> florisboard.launchSettings()
            KeyCode.SHIFT -> handleShiftUp()
            KeyCode.SHIFT_LOCK -> handleShiftLock()
            KeyCode.SHOW_INPUT_METHOD_PICKER -> florisboard.imeManager?.showInputMethodPicker()
            KeyCode.SPACE -> handleSpace(ev)
            KeyCode.SWITCH_TO_MEDIA_CONTEXT -> florisboard.setActiveInput(R.id.media_input)
            KeyCode.SWITCH_TO_CLIPBOARD_CONTEXT -> florisboard.setActiveInput(R.id.clip_input)
            KeyCode.SWITCH_TO_TEXT_CONTEXT -> florisboard.setActiveInput(R.id.text_input, forceSwitchToCharacters = true)
            KeyCode.TOGGLE_ONE_HANDED_MODE_LEFT -> florisboard.toggleOneHandedMode(isRight = false)
            KeyCode.TOGGLE_ONE_HANDED_MODE_RIGHT -> florisboard.toggleOneHandedMode(isRight = true)
            KeyCode.VIEW_CHARACTERS -> setActiveKeyboardMode(KeyboardMode.CHARACTERS)
            KeyCode.VIEW_NUMERIC -> setActiveKeyboardMode(KeyboardMode.NUMERIC)
            KeyCode.VIEW_NUMERIC_ADVANCED -> setActiveKeyboardMode(KeyboardMode.NUMERIC_ADVANCED)
            KeyCode.VIEW_PHONE -> setActiveKeyboardMode(KeyboardMode.PHONE)
            KeyCode.VIEW_PHONE2 -> setActiveKeyboardMode(KeyboardMode.PHONE2)
            KeyCode.VIEW_SYMBOLS -> setActiveKeyboardMode(KeyboardMode.SYMBOLS)
            KeyCode.VIEW_SYMBOLS2 -> setActiveKeyboardMode(KeyboardMode.SYMBOLS2)
            KeyCode.UNDO -> activeEditorInstance.performUndo()
            else -> {
                when (activeKeyboardMode) {
                    KeyboardMode.NUMERIC,
                    KeyboardMode.NUMERIC_ADVANCED,
                    KeyboardMode.PHONE,
                    KeyboardMode.PHONE2 -> when (data.type) {
                        KeyType.CHARACTER,
                        KeyType.NUMERIC -> {
                            val text = data.code.toChar().toString()
                            if (isGlidePostEffect && (CachedInput.isWordComponent(text) || text.isDigitsOnly())) {
                                activeEditorInstance.commitText(" ")
                            }
                            activeEditorInstance.commitText(text)
                        }
                        else -> when (data.code) {
                            KeyCode.PHONE_PAUSE,
                            KeyCode.PHONE_WAIT -> {
                                val text = data.code.toChar().toString()
                                if (isGlidePostEffect && (CachedInput.isWordComponent(text) || text.isDigitsOnly())) {
                                    activeEditorInstance.commitText(" ")
                                }
                                activeEditorInstance.commitText(text)
                            }
                        }
                    }
                    else -> when (data.type) {
                        KeyType.CHARACTER, KeyType.NUMERIC -> when (data.code) {
                            KeyCode.URI_COMPONENT_TLD -> {
                                val tld = data.label.toLowerCase(Locale.ENGLISH)
                                activeEditorInstance.commitText(tld)
                            }
                            else -> {
                                var text = data.code.toChar().toString()
                                val locale = if (florisboard.activeSubtype.locale.language == "el") { Locale.getDefault() } else { florisboard.activeSubtype.locale }
                                text = when (caps && activeKeyboardMode == KeyboardMode.CHARACTERS) {
                                    true -> text.toUpperCase(locale)
                                    false -> text
                                }
                                if (isGlidePostEffect && (CachedInput.isWordComponent(text) || text.isDigitsOnly())) {
                                    activeEditorInstance.commitText(" ")
                                }
                                activeEditorInstance.commitText(text)
                            }
                        }
                        else -> {
                            flogError(LogTopic.KEY_EVENTS) { "Received unknown key: $data" }
                        }
                    }
                }
            }
        }
        if (data.code != KeyCode.SHIFT && !capsLock && !inputEventDispatcher.isPressed(KeyCode.SHIFT)) {
            updateCapsState()
        }
        if (ev.data.code > KeyCode.SPACE) {
            isGlidePostEffect = false
        }
        smartbarView?.updateSmartbarState()
    }

    override fun onInputKeyRepeat(ev: InputKeyEvent) {
        florisboard.keyPressVibrate(isMovingGestureEffect = true)
        onInputKeyUp(ev)
    }

    override fun onInputKeyCancel(ev: InputKeyEvent) {
        val data = ev.data
        when (data.code) {
            KeyCode.SHIFT -> handleShiftCancel()
        }
    }

    fun handleGesture(word: String) {
        activeEditorInstance.commitGesture(fixCase(word))
    }

    /**
     * Changes a word to the current case.
     * eg if [capsLock] is true, abc -> ABC
     *    if [caps]     is true, abc -> Abc
     *    otherwise            , abc -> abc
     */
    fun fixCase(word: String): String {
        return when {
            capsLock -> word.toUpperCase(florisboard.activeSubtype.locale)
            caps -> word.capitalize(florisboard.activeSubtype.locale)
            else -> word
        }
    }
}
