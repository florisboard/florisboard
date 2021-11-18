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

import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.ProvideLocalizedResources
import dev.patrickgold.florisboard.app.ui.components.SystemUiIme
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.common.isOrientationLandscape
import dev.patrickgold.florisboard.common.isOrientationPortrait
import dev.patrickgold.florisboard.common.observeAsTransformingState
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
import dev.patrickgold.jetpref.datastore.model.observeAsState
import java.lang.ref.WeakReference

/**
 * Global weak reference for the [FlorisImeService] class. This is needed as certain actions (request hide, switch to
 * another input method, getting the editor instance / input connection, etc.) can only be performed by an IME
 * service class and no context-bound managers.
 *
 * Consider using this reference only if absolutely needed. This reference must **never** be used to access state, only
 * to trigger an action. This reference is weak and will not prevent GC of the service class, thus expect that this
 * reference's value can be null at any time given.
 */
var FlorisImeServiceReference = WeakReference<FlorisImeService?>(null)
    private set

/**
 * Core class responsible for linking together all managers and UI compose-ables to provide an IME service. Sets
 * up the window and context to be lifecycle-aware, so LiveData and Jetpack Compose can be used without issues.
 *
 * This is a new implementation for the keyboard service class and is replacing the old core class bit by bit.
 * The main objective for the new class is to hold as few state as possible and delegate tasks to context-bound
 * manager classes.
 */
class FlorisImeService : LifecycleInputMethodService() {
    companion object {
        fun activeEditorInstance(): EditorInstance? {
            return FlorisImeServiceReference.get()?.activeEditorInstance
        }

        fun inputFeedbackController(): InputFeedbackController? {
            return FlorisImeServiceReference.get()?.inputFeedbackController
        }
    }

    private val prefs by florisPreferenceModel()
    private val keyboardManager by keyboardManager()

    private val activeEditorInstance by lazy { EditorInstance(this) }
    private val activeState get() = keyboardManager.activeState
    private var composeInputView: View? = null
    private var composeInputViewInnerHeight: Int = 0
    private val inputFeedbackController by lazy { InputFeedbackController.new(this) }

    override fun onCreate() {
        super.onCreate()
        FlorisImeServiceReference = WeakReference(this)
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
        composeInputView = null
    }

    override fun onBindInput() {
        super.onBindInput()
        activeEditorInstance.bindInput()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        activeEditorInstance.startInput(attribute)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (info != null) {
            activeState.batchEdit {
                it.update(info)
                it.isSelectionMode = (info.initialSelEnd - info.initialSelStart) != 0
            }
        }
        activeEditorInstance.startInputView(info)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        activeState.reset()
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
                        }
                        SystemUiIme()
                    }
                }
            }
        }
    }

    @Composable
    private fun BoxScope.ImeUi() {
        val keyboardStyle = FlorisImeTheme.style.get(FlorisImeUi.Keyboard)
        SnyggSurface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.BottomStart)
                .onGloballyPositioned { coords -> composeInputViewInnerHeight = coords.size.height },
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
                    .height(IntrinsicSize.Min)
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
                Box(modifier = Modifier.weight(keyboardWeight).wrapContentHeight()) {
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

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        updateSoftInputWindowLayoutParameters()
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets == null) return

        val composeView = composeInputView ?: return
        // TODO: Check also if the keyboard is currently suppressed by a hardware keyboard
        if (!isInputViewShown) {
            outInsets.contentTopInsets = composeView.height
            outInsets.visibleTopInsets = composeView.height
            return
        }
        val visibleTopY = composeView.height - composeInputViewInnerHeight
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
