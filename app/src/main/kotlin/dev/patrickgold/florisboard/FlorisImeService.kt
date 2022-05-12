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

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.ExtractEditText
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.app.devtools.DevtoolsOverlay
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.ClipboardInputLayout
import dev.patrickgold.florisboard.ime.editor.EditorRange
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.input.InputFeedbackController
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.ProvideKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.lifecycle.LifecycleInputMethodService
import dev.patrickgold.florisboard.ime.media.MediaInputLayout
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.onehanded.OneHandedPanel
import dev.patrickgold.florisboard.ime.text.TextInputLayout
import dev.patrickgold.florisboard.ime.text.smartbar.SecondaryRowPlacement
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.android.AndroidInternalR
import dev.patrickgold.florisboard.lib.android.AndroidVersion
import dev.patrickgold.florisboard.lib.android.isOrientationLandscape
import dev.patrickgold.florisboard.lib.android.isOrientationPortrait
import dev.patrickgold.florisboard.lib.android.launchActivity
import dev.patrickgold.florisboard.lib.android.setLocale
import dev.patrickgold.florisboard.lib.android.systemServiceOrNull
import dev.patrickgold.florisboard.lib.compose.FlorisButton
import dev.patrickgold.florisboard.lib.compose.ProvideLocalizedResources
import dev.patrickgold.florisboard.lib.compose.SystemUiIme
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.devtools.flogWarning
import dev.patrickgold.florisboard.lib.observeAsTransformingState
import dev.patrickgold.florisboard.lib.snygg.ui.SnyggSurface
import dev.patrickgold.florisboard.lib.snygg.ui.shape
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBorder
import dev.patrickgold.florisboard.lib.snygg.ui.snyggShadow
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor
import dev.patrickgold.florisboard.lib.snygg.ui.spSize
import dev.patrickgold.florisboard.lib.util.ViewUtils
import dev.patrickgold.florisboard.lib.util.debugSummarize
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
 */
class FlorisImeService : LifecycleInputMethodService() {
    companion object {
        private val InlineSuggestionUiSmallestSize = Size(0, 0)
        private val InlineSuggestionUiBiggestSize = Size(Int.MAX_VALUE, Int.MAX_VALUE)

        fun currentInputConnection(): InputConnection? {
            return FlorisImeServiceReference.get()?.currentInputConnection
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
            val ims = FlorisImeServiceReference.get() ?: return
            if (AndroidVersion.ATLEAST_API28_P) {
                ims.requestShowSelf(0)
            } else {
                @Suppress("DEPRECATION")
                ims.systemServiceOrNull(InputMethodManager::class)
                    ?.showSoftInputFromInputMethod(ims.currentInputBinding.connectionToken, 0)
            }
        }

        fun hideUi() {
            val ims = FlorisImeServiceReference.get() ?: return
            if (AndroidVersion.ATLEAST_API28_P) {
                ims.requestHideSelf(0)
            } else {
                @Suppress("DEPRECATION")
                ims.systemServiceOrNull(InputMethodManager::class)
                    ?.hideSoftInputFromInputMethod(ims.currentInputBinding.connectionToken, 0)
            }
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
    private val editorInstance by editorInstance()
    private val keyboardManager by keyboardManager()
    private val nlpManager by nlpManager()
    private val subtypeManager by subtypeManager()
    private val themeManager by themeManager()

    private val activeState get() = keyboardManager.activeState
    private var inputWindowView by mutableStateOf<View?>(null)
    private var inputViewSize by mutableStateOf(IntSize.Zero)
    private val inputFeedbackController by lazy { InputFeedbackController.new(this) }
    private var isWindowShown: Boolean = false
    private var isFullscreenUiMode by mutableStateOf(false)
    private var isExtractUiShown by mutableStateOf(false)
    private var resourcesContext by mutableStateOf(this as Context)

    init {
        setTheme(R.style.FlorisImeTheme)
    }

    override fun onCreate() {
        super.onCreate()
        FlorisImeServiceReference = WeakReference(this)
        subtypeManager.activeSubtype.observe(this) { subtype ->
            val config = Configuration(resources.configuration)
            config.setLocale(subtype.primaryLocale)
            resourcesContext = createConfigurationContext(config)
        }
    }

    override fun onCreateInputView(): View {
        super.installViewTreeOwners()
        val composeView = ComposeInputView()
        inputWindowView = composeView
        return composeView
    }

    override fun onCreateCandidatesView(): View? {
        // Disable the default candidates view
        return null
    }

    override fun onCreateExtractTextView(): View {
        super.installViewTreeOwners()
        // Consider adding a fallback to the default extract edit layout if user reports come
        // that this causes a crash, especially if the device manufacturer of the user device
        // is a known one to break AOSP standards...
        val defaultExtractView = super.onCreateExtractTextView()
        if (defaultExtractView == null || defaultExtractView !is ViewGroup) {
            return ComposeExtractedLandscapeInputView(null)
        }
        val extractEditText = defaultExtractView.findViewById<ExtractEditText>(android.R.id.inputExtractEditText)
        (extractEditText?.parent as? ViewGroup)?.removeView(extractEditText)
        defaultExtractView.apply {
            removeAllViews()
            addView(ComposeExtractedLandscapeInputView(extractEditText))
        }
        return defaultExtractView
    }

    override fun onDestroy() {
        super.onDestroy()
        FlorisImeServiceReference = WeakReference(null)
        inputWindowView = null
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        flogInfo { "restarting=$restarting info=${info?.debugSummarize()}" }
        super.onStartInput(info, restarting)
        if (info == null) return
        val editorInfo = FlorisEditorInfo.wrap(info)
        editorInstance.handleStartInput(editorInfo)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        flogInfo { "restarting=$restarting info=${info?.debugSummarize()}" }
        super.onStartInputView(info, restarting)
        if (info == null) return
        val editorInfo = FlorisEditorInfo.wrap(info)
        activeState.batchEdit {
            activeState.imeUiMode = ImeUiMode.TEXT
            activeState.isSelectionMode = editorInfo.initialSelection.isSelectionMode
            editorInstance.handleStartInputView(editorInfo)
            keyboardManager.updateCapsState()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        flogInfo { "old={start=$oldSelStart,end=$oldSelEnd} new={start=$newSelStart,end=$newSelEnd} composing={start=$candidatesStart,end=$candidatesEnd}" }
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        activeState.batchEdit {
            activeState.isSelectionMode = (newSelEnd - newSelStart) != 0
            editorInstance.handleSelectionUpdate(
                oldSelection = EditorRange.normalized(oldSelStart, oldSelEnd),
                newSelection = EditorRange.normalized(newSelStart, newSelEnd),
                composing = EditorRange.normalized(candidatesStart, candidatesEnd),
            )
            keyboardManager.updateCapsState()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        flogInfo { "finishing=$finishingInput" }
        super.onFinishInputView(finishingInput)
        editorInstance.handleFinishInputView()
    }

    override fun onFinishInput() {
        flogInfo { "(no args)" }
        super.onFinishInput()
        editorInstance.handleFinishInput()
        nlpManager.clearInlineSuggestions()
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
        themeManager.updateActiveTheme()
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

    override fun onEvaluateFullscreenMode(): Boolean {
        return when (prefs.keyboard.landscapeInputUiMode.get()) {
            LandscapeInputUiMode.DYNAMICALLY_SHOW -> super.onEvaluateFullscreenMode()
            LandscapeInputUiMode.NEVER_SHOW -> false
            LandscapeInputUiMode.ALWAYS_SHOW -> true
        }
    }

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        isFullscreenUiMode = isFullscreenMode
        updateSoftInputWindowLayoutParameters()
    }

    override fun onUpdateExtractingVisibility(info: EditorInfo?) {
        if (info != null) {
            editorInstance.handleStartInputView(FlorisEditorInfo.wrap(info))
        }
        when (prefs.keyboard.landscapeInputUiMode.get()) {
            LandscapeInputUiMode.DYNAMICALLY_SHOW -> super.onUpdateExtractingVisibility(info)
            LandscapeInputUiMode.NEVER_SHOW -> isExtractViewShown = false
            LandscapeInputUiMode.ALWAYS_SHOW -> isExtractViewShown = true
        }
    }

    override fun setExtractViewShown(shown: Boolean) {
        super.setExtractViewShown(shown)
        isExtractUiShown = shown
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        return if (prefs.smartbar.enabled.get() && prefs.suggestion.api30InlineSuggestionsEnabled.get()) {
            flogInfo(LogTopic.IMS_EVENTS) {
                "Creating inline suggestions request because Smartbar and inline suggestions are enabled."
            }
            val stylesBundle = themeManager.createInlineSuggestionUiStyleBundle(this)
            val spec = InlinePresentationSpec.Builder(InlineSuggestionUiSmallestSize, InlineSuggestionUiBiggestSize)
                .setStyle(stylesBundle)
                .build()
            InlineSuggestionsRequest.Builder(listOf(spec)).let { request ->
                request.setMaxSuggestionCount(InlineSuggestionsRequest.SUGGESTION_COUNT_UNLIMITED)
                request.build()
            }
        } else {
            flogInfo(LogTopic.IMS_EVENTS) {
                "Ignoring inline suggestions request because Smartbar and/or inline suggestions are disabled."
            }
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        val inlineSuggestions = response.inlineSuggestions
        flogInfo(LogTopic.IMS_EVENTS) {
            "Received inline suggestions response with ${inlineSuggestions.size} suggestion(s) provided."
        }
        nlpManager.showInlineSuggestions(inlineSuggestions)
        return true
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets == null) return

        val inputWindowView = inputWindowView ?: return
        // TODO: Check also if the keyboard is currently suppressed by a hardware keyboard
        if (!isInputViewShown) {
            outInsets.contentTopInsets = inputWindowView.height
            outInsets.visibleTopInsets = inputWindowView.height
            return
        }

        val visibleTopY = inputWindowView.height - inputViewSize.height
        val needAdditionalOverlay =
            prefs.smartbar.enabled.get() &&
                prefs.smartbar.secondaryActionsEnabled.get() &&
                prefs.smartbar.secondaryActionsExpanded.get() &&
                prefs.smartbar.secondaryActionsPlacement.get() == SecondaryRowPlacement.OVERLAY_APP_UI &&
                keyboardManager.activeState.imeUiMode == ImeUiMode.TEXT

        outInsets.contentTopInsets = visibleTopY
        outInsets.visibleTopInsets = visibleTopY
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        val left = 0
        val top = visibleTopY - if (needAdditionalOverlay) FlorisImeSizing.Static.smartbarHeightPx else 0
        val right = inputViewSize.width
        val bottom = inputWindowView.height
        outInsets.touchableRegion.set(left, top, right, bottom)
    }

    /**
     * Updates the layout params of the window and compose input view.
     */
    private fun updateSoftInputWindowLayoutParameters() {
        val w = window?.window ?: return
        WindowCompat.setDecorFitsSystemWindows(w, true)
        ViewUtils.updateLayoutHeightOf(w, WindowManager.LayoutParams.MATCH_PARENT)
        val layoutHeight = if (isFullscreenUiMode) {
            WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }
        val inputArea = w.findViewById<View>(android.R.id.inputArea) ?: return
        ViewUtils.updateLayoutHeightOf(inputArea, layoutHeight)
        ViewUtils.updateLayoutGravityOf(inputArea, Gravity.BOTTOM)
        val inputWindowView = inputWindowView ?: return
        ViewUtils.updateLayoutHeightOf(inputWindowView, layoutHeight)
    }

    override fun getTextForImeAction(imeOptions: Int): String? {
        return try {
            when (imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_NONE -> null
                EditorInfo.IME_ACTION_GO -> resourcesContext.getString(AndroidInternalR.string.ime_action_go)
                EditorInfo.IME_ACTION_SEARCH -> resourcesContext.getString(AndroidInternalR.string.ime_action_search)
                EditorInfo.IME_ACTION_SEND -> resourcesContext.getString(AndroidInternalR.string.ime_action_send)
                EditorInfo.IME_ACTION_NEXT -> resourcesContext.getString(AndroidInternalR.string.ime_action_next)
                EditorInfo.IME_ACTION_DONE -> resourcesContext.getString(AndroidInternalR.string.ime_action_done)
                EditorInfo.IME_ACTION_PREVIOUS -> resourcesContext.getString(AndroidInternalR.string.ime_action_previous)
                else -> resourcesContext.getString(AndroidInternalR.string.ime_action_default)
            }
        } catch (_: Throwable) {
            super.getTextForImeAction(imeOptions)?.toString()
        }
    }

    @Composable
    private fun ImeUiWrapper() {
        ProvideLocalizedResources(resourcesContext) {
            ProvideKeyboardRowBaseHeight {
                CompositionLocalProvider(LocalInputFeedbackController provides inputFeedbackController) {
                    FlorisImeTheme {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (!(isFullscreenUiMode && isExtractUiShown)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                ) {
                                    DevtoolsUi()
                                }
                            }
                            ImeUi()
                        }
                        SystemUiIme()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ImeUi() {
        val activeState by keyboardManager.observeActiveState()
        val keyboardStyle = FlorisImeTheme.style.get(
            element = FlorisImeUi.Keyboard,
            mode = activeState.inputShiftState.value,
        )
        val layoutDirection = LocalLayoutDirection.current
        SideEffect {
            keyboardManager.activeState.layoutDirection = layoutDirection
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SnyggSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onGloballyPositioned { coords -> inputViewSize = coords.size }
                    // Do not remove below line or touch input may get stuck
                    .pointerInteropFilter { false },
                style = keyboardStyle,
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
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        Box(
                            modifier = Modifier
                                .weight(keyboardWeight)
                                .wrapContentHeight(),
                        ) {
                            when (activeState.imeUiMode) {
                                ImeUiMode.TEXT -> TextInputLayout()
                                ImeUiMode.MEDIA -> MediaInputLayout()
                                ImeUiMode.CLIPBOARD -> ClipboardInputLayout()
                            }
                        }
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
    }

    @Composable
    private fun DevtoolsUi() {
        val devtoolsEnabled by prefs.devtools.enabled.observeAsState()
        if (devtoolsEnabled) {
            DevtoolsOverlay(modifier = Modifier.fillMaxSize())
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

    private inner class ComposeExtractedLandscapeInputView(eet: ExtractEditText?) : FrameLayout(this) {
        val composeView: ComposeView
        val extractEditText: ExtractEditText

        init {
            isHapticFeedbackEnabled = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            extractEditText = (eet ?: ExtractEditText(context)).also {
                it.id = android.R.id.inputExtractEditText
                it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                it.background = null
                it.gravity = Gravity.TOP
                it.isVerticalScrollBarEnabled = true
            }
            addView(extractEditText)

            composeView = ComposeView(context).also { it.setContent { Content() } }
            addView(composeView)
        }

        @Composable
        fun Content() {
            ProvideLocalizedResources(resourcesContext, forceLayoutDirection = LayoutDirection.Ltr) {
                FlorisImeTheme {
                    val layoutStyle = FlorisImeTheme.style.get(FlorisImeUi.ExtractedLandscapeInputLayout)
                    val fieldStyle = FlorisImeTheme.style.get(FlorisImeUi.ExtractedLandscapeInputField)
                    val actionStyle = FlorisImeTheme.style.get(FlorisImeUi.ExtractedLandscapeInputAction)
                    val activeEditorInfo by editorInstance.activeInfoFlow.collectAsState()
                    Box(
                        modifier = Modifier
                            .snyggBackground(layoutStyle, FlorisImeTheme.fallbackSurfaceColor()),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val fieldColor = fieldStyle.foreground.solidColor(FlorisImeTheme.fallbackContentColor())
                            AndroidView(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .snyggShadow(fieldStyle)
                                    .snyggBorder(fieldStyle)
                                    .snyggBackground(fieldStyle),
                                factory = { extractEditText },
                                update = { view ->
                                    view.background = null
                                    view.backgroundTintList = null
                                    view.foregroundTintList = null
                                    view.setTextColor(fieldColor.toArgb())
                                    view.setHintTextColor(fieldColor.copy(fieldColor.alpha * 0.6f).toArgb())
                                    view.setTextSize(
                                        TypedValue.COMPLEX_UNIT_SP,
                                        fieldStyle.fontSize.spSize(default = 16.sp).value,
                                    )
                                },
                            )
                            FlorisButton(
                                onClick = {
                                    if (activeEditorInfo.extractedActionId != 0) {
                                        currentInputConnection?.performEditorAction(activeEditorInfo.extractedActionId)
                                    } else {
                                        editorInstance.performEnterAction(activeEditorInfo.imeOptions.action)
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                text = activeEditorInfo.extractedActionLabel
                                    ?: getTextForImeAction(activeEditorInfo.imeOptions.action.toInt())
                                    ?: "ACTION",
                                shape = actionStyle.shape.shape(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = actionStyle.background.solidColor(FlorisImeTheme.fallbackContentColor()),
                                    contentColor = actionStyle.foreground.solidColor(FlorisImeTheme.fallbackSurfaceColor()),
                                ),
                            )
                        }
                    }
                }
            }
        }

        override fun getAccessibilityClassName(): CharSequence {
            return javaClass.name
        }

        override fun onAttachedToWindow() {
            removeView(extractEditText)
            super.onAttachedToWindow()
            try {
                (parent as LinearLayout).let { extractEditLayout ->
                    extractEditLayout.layoutParams = LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                    ).also { it.setMargins(0, 0, 0, 0) }
                    extractEditLayout.setPadding(0, 0, 0, 0)
                }
            } catch (e: Throwable) {
                flogError { e.message.toString() }
            }
        }
    }
}
