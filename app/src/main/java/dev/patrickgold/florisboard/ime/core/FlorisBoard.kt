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
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.squareup.moshi.Json
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.media.MediaInputManager
import dev.patrickgold.florisboard.ime.popup.PopupLayerView
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import dev.patrickgold.florisboard.util.*
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Variable which holds the current [FlorisBoard] instance. To get this instance from another
 * package, see [FlorisBoard.getInstance].
 */
private var florisboardInstance: FlorisBoard? = null

/**
 * Core class responsible to link together both the text and media input managers as well as
 * managing the one-handed UI.
 */
class FlorisBoard : InputMethodService(), ClipboardManager.OnPrimaryClipChangedListener,
    ThemeManager.OnThemeUpdatedListener {
    lateinit var prefs: PrefHelper
        private set

    val context: Context
        get() = inputWindowView?.context ?: this
    var inputView: InputView? = null
        private set
    var popupLayerView: PopupLayerView? = null
        private set
    private var inputWindowView: InputWindowView? = null
    private var eventListeners: MutableList<WeakReference<EventListener?>?> = mutableListOf()

    private var audioManager: AudioManager? = null
    private var imeManager:InputMethodManager? = null
    var clipboardManager: ClipboardManager? = null
    private val themeManager: ThemeManager = ThemeManager.default()
    private var vibrator: Vibrator? = null
    private val osHandler = Handler()

    var activeEditorInstance: EditorInstance = EditorInstance.default()

    lateinit var subtypeManager: SubtypeManager
    lateinit var activeSubtype: Subtype
    private var currentThemeIsNight: Boolean = false
    private var currentThemeResId: Int = 0
    private var isNumberRowVisible: Boolean = false

    val textInputManager: TextInputManager
    val mediaInputManager: MediaInputManager

    init {
        florisboardInstance = this

        textInputManager = TextInputManager.getInstance()
        mediaInputManager = MediaInputManager.getInstance()
    }

    companion object {
        private const val IME_ID: String = "dev.patrickgold.florisboard/.ime.core.FlorisBoard"
        private const val IME_ID_DEBUG: String = "dev.patrickgold.florisboard.debug/dev.patrickgold.florisboard.ime.core.FlorisBoard"

        fun checkIfImeIsEnabled(context: Context): Boolean {
            val activeImeIds = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_INPUT_METHODS
            )
            Timber.i("List of active IMEs: $activeImeIds")
            return when {
                BuildConfig.DEBUG -> {
                    activeImeIds.split(":").contains(IME_ID_DEBUG)
                }
                else -> {
                    activeImeIds.split(":").contains(IME_ID)
                }
            }
        }

        fun checkIfImeIsSelected(context: Context): Boolean {
            val selectedImeId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            Timber.i("Selected IME: $selectedImeId")
            return when {
                BuildConfig.DEBUG -> {
                    selectedImeId == IME_ID_DEBUG
                }
                else -> {
                    selectedImeId == IME_ID
                }
            }
        }

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

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
        Timber.i("onCreate()")

        imeManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(this)
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        prefs = PrefHelper.getDefaultInstance(this)
        prefs.initDefaultPreferences()
        prefs.sync()
        subtypeManager = SubtypeManager(this, prefs)
        activeSubtype = subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT

        currentThemeIsNight = themeManager.activeTheme.isNightTheme
        currentThemeResId = getDayNightBaseThemeId(currentThemeIsNight)
        isNumberRowVisible = prefs.keyboard.numberRow
        setTheme(currentThemeResId)
        themeManager.registerOnThemeUpdatedListener(this)

        AppVersionUtils.updateVersionOnInstallAndLastUse(this, prefs)

        super.onCreate()
        eventListeners.toList().forEach { it?.get()?.onCreate() }
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View? {
        Timber.i("onCreateInputView()")

        baseContext.setTheme(currentThemeResId)

        inputWindowView = layoutInflater.inflate(R.layout.florisboard, null) as InputWindowView
        popupLayerView = inputWindowView?.findViewById(R.id.popup_layer)

        eventListeners.toList().forEach { it?.get()?.onCreateInputView() }

        return inputWindowView
    }

    fun registerInputView(inputView: InputView) {
        Timber.i("registerInputView($inputView)")

        this.inputView = inputView
        initializeOneHandedEnvironment()
        updateSoftInputWindowLayoutParameters()
        updateOneHandedPanelVisibility()
        themeManager.notifyCallbackReceivers()
        setActiveInput(R.id.text_input)

        eventListeners.toList().forEach { it?.get()?.onRegisterInputView(inputView) }
    }

    override fun onDestroy() {
        Timber.i("onDestroy()")

        themeManager.unregisterOnThemeUpdatedListener(this)
        clipboardManager?.removePrimaryClipChangedListener(this)
        osHandler.removeCallbacksAndMessages(null)
        florisboardInstance = null

        eventListeners.toList().forEach { it?.get()?.onDestroy() }
        eventListeners.clear()
        super.onDestroy()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        Timber.i("onStartInput($attribute, $restarting)")

        super.onStartInput(attribute, restarting)
        currentInputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        Timber.i("onStartInputView($info, $restarting)")
        Timber.i("onStartInputView: ${info?.debugSummarize()}")

        super.onStartInputView(info, restarting)
        activeEditorInstance = EditorInstance.from(info, this)
        themeManager.updateRemoteColorValues(activeEditorInstance.packageName)
        eventListeners.toList().forEach {
            it?.get()?.onStartInputView(activeEditorInstance, restarting)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        Timber.i( "onFinishInputView($finishingInput)")

        if (finishingInput) {
            activeEditorInstance = EditorInstance.default()
        }

        super.onFinishInputView(finishingInput)
        eventListeners.toList().forEach { it?.get()?.onFinishInputView(finishingInput) }
    }

    override fun onFinishInput() {
        Timber.i("onFinishInput()")

        super.onFinishInput()
        currentInputConnection?.requestCursorUpdates(0)
    }

    override fun onWindowShown() {
        Timber.i("onWindowShown()")

        prefs.sync()
        val newIsNumberRowVisible = prefs.keyboard.numberRow
        if (isNumberRowVisible != newIsNumberRowVisible) {
            textInputManager.layoutManager.clearLayoutCache(KeyboardMode.CHARACTERS)
            isNumberRowVisible = newIsNumberRowVisible
        }
        themeManager.update()
        updateOneHandedPanelVisibility()
        activeSubtype = subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT
        onSubtypeChanged(activeSubtype)
        setActiveInput(R.id.text_input)

        super.onWindowShown()
        eventListeners.toList().forEach { it?.get()?.onWindowShown() }
    }

    override fun onWindowHidden() {
        Timber.i("onWindowHidden()")

        super.onWindowHidden()
        eventListeners.toList().forEach { it?.get()?.onWindowHidden() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.i("onConfigurationChanged($newConfig)")
        if (isInputViewShown) {
            updateOneHandedPanelVisibility()
        }

        super.onConfigurationChanged(newConfig)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        Timber.i("onUpdateSelection($oldSelStart, $oldSelEnd, $newSelStart, $newSelEnd, $candidatesStart, $candidatesEnd)")

        super.onUpdateSelection(
            oldSelStart, oldSelEnd,
            newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )
        activeEditorInstance.onUpdateSelection(
            oldSelStart, oldSelEnd,
            newSelStart, newSelEnd
        )
        eventListeners.toList().forEach { it?.get()?.onUpdateSelection() }
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
        inputView?.setBackgroundColor(theme.getAttr(Theme.Attr.KEYBOARD_BACKGROUND).toSolidColor().color)
        inputView?.oneHandedCtrlPanelStart?.setBackgroundColor(theme.getAttr(Theme.Attr.ONE_HANDED_BACKGROUND).toSolidColor().color)
        inputView?.oneHandedCtrlPanelEnd?.setBackgroundColor(theme.getAttr(Theme.Attr.ONE_HANDED_BACKGROUND).toSolidColor().color)
        ColorStateList.valueOf(theme.getAttr(Theme.Attr.ONE_HANDED_FOREGROUND).toSolidColor().color).also {
            inputView?.oneHandedCtrlMoveStart?.imageTintList = it
            inputView?.oneHandedCtrlMoveEnd?.imageTintList = it
            inputView?.oneHandedCtrlCloseStart?.imageTintList = it
            inputView?.oneHandedCtrlCloseEnd?.imageTintList = it
        }
        eventListeners.toList().forEach { it?.get()?.onApplyThemeAttributes() }
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        val inputView = this.inputView ?: return
        val inputWindowView = this.inputWindowView ?: return
        // TODO: Check also if the keyboard is currently suppressed by a hardware keyboard
        if (!isInputViewShown) {
            outInsets?.contentTopInsets = inputWindowView.height
            outInsets?.visibleTopInsets = inputWindowView.height
            return
        }
        val visibleTopY = inputWindowView.height - inputView.measuredHeight
        outInsets?.contentTopInsets = visibleTopY
        outInsets?.visibleTopInsets = visibleTopY
    }

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        updateSoftInputWindowLayoutParameters()
    }

    /**
     * Updates the layout params of the window and input view.
     */
    private fun updateSoftInputWindowLayoutParameters() {
        val w = window?.window ?: return
        ViewLayoutUtils.updateLayoutHeightOf(w, WindowManager.LayoutParams.MATCH_PARENT)
        val inputWindowView = this.inputWindowView
        if (inputWindowView != null) {
            val layoutHeight = if (isFullscreenMode) {
                WindowManager.LayoutParams.WRAP_CONTENT
            } else {
                WindowManager.LayoutParams.MATCH_PARENT
            }
            val inputArea = w.findViewById<View>(android.R.id.inputArea)
            ViewLayoutUtils.updateLayoutHeightOf(inputArea, layoutHeight)
            ViewLayoutUtils.updateLayoutGravityOf(inputArea, Gravity.BOTTOM)
            ViewLayoutUtils.updateLayoutHeightOf(inputWindowView, layoutHeight)
        }
    }

    /**
     * Makes a key press vibration if the user has this feature enabled in the preferences.
     */
    fun keyPressVibrate() {
        if (prefs.keyboard.vibrationEnabled) {
            var vibrationStrength = prefs.keyboard.vibrationStrength
            if (vibrationStrength == -1 && prefs.keyboard.vibrationEnabledSystem) {
                vibrationStrength = 36
            }
            if (vibrationStrength > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(
                            vibrationStrength.toLong(), VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(vibrationStrength.toLong())
                }
            }
        }
    }

    /**
     * Makes a key press sound if the user has this feature enabled in the preferences.
     */
    fun keyPressSound(keyData: KeyData? = null) {
        if (prefs.keyboard.soundEnabled) {
            val soundVolume = prefs.keyboard.soundVolume
            val effect = when (keyData?.code) {
                KeyCode.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                KeyCode.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                KeyCode.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            if (soundVolume == -1 && prefs.keyboard.soundEnabledSystem) {
                audioManager!!.playSoundEffect(effect)
            } else if (soundVolume > 0) {
                audioManager!!.playSoundEffect(effect, soundVolume / 100f)
            }
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
     * Hides the IME and launches [SettingsMainActivity].
     */
    fun launchSettings() {
        requestHideSelf(0)
        val i = Intent(this, SettingsMainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                  Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                  Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
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
            Timber.e(e,"Unable to switch to the previous IME")
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
            Timber.e(e,"Unable to switch to the next IME")
            imeManager?.showInputMethodPicker()
        }
    }

    fun switchToPrevSubtype() {
        activeSubtype = subtypeManager.switchToPrevSubtype() ?: Subtype.DEFAULT
        onSubtypeChanged(activeSubtype)
    }

    fun switchToNextSubtype() {
        activeSubtype = subtypeManager.switchToNextSubtype() ?: Subtype.DEFAULT
        onSubtypeChanged(activeSubtype)
    }

    private fun onSubtypeChanged(newSubtype: Subtype) {
        textInputManager.onSubtypeChanged(newSubtype)
        mediaInputManager.onSubtypeChanged(newSubtype)
    }

    fun setActiveInput(type: Int) {
        when (type) {
            R.id.text_input -> {
                inputView?.mainViewFlipper?.displayedChild = 0
            }
            R.id.media_input -> {
                inputView?.mainViewFlipper?.displayedChild = 1
            }
        }
    }

    private fun initializeOneHandedEnvironment() {
        { v:View -> onOneHandedPanelButtonClick(v) }.also {
            inputView?.oneHandedCtrlMoveStart?.setOnClickListener(it)
            inputView?.oneHandedCtrlMoveEnd?.setOnClickListener(it)
            inputView?.oneHandedCtrlCloseStart?.setOnClickListener(it)
            inputView?.oneHandedCtrlCloseEnd?.setOnClickListener(it)
        }
    }

    private fun onOneHandedPanelButtonClick(v: View) {
        when (v.id) {
            R.id.one_handed_ctrl_move_start -> {
                prefs.keyboard.oneHandedMode = "start"
            }
            R.id.one_handed_ctrl_move_end -> {
                prefs.keyboard.oneHandedMode = "end"
            }
            R.id.one_handed_ctrl_close_start,
            R.id.one_handed_ctrl_close_end -> {
                prefs.keyboard.oneHandedMode = "off"
            }
        }
        updateOneHandedPanelVisibility()
    }

    fun toggleOneHandedMode() {
        when (prefs.keyboard.oneHandedMode) {
            "off" -> {
                prefs.keyboard.oneHandedMode = "end"
            }
            else -> {
                prefs.keyboard.oneHandedMode = "off"
            }
        }
        updateOneHandedPanelVisibility()
    }

    private fun updateOneHandedPanelVisibility() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            inputView?.oneHandedCtrlPanelStart?.visibility = View.GONE
            inputView?.oneHandedCtrlPanelEnd?.visibility = View.GONE
        } else {
            when (prefs.keyboard.oneHandedMode) {
                "off" -> {
                    inputView?.oneHandedCtrlPanelStart?.visibility = View.GONE
                    inputView?.oneHandedCtrlPanelEnd?.visibility = View.GONE
                }
                "start" -> {
                    inputView?.oneHandedCtrlPanelStart?.visibility = View.GONE
                    inputView?.oneHandedCtrlPanelEnd?.visibility = View.VISIBLE
                }
                "end" -> {
                    inputView?.oneHandedCtrlPanelStart?.visibility = View.VISIBLE
                    inputView?.oneHandedCtrlPanelEnd?.visibility = View.GONE
                }
            }
        }
        // Delay execution so this function can return, then refresh the whole layout
        osHandler.postDelayed({
            refreshLayoutOf(inputView)
        }, 0)
    }

    override fun onPrimaryClipChanged() {
        eventListeners.toList().forEach { it?.get()?.onPrimaryClipChanged() }
    }

    /**
     * Adds a given [listener] to the list which will receive FlorisBoard events.
     *
     * @param listener The listener object which receives the events.
     * @return True if the listener has been added successfully, false otherwise.
     */
    fun addEventListener(listener: EventListener): Boolean {
        return eventListeners.add(WeakReference(listener))
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
        eventListeners.toList().forEach {
            if (it?.get() == listener) {
                return eventListeners.remove(it)
            }
        }
        return false
    }

    interface EventListener {
        fun onCreate() {}
        fun onCreateInputView() {}
        fun onRegisterInputView(inputView: InputView) {}
        fun onDestroy() {}

        fun onStartInputView(instance: EditorInstance, restarting: Boolean) {}
        fun onFinishInputView(finishingInput: Boolean) {}

        fun onWindowShown() {}
        fun onWindowHidden() {}

        fun onUpdateSelection() {}

        fun onApplyThemeAttributes() {}
        fun onPrimaryClipChanged() {}
        fun onSubtypeChanged(newSubtype: Subtype) {}
    }

    /**
     * Data class which holds the base information for this IME. Matches the structure of
     * ime/config.json so it can be parsed. Used by [SubtypeManager] and by the prefs.
     * NOTE: this class and its corresponding json file is subject to change in future versions.
     * @property packageName The package name of this IME.
     * @property characterLayouts A map of valid layout names to use from. Each value defined
     *  should have a <layout_name>.json file in ime/text/characters/ to avoid empty layouts.
     *  The key is the layout name, the value is the layout label (string shown in UI).
     * @property defaultSubtypes A list of predefined default subtypes. This subtypes are used to
     *  define which locales are supported and which layout is preferred for that locale.
     * @property defaultSubtypesLanguageCodes Helper list for Settings Subtype Spinner elements.
     * @property defaultSubtypesLanguageNames Helper list for Settings Subtype Spinner elements.
     */
    data class ImeConfig(
        @Json(name = "package")
        val packageName: String,
        val characterLayouts: Map<String, String> = mapOf(),
        val defaultSubtypes: List<DefaultSubtype> = listOf()
    ) {
        val defaultSubtypesLanguageCodes: List<String>
        val defaultSubtypesLanguageNames: List<String>

        init {
            val tmpCodes = mutableListOf<String>()
            val tmpNames = mutableListOf<String>()
            for (defaultSubtype in defaultSubtypes) {
                tmpCodes.add(defaultSubtype.locale.toString())
                tmpNames.add(defaultSubtype.locale.displayName)
            }
            defaultSubtypesLanguageCodes = tmpCodes.toList()
            defaultSubtypesLanguageNames = tmpNames.toList()
        }
    }
}
