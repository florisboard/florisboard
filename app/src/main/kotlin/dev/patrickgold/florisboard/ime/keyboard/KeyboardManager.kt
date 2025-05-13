/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard

import android.content.Context
import android.icu.lang.UCharacter
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypePreset
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.editor.ImeOptions
import dev.patrickgold.florisboard.ime.editor.InputAttributes
import dev.patrickgold.florisboard.ime.input.CapitalizationBehavior
import dev.patrickgold.florisboard.ime.input.InputEventDispatcher
import dev.patrickgold.florisboard.ime.input.InputKeyEventReceiver
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.nlp.ClipboardSuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.PunctuationRule
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.popup.PopupMappingComponent
import dev.patrickgold.florisboard.ime.text.composing.Composer
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboardCache
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.titlecase
import dev.patrickgold.florisboard.lib.uppercase
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.subtypeManager
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.florisboard.lib.android.AndroidKeyguardManager
import org.florisboard.lib.android.showLongToast
import org.florisboard.lib.android.showShortToast
import org.florisboard.lib.android.systemService
import org.florisboard.lib.kotlin.collectIn
import org.florisboard.lib.kotlin.collectLatestIn
import java.util.concurrent.atomic.AtomicInteger

private val DoubleSpacePeriodMatcher = """([^.!?‽\s]\s)""".toRegex()

class KeyboardManager(context: Context) : InputKeyEventReceiver {
    private val prefs by florisPreferenceModel()
    private val appContext by context.appContext()
    private val clipboardManager by context.clipboardManager()
    private val editorInstance by context.editorInstance()
    private val extensionManager by context.extensionManager()
    private val nlpManager by context.nlpManager()
    private val subtypeManager by context.subtypeManager()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val layoutManager = LayoutManager(context)
    private val keyboardCache = TextKeyboardCache()

    val resources = KeyboardManagerResources()
    val activeState = ObservableKeyboardState.new()
    var smartbarVisibleDynamicActionsCount by mutableIntStateOf(0)
    private var lastToastReference = WeakReference<Toast>(null)

    private val activeEvaluatorGuard = Mutex(locked = false)
    private var activeEvaluatorVersion = AtomicInteger(0)
    private val _activeEvaluator = MutableStateFlow<ComputingEvaluator>(DefaultComputingEvaluator)
    val activeEvaluator get() = _activeEvaluator.asStateFlow()
    private val _activeSmartbarEvaluator = MutableStateFlow<ComputingEvaluator>(DefaultComputingEvaluator)
    val activeSmartbarEvaluator get() = _activeSmartbarEvaluator.asStateFlow()
    private val _lastCharactersEvaluator = MutableStateFlow<ComputingEvaluator>(DefaultComputingEvaluator)
    val lastCharactersEvaluator get() = _lastCharactersEvaluator.asStateFlow()

    val inputEventDispatcher = InputEventDispatcher.new(
        repeatableKeyCodes = intArrayOf(
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.DELETE,
            KeyCode.FORWARD_DELETE,
            KeyCode.UNDO,
            KeyCode.REDO,
        )
    ).also { it.keyEventReceiver = this }

    init {
        scope.launch(Dispatchers.Main.immediate) {
            resources.anyChanged.observeForever {
                updateActiveEvaluators {
                    keyboardCache.clear()
                }
            }
            prefs.keyboard.numberRow.observeForever {
                updateActiveEvaluators {
                    keyboardCache.clear(KeyboardMode.CHARACTERS)
                }
            }
            prefs.keyboard.hintedNumberRowEnabled.observeForever {
                updateActiveEvaluators()
            }
            prefs.keyboard.hintedSymbolsEnabled.observeForever {
                updateActiveEvaluators()
            }
            prefs.keyboard.utilityKeyEnabled.observeForever {
                updateActiveEvaluators()
            }
            prefs.keyboard.utilityKeyAction.observeForever {
                updateActiveEvaluators()
            }
            activeState.collectLatestIn(scope) {
                updateActiveEvaluators()
            }
            subtypeManager.subtypesFlow.collectLatestIn(scope) {
                updateActiveEvaluators()
            }
            subtypeManager.activeSubtypeFlow.collectLatestIn(scope) {
                reevaluateInputShiftState()
                updateActiveEvaluators()
                editorInstance.refreshComposing()
                resetSuggestions(editorInstance.activeContent)
            }
            clipboardManager.primaryClipFlow.collectLatestIn(scope) {
                updateActiveEvaluators()
            }
            editorInstance.activeContentFlow.collectIn(scope) { content ->
                resetSuggestions(content)
            }
            prefs.devtools.enabled.observeForever {
                reevaluateDebugFlags()
            }
            prefs.devtools.showDragAndDropHelpers.observeForever {
                reevaluateDebugFlags()
            }
        }
    }

    private fun updateActiveEvaluators(action: () -> Unit = { }) = scope.launch {
        activeEvaluatorGuard.withLock {
            action()
            val editorInfo = editorInstance.activeInfo
            val state = activeState.snapshot()
            val subtype = subtypeManager.activeSubtype
            val mode = state.keyboardMode
            // We need to reset the snapshot input shift state for non-character layouts, because the shift mechanic
            // only makes sense for the character layouts.
            if (mode != KeyboardMode.CHARACTERS) {
                state.inputShiftState = InputShiftState.UNSHIFTED
            }
            val computedKeyboard = keyboardCache.getOrElseAsync(mode, subtype) {
                layoutManager.computeKeyboardAsync(
                    keyboardMode = mode,
                    subtype = subtype,
                ).await()
            }
            val computingEvaluator = ComputingEvaluatorImpl(
                version = activeEvaluatorVersion.getAndAdd(1),
                keyboard = computedKeyboard,
                editorInfo = editorInfo,
                state = state,
                subtype = subtype,
            )
            for (key in computedKeyboard.keys()) {
                key.compute(computingEvaluator)
                key.computeLabelsAndDrawables(computingEvaluator)
            }
            _activeEvaluator.value = computingEvaluator
            _activeSmartbarEvaluator.value = computingEvaluator.asSmartbarQuickActionsEvaluator()
            if (computedKeyboard.mode == KeyboardMode.CHARACTERS) {
                _lastCharactersEvaluator.value = computingEvaluator
            }
        }
    }

    fun reevaluateInputShiftState() {
        if (activeState.inputShiftState != InputShiftState.CAPS_LOCK && !inputEventDispatcher.isPressed(KeyCode.SHIFT)) {
            val shift = prefs.correction.autoCapitalization.get()
                && subtypeManager.activeSubtype.primaryLocale.supportsCapitalization
                && editorInstance.activeCursorCapsMode != InputAttributes.CapsMode.NONE
            activeState.inputShiftState = when {
                shift -> InputShiftState.SHIFTED_AUTOMATIC
                else -> InputShiftState.UNSHIFTED
            }
        }
    }

    fun resetSuggestions(content: EditorContent) {
        if (!(activeState.isComposingEnabled || nlpManager.isSuggestionOn())) {
            nlpManager.clearSuggestions()
            return
        }
        nlpManager.suggest(subtypeManager.activeSubtype, content)
    }

    /**
     * @return If the language switch should be shown.
     */
    fun shouldShowLanguageSwitch(): Boolean {
        return subtypeManager.subtypes.size > 1
    }

    fun toggleOneHandedMode() {
        prefs.keyboard.oneHandedModeEnabled.set(!prefs.keyboard.oneHandedModeEnabled.get())
    }

    fun executeSwipeAction(swipeAction: SwipeAction) {
        val keyData = when (swipeAction) {
            SwipeAction.CYCLE_TO_PREVIOUS_KEYBOARD_MODE -> when (activeState.keyboardMode) {
                KeyboardMode.CHARACTERS -> TextKeyData.VIEW_NUMERIC_ADVANCED
                KeyboardMode.NUMERIC_ADVANCED -> TextKeyData.VIEW_SYMBOLS2
                KeyboardMode.SYMBOLS2 -> TextKeyData.VIEW_SYMBOLS
                else -> TextKeyData.VIEW_CHARACTERS
            }
            SwipeAction.CYCLE_TO_NEXT_KEYBOARD_MODE -> when (activeState.keyboardMode) {
                KeyboardMode.CHARACTERS -> TextKeyData.VIEW_SYMBOLS
                KeyboardMode.SYMBOLS -> TextKeyData.VIEW_SYMBOLS2
                KeyboardMode.SYMBOLS2 -> TextKeyData.VIEW_NUMERIC_ADVANCED
                else -> TextKeyData.VIEW_CHARACTERS
            }
            SwipeAction.DELETE_WORD -> TextKeyData.DELETE_WORD
            SwipeAction.HIDE_KEYBOARD -> TextKeyData.IME_HIDE_UI
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
            SwipeAction.SHOW_INPUT_METHOD_PICKER -> TextKeyData.SYSTEM_INPUT_METHOD_PICKER
            SwipeAction.SHOW_SUBTYPE_PICKER -> TextKeyData.SHOW_SUBTYPE_PICKER
            SwipeAction.SWITCH_TO_CLIPBOARD_CONTEXT -> TextKeyData.IME_UI_MODE_CLIPBOARD
            SwipeAction.SWITCH_TO_PREV_SUBTYPE -> TextKeyData.IME_PREV_SUBTYPE
            SwipeAction.SWITCH_TO_NEXT_SUBTYPE -> TextKeyData.IME_NEXT_SUBTYPE
            SwipeAction.SWITCH_TO_PREV_KEYBOARD -> TextKeyData.SYSTEM_PREV_INPUT_METHOD
            SwipeAction.TOGGLE_SMARTBAR_VISIBILITY -> TextKeyData.TOGGLE_SMARTBAR_VISIBILITY
            else -> null
        }
        if (keyData != null) {
            inputEventDispatcher.sendDownUp(keyData)
        }
    }

    fun commitCandidate(candidate: SuggestionCandidate) {
        scope.launch {
            candidate.sourceProvider?.notifySuggestionAccepted(subtypeManager.activeSubtype, candidate)
        }
        when (candidate) {
            is ClipboardSuggestionCandidate -> editorInstance.commitClipboardItem(candidate.clipboardItem)
            else -> editorInstance.commitCompletion(candidate)
        }
    }

    fun commitGesture(word: String) {
        editorInstance.commitGesture(fixCase(word))
    }

    /**
     * Changes a word to the current case.
     * eg if [KeyboardState.isUppercase] is true, abc -> ABC
     *    if [caps]     is true, abc -> Abc
     *    otherwise            , abc -> abc
     */
    fun fixCase(word: String): String {
        return when(activeState.inputShiftState) {
            InputShiftState.CAPS_LOCK -> {
                word.uppercase(subtypeManager.activeSubtype.primaryLocale)
            }
            InputShiftState.SHIFTED_MANUAL, InputShiftState.SHIFTED_AUTOMATIC -> {
                word.titlecase(subtypeManager.activeSubtype.primaryLocale)
            }
            else -> word
        }
    }

    /**
     * Handles [KeyCode] arrow and move events, behaves differently depending on text selection.
     */
    fun handleArrow(code: Int, count: Int = 1) = editorInstance.apply {
        val isShiftPressed = activeState.isManualSelectionMode || inputEventDispatcher.isPressed(KeyCode.SHIFT)
        val content = activeContent
        val selection = content.selection
        when (code) {
            KeyCode.ARROW_LEFT -> {
                if (!selection.isSelectionMode && activeState.isManualSelectionMode) {
                    activeState.isManualSelectionModeStart = true
                    activeState.isManualSelectionModeEnd = false
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, meta(shift = isShiftPressed), count)
            }
            KeyCode.ARROW_RIGHT -> {
                if (!selection.isSelectionMode && activeState.isManualSelectionMode) {
                    activeState.isManualSelectionModeStart = false
                    activeState.isManualSelectionModeEnd = true
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, meta(shift = isShiftPressed), count)
            }
            KeyCode.ARROW_UP -> {
                if (!selection.isSelectionMode && activeState.isManualSelectionMode) {
                    activeState.isManualSelectionModeStart = true
                    activeState.isManualSelectionModeEnd = false
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_UP, meta(shift = isShiftPressed), count)
            }
            KeyCode.ARROW_DOWN -> {
                if (!selection.isSelectionMode && activeState.isManualSelectionMode) {
                    activeState.isManualSelectionModeStart = false
                    activeState.isManualSelectionModeEnd = true
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, meta(shift = isShiftPressed), count)
            }
            KeyCode.MOVE_START_OF_PAGE -> {
                if (!selection.isSelectionMode && activeState.isManualSelectionMode) {
                    activeState.isManualSelectionModeStart = true
                    activeState.isManualSelectionModeEnd = false
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_UP, meta(alt = true, shift = isShiftPressed), count)
            }
            KeyCode.MOVE_END_OF_PAGE -> {
                if (!selection.isSelectionMode && activeState.isManualSelectionMode) {
                    activeState.isManualSelectionModeStart = false
                    activeState.isManualSelectionModeEnd = true
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, meta(alt = true, shift = isShiftPressed), count)
            }
            KeyCode.MOVE_START_OF_LINE -> {
                if (!selection.isSelectionMode && activeState.isManualSelectionMode) {
                    activeState.isManualSelectionModeStart = true
                    activeState.isManualSelectionModeEnd = false
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, meta(alt = true, shift = isShiftPressed), count)
            }
            KeyCode.MOVE_END_OF_LINE -> {
                if (!selection.isSelectionMode && activeState.isManualSelectionMode) {
                    activeState.isManualSelectionModeStart = false
                    activeState.isManualSelectionModeEnd = true
                }
                sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, meta(alt = true, shift = isShiftPressed), count)
            }
        }
    }

    /**
     * Handles a [KeyCode.CLIPBOARD_SELECT] event.
     */
    private fun handleClipboardSelect() {
        val activeSelection = editorInstance.activeContent.selection
        activeState.isManualSelectionMode = if (activeSelection.isSelectionMode) {
            if (activeState.isManualSelectionMode && activeState.isManualSelectionModeStart) {
                editorInstance.setSelection(activeSelection.start, activeSelection.start)
            } else {
                editorInstance.setSelection(activeSelection.end, activeSelection.end)
            }
            false
        } else {
            !activeState.isManualSelectionMode
        }
    }

    private fun revertPreviouslyAcceptedCandidate() {
        editorInstance.phantomSpace.candidateForRevert?.let { candidateForRevert ->
            candidateForRevert.sourceProvider?.let { sourceProvider ->
                scope.launch {
                    sourceProvider.notifySuggestionReverted(
                        subtype = subtypeManager.activeSubtype,
                        candidate = candidateForRevert,
                    )
                }
            }
        }
    }

    /**
     * Handles a [KeyCode.DELETE] event.
     */
    private fun handleDelete() {
        activeState.batchEdit {
            it.isManualSelectionMode = false
            it.isManualSelectionModeStart = false
            it.isManualSelectionModeEnd = false
        }
        revertPreviouslyAcceptedCandidate()
        editorInstance.deleteBackwards()
    }

    /**
     * Handles a [KeyCode.DELETE_WORD] event.
     */
    private fun handleDeleteWord() {
        activeState.batchEdit {
            it.isManualSelectionMode = false
            it.isManualSelectionModeStart = false
            it.isManualSelectionModeEnd = false
        }
        revertPreviouslyAcceptedCandidate()
        editorInstance.deleteWordBackwards()
    }

    /**
     * Handles a [KeyCode.ENTER] event.
     */
    private fun handleEnter() {
        val info = editorInstance.activeInfo
        val isShiftPressed = inputEventDispatcher.isPressed(KeyCode.SHIFT)
        if (editorInstance.tryPerformEnterCommitRaw()) {
            return
        }
        if (info.imeOptions.flagNoEnterAction || info.inputAttributes.flagTextMultiLine && isShiftPressed) {
            editorInstance.performEnter()
        } else {
            when (val action = info.imeOptions.action) {
                ImeOptions.Action.DONE,
                ImeOptions.Action.GO,
                ImeOptions.Action.NEXT,
                ImeOptions.Action.PREVIOUS,
                ImeOptions.Action.SEARCH,
                ImeOptions.Action.SEND -> {
                    editorInstance.performEnterAction(action)
                }
                else -> editorInstance.performEnter()
            }
        }
    }

    /**
     * Handles a [KeyCode.LANGUAGE_SWITCH] event. Also handles if the language switch should cycle
     * FlorisBoard internal or system-wide.
     */
    private fun handleLanguageSwitch() {
        when (prefs.keyboard.utilityKeyAction.get()) {
            UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS,
            UtilityKeyAction.SWITCH_LANGUAGE -> subtypeManager.switchToNextSubtype()
            else -> FlorisImeService.switchToNextInputMethod()
        }
    }

    /**
     * Handles a [KeyCode.SHIFT] down event.
     */
    private fun handleShiftDown(data: KeyData) {
        val prefs = prefs.keyboard.capitalizationBehavior
        when (prefs.get()) {
            CapitalizationBehavior.CAPSLOCK_BY_DOUBLE_TAP -> {
                if (inputEventDispatcher.isConsecutiveDown(data)) {
                    activeState.inputShiftState = InputShiftState.CAPS_LOCK
                } else {
                    if (activeState.inputShiftState == InputShiftState.UNSHIFTED) {
                        activeState.inputShiftState = InputShiftState.SHIFTED_MANUAL
                    } else {
                        activeState.inputShiftState = InputShiftState.UNSHIFTED
                    }
                }
            }
            CapitalizationBehavior.CAPSLOCK_BY_CYCLE -> {
                activeState.inputShiftState = when (activeState.inputShiftState) {
                    InputShiftState.UNSHIFTED -> InputShiftState.SHIFTED_MANUAL
                    InputShiftState.SHIFTED_MANUAL -> InputShiftState.CAPS_LOCK
                    InputShiftState.SHIFTED_AUTOMATIC -> InputShiftState.UNSHIFTED
                    InputShiftState.CAPS_LOCK -> InputShiftState.UNSHIFTED
                }
            }
        }
    }

    /**
     * Handles a [KeyCode.SHIFT] up event.
     */
    private fun handleShiftUp(data: KeyData) {
        if (activeState.inputShiftState != InputShiftState.CAPS_LOCK && !inputEventDispatcher.isAnyPressed() &&
            !inputEventDispatcher.isUninterruptedEventSequence(data)) {
            activeState.inputShiftState = InputShiftState.UNSHIFTED
        }
    }

    /**
     * Handles a [KeyCode.CAPS_LOCK] event.
     */
    private fun handleCapsLock() {
        activeState.inputShiftState = InputShiftState.CAPS_LOCK
    }

    /**
     * Handles a [KeyCode.SHIFT] cancel event.
     */
    private fun handleShiftCancel() {
        activeState.inputShiftState = InputShiftState.UNSHIFTED
    }

    /**
     * Handles a hardware [KeyEvent.KEYCODE_SPACE] event. Same as [handleSpace],
     * but skips handling changing to characters keyboard and double space periods.
     */
    fun handleHardwareKeyboardSpace() {
        val candidate = nlpManager.getAutoCommitCandidate()
        candidate?.let { commitCandidate(it) }
        // Skip handling changing to characters keyboard and double space periods
        // TODO: this is whether we commit space after selecting candidate. Should be determined by SuggestionProvider
        if (!subtypeManager.activeSubtype.primaryLocale.supportsAutoSpace &&
                candidate != null) { /* Do nothing */ } else {
            editorInstance.commitText(KeyCode.SPACE.toChar().toString())
        }
    }

    /**
     * Handles a [KeyCode.SPACE] event. Also handles the auto-correction of two space taps if
     * enabled by the user.
     */
    private fun handleSpace(data: KeyData) {
        val candidate = nlpManager.getAutoCommitCandidate()
        candidate?.let { commitCandidate(it) }
        if (prefs.keyboard.spaceBarSwitchesToCharacters.get()) {
            when (activeState.keyboardMode) {
                KeyboardMode.NUMERIC_ADVANCED,
                KeyboardMode.SYMBOLS,
                KeyboardMode.SYMBOLS2 -> {
                    activeState.keyboardMode = KeyboardMode.CHARACTERS
                }
                else -> { /* Do nothing */ }
            }
        }
        if (prefs.correction.doubleSpacePeriod.get()) {
            if (inputEventDispatcher.isConsecutiveUp(data)) {
                val text = editorInstance.run { activeContent.getTextBeforeCursor(2) }
                if (text.length == 2 && DoubleSpacePeriodMatcher.matches(text)) {
                    editorInstance.deleteBackwards()
                    editorInstance.commitText(". ")
                    return
                }
            }
        }
        // TODO: this is whether we commit space after selecting candidate. Should be determined by SuggestionProvider
        if (!subtypeManager.activeSubtype.primaryLocale.supportsAutoSpace &&
                candidate != null) { /* Do nothing */ } else {
            editorInstance.commitText(KeyCode.SPACE.toChar().toString())
        }
    }

    /**
     * Handles a [KeyCode.TOGGLE_INCOGNITO_MODE] event.
     */
    private fun handleToggleIncognitoMode() {
        prefs.suggestion.forceIncognitoModeFromDynamic.set(!prefs.suggestion.forceIncognitoModeFromDynamic.get())
        val newState = !activeState.isIncognitoMode
        activeState.isIncognitoMode = newState
        lastToastReference.get()?.cancel()
        lastToastReference = WeakReference(
            if (newState) {
                appContext.showLongToast(
                    R.string.incognito_mode__toast_after_enabled,
                    "app_name" to appContext.getString(R.string.floris_app_name),
                )
            } else {
                appContext.showLongToast(
                    R.string.incognito_mode__toast_after_disabled,
                    "app_name" to appContext.getString(R.string.floris_app_name),
                )
            }
        )
    }

    /**
     * Handles a [KeyCode.TOGGLE_AUTOCORRECT] event.
     */
    private fun handleToggleAutocorrect() {
        lastToastReference.get()?.cancel()
        lastToastReference = WeakReference(
            appContext.showLongToast("Autocorrect toggle is a placeholder and not yet implemented")
        )
    }

    /**
     * Handles a [KeyCode.KANA_SWITCHER] event
     */
    private fun handleKanaSwitch() {
        activeState.batchEdit {
            it.isKanaKata = !it.isKanaKata
            it.isCharHalfWidth = false
        }
    }

    /**
     * Handles a [KeyCode.KANA_HIRA] event
     */
    private fun handleKanaHira() {
        activeState.batchEdit {
            it.isKanaKata = false
            it.isCharHalfWidth = false
        }
    }

    /**
     * Handles a [KeyCode.KANA_KATA] event
     */
    private fun handleKanaKata() {
        activeState.batchEdit {
            it.isKanaKata = true
            it.isCharHalfWidth = false
        }
    }

    /**
     * Handles a [KeyCode.KANA_HALF_KATA] event
     */
    private fun handleKanaHalfKata() {
        activeState.batchEdit {
            it.isKanaKata = true
            it.isCharHalfWidth = true
        }
    }

    /**
     * Handles a [KeyCode.CHAR_WIDTH_SWITCHER] event
     */
    private fun handleCharWidthSwitch() {
        activeState.isCharHalfWidth = !activeState.isCharHalfWidth
    }

    /**
     * Handles a [KeyCode.CHAR_WIDTH_SWITCHER] event
     */
    private fun handleCharWidthFull() {
        activeState.isCharHalfWidth = false
    }

    /**
     * Handles a [KeyCode.CHAR_WIDTH_SWITCHER] event
     */
    private fun handleCharWidthHalf() {
        activeState.isCharHalfWidth = true
    }

    override fun onInputKeyDown(data: KeyData) {
        when (data.code) {
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.MOVE_START_OF_PAGE,
            KeyCode.MOVE_END_OF_PAGE,
            KeyCode.MOVE_START_OF_LINE,
            KeyCode.MOVE_END_OF_LINE -> {
                editorInstance.massSelection.begin()
            }
            KeyCode.SHIFT -> handleShiftDown(data)
        }
    }

    override fun onInputKeyUp(data: KeyData) = activeState.batchEdit {
        when (data.code) {
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.MOVE_START_OF_PAGE,
            KeyCode.MOVE_END_OF_PAGE,
            KeyCode.MOVE_START_OF_LINE,
            KeyCode.MOVE_END_OF_LINE -> {
                editorInstance.massSelection.end()
                handleArrow(data.code)
            }
            KeyCode.CAPS_LOCK -> handleCapsLock()
            KeyCode.CHAR_WIDTH_SWITCHER -> handleCharWidthSwitch()
            KeyCode.CHAR_WIDTH_FULL -> handleCharWidthFull()
            KeyCode.CHAR_WIDTH_HALF -> handleCharWidthHalf()
            KeyCode.CLIPBOARD_CUT -> editorInstance.performClipboardCut()
            KeyCode.CLIPBOARD_COPY -> editorInstance.performClipboardCopy()
            KeyCode.CLIPBOARD_PASTE -> editorInstance.performClipboardPaste()
            KeyCode.CLIPBOARD_SELECT -> handleClipboardSelect()
            KeyCode.CLIPBOARD_SELECT_ALL -> editorInstance.performClipboardSelectAll()
            KeyCode.CLIPBOARD_CLEAR_HISTORY -> clipboardManager.clearHistory()
            KeyCode.CLIPBOARD_CLEAR_FULL_HISTORY -> clipboardManager.clearFullHistory()
            KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> {
                if (prefs.clipboard.clearPrimaryClipDeletesLastItem.get()) {
                    clipboardManager.primaryClip?.let { clipboardManager.deleteClip(it) }
                }
                clipboardManager.updatePrimaryClip(null)
                appContext.showShortToast(R.string.clipboard__cleared_primary_clip)
            }
            KeyCode.TOGGLE_COMPACT_LAYOUT -> toggleOneHandedMode()
            KeyCode.COMPACT_LAYOUT_TO_LEFT -> {
                prefs.keyboard.oneHandedMode.set(OneHandedMode.START)
                toggleOneHandedMode()
            }
            KeyCode.COMPACT_LAYOUT_TO_RIGHT -> {
                prefs.keyboard.oneHandedMode.set(OneHandedMode.END)
                toggleOneHandedMode()
            }
            KeyCode.DELETE -> handleDelete()
            KeyCode.DELETE_WORD -> handleDeleteWord()
            KeyCode.ENTER -> handleEnter()
            KeyCode.IME_SHOW_UI -> FlorisImeService.showUi()
            KeyCode.IME_HIDE_UI -> FlorisImeService.hideUi()
            KeyCode.IME_PREV_SUBTYPE -> subtypeManager.switchToPrevSubtype()
            KeyCode.IME_NEXT_SUBTYPE -> subtypeManager.switchToNextSubtype()
            KeyCode.IME_UI_MODE_TEXT -> activeState.imeUiMode = ImeUiMode.TEXT
            KeyCode.IME_UI_MODE_MEDIA -> activeState.imeUiMode = ImeUiMode.MEDIA
            KeyCode.IME_UI_MODE_CLIPBOARD -> activeState.imeUiMode = ImeUiMode.CLIPBOARD
            KeyCode.VOICE_INPUT -> FlorisImeService.switchToVoiceInputMethod()
            KeyCode.KANA_SWITCHER -> handleKanaSwitch()
            KeyCode.KANA_HIRA -> handleKanaHira()
            KeyCode.KANA_KATA -> handleKanaKata()
            KeyCode.KANA_HALF_KATA -> handleKanaHalfKata()
            KeyCode.LANGUAGE_SWITCH -> handleLanguageSwitch()
            KeyCode.REDO -> editorInstance.performRedo()
            KeyCode.SETTINGS -> FlorisImeService.launchSettings()
            KeyCode.SHIFT -> handleShiftUp(data)
            KeyCode.SPACE -> handleSpace(data)
            KeyCode.SYSTEM_INPUT_METHOD_PICKER -> InputMethodUtils.showImePicker(appContext)
            KeyCode.SHOW_SUBTYPE_PICKER -> {
                appContext.keyboardManager.value.activeState.isSubtypeSelectionVisible = true
            }
            KeyCode.SYSTEM_PREV_INPUT_METHOD -> FlorisImeService.switchToPrevInputMethod()
            KeyCode.SYSTEM_NEXT_INPUT_METHOD -> FlorisImeService.switchToNextInputMethod()
            KeyCode.TOGGLE_SMARTBAR_VISIBILITY -> {
                prefs.smartbar.enabled.let { it.set(!it.get()) }
            }
            KeyCode.TOGGLE_ACTIONS_OVERFLOW -> {
                activeState.isActionsOverflowVisible = !activeState.isActionsOverflowVisible
            }
            KeyCode.TOGGLE_ACTIONS_EDITOR -> {
                activeState.isActionsEditorVisible = !activeState.isActionsEditorVisible
            }
            KeyCode.TOGGLE_INCOGNITO_MODE -> handleToggleIncognitoMode()
            KeyCode.TOGGLE_AUTOCORRECT -> handleToggleAutocorrect()
            KeyCode.UNDO -> editorInstance.performUndo()
            KeyCode.VIEW_CHARACTERS -> activeState.keyboardMode = KeyboardMode.CHARACTERS
            KeyCode.VIEW_NUMERIC -> activeState.keyboardMode = KeyboardMode.NUMERIC
            KeyCode.VIEW_NUMERIC_ADVANCED -> activeState.keyboardMode = KeyboardMode.NUMERIC_ADVANCED
            KeyCode.VIEW_PHONE -> activeState.keyboardMode = KeyboardMode.PHONE
            KeyCode.VIEW_PHONE2 -> activeState.keyboardMode = KeyboardMode.PHONE2
            KeyCode.VIEW_SYMBOLS -> activeState.keyboardMode = KeyboardMode.SYMBOLS
            KeyCode.VIEW_SYMBOLS2 -> activeState.keyboardMode = KeyboardMode.SYMBOLS2
            else -> {
                if (activeState.imeUiMode == ImeUiMode.MEDIA) {
                    nlpManager.getAutoCommitCandidate()?.let { commitCandidate(it) }
                    editorInstance.commitText(data.asString(isForDisplay = false))
                    return@batchEdit
                }
                when (activeState.keyboardMode) {
                    KeyboardMode.NUMERIC,
                    KeyboardMode.NUMERIC_ADVANCED,
                    KeyboardMode.PHONE,
                    KeyboardMode.PHONE2 -> when (data.type) {
                        KeyType.CHARACTER,
                        KeyType.NUMERIC -> {
                            val text = data.asString(isForDisplay = false)
                            editorInstance.commitText(text)
                        }
                        else -> when (data.code) {
                            KeyCode.PHONE_PAUSE,
                            KeyCode.PHONE_WAIT -> {
                                val text = data.asString(isForDisplay = false)
                                editorInstance.commitText(text)
                            }
                        }
                    }
                    else -> when (data.type) {
                        KeyType.CHARACTER, KeyType.NUMERIC ->{
                            val text = data.asString(isForDisplay = false)
                            if (!UCharacter.isUAlphabetic(UCharacter.codePointAt(text, 0))) {
                                nlpManager.getAutoCommitCandidate()?.let { commitCandidate(it) }
                            }
                            editorInstance.commitChar(text)
                        }
                        else -> {
                            flogError(LogTopic.KEY_EVENTS) { "Received unknown key: $data" }
                        }
                    }
                }
                if (activeState.inputShiftState != InputShiftState.CAPS_LOCK && !inputEventDispatcher.isPressed(KeyCode.SHIFT)) {
                    activeState.inputShiftState = InputShiftState.UNSHIFTED
                }
            }
        }
    }

    override fun onInputKeyCancel(data: KeyData) {
        when (data.code) {
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.MOVE_START_OF_PAGE,
            KeyCode.MOVE_END_OF_PAGE,
            KeyCode.MOVE_START_OF_LINE,
            KeyCode.MOVE_END_OF_LINE -> {
                editorInstance.massSelection.end()
            }
            KeyCode.SHIFT -> handleShiftCancel()
        }
    }

    override fun onInputKeyRepeat(data: KeyData) {
        FlorisImeService.inputFeedbackController()?.keyRepeatedAction(data)
        when (data.code) {
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.MOVE_START_OF_PAGE,
            KeyCode.MOVE_END_OF_PAGE,
            KeyCode.MOVE_START_OF_LINE,
            KeyCode.MOVE_END_OF_LINE -> handleArrow(data.code)
            else -> onInputKeyUp(data)
        }
    }

    private fun reevaluateDebugFlags() {
        val devtoolsEnabled = prefs.devtools.enabled.get()
        activeState.batchEdit {
            activeState.debugShowDragAndDropHelpers = devtoolsEnabled && prefs.devtools.showDragAndDropHelpers.get()
        }
    }

    fun onHardwareKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                handleHardwareKeyboardSpace()
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                handleEnter()
                return true
            }
            else -> return false
        }
    }

    inner class KeyboardManagerResources {
        val composers = MutableLiveData<Map<ExtensionComponentName, Composer>>(emptyMap())
        val currencySets = MutableLiveData<Map<ExtensionComponentName, CurrencySet>>(emptyMap())
        val layouts = MutableLiveData<Map<LayoutType, Map<ExtensionComponentName, LayoutArrangementComponent>>>(emptyMap())
        val popupMappings = MutableLiveData<Map<ExtensionComponentName, PopupMappingComponent>>(emptyMap())
        val punctuationRules = MutableLiveData<Map<ExtensionComponentName, PunctuationRule>>(emptyMap())
        val subtypePresets = MutableLiveData<List<SubtypePreset>>(emptyList())

        private val anyChangedGuard = Mutex(locked = false)
        val anyChanged = MutableLiveData(Unit)

        init {
            scope.launch(Dispatchers.Main.immediate) {
                extensionManager.keyboardExtensions.observeForever { keyboardExtensions ->
                    scope.launch {
                        anyChangedGuard.withLock {
                            parseKeyboardExtensions(keyboardExtensions)
                        }
                    }
                }
            }
        }

        private fun parseKeyboardExtensions(keyboardExtensions: List<KeyboardExtension>) {
            val localComposers = mutableMapOf<ExtensionComponentName, Composer>()
            val localCurrencySets = mutableMapOf<ExtensionComponentName, CurrencySet>()
            val localLayouts = mutableMapOf<LayoutType, MutableMap<ExtensionComponentName, LayoutArrangementComponent>>()
            val localPopupMappings = mutableMapOf<ExtensionComponentName, PopupMappingComponent>()
            val localPunctuationRules = mutableMapOf<ExtensionComponentName, PunctuationRule>()
            val localSubtypePresets = mutableListOf<SubtypePreset>()
            for (layoutType in LayoutType.entries) {
                localLayouts[layoutType] = mutableMapOf()
            }
            for (keyboardExtension in keyboardExtensions) {
                keyboardExtension.composers.forEach { composer ->
                    localComposers[ExtensionComponentName(keyboardExtension.meta.id, composer.id)] = composer
                }
                keyboardExtension.currencySets.forEach { currencySet ->
                    localCurrencySets[ExtensionComponentName(keyboardExtension.meta.id, currencySet.id)] = currencySet
                }
                keyboardExtension.layouts.forEach { (type, layoutComponents) ->
                    for (layoutComponent in layoutComponents) {
                        localLayouts[LayoutType.entries.first { it.id == type }]!![ExtensionComponentName(keyboardExtension.meta.id, layoutComponent.id)] = layoutComponent
                    }
                }
                keyboardExtension.popupMappings.forEach { popupMapping ->
                    localPopupMappings[ExtensionComponentName(keyboardExtension.meta.id, popupMapping.id)] = popupMapping
                }
                keyboardExtension.punctuationRules.forEach { punctuationRule ->
                    localPunctuationRules[ExtensionComponentName(keyboardExtension.meta.id, punctuationRule.id)] = punctuationRule
                }
                localSubtypePresets.addAll(keyboardExtension.subtypePresets)
            }
            localSubtypePresets.sortBy { it.locale.displayName() }
            for (languageCode in listOf("en-CA", "en-AU", "en-UK", "en-US")) {
                val index: Int = localSubtypePresets.indexOfFirst { it.locale.languageTag() == languageCode }
                if (index > 0) {
                    localSubtypePresets.add(0, localSubtypePresets.removeAt(index))
                }
            }
            subtypePresets.postValue(localSubtypePresets)
            composers.postValue(localComposers)
            currencySets.postValue(localCurrencySets)
            layouts.postValue(localLayouts)
            popupMappings.postValue(localPopupMappings)
            punctuationRules.postValue(localPunctuationRules)
            anyChanged.postValue(Unit)
        }
    }

    private inner class ComputingEvaluatorImpl(
        override val version: Int,
        override val keyboard: Keyboard,
        override val editorInfo: FlorisEditorInfo,
        override val state: KeyboardState,
        override val subtype: Subtype,
    ) : ComputingEvaluator {

        override fun context(): Context = appContext

        val androidKeyguardManager = context().systemService(AndroidKeyguardManager::class)

        override fun displayLanguageNamesIn(): DisplayLanguageNamesIn {
            return prefs.localization.displayLanguageNamesIn.get()
        }

        override fun evaluateEnabled(data: KeyData): Boolean {
            return when (data.code) {
                KeyCode.CLIPBOARD_COPY,
                KeyCode.CLIPBOARD_CUT -> {
                    state.isSelectionMode && editorInfo.isRichInputEditor
                }
                KeyCode.CLIPBOARD_PASTE -> {
                    !androidKeyguardManager.let { it.isDeviceLocked || it.isKeyguardLocked }
                        && clipboardManager.canBePasted(clipboardManager.primaryClip)
                }
                KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> {
                    clipboardManager.canBePasted(clipboardManager.primaryClip)
                }
                KeyCode.CLIPBOARD_SELECT_ALL -> {
                    editorInfo.isRichInputEditor
                }
                KeyCode.TOGGLE_INCOGNITO_MODE -> when (prefs.suggestion.incognitoMode.get()) {
                    IncognitoMode.FORCE_OFF, IncognitoMode.FORCE_ON -> false
                    IncognitoMode.DYNAMIC_ON_OFF -> !editorInfo.imeOptions.flagNoPersonalizedLearning
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    subtypeManager.subtypes.size > 1
                }
                else -> true
            }
        }

        override fun evaluateVisible(data: KeyData): Boolean {
            return when (data.code) {
                KeyCode.IME_UI_MODE_TEXT,
                KeyCode.IME_UI_MODE_MEDIA -> {
                    val tempUtilityKeyAction = when {
                        prefs.keyboard.utilityKeyEnabled.get() -> prefs.keyboard.utilityKeyAction.get()
                        else -> UtilityKeyAction.DISABLED
                    }
                    when (tempUtilityKeyAction) {
                        UtilityKeyAction.DISABLED,
                        UtilityKeyAction.SWITCH_LANGUAGE,
                        UtilityKeyAction.SWITCH_KEYBOARD_APP -> false
                        UtilityKeyAction.SWITCH_TO_EMOJIS -> true
                        UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS -> !shouldShowLanguageSwitch()
                    }
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    val tempUtilityKeyAction = when {
                        prefs.keyboard.utilityKeyEnabled.get() -> prefs.keyboard.utilityKeyAction.get()
                        else -> UtilityKeyAction.DISABLED
                    }
                    when (tempUtilityKeyAction) {
                        UtilityKeyAction.DISABLED,
                        UtilityKeyAction.SWITCH_TO_EMOJIS -> false
                        UtilityKeyAction.SWITCH_LANGUAGE,
                        UtilityKeyAction.SWITCH_KEYBOARD_APP -> true
                        UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS -> shouldShowLanguageSwitch()
                    }
                }
                else -> true
            }
        }

        override fun isSlot(data: KeyData): Boolean {
            return CurrencySet.isCurrencySlot(data.code)
        }

        override fun slotData(data: KeyData): KeyData? {
            return subtypeManager.getCurrencySet(subtype).getSlot(data.code)
        }

        fun asSmartbarQuickActionsEvaluator(): ComputingEvaluatorImpl {
            return ComputingEvaluatorImpl(
                version = version,
                keyboard = SmartbarQuickActionsKeyboard,
                editorInfo = editorInfo,
                state = state,
                subtype = Subtype.DEFAULT,
            )
        }
    }
}
