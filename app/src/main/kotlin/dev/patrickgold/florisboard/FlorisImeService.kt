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

package dev.patrickgold.florisboard

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.inputmethodservice.ExtractEditText
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.editor.EditorRange
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.input.InputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.isFullscreenInputRequired
import dev.patrickgold.florisboard.ime.landscapeinput.ExtractedInputRootView
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.lifecycle.LifecycleInputMethodService
import dev.patrickgold.florisboard.ime.nlp.NlpInlineAutofill
import dev.patrickgold.florisboard.ime.theme.WallpaperChangeReceiver
import dev.patrickgold.florisboard.ime.window.ImeRootView
import dev.patrickgold.florisboard.ime.window.ImeWindowController
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.devtools.flogWarning
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.florisboard.lib.util.debugSummarize
import dev.patrickgold.florisboard.lib.util.launchActivity
import kotlinx.coroutines.flow.update
import org.florisboard.lib.android.AndroidInternalR
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.showShortToastSync
import org.florisboard.lib.android.systemServiceOrNull
import org.florisboard.lib.kotlin.collectIn
import org.florisboard.lib.kotlin.collectLatestIn
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
            ims.showUi()
        }

        fun hideUi() {
            val ims = FlorisImeServiceReference.get() ?: return
            ims.hideUi()
        }

        fun switchToPrevInputMethod(): Boolean {
            val ims = FlorisImeServiceReference.get() ?: return false
            return ims.switchToPrevInputMethod()
        }

        fun switchToNextInputMethod(): Boolean {
            val ims = FlorisImeServiceReference.get() ?: return false
            return ims.switchToNextInputMethod()
        }

        fun switchToVoiceInputMethod(): Boolean {
            val ims = FlorisImeServiceReference.get() ?: return false
            return ims.switchToVoiceInputMethod()
        }

        fun showImePicker(): Boolean {
            val ims = FlorisImeServiceReference.get() ?: return false
            return InputMethodUtils.showImePicker(ims)
        }

        fun windowControllerOrNull(): ImeWindowController? {
            val ims = FlorisImeServiceReference.get() ?: return null
            return ims.windowController
        }
    }

    fun hideUi() {
        requestHideSelf(0)
    }

    /**
     * Show the Ime UI
     *
     * Note: This function can be replaced with a `requestShowSelf(0)`
     * call once we've set the minApiLevel to 28 (Android 9)
     */
    fun showUi() {
        if (AndroidVersion.ATLEAST_API28_P) {
            requestShowSelf(0)
        } else {
            @Suppress("DEPRECATION")
            systemServiceOrNull(InputMethodManager::class)
                ?.showSoftInputFromInputMethod(currentInputBinding.connectionToken, 0)
        }
    }


    /**
     * Switch to previous input method
     *
     * Note: This function can be replaced with a `switchToPreviousInputMethod()`
     * call once we've set the minApiLevel to 28 (Android 9)
     *
     * @return true if the switch was successful
     */
    fun switchToPrevInputMethod(): Boolean {
        val imm = systemServiceOrNull(InputMethodManager::class)
        try {
            if (AndroidVersion.ATLEAST_API28_P) {
                return switchToPreviousInputMethod()
            } else {
                window.window?.let { window ->
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

    /**
     * Switch to next input method
     *
     * Note: This function can be replaced with a `switchToNextInputMethod(false)`
     * call once we've set the minApiLevel to 28 (Android 9)
     *
     * @return true if the switch was successful
     */
    fun switchToNextInputMethod(): Boolean {
        val imm = systemServiceOrNull(InputMethodManager::class)
        try {
            if (AndroidVersion.ATLEAST_API28_P) {
                return switchToNextInputMethod(false)
            } else {
                window.window?.let { window ->
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

    /**
     * Switch to next input method
     *
     * Note: The inner part of this function can be replaced with a
     *
     * `switchInputMethod(el.id, el.getSubtypeAt(i))` call once we've set the minApiLevel to 28 (Android 9)
     *
     * @return true if the switch was successful
     */
    fun switchToVoiceInputMethod(): Boolean {
        val imm = systemServiceOrNull(InputMethodManager::class) ?: return false
        val list: List<InputMethodInfo> = imm.enabledInputMethodList
        for (el in list) {
            for (i in 0 until el.subtypeCount) {
                // Check if the subtype is a voice input method.
                // We need to hardcode 'voice' here because the SUBTYPE_MODE_VOICE constant is private.
                // https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/core/java/android/view/inputmethod/InputMethodManager.java;drc=2b278ab3ac73bb5596327aac1298df85cd94e454;l=309
                if (el.getSubtypeAt(i).mode != "voice") continue
                if (AndroidVersion.ATLEAST_API28_P) {
                    switchInputMethod(el.id, el.getSubtypeAt(i))
                    return true
                } else {
                    window.window?.let { window ->
                        @Suppress("DEPRECATION")
                        imm.setInputMethod(window.attributes.token, el.id)
                        return true
                    }
                }
            }
        }
        showShortToastSync("Failed to find voice IME, do you have one installed?")
        return false
    }

    private val prefs by FlorisPreferenceStore
    val editorInstance by editorInstance()
    private val keyboardManager by keyboardManager()
    private val nlpManager by nlpManager()
    private val subtypeManager by subtypeManager()
    private val themeManager by themeManager()

    val windowController = ImeWindowController(prefs, lifecycleScope)

    private val activeState get() = keyboardManager.activeState
    val inputFeedbackController by lazy { InputFeedbackController.new(this) }
    var resourcesContext by mutableStateOf(this as Context)
        private set

    private val wallpaperChangeReceiver = WallpaperChangeReceiver()

    init {
        setTheme(R.style.FlorisImeTheme)
    }

    override fun onCreate() {
        super.onCreate()
        FlorisImeServiceReference = WeakReference(this)
        WindowCompat.setDecorFitsSystemWindows(window.window!!, false)
        windowController.onConfigurationChanged(resources.configuration)
        windowController.activeWindowConfig.collectLatestIn(lifecycleScope) {
            keyboardManager.updateActiveEvaluators() // TODO: wacky solution, but works for now
        }
        subtypeManager.activeSubtypeFlow.collectIn(lifecycleScope) { subtype ->
            val config = Configuration(resources.configuration)
            if (prefs.localization.displayKeyboardLabelsInSubtypeLanguage.get()) {
                config.setLocale(subtype.primaryLocale.base)
            }
            resourcesContext = createConfigurationContext(config)
        }
        prefs.localization.displayKeyboardLabelsInSubtypeLanguage.asFlow().collectIn(lifecycleScope) { shouldSync ->
            val config = Configuration(resources.configuration)
            if (shouldSync) {
                config.setLocale(subtypeManager.activeSubtype.primaryLocale.base)
            }
            resourcesContext = createConfigurationContext(config)
        }
        prefs.physicalKeyboard.showOnScreenKeyboard.asFlow().collectIn(lifecycleScope) {
            updateInputViewShown()
        }
        @Suppress("DEPRECATION") // We do not retrieve the wallpaper but only listen to changes
        registerReceiver(wallpaperChangeReceiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED))
    }

    override fun onCreateInputView(): View? {
        super.installViewTreeOwners()
        val content = window.window!!.findViewById<ViewGroup>(android.R.id.content)
        content.addView(ImeRootView(this))
        // Disable the default input view placement
        return null
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
            return ExtractedInputRootView(this, null)
        }
        val extractEditText = defaultExtractView.findViewById<ExtractEditText>(android.R.id.inputExtractEditText)
        (extractEditText?.parent as? ViewGroup)?.removeView(extractEditText)
        defaultExtractView.let {
            it.removeAllViews()
            it.addView(ExtractedInputRootView(this, extractEditText))
        }
        return defaultExtractView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        themeManager.configurationChangeCounter.update { it + 1 }
        windowController.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wallpaperChangeReceiver)
        FlorisImeServiceReference = WeakReference(null)
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
            if (activeState.imeUiMode != ImeUiMode.CLIPBOARD || prefs.clipboard.historyHideOnNextTextField.get()) {
                activeState.imeUiMode = ImeUiMode.TEXT
            }
            activeState.isSelectionMode = editorInfo.initialSelection.isSelectionMode
            editorInstance.handleStartInputView(editorInfo, isRestart = restarting)
        }
    }

    override fun onEvaluateInputViewShown(): Boolean {
        val config = resources.configuration
        return super.onEvaluateInputViewShown()
            || config.keyboard == Configuration.KEYBOARD_NOKEYS
            || prefs.physicalKeyboard.showOnScreenKeyboard.get()
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
        NlpInlineAutofill.clearInlineSuggestions()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (windowController.onWindowShown()) {
            flogInfo(LogTopic.IMS_EVENTS)
            inputFeedbackController.updateSystemPrefsState()
        } else {
            flogWarning(LogTopic.IMS_EVENTS) { "Ignoring (is already shown)" }
        }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (windowController.onWindowHidden()) {
            flogInfo(LogTopic.IMS_EVENTS)
            activeState.batchEdit {
                activeState.imeUiMode = ImeUiMode.TEXT
                activeState.isActionsOverflowVisible = false
                activeState.isActionsEditorVisible = false
            }
        } else {
            flogWarning(LogTopic.IMS_EVENTS) { "Ignoring (is already hidden)" }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        val config = resources.configuration
        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            return false
        }
        return when (prefs.keyboard.landscapeInputUiMode.get()) {
            LandscapeInputUiMode.DYNAMICALLY_SHOW -> super.onEvaluateFullscreenMode()
            LandscapeInputUiMode.NEVER_SHOW -> false
            LandscapeInputUiMode.ALWAYS_SHOW -> true
        }
    }

    override fun onUpdateExtractingVisibility(info: EditorInfo?) {
        if (info != null) {
            editorInstance.handleStartInputView(FlorisEditorInfo.wrap(info), isRestart = true)
        }
        when (prefs.keyboard.landscapeInputUiMode.get()) {
            LandscapeInputUiMode.DYNAMICALLY_SHOW -> super.onUpdateExtractingVisibility(info)
            LandscapeInputUiMode.NEVER_SHOW -> isExtractViewShown = false
            LandscapeInputUiMode.ALWAYS_SHOW -> isExtractViewShown = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        if (!prefs.smartbar.enabled.get() || !prefs.suggestion.api30InlineSuggestionsEnabled.get()) {
            flogInfo(LogTopic.IMS_EVENTS) {
                "Ignoring inline suggestions request because Smartbar and/or inline suggestions are disabled."
            }
            return null
        }

        flogInfo(LogTopic.IMS_EVENTS) { "Creating inline suggestions request" }
        val stylesBundle = themeManager.createInlineSuggestionUiStyleBundle(this)
        if (stylesBundle == null) {
            flogWarning(LogTopic.IMS_EVENTS) { "Failed to retrieve inline suggestions style bundle" }
            return null
        }
        val spec = InlinePresentationSpec.Builder(
            InlineSuggestionUiSmallestSize,
            InlineSuggestionUiBiggestSize,
        ).run {
            setStyle(stylesBundle)
            build()
        }

        return InlineSuggestionsRequest.Builder(listOf(spec)).run {
            setMaxSuggestionCount(InlineSuggestionsRequest.SUGGESTION_COUNT_UNLIMITED)
            build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        val inlineSuggestions = response.inlineSuggestions
        flogInfo(LogTopic.IMS_EVENTS) {
            "Received inline suggestions response with ${inlineSuggestions.size} suggestion(s) provided."
        }
        return NlpInlineAutofill.showInlineSuggestions(this, inlineSuggestions)
    }

    override fun onComputeInsets(outInsets: Insets?) {
        if (outInsets == null) return
        val state = keyboardManager.activeState.snapshot()
        windowController.onComputeInsets(outInsets, state.isFullscreenInputRequired())
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyboardManager.onHardwareKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return keyboardManager.onHardwareKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
    }
}
