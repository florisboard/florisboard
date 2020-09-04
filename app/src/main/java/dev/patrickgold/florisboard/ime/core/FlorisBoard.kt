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
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.ImageButton
import android.widget.LinearLayout
import com.squareup.moshi.Json
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.media.MediaInputManager
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import dev.patrickgold.florisboard.util.*

/**
 * Variable which holds the current [FlorisBoard] instance. To get this instance from another
 * package, see [FlorisBoard.getInstance].
 */
private var florisboardInstance: FlorisBoard? = null

/**
 * Core class responsible to link together both the text and media input managers as well as
 * managing the one-handed UI.
 */
class FlorisBoard : InputMethodService() {
    lateinit var prefs: PrefHelper
        private set

    val context: Context
        get() = inputView?.context ?: this
    private var inputView: InputView? = null
    private var eventListeners: MutableList<EventListener> = mutableListOf()

    private var audioManager: AudioManager? = null
    var clipboardManager: ClipboardManager? = null
    private var vibrator: Vibrator? = null
    private val osHandler = Handler()

    lateinit var subtypeManager: SubtypeManager
    lateinit var activeSubtype: Subtype
    private var currentThemeResId: Int = 0

    val textInputManager: TextInputManager
    val mediaInputManager: MediaInputManager

    init {
        florisboardInstance = this

        textInputManager = TextInputManager.getInstance()
        mediaInputManager = MediaInputManager.getInstance()
    }

    companion object {
        private const val IME_ID: String = "dev.patrickgold.florisboard/.ime.core.FlorisBoard"

        fun checkIfImeIsEnabled(context: Context): Boolean {
            val activeImeIds = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_INPUT_METHODS
            )
            if (BuildConfig.DEBUG) Log.i(FlorisBoard::class.simpleName, "List of active IMEs: $activeImeIds")
            return activeImeIds.split(":").contains(IME_ID)
        }

        fun checkIfImeIsSelected(context: Context): Boolean {
            val selectedImeId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            if (BuildConfig.DEBUG) Log.i(FlorisBoard::class.simpleName, "Selected IME: $selectedImeId")
            return selectedImeId == IME_ID
        }

        @Synchronized
        fun getInstance(): FlorisBoard {
            return florisboardInstance!!
        }

        @Synchronized
        fun getInstanceOrNull(): FlorisBoard? {
            return florisboardInstance
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
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onCreate()")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        prefs = PrefHelper.getDefaultInstance(this)
        prefs.initDefaultPreferences()
        prefs.sync()
        subtypeManager = SubtypeManager(this, prefs)
        activeSubtype = subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT

        currentThemeResId = prefs.theme.getSelectedThemeResId()
        setTheme(currentThemeResId)
        updateTheme()

        AppVersionUtils.updateVersionOnInstallAndLastUse(this, prefs)

        super.onCreate()
        eventListeners.forEach { it.onCreate() }
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View? {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onCreateInputView()")

        baseContext.setTheme(currentThemeResId)

        inputView = layoutInflater.inflate(R.layout.florisboard, null) as InputView

        eventListeners.forEach { it.onCreateInputView() }

        return inputView
    }

    fun registerInputView(inputView: InputView) {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "registerInputView(inputView)")

        this.inputView = inputView
        initializeOneHandedEnvironment()
        updateTheme()
        updateSoftInputWindowLayoutParameters()
        updateOneHandedPanelVisibility()

        eventListeners.forEach { it.onRegisterInputView(inputView) }
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

        osHandler.removeCallbacksAndMessages(null)
        florisboardInstance = null

        eventListeners.forEach { it.onDestroy() }
        eventListeners.clear()
        super.onDestroy()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        currentInputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)

        super.onStartInputView(info, restarting)
        eventListeners.forEach { it.onStartInputView(info, restarting) }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        currentInputConnection?.requestCursorUpdates(0)

        super.onFinishInputView(finishingInput)
        eventListeners.forEach { it.onFinishInputView(finishingInput) }
    }

    override fun onWindowShown() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onWindowShown()")

        prefs.sync()
        updateTheme()
        updateOneHandedPanelVisibility()
        activeSubtype = subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT
        onSubtypeChanged(activeSubtype)
        setActiveInput(R.id.text_input)

        super.onWindowShown()
        eventListeners.forEach { it.onWindowShown() }
    }

    override fun onWindowHidden() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onWindowHidden()")

        super.onWindowHidden()
        eventListeners.forEach { it.onWindowHidden() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (isInputViewShown) {
            updateOneHandedPanelVisibility()
        }

        super.onConfigurationChanged(newConfig)
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        eventListeners.forEach { it.onUpdateCursorAnchorInfo(cursorAnchorInfo) }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
        eventListeners.forEach {
            it.onUpdateSelection(
                oldSelStart,
                oldSelEnd,
                newSelStart,
                newSelEnd,
                candidatesStart,
                candidatesEnd
            )
        }
    }

    /**
     * Reapplies the supplies colors and settings from prefs to navigation bar.
     */
    private fun updateTheme() {
        val w = window?.window ?: return
        inputView?.mainViewFlipper?.setBackgroundColor(prefs.theme.keyboardBgColor)
        w.navigationBarColor = prefs.theme.navBarColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            var flags = w.decorView.systemUiVisibility
            flags = if (prefs.theme.navBarIsLight) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            w.decorView.systemUiVisibility = flags
        }
        eventListeners.forEach { it.onApplyThemeAttributes() }
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        val inputView = this.inputView ?: return
        // TODO: Check also if the keyboard is currently suppressed by a hardware keyboard
        if (!isInputViewShown) {
            outInsets?.contentTopInsets = inputView.height
            outInsets?.visibleTopInsets = inputView.height
            return
        }
        val innerInputViewContainer =
            inputView.findViewById<LinearLayout>(R.id.inner_input_view_container) ?: return
        val visibleTopY = inputView.height - innerInputViewContainer.measuredHeight
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
        val inputView = this.inputView
        if (inputView != null) {
            val layoutHeight = if (isFullscreenMode) {
                WindowManager.LayoutParams.WRAP_CONTENT
            } else {
                WindowManager.LayoutParams.MATCH_PARENT
            }
            val inputArea = w.findViewById<View>(android.R.id.inputArea)
            ViewLayoutUtils.updateLayoutHeightOf(inputArea, layoutHeight)
            ViewLayoutUtils.updateLayoutGravityOf(inputArea, Gravity.BOTTOM)
            ViewLayoutUtils.updateLayoutHeightOf(inputView, layoutHeight)
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
                inputView?.mainViewFlipper?.displayedChild =
                    inputView?.mainViewFlipper?.indexOfChild(textInputManager.textViewGroup) ?: 0
            }
            R.id.media_input -> {
                inputView?.mainViewFlipper?.displayedChild =
                    inputView?.mainViewFlipper?.indexOfChild(mediaInputManager.mediaViewGroup) ?: 0
            }
        }
    }

    private fun initializeOneHandedEnvironment() {
        inputView?.findViewById<ImageButton>(R.id.one_handed_ctrl_move_start)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        inputView?.findViewById<ImageButton>(R.id.one_handed_ctrl_move_end)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        inputView?.findViewById<ImageButton>(R.id.one_handed_ctrl_close_start)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        inputView?.findViewById<ImageButton>(R.id.one_handed_ctrl_close_end)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
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

    /**
     * Adds a given [listener] to the list which will receive FlorisBoard events.
     *
     * @param listener The listener object which receives the events.
     * @returns True if the listener has been added successfully, false otherwise.
     */
    fun addEventListener(listener: EventListener): Boolean {
        return eventListeners.add(listener)
    }

    /**
     * Removes a given [listener] from the list which will receive FlorisBoard events.
     *
     * @param listener The same listener object which was used in [addEventListener].
     * @returns True if the listener has been removed successfully, false otherwise. A false return
     *  value may also indicate that the [listener] was not added previously.
     */
    fun removeEventListener(listener: EventListener): Boolean {
        return eventListeners.remove(listener)
    }

    interface EventListener {
        fun onCreate() {}
        fun onCreateInputView() {}
        fun onRegisterInputView(inputView: InputView) {}
        fun onDestroy() {}

        fun onStartInputView(info: EditorInfo?, restarting: Boolean) {}
        fun onFinishInputView(finishingInput: Boolean) {}

        fun onWindowShown() {}
        fun onWindowHidden() {}

        fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {}
        fun onUpdateSelection(
            oldSelStart: Int,
            oldSelEnd: Int,
            newSelStart: Int,
            newSelEnd: Int,
            candidatesStart: Int,
            candidatesEnd: Int
        ) {}

        fun onApplyThemeAttributes() {}
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
