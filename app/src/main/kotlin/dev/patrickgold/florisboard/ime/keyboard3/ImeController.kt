/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3

import android.icu.text.BreakIterator
import android.text.TextUtils
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.staticCompositionLocalOf
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.ClipboardManager
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.editor.ImeOptions
import dev.patrickgold.florisboard.ime.editor.InputAttributes
import dev.patrickgold.florisboard.ime.io.StorageController
import dev.patrickgold.florisboard.ime.keyboard.IncognitoMode
import dev.patrickgold.florisboard.ime.keyboard3.extension.loadFoundationKeyboard
import dev.patrickgold.florisboard.ime.keyboard3.hint.FlickKeyHintPlacement
import dev.patrickgold.florisboard.ime.keyboard3.hint.LongPressKeyHintPlacement
import dev.patrickgold.florisboard.ime.keyboard3.touch.FnKeyArrangement
import dev.patrickgold.florisboard.ime.keyboard3.touch.InputShiftState
import dev.patrickgold.florisboard.ime.keyboard3.touch.ShiftKeyBehavior
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchModelCache
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchModelOptions
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSuggestionType
import dev.patrickgold.florisboard.ime.mozc.MozcEngine
import dev.patrickgold.florisboard.ime.mozc.MozcKeyboardSpec
import dev.patrickgold.florisboard.ime.nlp.BreakIterators
import dev.patrickgold.florisboard.ime.nlp.ClipboardSuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.florisboard.lib.kotlin.collectIn
import org.k3lp.lib.text.K3Descriptor
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.lib.text.asK3String
import org.k3lp.lib.text.normalize
import org.k3lp.lib.text.unicode.NormalizationForm
import org.k3lp.model.K3Model
import org.k3lp.model.layer.K3LayerId
import org.k3lp.runtime.K3Content
import org.k3lp.runtime.K3InputMethod
import org.k3lp.runtime.K3SurroundingText
import org.k3lp.runtime.K3TextRange
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import java.lang.ref.WeakReference
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Provides the [ImeController] instance this composition tree is associated with.
 */
val LocalImeController = staticCompositionLocalOf<ImeController> {
    error("No IME controller is associated with this composition tree.")
}

@OptIn(ExperimentalAtomicApi::class)
class ImeController(
    storageController: StorageController,
    val clipboardManager: ClipboardManager, // TODO ClipboardController
    initialState: ImeState = ImeState(),
    val touchModelCache: TouchModelCache = TouchModelCache(),
) : K3InputMethod<ImeState, ImeEditor, ImeController.UpdateImeStateScope>(
    initialState = initialState,
) {
    private val prefs by FlorisPreferenceStore
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val breakIterators = BreakIterators()
    private val expectedContentQueue = ExpectedContentQueue()
    private var emitInterceptor = AtomicReference<EmitInterceptor?>(null)

    val activeTouchModelOptions = combine<Any, TouchModelOptions>(
        prefs.keyboard.numberRow.asFlow(),
        prefs.keyboard.fnKeyEnabled.asFlow(),
        prefs.keyboard.fnKeyArrangement.asFlow(),
        prefs.keyboard.longPressKeyHintEnabled.asFlow(),
        prefs.keyboard.longPressKeyHintPlacement.asFlow(),
        prefs.keyboard.multiTapHighlightEnabled.asFlow(),
        prefs.keyboard.flickKeyHintEnabled.asFlow(),
        prefs.keyboard.flickKeyHintPlacement.asFlow(),
    ) { values ->
        TouchModelOptions(
            showNumberRow = values[0] as Boolean,
            fnKeyEnabled = values[1] as Boolean,
            fnKeyArrangement = values[2] as FnKeyArrangement,
            longPressKeyHintEnabled = values[3] as Boolean,
            longPressKeyHintPlacement = values[4] as LongPressKeyHintPlacement,
            multiTapHighlightEnabled = values[5] as Boolean,
            flickKeyHintEnabled = values[6] as Boolean,
            flickKeyHintPlacement = values[7] as FlickKeyHintPlacement,
        )
    }.stateIn(scope, SharingStarted.Eagerly, TouchModelOptions.Default)

    // TODO check if we can implement this differently
    val activeSmartbarVisibleDynamicActionsCount = MutableStateFlow(0)

    init {
        combine(
            prefs.devtools.enabled.asFlow(),
            prefs.devtools.showDragAndDropHelpers.asFlow(),
        ) { devtoolsEnabled, showDragAndDropHelpers ->
            devtoolsEnabled && showDragAndDropHelpers
        }.collectIn(scope) { showDragAndDropHelpers ->
            updateState {
                state = state.copy(
                    flags = state.flags
                        .withDebugShowDragAndDropHelpers(showDragAndDropHelpers),
                )
            }
        }

        // TODO proper stateful compilation
        scope.launch {
            loadFoundationKeyboard(this@ImeController, storageController.activeStorage.value)
        }
    }

    fun onHardwareKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) {
            return false
        }
        return when (keyCode) {
            // TODO KeyEvent.KEYCODE_SPACE (auto-commit candidate)
            KeyEvent.KEYCODE_DEL -> true
            KeyEvent.KEYCODE_FORWARD_DEL -> true
            // TODO KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT
            else -> false
        }
    }

    fun onHardwareKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) {
            return false
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                updateStateBlocking {
                    emitBackspace()
                }
                true
            }
            KeyEvent.KEYCODE_FORWARD_DEL -> {
                updateStateBlocking {
                    emitForwardDelete()
                }
                true
            }
            // TODO KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT
            else -> false
        }
    }

    fun setEmitInterceptor(interceptor: EmitInterceptor?) {
        emitInterceptor.store(interceptor)
    }

    fun snapshotState(): ImeState = activeState.value

    // TODO evaluate if we can move to a clean coroutine-based approach in FlorisBoard
    //  for now this helper can be used to update the state from non-suspending contexts
    inline fun updateStateBlocking(crossinline function: UpdateImeStateScope.() -> Unit) {
        runBlocking {
            updateState(function)
        }
    }

    override fun updateStateScopeOf(state: ImeState): UpdateImeStateScope {
        return UpdateImeStateScope(state)
    }

    inner class UpdateImeStateScope(
        state: ImeState,
    ) : UpdateStateScope<ImeState, ImeEditor>(state) {
        fun handleStartInputView(
            ic: WeakReference<InputConnection>,
            info: FlorisEditorInfo,
            service: FlorisImeService,
        ) {
            val newTouchLayerId: K3LayerId
            val keyVariation: KeyVariation
            when (info.inputAttributes.type) {
                InputAttributes.Type.NUMBER -> {
                    keyVariation = KeyVariation.NORMAL
                    newTouchLayerId = ImeLayerIds.Numpad
                }
                InputAttributes.Type.PHONE -> {
                    keyVariation = KeyVariation.NORMAL
                    newTouchLayerId = ImeLayerIds.Telpad
                }
                InputAttributes.Type.TEXT -> {
                    keyVariation = when (info.inputAttributes.variation) {
                        InputAttributes.Variation.EMAIL_ADDRESS,
                        InputAttributes.Variation.WEB_EMAIL_ADDRESS,
                            -> {
                            KeyVariation.EMAIL_ADDRESS
                        }
                        InputAttributes.Variation.PASSWORD,
                        InputAttributes.Variation.VISIBLE_PASSWORD,
                        InputAttributes.Variation.WEB_PASSWORD,
                            -> {
                            KeyVariation.PASSWORD
                        }
                        InputAttributes.Variation.URI -> {
                            KeyVariation.URI
                        }
                        else -> {
                            KeyVariation.NORMAL
                        }
                    }
                    newTouchLayerId = ImeLayerIds.Base
                }
                else -> {
                    keyVariation = KeyVariation.NORMAL
                    newTouchLayerId = ImeLayerIds.Base
                }
            }
            val initialSelection = info.initialSelection
            val initialSurrounding = K3SurroundingText(
                textBefore = info.getInitialTextBeforeCursor(20)?.toString() ?: "",
                textSelected = info.getInitialSelectedText()?.toString() ?: "",
                textAfter = info.getInitialTextAfterCursor(20)?.toString() ?: "",
            )

            val rememberCapsLockState = prefs.typing.rememberCapsLockState.get() &&
                state.flags.inputShiftState == InputShiftState.CAPS_LOCK

            state = state.copy(
                editor = ImeEditor(ic, info, service),
                touchLayerId = when {
                    rememberCapsLockState && newTouchLayerId.isTextLayer() -> ImeLayerIds.Caps
                    else -> newTouchLayerId
                },
                flags = state.flags
                    .withKeyVariation(keyVariation)
                    .withImeUiMode(
                        if (state.flags.imeUiMode != ImeUiMode.CLIPBOARD || prefs.clipboard.historyHideOnNextTextField.get()) {
                            ImeUiMode.TEXT
                        } else {
                            state.flags.imeUiMode
                        }
                    )
                    .withActionsOverflowVisible(false)
                    .withActionsEditorVisible(false)
                    .withInputShiftState(
                        when {
                            rememberCapsLockState -> InputShiftState.CAPS_LOCK
                            else -> InputShiftState.UNSHIFTED
                        }
                    )
                    .withComposingEnabled(
                        when (newTouchLayerId) {
                            ImeLayerIds.Numpad, ImeLayerIds.Telpad -> false
                            else -> keyVariation != KeyVariation.PASSWORD &&
                                // TODO review if this is the correct approach for composing region support detection
                                //  important: for codemirror6 in browsers, it is important that composing is disabled,
                                //  else all sorts of weird behavior starts to occur
                                info.isRichInputEditor &&
                                info.inputAttributes.flagTextAutoCorrect
                        }
                    )
                    .withIncognitoMode(
                        when (prefs.suggestion.incognitoMode.get()) {
                            IncognitoMode.FORCE_OFF -> false
                            IncognitoMode.FORCE_ON -> true
                            IncognitoMode.DYNAMIC_ON_OFF -> {
                                info.imeOptions.flagNoPersonalizedLearning ||
                                    prefs.suggestion.forceIncognitoModeFromDynamic.get()
                            }
                        }
                    )
            )
            // TODO: find a better way of determining language, probably when the rest of the infra comes with the final impl of k3lp
            if (state.model.info.indicator?.contains("mozcJa") ?: false) {
                MozcEngine.instance.keyboardSpec = getCurrentConfigSpec()
            }
            resetContent(initialSelection, initialSurrounding)
            expectedContentQueue.clear()
        }

        private fun getCurrentConfigSpec(): MozcKeyboardSpec {
            val keyboardSpecification = when {
                state.touchLayerId == ImeLayerIds.Base && state.model.info.layout == "QWERTY" ->
                    MozcKeyboardSpec.QWERTY_KANA

                state.touchLayerId == ImeLayerIds.Alpha && state.model.info.layout == "QWERTY" ->
                    MozcKeyboardSpec.QWERTY_ALPHABET

                else -> MozcKeyboardSpec.TWELVE_KEY_FLICK_KANA
            }

            return keyboardSpecification
        }

        fun handleUpdateSelection(newSelection: K3TextRange) {
            val content = expectedContentQueue.popUntilOrNull { it.selection == newSelection }
            if (content != null) {
                flogDebug { "DEDUPLICATED!!1" }
                return
            }
            resetContent(newSelection, state.editor.getSurroundingText(50, 10))
            expectedContentQueue.push(state.content)
        }

        override fun emit(value: K3StringOrDescriptor) {
            val interceptor = emitInterceptor.load()
            if (interceptor != null) {
                interceptor.onEmit(value)
                return
            }
            super.emit(value)
        }

        override fun emitText(value: K3String) {
            // TODO: find a better way of determining language, probably when the rest of the infra comes with the final impl of k3lp
            if (state.model.info.indicator?.contains("mozcJa") ?: false && (state.touchLayerId == ImeLayerIds.Alpha || state.touchLayerId == ImeLayerIds.Base) && state.flags.keyVariation != KeyVariation.PASSWORD) { // assume false if null
                for (key in value.toText()) {
                    if (key == ' ') {
                        // for some reason, k3lp hardcodes space to output a space, so we replace it with the space keycode and send it to mozc
                        MozcEngine.instance.sendKey(ProtoCommands.KeyEvent.SpecialKey.SPACE)
                    } else {
                        MozcEngine.instance.sendKey(key)
                    }
                }
                val preedit = MozcEngine.instance.preedit.value
                if (state.content.selection.isNotCollapsed()) {
                    val range = state.content.selection.asRange()
                    val text = preedit.asK3String().normalize(NormalizationForm.NFC).toText()
                    val newSurroundingText = state.content.surroundingText.copy(
                        textBefore = state.content.surroundingText.textBefore + text,
                        textSelected = "",
                    )
                    val newSelection = K3TextRange(state.content.selection.min + text.length)
                    val newComposition = evaluateCompositionOf(state.model, newSelection, newSurroundingText)
                    state = state.copy(
                        content = state.content.copy(
                            selection = newSelection,
                            composition = newComposition,
                            surroundingText = newSurroundingText,
                            inputContext = state.content.inputContext + value,
                        ),
                    )
                    state.editor.composeMozcInput(range, text, newSelection, newComposition)
                } else {
                    sendPreedit(preedit)
                }
            } else {
                super.emitText(value)
            }
            expectedContentQueue.push(state.content)
            reevaluateInputShiftState()
        }

        fun sendPreedit(preedit: String) {
            val oldComposition = state.content.composition
            val (oldStart, oldEnd) =
                if (oldComposition != null) {
                    oldComposition.start to oldComposition.end
                } else {
                    val cursor = state.content.selection.min
                    cursor to cursor
                }
            val localStart = oldStart - state.content.offset
            val localEnd = oldEnd - state.content.offset
            val selection = K3TextRange(localStart, localEnd)
            val range = selection.asRange()
            val text = preedit.asK3String().normalize(NormalizationForm.NFC).toText()
            val textBefore = state.content.surroundingText.textBefore
            val newSurroundingText = state.content.surroundingText.copy(
                textBefore = textBefore.substring(0, localStart) + preedit + textBefore.substring(localEnd),
                textSelected = "",
            )
            val newSelection = K3TextRange(state.content.offset + newSurroundingText.textBefore.length)
            val newComposition = K3TextRange(
                newSelection.start - preedit.length,
                newSelection.start
            )
            state = state.copy(
                content = state.content.copy(
                    selection = newSelection,
                    composition = newComposition,
                    surroundingText = newSurroundingText,
                    inputContext = (state.content.inputContext.toText().substring(0, localStart) + preedit + state.content.inputContext.toText().substring(localEnd)).asK3String(),
                ),
            )
            state.editor.composeMozcInput(range, text, newSelection, newComposition)
        }

        // override this to intercept the swap mode and 'base' buttons
        override fun switchTouchLayer(newTouchLayerId: K3LayerId) {
            super.switchTouchLayer(newTouchLayerId)
            // TODO: find a better way of determining language, probably when the rest of the infra comes with the final impl of k3lp
            if (state.model.info.indicator?.contains("mozcJa") ?: false) { // assume false if null
                MozcEngine.instance.keyboardSpec = getCurrentConfigSpec()
                if (MozcEngine.instance.preedit.value.isNotEmpty()) {
                    MozcEngine.instance.sendKey(ProtoCommands.KeyEvent.SpecialKey.ENTER)
                    state = state.copy(
                        content = state.content.copy(
                            composition = null,
                        ),
                    )
                    state.editor.finishComposingMozc()
                }
            }
        }

        override fun emitDescriptor(descriptor: K3Descriptor) {
            val windowController = FlorisImeService.windowControllerOrNull()
            when (descriptor) {
                // TODO once k3lp supports this remove
                ImeActions.Delete -> emitForwardDelete()
                // TODO evaluate use of modern cursor anchor API instead of sending raw key events
                ImeActions.ArrowDown -> state.editor.sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN)
                ImeActions.ArrowLeft -> state.editor.sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT)
                ImeActions.ArrowRight -> state.editor.sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
                ImeActions.ArrowUp -> state.editor.sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_UP)
                // TODO clipboard manager needs a rewrite to match the new state paradigm
                ImeActions.ClipboardCopy -> {
                    if (state.editor.info.isRichInputEditor) {
                        val text = state.content.surroundingText.textSelected
                        if (text.isNotEmpty()) {
                            clipboardManager.addNewPlaintext(text)
                        }
                    } else {
                        state.editor.performClipboardCopy()
                    }
                }
                ImeActions.ClipboardCut -> {
                    if (state.editor.info.isRichInputEditor) {
                        val text = state.content.surroundingText.textSelected
                        if (text.isNotEmpty()) {
                            clipboardManager.addNewPlaintext(text)
                            emitBackspace()
                        }
                    } else {
                        state.editor.performClipboardCut()
                    }
                }
                ImeActions.ClipboardPaste -> {
                    if (state.editor.info.isRichInputEditor) {
                        emitClipboardItem(clipboardManager.primaryClip)
                    } else {
                        state.editor.performClipboardPaste()
                    }
                }
                ImeActions.ClipboardClearHistory -> clipboardManager.clearHistory()
                ImeActions.ClipboardClearFullHistory -> clipboardManager.clearFullHistory()
                ImeActions.ClipboardClearPrimaryClip -> {
                    if (prefs.clipboard.clearPrimaryClipAffectsHistoryIfUnpinned.get()) {
                        clipboardManager.primaryClip?.let { clipboardManager.deleteClip(it, onlyIfUnpinned = true) }
                    }
                    clipboardManager.updatePrimaryClip(null)
                    // TODO we have no ref here:
                    //  appContext.showShortToastSync(R.string.clipboard__cleared_primary_clip)
                }
                ImeActions.SelectAll -> state.editor.performSelectAll()
                ImeActions.ShowImeWindow -> FlorisImeService.showUi()
                ImeActions.HideImeWindow -> FlorisImeService.hideUi()
                ImeActions.ShowTextPanel -> {
                    // just in case the keycode is triggered while in text mode
                    commitMozcOnPanelChange()
                    state = state.copy(
                        flags = state.flags.withImeUiMode(ImeUiMode.TEXT),
                    )
                }
                ImeActions.ShowMediaPanel -> {
                    commitMozcOnPanelChange()
                    state = state.copy(
                        flags = state.flags.withImeUiMode(ImeUiMode.MEDIA),
                    )
                }
                ImeActions.ShowClipboardPanel -> {
                    commitMozcOnPanelChange()
                    state = state.copy(
                        flags = state.flags.withImeUiMode(ImeUiMode.CLIPBOARD),
                    )
                }
                ImeActions.Undo -> state.editor.performUndo()
                ImeActions.Redo -> state.editor.performRedo()
                ImeActions.Settings -> FlorisImeService.launchSettings()
                ImeActions.ShowInputMethodPicker -> FlorisImeService.showImePicker()
                ImeActions.SwitchToPrevInputMethod -> FlorisImeService.switchToPrevInputMethod()
                ImeActions.SwitchToNextInputMethod -> FlorisImeService.switchToNextInputMethod()
                ImeActions.ShowSubtypePicker -> {} // TODO
                ImeActions.SwitchToPrevSubtype -> {} // TODO
                ImeActions.SwitchToNextSubtype -> {} // TODO
                ImeActions.ToggleActionsEditor -> {
                    state = state.copy(
                        flags = state.flags
                            .withActionsEditorVisible(!state.flags.isActionsEditorVisible),
                    )
                }
                ImeActions.ToggleActionsOverflow -> {
                    state = state.copy(
                        flags = state.flags
                            .withActionsOverflowVisible(!state.flags.isActionsOverflowVisible),
                    )
                }
                ImeActions.ToggleCompactLayout -> windowController?.actions?.toggleCompactLayout()
                ImeActions.ToggleFloatingWindow -> windowController?.actions?.toggleFloatingWindow()
                ImeActions.ToggleResizeMode -> windowController?.editor?.toggleEnabled()
                ImeActions.CompactLayoutToLeft -> windowController?.actions?.compactLayoutToLeft()
                ImeActions.CompactLayoutToRight -> windowController?.actions?.compactLayoutToRight()
                ImeActions.ExternalVoiceInput -> FlorisImeService.switchToVoiceInputMethod()
                else -> super.emitDescriptor(descriptor)
            }
            reevaluateInputShiftState()
        }

        fun commitMozcOnPanelChange() {
            // TODO: find a better way of determining language, probably when the rest of the infra comes with the final impl of k3lp
            if (state.model.info.indicator?.contains("mozcJa") ?: false) {
                if (MozcEngine.instance.preedit.value.isNotEmpty()) {
                    MozcEngine.instance.sendKey(ProtoCommands.KeyEvent.SpecialKey.ENTER)
                    state = state.copy(
                        content = state.content.copy(
                            composition = null,
                        ),
                    )
                    state.editor.finishComposingMozc()
                }
            }
        }

        override fun emitBackspace() {
            // TODO: find a better way of determining language, probably when the rest of the infra comes with the final impl of k3lp
            if (state.model.info.indicator?.contains("mozcJa") ?: false && (state.touchLayerId == ImeLayerIds.Alpha || state.touchLayerId == ImeLayerIds.Base) && state.flags.keyVariation != KeyVariation.PASSWORD) {
                if (MozcEngine.instance.preedit.value.isNotEmpty()) {
                    MozcEngine.instance.sendKey(ProtoCommands.KeyEvent.SpecialKey.BACKSPACE)
                    sendPreedit(MozcEngine.instance.preedit.value)
                } else {
                    super.emitBackspace()
                }
            } else {
                super.emitBackspace()
            }
            expectedContentQueue.push(state.content)
        }

        override fun emitEnter() {
            // TODO: find a better way of determining language, probably when the rest of the infra comes with the final impl of k3lp
            if (state.model.info.indicator?.contains("mozcJa") ?: false && (state.touchLayerId == ImeLayerIds.Alpha || state.touchLayerId == ImeLayerIds.Base) && state.flags.keyVariation != KeyVariation.PASSWORD) {
                if (MozcEngine.instance.preedit.value.isNotEmpty()) {
                    MozcEngine.instance.sendKey(ProtoCommands.KeyEvent.SpecialKey.ENTER)
                    state = state.copy(
                        content = state.content.copy(
                            composition = null,
                        ),
                    )
                    state.editor.finishComposingMozc()
                    return
                }
            }
            val info = state.editor.info
            val isShiftPressed = false // TODO inputEventDispatcher.isPressed(KeyCode.SHIFT)
            if (info.imeOptions.flagNoEnterAction || info.inputAttributes.flagTextMultiLine && isShiftPressed) {
                emitEnterKey()
            } else {
                when (val action = info.imeOptions.action) {
                    ImeOptions.Action.UNSPECIFIED,
                    ImeOptions.Action.DONE,
                    ImeOptions.Action.GO,
                    ImeOptions.Action.NEXT,
                    ImeOptions.Action.PREVIOUS,
                    ImeOptions.Action.SEARCH,
                    ImeOptions.Action.SEND -> emitEnterAction(action)
                    else -> emitEnterKey()
                }
            }
        }

        fun emitEnterKey() {
            val info = state.editor.info
            if (info.isRawInputEditor) {
                state.editor.sendDownUpKeyEvent(KeyEvent.KEYCODE_ENTER)
            } else {
                emitText(NEWLINE_SEQ)
            }
        }

        fun emitEnterAction(action: ImeOptions.Action) {
            state.editor.performEditorAction(action)
        }

        fun emitForwardDelete() {
            // TODO request additional text if too low on context length
            if (state.content.selection.isNotCollapsed()) {
                emitBackspace()
            } else {
                val newSurroundingText = state.content.surroundingText.copy(
                    textAfter = state.content.surroundingText.textAfter.let { text ->
                        // TODO unicode
                        if (text.isEmpty()) text else text.substring(1)
                    },
                )
                state = state.copy(
                    content = state.content.copy(
                        surroundingText = newSurroundingText,
                    ),
                )
                state.editor.deleteSurroundingText(
                    charsBefore = 0,
                    charsAfter = 1, // TODO
                )
            }
        }

        fun emitCandidate(candidate: SuggestionCandidate) {
//            scope.launch {
//                candidate.sourceProvider?.notifySuggestionAccepted(subtypeManager.activeSubtype, candidate)
//            }
            when (candidate) {
                is ClipboardSuggestionCandidate -> emitClipboardItem(candidate.clipboardItem)
                else -> emitCompletion(candidate)
            }
        }

        fun emitClipboardItem(item: ClipboardItem?): Boolean {
            return item != null && when (item.type) {
                ItemType.TEXT -> {
                    val text = item.text?.asK3String()
                    if (text != null) {
                        emitText(text)
                        true
                    } else {
                        false
                    }
                }
                ItemType.IMAGE, ItemType.VIDEO -> {
                    state.editor.performClipboardPasteMedia(item)
                }
            }.also {
                if (prefs.clipboard.historyHideOnPaste.get()) {
                    state = state.copy(
                        flags = state.flags.withImeUiMode(ImeUiMode.TEXT)
                    )
                }
            }
        }

        fun emitCompletion(candidate: SuggestionCandidate): Boolean {
            val text = candidate.text.toString()
            if (text.isEmpty() || state.editor.info.isRawInputEditor) return false
//            val content = activeContent
//            return if (content.composing.isValid) {
//                phantomSpace.setActive(showComposingRegion = false, candidate = candidate)
//                super.finalizeComposingText(text)
//            } else {
//                val isPhantomSpaceActive = phantomSpace.determine(text)
//                phantomSpace.setActive(showComposingRegion = false, candidate = candidate)
//                return if (isPhantomSpaceActive) {
//                    super.commitText("$SPACE$text")
//                } else {
//                    super.commitText(text)
//                }.also {
//                    // handled in finalizeComposingText if content.composing.isValid
//                    updateLastCommitPosition()
//                }
//            }
            return false
        }

        override fun switchTouchLayer(newTouchLayerId: K3LayerId) {
            if (newTouchLayerId.isTextLayer()) {
                super.switchTouchLayer(state.flags.inputShiftState.correspondingLayerId)
            } else {
                super.switchTouchLayer(newTouchLayerId)
            }
        }

        fun cycleInputShiftState(isDoubleTap: Boolean) {
            val shiftKeyBehavior = prefs.typing.shiftKeyBehavior.get()
            val oldInputShiftState = state.flags.inputShiftState
            val newInputShiftState = when (shiftKeyBehavior) {
                ShiftKeyBehavior.CAPSLOCK_BY_DOUBLE_TAP -> when {
                    isDoubleTap -> InputShiftState.CAPS_LOCK
                    oldInputShiftState == InputShiftState.UNSHIFTED -> InputShiftState.SHIFTED_MANUAL
                    else -> InputShiftState.UNSHIFTED
                }
                ShiftKeyBehavior.CAPSLOCK_BY_CYCLE -> when (oldInputShiftState) {
                    InputShiftState.UNSHIFTED -> InputShiftState.SHIFTED_MANUAL
                    InputShiftState.SHIFTED_MANUAL -> InputShiftState.CAPS_LOCK
                    InputShiftState.SHIFTED_AUTOMATIC -> InputShiftState.UNSHIFTED
                    InputShiftState.CAPS_LOCK -> InputShiftState.UNSHIFTED
                }
            }
            state = state.copy(
                touchLayerId = newInputShiftState.correspondingLayerId,
                flags = state.flags.withInputShiftState(newInputShiftState),
            )
        }

        fun revertCycleInputShiftState(
            newTouchLayerId: K3LayerId,
            newInputShiftState: InputShiftState,
        ) {
            state = state.copy(
                touchLayerId = newTouchLayerId,
                flags = state.flags.withInputShiftState(newInputShiftState),
            )
        }

        fun reevaluateInputShiftState() {
            val isPeekOngoing = false // TODO
            if (state.flags.inputShiftState == InputShiftState.CAPS_LOCK || isPeekOngoing) return
            val capsMode = state.content.cursorCapsMode(state.editor.info.inputAttributes)
            val shift = prefs.typing.autoCapitalization.get()
                && capsMode != InputAttributes.CapsMode.NONE
                // && subtypeManager.activeSubtype.primaryLocale.supportsCapitalization
            val inputShiftState = when {
                shift -> InputShiftState.SHIFTED_AUTOMATIC
                else -> InputShiftState.UNSHIFTED
            }
            val touchLayerId = when {
                state.touchLayerId.isTextLayer() -> inputShiftState.correspondingLayerId
                else -> state.touchLayerId
            }
            state = state.copy(
                touchLayerId = touchLayerId,
                flags = state.flags.withInputShiftState(inputShiftState),
            )
        }

        override fun resetContent(newSelection: K3TextRange, newSurrounding: K3SurroundingText) {
            super.resetContent(newSelection, newSurrounding)
            reevaluateInputShiftState()
        }

        override fun resetContent() {
            super.resetContent()
            reevaluateInputShiftState()
        }

        fun handleFinishInputView() {
            resetContent()
            state = state.copy(editor = ImeEditor.Disconnected)
            expectedContentQueue.clear()
        }

        override fun evaluateCompositionOf(
            model: K3Model,
            selection: K3TextRange,
            surroundingText: K3SurroundingText
        ): K3TextRange? {
            if (selection.isNotCollapsed() || !state.flags.isComposingEnabled) {
                return null
            }
            // TODO rework how we get the primary locale
            val locale = FlorisLocale.fromTag(model.locales.getOrElse(0) { "" })
            return breakIterators.word(locale) {
                it.setText(surroundingText.textBefore)
                val end = it.last()
                val isWord = it.ruleStatus != BreakIterator.WORD_NONE
                if (isWord) {
                    val start = it.previous().let { pos ->
                        // Include Emoji indicator in local composing. This is required so that emoji suggestion indicator
                        // can be detected in the composing text.
                        (pos - 1).takeIf { updatedPos ->
                            surroundingText.textBefore.getOrNull(updatedPos) == EmojiSuggestionType.LEADING_COLON.prefix.first()
                        } ?: pos
                    }
                    val offset = (selection.min - surroundingText.textBefore.length).coerceAtLeast(0)
                    K3TextRange(start + offset, end + offset)
                } else {
                    null
                }
            }
        }
    }

    companion object {
        private val NEWLINE_SEQ = "\n".asK3String()
    }
}

enum class OperationUnit {
    CHARACTERS,
    WORDS;
}

enum class OperationScope {
    BEFORE_CURSOR,
    AFTER_CURSOR;
}

fun interface EmitInterceptor {
    fun onEmit(value: K3StringOrDescriptor)
}

private class ExpectedContentQueue {
    private val list = mutableListOf<K3Content>()

    fun popUntilOrNull(predicate: (K3Content) -> Boolean): K3Content? {
        while (list.isNotEmpty()) {
            val item = list[0]
            if (predicate(item)) return item
            list.removeAt(0)
        }
        return null
    }

    fun push(item: K3Content) {
        list.add(item)
    }

    fun clear() {
        list.clear()
    }
}

fun K3Content.cursorCapsMode(inputAttributes: InputAttributes): InputAttributes.CapsMode {
    return InputAttributes.CapsMode.fromFlags(
        TextUtils.getCapsMode(surroundingText.textBefore, surroundingText.textBefore.length, inputAttributes.raw)
    )
}
