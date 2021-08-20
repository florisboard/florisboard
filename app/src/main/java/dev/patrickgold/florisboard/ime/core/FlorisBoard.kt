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

package dev.patrickgold.florisboard.ime.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.inputmethodservice.ExtractEditText
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.*
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.crashutility.CrashUtility
import dev.patrickgold.florisboard.debug.*
import dev.patrickgold.florisboard.ime.clip.ClipboardInputManager
import dev.patrickgold.florisboard.ime.clip.FlorisClipboardManager
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.media.MediaInputManager
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.popup.PopupLayerView
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.composing.Appender
import dev.patrickgold.florisboard.ime.text.composing.Composer
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.CurrencySet
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.setup.SetupActivity
import dev.patrickgold.florisboard.util.AppVersionUtils
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.databinding.FlorisboardBinding
import dev.patrickgold.florisboard.ime.keyboard.InputFeedbackManager
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.keyboard.updateKeyboardState
import dev.patrickgold.florisboard.util.debugSummarize
import dev.patrickgold.florisboard.util.findViewWithType
import dev.patrickgold.florisboard.util.refreshLayoutOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Variable which holds the current [FlorisBoard] instance. To get this instance from another
 * package, see [FlorisBoard.getInstance].
 * TODO: The end goal is to have no static field for service/manager class anymore. This will take a long time to
 *  rework the codebase but it should be doable.
 */
private var florisboardInstance: FlorisBoard? = null

/**
 * Core class responsible to link together both the text and media input managers as well as
 * managing the one-handed UI.
 *
 * All inline suggestion code has been added based on this demo autofill IME provided by Android directly:
 *  https://cs.android.com/android/platform/superproject/+/master:development/samples/AutofillKeyboard/src/com/example/android/autofillkeyboard/AutofillImeService.java
 */
open class FlorisBoard : InputMethodService(), LifecycleOwner, FlorisClipboardManager.OnPrimaryClipChangedListener,
    ThemeManager.OnThemeUpdatedListener {

    private val serviceLifecycleDispatcher: ServiceLifecycleDispatcher = ServiceLifecycleDispatcher(this)
    private val uiScope: LifecycleCoroutineScope
        get() = lifecycle.coroutineScope
    private var devtoolsOverlaySyncJob: Job? = null

    /**
     * The theme context for the UI. Must only be used for inflating/creating Views for the keyboard UI, else the IME
     * service class should be used directly.
     */
    private var _themeContext: Context? = null
    val themeContext: Context
        get() = _themeContext ?: this

    private val prefs: Preferences get() = Preferences.default()
    val activeState: KeyboardState = KeyboardState.new()

    var uiBinding: FlorisboardBinding? = null
        private set
    private var extractEditLayout: WeakReference<ViewGroup?> = WeakReference(null)
    var popupLayerView: PopupLayerView? = null
        private set
    private var eventListeners: CopyOnWriteArrayList<EventListener> = CopyOnWriteArrayList()

    var imeManager: InputMethodManager? = null
    lateinit var inputFeedbackManager: InputFeedbackManager
    var florisClipboardManager: FlorisClipboardManager? = null
    private val themeManager: ThemeManager = ThemeManager.default()

    private var internalBatchNestingLevel: Int = 0
    private val internalSelectionCache = object {
        var selectionCatchCount: Int = 0
        var oldSelStart: Int = -1
        var oldSelEnd: Int = -1
        var newSelStart: Int = -1
        var newSelEnd: Int = -1
        var candidatesStart: Int = -1
        var candidatesEnd: Int = -1
    }

    lateinit var activeEditorInstance: EditorInstance

    val subtypeManager: SubtypeManager get() = SubtypeManager.default()
    val composer: Composer get() = subtypeManager.imeConfig.composerFromName.getValue(activeSubtype.composerName)
    lateinit var activeSubtype: Subtype
    private var currentThemeIsNight: Boolean = false
    private var currentThemeResId: Int = 0
    private var isWindowShown: Boolean = false

    private var responseState = ResponseState.RESET
    private var pendingResponse: Runnable? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    lateinit var textInputManager: TextInputManager
    lateinit var mediaInputManager: MediaInputManager
    lateinit var clipInputManager: ClipboardInputManager

    var isClipboardContextMenuShown = false

    init {
        // MUST WRAP all code within Service init in try..catch to prevent any crash loops
        try {
            florisboardInstance = this

            textInputManager = TextInputManager.getInstance()
            mediaInputManager = MediaInputManager.getInstance()
            clipInputManager = ClipboardInputManager.getInstance()
        } catch (e: Exception) {
            CrashUtility.stageException(e)
        }
    }

    companion object {
        @Synchronized
        fun getInstance(): FlorisBoard {
            return florisboardInstance!!
        }

        @Synchronized
        fun getInstanceOrNull(): FlorisBoard? {
            return florisboardInstance
        }

        fun getDayNightBaseThemeId(isNightTheme: Boolean): Int {
            return when (isNightTheme) {
                true -> R.style.KeyboardThemeBase_Night
                else -> R.style.KeyboardThemeBase_Day
            }
        }
    }

    override fun getLifecycle(): Lifecycle {
        return serviceLifecycleDispatcher.lifecycle
    }

    private fun updateThemeContext(@StyleRes themeId: Int) {
        _themeContext = ContextThemeWrapper(this, themeId)
    }

    override fun onCreate() {
        // MUST WRAP all code within Service onCreate() in try..catch to prevent any crash loops
        try {
            // Additional try..catch wrapper as the event listeners chain or the super.onCreate() method could crash
            //  and lead to a crash loop
            try {
                // "Main" try..catch block
                flogInfo(LogTopic.IMS_EVENTS)
                serviceLifecycleDispatcher.onServicePreSuperOnCreate()

                activeEditorInstance = EditorInstance(this, activeState)

                imeManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                inputFeedbackManager = InputFeedbackManager.new(this)
                activeSubtype = subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT

                currentThemeIsNight = themeManager.activeTheme.isNightTheme
                currentThemeResId = getDayNightBaseThemeId(currentThemeIsNight)
                setTheme(currentThemeResId)
                themeManager.registerOnThemeUpdatedListener(this)

                AppVersionUtils.updateVersionOnInstallAndLastUse(this, prefs)

                florisClipboardManager = FlorisClipboardManager.getInstance().also {
                    it.initialize(this)
                    it.addPrimaryClipChangedListener(this)
                }
            } catch (e: Exception) {
                super.onCreate() // MUST CALL even if exception thrown or crash loop is imminent
                CrashUtility.stageException(e)
                return
            }
            // Code executed here indicates no crashes occurred, so we execute the onCreate() event as normal
            super.onCreate()
            eventListeners.toList().forEach { it?.onCreate() }
        } catch (e: Exception) {
            CrashUtility.stageException(e)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View? {
        flogInfo(LogTopic.IMS_EVENTS)
        CrashUtility.handleStagedButUnhandledExceptions()

        updateThemeContext(currentThemeResId)

        popupLayerView = PopupLayerView(themeContext)
        window?.window?.findViewById<View>(android.R.id.content)?.let { content ->
            if (content is ViewGroup) {
                content.addView(popupLayerView)
            }
        }

        uiBinding = FlorisboardBinding.inflate(LayoutInflater.from(themeContext))

        eventListeners.toList().forEach { it?.onInitializeInputUi(uiBinding!!) }

        return uiBinding!!.inputWindowView
    }

    fun initWindow() {
        flogInfo(LogTopic.IMS_EVENTS)

        updateSoftInputWindowLayoutParameters()
        updateOneHandedPanelVisibility()

        themeManager.requestThemeUpdate(this)

        dispatchCurrentStateToInputUi()
    }

    /**
     * Disable the default candidates view.
     */
    override fun onCreateCandidatesView(): View? {
        return null
    }

    @SuppressLint("InflateParams")
    override fun onCreateExtractTextView(): View? {
        val eel = super.onCreateExtractTextView()
        if (eel !is ViewGroup) {
            return null
        }
        extractEditLayout = WeakReference(eel)
        eel.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                extractEditLayout.get()?.let { eel ->
                    eel.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    eel.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                    ).also {
                        it.setMargins(0, 0, 0, 0)
                    }
                }
            }
        })
        return eel
    }

    override fun onDestroy() {
        flogInfo(LogTopic.IMS_EVENTS)
        serviceLifecycleDispatcher.onServicePreSuperOnDestroy()

        themeManager.unregisterOnThemeUpdatedListener(this)
        florisClipboardManager?.let {
            it.removePrimaryClipChangedListener(this)
            it.close()
            florisClipboardManager = null
        }
        imeManager = null
        popupLayerView = null
        uiBinding = null

        eventListeners.toList().forEach { it?.onDestroy() }
        eventListeners.clear()
        super.onDestroy()

        florisboardInstance = null
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return resources?.configuration?.let { config ->
            if (config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                false
            } else {
                when (prefs.keyboard.landscapeInputUiMode) {
                    LandscapeInputUiMode.DYNAMICALLY_SHOW -> !activeState.imeOptions.flagNoFullscreen && !activeState.imeOptions.flagNoExtractUi
                    LandscapeInputUiMode.NEVER_SHOW -> false
                    LandscapeInputUiMode.ALWAYS_SHOW -> true
                }
            }
        } ?: false
    }

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        updateSoftInputWindowLayoutParameters()
    }

    override fun onUpdateExtractedText(token: Int, text: ExtractedText?) {
        super.onUpdateExtractedText(token, text)
        activeEditorInstance.updateText(token, text)
    }

    override fun onUpdateExtractingViews(ei: EditorInfo?) {
        super.onUpdateExtractingViews(ei)
    }

    override fun onUpdateExtractingVisibility(ei: EditorInfo?) {
        isExtractViewShown = activeState.isRichInputEditor && when (prefs.keyboard.landscapeInputUiMode) {
            LandscapeInputUiMode.DYNAMICALLY_SHOW -> !activeState.imeOptions.flagNoExtractUi
            LandscapeInputUiMode.NEVER_SHOW -> false
            LandscapeInputUiMode.ALWAYS_SHOW -> true
        }
    }

    override fun onBindInput() {
        activeEditorInstance.bindInput()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        flogInfo(LogTopic.IMS_EVENTS)

        super.onStartInput(attribute, restarting)
        responseState = if (responseState == ResponseState.RECEIVE_RESPONSE) {
            ResponseState.START_INPUT
        } else {
            ResponseState.RESET
        }
        activeEditorInstance.startInput(attribute)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        flogInfo(LogTopic.IMS_EVENTS) { "restarting=$restarting" }
        flogInfo(LogTopic.IMS_EVENTS) { info?.debugSummarize() ?: "" }

        super.onStartInputView(info, restarting)
        if (info != null) {
            activeState.update(info)
            activeState.isSelectionMode = (info.initialSelEnd - info.initialSelStart) != 0
        }
        activeEditorInstance.startInputView(info)
        themeManager.updateRemoteColorValues(activeEditorInstance.packageName ?: "")
        eventListeners.toList().forEach {
            it?.onStartInputView(activeEditorInstance, restarting)
        }
        dispatchCurrentStateToInputUi()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        flogInfo(LogTopic.IMS_EVENTS) { "finishingInput=$finishingInput" }

        if (!finishingInput) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                textInputManager.smartbarView?.clearInlineSuggestions()
            }
        }
        activeEditorInstance.finishInputView()

        super.onFinishInputView(finishingInput)
        eventListeners.toList().forEach { it?.onFinishInputView(finishingInput) }
        dispatchCurrentStateToInputUi()
    }

    override fun onFinishInput() {
        flogInfo(LogTopic.IMS_EVENTS)

        activeEditorInstance.finishInput()
        super.onFinishInput()
    }

    override fun onUnbindInput() {
        activeEditorInstance.unbindInput()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        return if (prefs.smartbar.enabled && prefs.suggestion.api30InlineSuggestionsEnabled) {
            flogInfo(LogTopic.IMS_EVENTS) {
                "Creating inline suggestions request because Smartbar and inline suggestions are enabled."
            }
            val stylesBundle = themeManager.createInlineSuggestionUiStyleBundle(themeContext)
            InlinePresentationSpec.Builder(
                Size(
                    uiBinding?.inputView?.desiredInlineSuggestionsMinWidth ?: 0,
                    uiBinding?.inputView?.desiredInlineSuggestionsMinHeight ?: 0
                ),
                Size(
                    uiBinding?.inputView?.desiredInlineSuggestionsMaxWidth ?: 0,
                    uiBinding?.inputView?.desiredInlineSuggestionsMaxHeight ?: 0
                )
            ).let { spec ->
                spec.setStyle(stylesBundle)
                InlineSuggestionsRequest.Builder(listOf(spec.build())).let { request ->
                    request.setMaxSuggestionCount(6)
                    request.build()
                }
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
        flogInfo(LogTopic.IMS_EVENTS) {
            "Received inline suggestions response with ${response.inlineSuggestions.size} suggestion(s) provided."
        }
        textInputManager.smartbarView?.clearInlineSuggestions()
        postPendingResponse(response)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun cancelPendingResponse() {
        pendingResponse?.let {
            handler.removeCallbacks(it)
            pendingResponse = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun postPendingResponse(response: InlineSuggestionsResponse) {
        cancelPendingResponse()
        val inlineSuggestions = response.inlineSuggestions
        responseState = ResponseState.RECEIVE_RESPONSE
        pendingResponse = Runnable {
            pendingResponse = null
            if (responseState == ResponseState.START_INPUT && inlineSuggestions.isEmpty()) {
                textInputManager.smartbarView?.clearInlineSuggestions()
            } else {
                textInputManager.smartbarView?.showInlineSuggestions(inlineSuggestions)
            }
            responseState = ResponseState.RESET
        }.also { handler.post(it) }
    }

    fun dispatchCurrentStateToInputUi() {
        uiBinding?.inputView?.updateKeyboardState(activeState)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (isWindowShown) {
            flogInfo(LogTopic.IMS_EVENTS) { "Ignoring (is already shown)" }
            return
        } else {
            flogInfo(LogTopic.IMS_EVENTS)
        }
        isWindowShown = true

        val newActiveSubtype = subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT
        if (newActiveSubtype != activeSubtype) {
            activeSubtype = newActiveSubtype
            onSubtypeChanged(activeSubtype, true)
        } else {
            onSubtypeChanged(activeSubtype, false)
        }
        setActiveInput(R.id.text_input)
        updateOneHandedPanelVisibility()
        themeManager.update()

        if (prefs.devtools.enabled && prefs.devtools.showHeapMemoryStats) {
            devtoolsOverlaySyncJob?.cancel()
            devtoolsOverlaySyncJob = uiScope.launch(Dispatchers.Default) {
                while (true) {
                    if (!isActive) break
                    withContext(Dispatchers.Main) { uiBinding?.inputView?.invalidate() }
                    delay(1000)
                }
            }
        }

        eventListeners.toList().forEach { it?.onWindowShown() }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (!isWindowShown) {
            flogInfo(LogTopic.IMS_EVENTS) { "Ignoring (is already hidden)" }
            return
        } else {
            flogInfo(LogTopic.IMS_EVENTS)
        }
        isWindowShown = false

        devtoolsOverlaySyncJob?.cancel()
        devtoolsOverlaySyncJob = null

        eventListeners.toList().forEach { it?.onWindowHidden() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        flogInfo(LogTopic.IMS_EVENTS)
        if (isInputViewShown) {
            updateOneHandedPanelVisibility()
        }

        super.onConfigurationChanged(newConfig)
    }

    /**
     * Begins a FlorisBoard internal batch edit. This enables the application to continue sending selection updates
     * (some apps need to to this else they absolutely refuse to give visual feedback on cursor movement etc.). The
     * selection update is then caught if [internalBatchNestingLevel] is greater than 0, thus not delegating the
     * update to the editor instance. This is needed because else the UI stutters when too many updates arrive in a
     * row.
     */
    fun beginInternalBatchEdit() {
        internalBatchNestingLevel++
    }

    /**
     * Ends an internal batch edit, if [internalBatchNestingLevel] is <= 1 and calls [onUpdateSelection] with the
     * corresponding reported selection values. This call is not caught and the editor instance and other classes are
     * able to update the UI. Resets the internal selection cache and is ready for the next batch edit.
     */
    fun endInternalBatchEdit() {
        internalBatchNestingLevel = (internalBatchNestingLevel - 1).coerceAtLeast(0)
        if (internalBatchNestingLevel == 0) {
            internalSelectionCache.apply {
                if (selectionCatchCount > 0) {
                    onUpdateSelection(
                        oldSelStart, oldSelEnd,
                        newSelStart, newSelEnd,
                        candidatesStart, candidatesEnd
                    )
                    selectionCatchCount = 0
                    oldSelStart = -1
                    oldSelEnd = -1
                    newSelStart = -1
                    newSelEnd = -1
                    candidatesStart = -1
                    candidatesEnd = -1
                }
            }
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd,
            newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )

        activeState.isSelectionMode = (newSelEnd - newSelStart) != 0
        if (internalBatchNestingLevel == 0) {
            flogInfo(LogTopic.IMS_EVENTS) { "onUpdateSelection($oldSelStart, $oldSelEnd, $newSelStart, $newSelEnd, $candidatesStart, $candidatesEnd)" }
            activeEditorInstance.updateSelection(
                oldSelStart, oldSelEnd,
                newSelStart, newSelEnd,
                candidatesStart, candidatesEnd
            )
            eventListeners.toList().forEach { it?.onUpdateSelection() }
        } else {
            flogInfo(LogTopic.IMS_EVENTS) { "onUpdateSelection($oldSelStart, $oldSelEnd, $newSelStart, $newSelEnd, $candidatesStart, $candidatesEnd): caught due to internal batch level of $internalBatchNestingLevel!" }
            if (internalSelectionCache.selectionCatchCount++ == 0) {
                internalSelectionCache.oldSelStart = oldSelStart
                internalSelectionCache.oldSelEnd = oldSelEnd
            }
            internalSelectionCache.newSelStart = newSelStart
            internalSelectionCache.newSelEnd = newSelEnd
            internalSelectionCache.candidatesStart = candidatesStart
            internalSelectionCache.candidatesEnd = candidatesEnd
        }
        dispatchCurrentStateToInputUi()
    }

    override fun onThemeUpdated(theme: Theme) {
        // Rebuild the UI if the theme has changed from day to night or vice versa to prevent
        //  theme glitches with scrollbars and hints of buttons in the media UI. If the UI must be
        //  rebuild, quit this method, as it will be called again by the newly created UI.
        val newThemeIsNightMode =  theme.isNightTheme
        if (currentThemeIsNight != newThemeIsNightMode) {
            currentThemeResId = getDayNightBaseThemeId(newThemeIsNightMode)
            currentThemeIsNight = newThemeIsNightMode
            setInputView(onCreateInputView())
            return
        }

        // Get Window and the flags of the DecorView
        val w = window?.window ?: return
        var flags = w.decorView.systemUiVisibility

        // Update navigation bar theme
        w.navigationBarColor = theme.getAttr(Theme.Attr.WINDOW_NAVIGATION_BAR_COLOR).toSolidColor().color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            flags = if (theme.getAttr(Theme.Attr.WINDOW_NAVIGATION_BAR_LIGHT).toOnOff().state) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }

        // Update status bar to be transparent
        //  Done as starting with Android 11 the IME Window takes the primaryColorDark value and
        //  colors the status bar, which isn't the desired behavior. (See issue #43)
        flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.statusBarColor = Color.TRANSPARENT

        // Apply the new flags to the DecorView
        w.decorView.systemUiVisibility = flags

        // Update InputView theme
        uiBinding?.inputView?.setBackgroundColor(theme.getAttr(Theme.Attr.KEYBOARD_BACKGROUND).toSolidColor().color)
        uiBinding?.inputView?.invalidate()

        // Update ExtractTextView theme and attributes
        extractEditLayout.get()?.let { eel ->
            val p = resources.getDimension(R.dimen.landscapeInputUi_padding).toInt()
            eel.setPadding(p, p, 0, p)
            eel.setBackgroundColor(theme.getAttr(Theme.Attr.EXTRACT_EDIT_LAYOUT_BACKGROUND).toSolidColor().color)
            eel.findViewById<ExtractEditText>(android.R.id.inputExtractEditText)?.let { eet ->
                val p2 = resources.getDimension(R.dimen.landscapeInputUi_editText_padding).toInt()
                eet.setPadding(p2, p2, p2, p2)
                eet.background = ContextCompat.getDrawable(this, R.drawable.edit_text_background)?.also { d ->
                    DrawableCompat.setTint(d, theme.getAttr(Theme.Attr.WINDOW_COLOR_PRIMARY).toSolidColor().color)
                }
                eet.setTextColor(theme.getAttr(Theme.Attr.EXTRACT_EDIT_LAYOUT_FOREGROUND).toSolidColor().color)
                eet.setHintTextColor(theme.getAttr(Theme.Attr.EXTRACT_EDIT_LAYOUT_FOREGROUND_ALT).toSolidColor().color)
                eet.highlightColor = theme.getAttr(Theme.Attr.WINDOW_COLOR_PRIMARY).toSolidColor().color
            }
            eel.findViewWithType(FrameLayout::class)?.let { fra ->
                fra.background = null
            }
            eel.findViewWithType(Button::class)?.let { btn ->
                btn.background = ContextCompat.getDrawable(this, R.drawable.shape_rect_rounded)?.also { d ->
                    DrawableCompat.setTint(d, theme.getAttr(Theme.Attr.EXTRACT_ACTION_BUTTON_BACKGROUND).toSolidColor().color)
                }
                btn.setTextColor(theme.getAttr(Theme.Attr.EXTRACT_ACTION_BUTTON_FOREGROUND).toSolidColor().color)
            }
            eel.invalidate()
        }

        eventListeners.toList().forEach { it?.onApplyThemeAttributes() }
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        val inputView = uiBinding?.inputView ?: return
        val inputWindowView = uiBinding?.inputWindowView ?: return
        // TODO: Check also if the keyboard is currently suppressed by a hardware keyboard

        if (!isInputViewShown) {
            outInsets?.contentTopInsets = inputWindowView.height
            outInsets?.visibleTopInsets = inputWindowView.height
            return
        }
        val visibleTopY = inputWindowView.height - inputView.measuredHeight
        outInsets?.contentTopInsets = visibleTopY
        outInsets?.visibleTopInsets = visibleTopY

        if (isClipboardContextMenuShown) {
            outInsets?.touchableInsets = Insets.TOUCHABLE_INSETS_FRAME
            outInsets?.touchableRegion?.setEmpty()
        }
    }

    /**
     * Updates the layout params of the window and input view.
     */
    private fun updateSoftInputWindowLayoutParameters() {
        val w = window?.window ?: return
        ViewUtils.updateLayoutHeightOf(w, WindowManager.LayoutParams.MATCH_PARENT)
        val inputWindowView = uiBinding?.inputWindowView
        if (inputWindowView != null) {
            val layoutHeight = if (isFullscreenMode) {
                WindowManager.LayoutParams.WRAP_CONTENT
            } else {
                WindowManager.LayoutParams.MATCH_PARENT
            }
            val inputArea = w.findViewById<View>(android.R.id.inputArea)
            ViewUtils.updateLayoutHeightOf(inputArea, layoutHeight)
            ViewUtils.updateLayoutGravityOf(inputArea, Gravity.BOTTOM)
            ViewUtils.updateLayoutHeightOf(inputWindowView, layoutHeight)
        }
    }

    /**
     * Executes a given [SwipeAction]. Ignores any [SwipeAction] but the ones relevant for this
     * class.
     */
    fun executeSwipeAction(swipeAction: SwipeAction) {
        when (swipeAction) {
            SwipeAction.HIDE_KEYBOARD -> requestHideSelf(0)
            SwipeAction.SWITCH_TO_PREV_SUBTYPE -> switchToPrevSubtype()
            SwipeAction.SWITCH_TO_NEXT_SUBTYPE -> switchToNextSubtype()
            SwipeAction.SWITCH_TO_PREV_KEYBOARD -> switchToPrevKeyboard()
            else -> textInputManager.executeSwipeAction(swipeAction)
        }
    }

    /**
     * Hides the IME and launches [SetupActivity].
     */
    fun launchSettings() {
        requestHideSelf(0)
        val i = Intent(this, SetupActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                  Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                  Intent.FLAG_ACTIVITY_CLEAR_TOP
        applicationContext.startActivity(i)
    }

    /**
     * @return If the language switch should be shown.
     */
    fun shouldShowLanguageSwitch(): Boolean {
        return subtypeManager.subtypes.size > 1
    }

    fun switchToPrevKeyboard(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod()
            } else {
                window.window?.let { window ->
                    @Suppress("DEPRECATION")
                    imeManager?.switchToLastInputMethod(window.attributes.token)
                }
            }
        } catch (e: Exception) {
            flogError { "Unable to switch to the previous IME" }
            imeManager?.showInputMethodPicker()
        }
    }

    fun switchToNextKeyboard(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToNextInputMethod(false)
            } else {
                window.window?.let { window ->
                    @Suppress("DEPRECATION")
                    imeManager?.switchToNextInputMethod(window.attributes.token, false)
                }
            }
        } catch (e: Exception) {
            flogError { "Unable to switch to the next IME" }
            imeManager?.showInputMethodPicker()
        }
    }

    fun switchToPrevSubtype() {
        flogInfo(LogTopic.IMS_EVENTS)
        activeSubtype = subtypeManager.switchToPrevSubtype() ?: Subtype.DEFAULT
        onSubtypeChanged(activeSubtype, true)
    }

    fun switchToNextSubtype() {
        flogInfo(LogTopic.IMS_EVENTS)
        activeSubtype = subtypeManager.switchToNextSubtype() ?: Subtype.DEFAULT
        onSubtypeChanged(activeSubtype, true)
    }

    private fun onSubtypeChanged(newSubtype: Subtype, doRefreshLayouts: Boolean) {
        flogInfo(LogTopic.SUBTYPE_MANAGER) { "New subtype: $newSubtype" }
        textInputManager.onSubtypeChanged(newSubtype, doRefreshLayouts)
        mediaInputManager.onSubtypeChanged(newSubtype, doRefreshLayouts)
        clipInputManager.onSubtypeChanged(newSubtype, doRefreshLayouts)
    }

    fun setActiveInput(type: Int, forceSwitchToCharacters: Boolean = false) {
        when (type) {
            R.id.text_input -> {
                uiBinding?.mainViewFlipper?.displayedChild = 0
                if (forceSwitchToCharacters) {
                    textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(TextKeyData.VIEW_CHARACTERS))
                }
            }
            R.id.media_input -> {
                uiBinding?.mainViewFlipper?.displayedChild = 1
            }
            R.id.clip_input -> {
                uiBinding?.mainViewFlipper?.displayedChild = 2
            }
        }
        textInputManager.isGlidePostEffect = false
    }

    fun toggleOneHandedMode(isRight: Boolean) {
        prefs.keyboard.oneHandedMode = when (prefs.keyboard.oneHandedMode) {
            OneHandedMode.OFF -> if (isRight) { OneHandedMode.END } else { OneHandedMode.START }
            else -> OneHandedMode.OFF
        }
        updateOneHandedPanelVisibility()
    }

    fun updateOneHandedPanelVisibility() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            uiBinding?.oneHandedCtrlPanelStart?.visibility = View.GONE
            uiBinding?.oneHandedCtrlPanelEnd?.visibility = View.GONE
        } else {
            when (prefs.keyboard.oneHandedMode) {
                OneHandedMode.OFF -> {
                    uiBinding?.oneHandedCtrlPanelStart?.visibility = View.GONE
                    uiBinding?.oneHandedCtrlPanelEnd?.visibility = View.GONE
                }
                OneHandedMode.START -> {
                    uiBinding?.oneHandedCtrlPanelStart?.visibility = View.GONE
                    uiBinding?.oneHandedCtrlPanelEnd?.visibility = View.VISIBLE
                }
                OneHandedMode.END -> {
                    uiBinding?.oneHandedCtrlPanelStart?.visibility = View.VISIBLE
                    uiBinding?.oneHandedCtrlPanelEnd?.visibility = View.GONE
                }
            }
        }
        // Delay execution so this function can return, then refresh the whole layout
        uiScope.launch {
            refreshLayoutOf(uiBinding?.inputView)
        }
    }

    override fun onPrimaryClipChanged() {
        eventListeners.toList().forEach { it?.onPrimaryClipChanged() }
    }

    /**
     * Adds a given [listener] to the list which will receive FlorisBoard events.
     *
     * @param listener The listener object which receives the events.
     * @return True if the listener has been added successfully, false otherwise.
     */
    fun addEventListener(listener: EventListener): Boolean {
        return eventListeners.add(listener)
    }

    /**
     * Removes a given [listener] from the list which will receive FlorisBoard events.
     *
     * TODO: implement this function with a proper iterator
     *
     * @param listener The same listener object which was used in [addEventListener].
     * @return True if the listener has been removed successfully, false otherwise. A false return
     *  value may also indicate that the [listener] was not added previously.
     */
    fun removeEventListener(listener: EventListener): Boolean {
        return eventListeners.remove(listener)
    }

    interface EventListener {
        fun onCreate() {}
        fun onInitializeInputUi(uiBinding: FlorisboardBinding) {}
        fun onDestroy() {}

        fun onStartInputView(instance: EditorInstance, restarting: Boolean) {}
        fun onFinishInputView(finishingInput: Boolean) {}

        fun onWindowShown() {}
        fun onWindowHidden() {}

        fun onUpdateSelection() {}

        fun onApplyThemeAttributes() {}
        fun onPrimaryClipChanged() {}
        fun onSubtypeChanged(newSubtype: Subtype, doRefreshLayouts: Boolean) {}
    }

    private enum class ResponseState {
        RESET, RECEIVE_RESPONSE, START_INPUT
    }

    /**
     * Data class which holds the base information for this IME. Matches the structure of
     * ime/config.json so it can be parsed. Used by [SubtypeManager] and by the prefs.
     * NOTE: this class and its corresponding json file is subject to change in future versions.
     * @property packageName The package name of this IME.
     * @property currencySets The predefined currency sets for this IME, available for selection
     *  in the Settings UI.
     * @property defaultSubtypes A list of predefined default subtypes. This subtypes are used to
     *  define which locales are supported and which layout is preferred for that locale.
     * @property currencySetNames Helper list for Settings Subtype Spinner elements.
     * @property currencySetLabels Helper list for Settings Subtype Spinner elements.
     * @property defaultSubtypesLanguageCodes Helper list for Settings Subtype Spinner elements.
     * @property defaultSubtypesLanguageNames Helper list for Settings Subtype Spinner elements.
     */
    @Serializable
    data class ImeConfig(
        @SerialName("package")
        val packageName: String,
        @SerialName("composers")
        val composers: List<Composer> = listOf(),
        @SerialName("currencySets")
        val currencySets: List<CurrencySet> = listOf(),
        @SerialName("defaultSubtypes")
        val defaultSubtypes: List<DefaultSubtype> = listOf()
    ) {
        @Transient var currencySetNames: List<String> = listOf()
        @Transient var currencySetLabels: List<String> = listOf()
        @Transient var composerNames: List<String> = listOf()
        @Transient var composerLabels: List<String> = listOf()
        @Transient val composerFromName: Map<String, Composer> = composers.map { it.name to it }.toMap()
        @Transient var defaultSubtypesLanguageCodes: List<String> = listOf()
        @Transient var defaultSubtypesLanguageNames: List<String> = listOf()

        init {
            val tmpComposerList = composers.map { Pair(it.name, it.label) }.toMutableList()
            // Sort composer list alphabetically by the label of a composer
            tmpComposerList.sortBy { it.second }
            // Move selected composers to the top of the list
            for (composerName in listOf(Appender.name)) {
                val index: Int = tmpComposerList.indexOfFirst { it.first == composerName }
                if (index > 0) {
                    tmpComposerList.add(0, tmpComposerList.removeAt(index))
                }
            }
            composerNames = tmpComposerList.map { it.first }.toList()
            composerLabels = tmpComposerList.map { it.second }.toList()

            val tmpCurrencyList = mutableListOf<Pair<String, String>>()
            for (currencySet in currencySets) {
                tmpCurrencyList.add(Pair(currencySet.name, currencySet.label))
            }
            // Sort currency set list alphabetically by the label of a currency set
            tmpCurrencyList.sortBy { it.second }
            // Move selected currency variants to the top of the list
            for (currencyName in listOf("euro", "dollar")) {
                val index: Int = tmpCurrencyList.indexOfFirst { it.first == currencyName }
                if (index > 0) {
                    tmpCurrencyList.add(0, tmpCurrencyList.removeAt(index))
                }
            }
            currencySetNames = tmpCurrencyList.map { it.first }.toList()
            currencySetLabels = tmpCurrencyList.map { it.second }.toList()

            val tmpSubtypeList = mutableListOf<Pair<String, String>>()
            for (defaultSubtype in defaultSubtypes) {
                tmpSubtypeList.add(Pair(defaultSubtype.locale.localeTag(), defaultSubtype.locale.displayName()))
            }
            // Sort language list alphabetically by the display name of a language
            tmpSubtypeList.sortBy { it.second }
            // Move selected English variants to the top of the list
            for (languageCode in listOf("en_CA", "en_AU", "en_UK", "en_US")) {
                val index: Int = tmpSubtypeList.indexOfFirst { it.first == languageCode }
                if (index > 0) {
                    tmpSubtypeList.add(0, tmpSubtypeList.removeAt(index))
                }
            }
            defaultSubtypesLanguageCodes = tmpSubtypeList.map { it.first }.toList()
            defaultSubtypesLanguageNames = tmpSubtypeList.map { it.second }.toList()
        }
    }
}
