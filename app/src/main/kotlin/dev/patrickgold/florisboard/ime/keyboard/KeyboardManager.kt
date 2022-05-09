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

package dev.patrickgold.florisboard.ime.keyboard

import android.content.Context
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.neverEqualPolicy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.glideTypingManager
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypePreset
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.editor.ImeOptions
import dev.patrickgold.florisboard.ime.editor.InputAttributes
import dev.patrickgold.florisboard.ime.input.InputEventDispatcher
import dev.patrickgold.florisboard.ime.input.InputKeyEventReceiver
import dev.patrickgold.florisboard.ime.nlp.NlpManager
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.popup.PopupMappingComponent
import dev.patrickgold.florisboard.ime.text.composing.Composer
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboard
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboardCache
import dev.patrickgold.florisboard.ime.text.smartbar.SmartbarActions
import dev.patrickgold.florisboard.lib.android.showShortToast
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RenderInfo(
    val version: Int = 0,
    val keyboard: TextKeyboard = PlaceholderLoadingKeyboard,
    val state: KeyboardState = KeyboardState.new(),
    val evaluator: ComputingEvaluator = DefaultComputingEvaluator,
)

private val DefaultRenderInfo = RenderInfo()
private val DoubleSpacePeriodMatcher = """([^.!?‽\s]\s)""".toRegex()

class KeyboardManager(context: Context) : InputKeyEventReceiver {
    private val prefs by florisPreferenceModel()
    private val appContext by context.appContext()
    private val clipboardManager by context.clipboardManager()
    private val editorInstance by context.editorInstance()
    private val extensionManager by context.extensionManager()
    private val glideTypingManager by context.glideTypingManager()
    private val nlpManager by context.nlpManager()
    private val subtypeManager by context.subtypeManager()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val layoutManager = LayoutManager(context)
    private val keyboardCache = TextKeyboardCache()

    val resources = KeyboardManagerResources()
    val smartbarActions = SmartbarActions()
    val activeState = KeyboardState.new()

    private val renderInfoGuard = Mutex(locked = false)
    private var renderInfoVersion: Int = 1
    private val _renderInfo = MutableLiveData(DefaultRenderInfo)
    val renderInfo: LiveData<RenderInfo> get() = _renderInfo
    private val _smartbarRenderInfo = MutableLiveData(DefaultRenderInfo)
    val smartbarRenderInfo: LiveData<RenderInfo> get() = _smartbarRenderInfo

    val inputEventDispatcher = InputEventDispatcher.new(
        repeatableKeyCodes = intArrayOf(
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.DELETE,
            KeyCode.FORWARD_DELETE
        )
    ).also { it.keyEventReceiver = this }

    init {
        resources.anyChanged.observeForever {
            updateRenderInfo {
                keyboardCache.clear()
            }
        }
        prefs.keyboard.numberRow.observeForever {
            updateRenderInfo {
                keyboardCache.clear(KeyboardMode.CHARACTERS)
            }
        }
        prefs.keyboard.hintedNumberRowEnabled.observeForever {
            updateRenderInfo()
        }
        prefs.keyboard.hintedSymbolsEnabled.observeForever {
            updateRenderInfo()
        }
        prefs.keyboard.utilityKeyEnabled.observeForever {
            updateRenderInfo()
        }
        prefs.glide.enabled.observeForever { enabled ->
            if (enabled) {
                glideTypingManager.setWordData(subtypeManager.activeSubtype())
            }
        }
        activeState.observeForever {
            updateRenderInfo()
        }
        subtypeManager.activeSubtype.observeForever { newSubtype ->
            updateRenderInfo()
            updateCapsState()
            if (prefs.glide.enabled.get()) {
                glideTypingManager.setWordData(newSubtype)
            }
        }
        clipboardManager.primaryClip.observeForever {
            updateRenderInfo()
        }
        scope.launch {
            withContext(Dispatchers.Main) {
                nlpManager.clearSuggestions()
            }
            editorInstance.activeContentFlow.collect { content ->
                if (content.composing.isNotValid || !activeState.isComposingEnabled) {
                    nlpManager.clearSuggestions()
                    return@collect
                }
                nlpManager.suggest(content.composingText, listOf())
            }
        }
    }

    private fun updateRenderInfo(action: () -> Unit = { }) = scope.launch {
        renderInfoGuard.withLock {
            action()
            val editorInfo = editorInstance.activeInfo
            val state = activeState.snapshot()
            val subtype = subtypeManager.activeSubtype()
            val mode = activeState.keyboardMode
            val computedKeyboard = keyboardCache.getOrElseAsync(mode, subtype) {
                layoutManager.computeKeyboardAsync(
                    keyboardMode = mode,
                    subtype = subtype,
                ).await()
            }.await()
            val computingEvaluator = KeyboardComputingEvaluator(
                keyboard = computedKeyboard,
                editorInfo = editorInfo,
                state = state,
                subtype = subtype,
            )
            for (key in computedKeyboard.keys()) {
                key.compute(computingEvaluator)
                key.computeLabelsAndDrawables(computingEvaluator)
            }
            _renderInfo.postValue(RenderInfo(
                version = renderInfoVersion++,
                keyboard = computedKeyboard,
                state = state,
                evaluator = computingEvaluator,
            ))
            smartbarClipboardCursorRenderInfo(editorInfo, state)
        }
    }

    private suspend fun smartbarClipboardCursorRenderInfo(editorInfo: FlorisEditorInfo, state: KeyboardState) {
        val mode = KeyboardMode.SMARTBAR_CLIPBOARD_CURSOR_ROW
        val subtype = Subtype.DEFAULT
        val computedKeyboard = keyboardCache.getOrElseAsync(mode, subtype) {
            layoutManager.computeKeyboardAsync(
                keyboardMode = mode,
                subtype = subtype,
            ).await()
        }.await()
        val computingEvaluator = KeyboardComputingEvaluator(
            keyboard = computedKeyboard,
            editorInfo = editorInfo,
            state = state,
            subtype = subtype,
        )
        for (key in computedKeyboard.keys()) {
            key.compute(computingEvaluator)
            key.computeLabelsAndDrawables(computingEvaluator)
        }
        _smartbarRenderInfo.postValue(RenderInfo(
            keyboard = computedKeyboard,
            state = state,
            evaluator = computingEvaluator,
        ))
    }

    @Composable
    fun observeActiveState(): State<KeyboardState> {
        return activeState.observeAsNonNullState(neverEqualPolicy())
    }

    fun updateCapsState() {
        if (activeState.inputShiftState != InputShiftState.CAPS_LOCK && !inputEventDispatcher.isPressed(KeyCode.SHIFT)) {
            val shift = prefs.correction.autoCapitalization.get()
                && subtypeManager.activeSubtype().primaryLocale.supportsCapitalization
                && editorInstance.activeCursorCapsMode != InputAttributes.CapsMode.NONE
            activeState.inputShiftState = when {
                shift -> InputShiftState.SHIFTED_AUTOMATIC
                else -> InputShiftState.UNSHIFTED
            }
        }
    }

    /**
     * @return If the language switch should be shown.
     */
    fun shouldShowLanguageSwitch(): Boolean {
        return subtypeManager.subtypes().size > 1
    }

    fun toggleOneHandedMode(isRight: Boolean) {
        prefs.keyboard.oneHandedMode.set(when (prefs.keyboard.oneHandedMode.get()) {
            OneHandedMode.OFF -> if (isRight) { OneHandedMode.END } else { OneHandedMode.START }
            else -> OneHandedMode.OFF
        })
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

    fun commitCandidate(candidate: NlpManager.Candidate) {
        when (candidate) {
            is NlpManager.Candidate.Word -> editorInstance.commitCompletion(candidate.word)
            is NlpManager.Candidate.Clip -> editorInstance.commitClipboardItem(candidate.clipboardItem)
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
                word.uppercase(subtypeManager.activeSubtype().primaryLocale.base)
            }
            InputShiftState.SHIFTED_MANUAL, InputShiftState.SHIFTED_AUTOMATIC -> {
                word.replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(subtypeManager.activeSubtype().primaryLocale.base)
                    } else {
                        it.toString()
                    }
                }
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

    /**
     * Handles a [KeyCode.DELETE] event.
     */
    private fun handleDelete() {
        activeState.batchEdit {
            it.isManualSelectionMode = false
            it.isManualSelectionModeStart = false
            it.isManualSelectionModeEnd = false
        }
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
        editorInstance.deleteWordBackwards()
    }

    /**
     * Handles a [KeyCode.ENTER] event.
     */
    private fun handleEnter() {
        val info = editorInstance.activeInfo
        val isShiftPressed = inputEventDispatcher.isPressed(KeyCode.SHIFT)
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
     * Handles a [KeyCode.SPACE] event. Also handles the auto-correction of two space taps if
     * enabled by the user.
     */
    private fun handleSpace(data: KeyData) {
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
        editorInstance.commitText(KeyCode.SPACE.toChar().toString())
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
                    clipboardManager.primaryClip()?.let { clipboardManager.deleteClip(it) }
                }
                clipboardManager.setPrimaryClip(null)
                appContext.showShortToast(R.string.clipboard__cleared_primary_clip)
            }
            KeyCode.COMPACT_LAYOUT_TO_LEFT -> toggleOneHandedMode(isRight = false)
            KeyCode.COMPACT_LAYOUT_TO_RIGHT -> toggleOneHandedMode(isRight = true)
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
            KeyCode.SYSTEM_PREV_INPUT_METHOD -> FlorisImeService.switchToPrevInputMethod()
            KeyCode.SYSTEM_NEXT_INPUT_METHOD -> FlorisImeService.switchToNextInputMethod()
            KeyCode.TOGGLE_SMARTBAR_VISIBILITY -> {
                prefs.smartbar.enabled.let { it.set(!it.get()) }
            }
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

    inner class KeyboardManagerResources {
        val composers = MutableLiveData<Map<ExtensionComponentName, Composer>>(emptyMap())
        val currencySets = MutableLiveData<Map<ExtensionComponentName, CurrencySet>>(emptyMap())
        val layouts = MutableLiveData<Map<LayoutType, Map<ExtensionComponentName, LayoutArrangementComponent>>>(emptyMap())
        val popupMappings = MutableLiveData<Map<ExtensionComponentName, PopupMappingComponent>>(emptyMap())
        val subtypePresets = MutableLiveData<List<SubtypePreset>>(emptyList())

        val anyChanged = MutableLiveData(Unit)

        init {
            extensionManager.keyboardExtensions.observeForever { keyboardExtensions ->
                parseKeyboardExtensions(keyboardExtensions)
            }
        }

        private fun parseKeyboardExtensions(keyboardExtensions: List<KeyboardExtension>) = scope.launch {
            val localComposers = mutableMapOf<ExtensionComponentName, Composer>()
            val localCurrencySets = mutableMapOf<ExtensionComponentName, CurrencySet>()
            val localLayouts = mutableMapOf<LayoutType, MutableMap<ExtensionComponentName, LayoutArrangementComponent>>()
            val localPopupMappings = mutableMapOf<ExtensionComponentName, PopupMappingComponent>()
            val localSubtypePresets = mutableListOf<SubtypePreset>()
            for (layoutType in LayoutType.values()) {
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
                        localLayouts[LayoutType.values().first { it.id == type }]!![ExtensionComponentName(keyboardExtension.meta.id, layoutComponent.id)] = layoutComponent
                    }
                }
                keyboardExtension.popupMappings.forEach { popupMapping ->
                    localPopupMappings[ExtensionComponentName(keyboardExtension.meta.id, popupMapping.id)] = popupMapping
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
            anyChanged.postValue(Unit)
        }
    }

    private inner class KeyboardComputingEvaluator(
        val keyboard: Keyboard,
        val editorInfo: FlorisEditorInfo,
        val state: KeyboardState,
        val subtype: Subtype,
    ) : ComputingEvaluator {

        override fun activeEditorInfo(): FlorisEditorInfo = editorInfo

        override fun activeState(): KeyboardState = state

        override fun activeSubtype(): Subtype = subtype

        override fun context(): Context = appContext

        override fun displayLanguageNamesIn(): DisplayLanguageNamesIn {
            return prefs.localization.displayLanguageNamesIn.get()
        }

        override fun evaluateEnabled(data: KeyData): Boolean {
            return when (data.code) {
                KeyCode.CLIPBOARD_COPY,
                KeyCode.CLIPBOARD_CUT -> {
                    state.isSelectionMode && editorInfo.isRichInputEditor
                }
                KeyCode.CLIPBOARD_PASTE,
                KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> {
                    clipboardManager.canBePasted(clipboardManager.primaryClip.value)
                }
                KeyCode.CLIPBOARD_SELECT_ALL -> {
                    editorInfo.isRichInputEditor
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

        override fun keyboard(): Keyboard = keyboard

        override fun isSlot(data: KeyData): Boolean {
            return CurrencySet.isCurrencySlot(data.code)
        }

        override fun slotData(data: KeyData): KeyData? {
            return subtypeManager.getCurrencySet(activeSubtype()).getSlot(data.code)
        }
    }
}
