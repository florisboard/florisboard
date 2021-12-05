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

package dev.patrickgold.florisboard

import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.ProvideLocalizedResources
import dev.patrickgold.florisboard.app.ui.components.SystemUiIme
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.common.android.isOrientationLandscape
import dev.patrickgold.florisboard.common.android.isOrientationPortrait
import dev.patrickgold.florisboard.common.android.launchActivity
import dev.patrickgold.florisboard.common.android.systemServiceOrNull
import dev.patrickgold.florisboard.common.observeAsTransformingState
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.ime.core.EditorInstance
import dev.patrickgold.florisboard.ime.keyboard.InputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.ProvideKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.lifecycle.LifecycleInputMethodService
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.onehanded.OneHandedPanel
import dev.patrickgold.florisboard.ime.text.TextInputLayout
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.snygg.ui.SnyggSurface
import dev.patrickgold.florisboard.common.android.AndroidVersion
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogDebug
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.debug.flogWarning
import dev.patrickgold.jetpref.datastore.model.observeAsState
import java.lang.ref.WeakReference

/**
 * Global weak reference for the [FlorisImeService] class. This is needed as certain actions (request hide, switch to
 * another input method, getting the editor instance / input connection, etc.) can only be performed by an IME
 * service class and no context-bound managers. This reference is exclusively used by the companion helper methods
 * of [FlorisImeService], which provide a safe and memory-leak-free way of performing certain actions on the Floris
 * input method service instance.
 */
private var FlorisImeServiceReference = WeakReference<FlorisImeService?>(null)

/**
 * Core class responsible for linking together all managers and UI compose-ables to provide an IME service. Sets
 * up the window and context to be lifecycle-aware, so LiveData and Jetpack Compose can be used without issues.
 *
 * This is a new implementation for the keyboard service class and is replacing the old core class bit by bit.
 * The main objective for the new class is to hold as few state as possible and delegate tasks to context-bound
 * manager classes.
 */
class FlorisImeService : LifecycleInputMethodService(), EditorInstance.WordHistoryChangedListener {
    companion object {
        fun activeEditorInstance(): EditorInstance? {
            return FlorisImeServiceReference.get()?.activeEditorInstance
        }

        fun inputFeedbackController(): InputFeedbackController? {
            return FlorisImeServiceReference.get()?.inputFeedbackController
        }

        /**
         * Hides the IME and launches [FlorisAppActivity].
         */
        fun launchSettings() {
            val ims = FlorisImeServiceReference.get() ?: return
            ims.requestHideSelf(0)
            ims.launchActivity(FlorisAppActivity::class) {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        fun showUi() {
            if (AndroidVersion.ATLEAST_API28_P) {
                FlorisImeServiceReference.get()?.requestShowSelf(0)
            }
        }

        fun hideUi() {
            FlorisImeServiceReference.get()?.requestHideSelf(0)
        }

        fun switchToPrevInputMethod(): Boolean {
            val ims = FlorisImeServiceReference.get() ?: return false
            val imm = ims.systemServiceOrNull(InputMethodManager::class)
            try {
                if (AndroidVersion.ATLEAST_API28_P) {
                    return ims.switchToPreviousInputMethod()
                } else {
                    ims.window.window?.let { window ->
                        @Suppress("DEPRECATION")
                        return imm?.switchToLastInputMethod(window.attributes.token) == true
                    }
                }
            } catch (e: Exception) {
                flogError { "Unable to switch to the previous IME" }
                imm?.showInputMethodPicker()
            }
            return false
        }

        fun switchToNextInputMethod(): Boolean {
            val ims = FlorisImeServiceReference.get() ?: return false
            val imm = ims.systemServiceOrNull(InputMethodManager::class)
            try {
                if (AndroidVersion.ATLEAST_API28_P) {
                    return ims.switchToNextInputMethod(false)
                } else {
                    ims.window.window?.let { window ->
                        @Suppress("DEPRECATION")
                        return imm?.switchToNextInputMethod(window.attributes.token, false) == true
                    }
                }
            } catch (e: Exception) {
                flogError { "Unable to switch to the next IME" }
                imm?.showInputMethodPicker()
            }
            return false
        }
    }

    private val prefs by florisPreferenceModel()
    private val clipboardManager by clipboardManager()
    private val keyboardManager by keyboardManager()
    private val nlpManager by nlpManager()

    private val activeEditorInstance by lazy { EditorInstance(this) }
    private val activeState get() = keyboardManager.activeState
    private var composeInputView: View? = null
    private var composeInputViewInnerHeight: Int = 0
    private val inputFeedbackController by lazy { InputFeedbackController.new(this) }
    private var isWindowShown: Boolean = false

    override fun onCreate() {
        super.onCreate()
        FlorisImeServiceReference = WeakReference(this)
        activeEditorInstance.wordHistoryChangedListener = this
    }

    override fun onCreateInputView(): View {
        super.onCreateInputView()
        val composeView = ComposeInputView()
        composeInputView = composeView
        return composeView
    }

    override fun onCreateCandidatesView(): View? {
        // Disable the default candidates view
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        FlorisImeServiceReference = WeakReference(null)
        activeEditorInstance.wordHistoryChangedListener = null
        composeInputView = null
    }

    override fun onBindInput() {
        super.onBindInput()
        activeEditorInstance.bindInput()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (attribute == null) return
        activeEditorInstance.startInput(attribute)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (info == null) return
        activeState.batchEdit {
            activeState.update(info)
            activeState.isSelectionMode = (info.initialSelEnd - info.initialSelStart) != 0
            activeEditorInstance.startInputView(info)
            keyboardManager.updateCapsState()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        activeState.batchEdit {
            activeState.isSelectionMode = (newSelEnd - newSelStart) != 0
            activeEditorInstance.updateSelection(
                oldSelStart, oldSelEnd,
                newSelStart, newSelEnd,
                candidatesStart, candidatesEnd,
            )
            keyboardManager.updateCapsState()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // TODO: evaluate which parts to reset. Resetting everything is too much,
        //  resetting nothing could be problematic too.
        //activeState.reset()
        activeEditorInstance.finishInputView()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        activeEditorInstance.finishInput()
    }

    override fun onUnbindInput() {
        super.onUnbindInput()
        activeEditorInstance.unbindInput()
    }

    override fun onWordHistoryChanged(
        currentWord: EditorInstance.Region?,
        wordsBeforeCurrent: List<EditorInstance.Region>,
        wordsAfterCurrent: List<EditorInstance.Region>,
    ) {
        if (currentWord == null || !currentWord.isValid || !activeState.isComposingEnabled) {
            nlpManager.clearSuggestions()
            return
        }
        nlpManager.suggest(currentWord.text, listOf())
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (isWindowShown) {
            flogWarning(LogTopic.IMS_EVENTS) { "Ignoring (is already shown)" }
            return
        } else {
            flogInfo(LogTopic.IMS_EVENTS)
        }
        isWindowShown = true

        // Must perform this because of strict privacy rules on modern Android versions, where
        // else FlorisBoard would fail to recognize a current primary clip which was copied
        // while the IME window was hidden.
        //
        // onPrimaryClipChanged() automatically checks if the clips are equal, thus this poses
        // no problem in case the clip did not change while the window was hidden.
        clipboardManager.onPrimaryClipChanged()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (!isWindowShown) {
            flogWarning(LogTopic.IMS_EVENTS) { "Ignoring (is already hidden)" }
            return
        } else {
            flogInfo(LogTopic.IMS_EVENTS)
        }
        isWindowShown = false
    }

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        updateSoftInputWindowLayoutParameters()
    }

    override fun onUpdateExtractedText(token: Int, text: ExtractedText?) {
        super.onUpdateExtractedText(token, text)
        activeEditorInstance.updateText(token, text)
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets == null) return

        flogDebug { "comp insets" }
        val composeView = composeInputView ?: return
        // TODO: Check also if the keyboard is currently suppressed by a hardware keyboard
        if (!isInputViewShown) {
            outInsets.contentTopInsets = composeView.height
            outInsets.visibleTopInsets = composeView.height
            return
        }

        val visibleTopY = composeView.height - composeInputViewInnerHeight
        flogDebug { "comp insets: $visibleTopY" }
        outInsets.contentTopInsets = visibleTopY
        outInsets.visibleTopInsets = visibleTopY
        //if (isClipboardContextMenuShown) {
        //    outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_FRAME
        //    outInsets.touchableRegion?.setEmpty()
        //}
    }

    /**
     * Updates the layout params of the window and compose input view.
     */
    private fun updateSoftInputWindowLayoutParameters() {
        val w = window?.window ?: return
        WindowCompat.setDecorFitsSystemWindows(w, true)
        ViewUtils.updateLayoutHeightOf(w, WindowManager.LayoutParams.MATCH_PARENT)
        val layoutHeight = if (isFullscreenMode) {
            WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }
        val inputArea = w.findViewById<View>(android.R.id.inputArea) ?: return
        ViewUtils.updateLayoutHeightOf(inputArea, layoutHeight)
        ViewUtils.updateLayoutGravityOf(inputArea, Gravity.BOTTOM)
        val composeView = composeInputView ?: return
        ViewUtils.updateLayoutHeightOf(composeView, layoutHeight)
    }

    @Composable
    private fun ImeUiWrapper() {
        ProvideLocalizedResources(this) {
            ProvideKeyboardRowBaseHeight {
                CompositionLocalProvider(
                    LocalInputFeedbackController provides inputFeedbackController,
                    LocalLayoutDirection provides LayoutDirection.Ltr,
                ) {
                    FlorisImeTheme {
                        // Outer box is necessary as an "outer window"
                        Box(modifier = Modifier.fillMaxSize()) {
                            ImeUi()
                            DevtoolsOverlays()
                        }
                        SystemUiIme()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun BoxScope.ImeUi() {
        val keyboardStyle = FlorisImeTheme.style.get(FlorisImeUi.Keyboard)
        SnyggSurface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.BottomStart)
                .onGloballyPositioned { coords -> composeInputViewInnerHeight = coords.size.height }
                // Do not remove below line or touch input may get stuck
                .pointerInteropFilter { false },
            background = keyboardStyle.background,
        ) {
            val configuration = LocalConfiguration.current
            val bottomOffset by if (configuration.isOrientationPortrait()) {
                prefs.keyboard.bottomOffsetPortrait
            } else {
                prefs.keyboard.bottomOffsetLandscape
            }.observeAsTransformingState { it.dp }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    // FIXME: removing this fixes the Smartbar sizing but breaks one-handed-mode
                    //.height(IntrinsicSize.Min)
                    .padding(bottom = bottomOffset),
            ) {
                val oneHandedMode by prefs.keyboard.oneHandedMode.observeAsState()
                val oneHandedModeScaleFactor by prefs.keyboard.oneHandedModeScaleFactor.observeAsState()
                val keyboardWeight = when {
                    oneHandedMode == OneHandedMode.OFF || configuration.isOrientationLandscape() -> 1f
                    else -> oneHandedModeScaleFactor / 100f
                }
                if (oneHandedMode == OneHandedMode.END && configuration.isOrientationPortrait()) {
                    OneHandedPanel(
                        panelSide = OneHandedMode.START,
                        weight = 1f - keyboardWeight,
                    )
                }
                Box(modifier = Modifier
                    .weight(keyboardWeight)
                    .wrapContentHeight()) {
                    TextInputLayout()
                }
                if (oneHandedMode == OneHandedMode.START && configuration.isOrientationPortrait()) {
                    OneHandedPanel(
                        panelSide = OneHandedMode.END,
                        weight = 1f - keyboardWeight,
                    )
                }
            }
        }
    }

    @Composable
    private fun BoxScope.DevtoolsOverlays() {
        val devtoolsEnabled by prefs.devtools.enabled.observeAsState()
        if (devtoolsEnabled) {
            val devtoolsShowPrimaryClip by prefs.devtools.showPrimaryClip.observeAsState()
            if (devtoolsShowPrimaryClip) {
                val primaryClip by clipboardManager.primaryClip.observeAsState()
                Text(
                    modifier = Modifier.align(Alignment.TopStart),
                    text = primaryClip.toString(),
                    color = Color.White,
                )
            }
        }
    }

    private inner class ComposeInputView : AbstractComposeView(this) {
        init {
            isHapticFeedbackEnabled = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        @Composable
        override fun Content() {
            ImeUiWrapper()
        }

        override fun getAccessibilityClassName(): CharSequence {
            return javaClass.name
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            updateSoftInputWindowLayoutParameters()
        }
    }
}
