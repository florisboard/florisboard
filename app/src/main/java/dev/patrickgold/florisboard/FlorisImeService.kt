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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
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
import androidx.core.view.WindowCompat
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.ProvideLocalizedResources
import dev.patrickgold.florisboard.app.ui.components.SystemUiIme
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.common.isOrientationLandscape
import dev.patrickgold.florisboard.common.isOrientationPortrait
import dev.patrickgold.florisboard.debug.flogDebug
import dev.patrickgold.florisboard.ime.keyboard.InputFeedbackManager
import dev.patrickgold.florisboard.ime.keyboard.LocalKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.keyboard.ProvideKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.lifecycle.LifecycleInputMethodService
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.onehanded.OneHandedPanel
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.snygg.ui.SnyggSurface
import dev.patrickgold.jetpref.datastore.model.observeAsState

/**
 * Core class responsible for linking together all managers and UI providers to provide an IME service.
 */
class FlorisImeService : LifecycleInputMethodService() {
    private val prefs by florisPreferenceModel()
    private val keyboardManager by keyboardManager()

    private var composeInputView: View? = null
    private var composeInputViewInnerHeight: Int = 0
    private val inputFeedbackManager by lazy { InputFeedbackManager.new(this) }

    @Composable
    private fun ImeUiWrapper() {
        ProvideLocalizedResources(this) {
            ProvideKeyboardRowBaseHeight {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    FlorisImeTheme {
                        // Outer box is necessary as an "outer window"
                        Box(modifier = Modifier.fillMaxWidth()) {
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
        flogDebug { keyboardStyle.toString() }
        val height = LocalKeyboardRowBaseHeight.current * 5
        SnyggSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.BottomStart)
                .onGloballyPositioned { coords -> composeInputViewInnerHeight = coords.size.height },
            background = keyboardStyle.background,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val configuration = LocalConfiguration.current
                val oneHandedMode by prefs.keyboard.oneHandedMode.observeAsState()
                val oneHandedModeScaleFactor by prefs.keyboard.oneHandedModeScaleFactor.observeAsState()
                val keyboardWeight = when {
                    oneHandedMode == OneHandedMode.OFF || configuration.isOrientationLandscape() -> 1f
                    else -> oneHandedModeScaleFactor / 100f
                }
                if (oneHandedMode == OneHandedMode.END && configuration.isOrientationPortrait()) {
                    OneHandedPanel(
                        inputFeedbackManager,
                        panelSide = OneHandedMode.START,
                        weight = 1f - keyboardWeight,
                    )
                }
                Box(modifier = Modifier.weight(keyboardWeight)) {
                    Button(onClick = { toggleOneHandedMode(isRight = true) }) { Text(text = "Content") }
                }
                if (oneHandedMode == OneHandedMode.START && configuration.isOrientationPortrait()) {
                    OneHandedPanel(
                        inputFeedbackManager,
                        panelSide = OneHandedMode.END,
                        weight = 1f - keyboardWeight,
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        super.onCreateInputView()
        val composeView = ComposeInputView()
        composeInputView = composeView
        return composeView
    }

    override fun onCreateCandidatesView(): View? {
        // We do not use the framework's candidate view, but an integrated one within the IME Ui.
        return null
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

    fun toggleOneHandedMode(isRight: Boolean) {
        prefs.keyboard.oneHandedMode.set(when (prefs.keyboard.oneHandedMode.get()) {
            OneHandedMode.OFF -> if (isRight) { OneHandedMode.END } else { OneHandedMode.START }
            else -> OneHandedMode.OFF
        })
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
