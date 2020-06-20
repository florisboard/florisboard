package dev.patrickgold.florisboard.ime.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.media.MediaInputManager
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import dev.patrickgold.florisboard.util.*
import java.util.*

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

    lateinit var activeSubtype: Subtype
    private var audioManager: AudioManager? = null
    val context: Context
        get() = inputView?.context ?: this
    private var currentThemeResId: Int = 0
    lateinit var prefs: PrefHelper
        private set
    private val osHandler = Handler()
    private var inputView: InputView? = null

    val textInputManager: TextInputManager
    val mediaInputManager: MediaInputManager

    init {
        florisboardInstance = this

        textInputManager = TextInputManager.getInstance()
        mediaInputManager = MediaInputManager.getInstance()
    }

    companion object {
        @Synchronized
        fun getInstance(): FlorisBoard {
            return florisboardInstance!!
        }
    }

    override fun onCreate() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onCreate()")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = PrefHelper(this, PreferenceManager.getDefaultSharedPreferences(this))
        prefs.initDefaultPreferences()
        prefs.sync()

        activeSubtype = prefs.keyboard.fetchActiveSubtype() ?: Subtype(-1, Locale.ENGLISH, "qwerty")

        currentThemeResId = prefs.theme.getSelectedThemeResId()
        setTheme(currentThemeResId)

        super.onCreate()
        textInputManager.onCreate()
        mediaInputManager.onCreate()
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View? {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onCreateInputView()")

        baseContext.setTheme(currentThemeResId)

        inputView = layoutInflater.inflate(R.layout.florisboard, null) as InputView

        textInputManager.onCreateInputView()
        mediaInputManager.onCreateInputView()

        return inputView
    }

    fun registerInputView(inputView: InputView) {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "registerInputView(inputView)")

        this.inputView = inputView
        initializeOneHandedEnvironment()
        updateSoftInputWindowLayoutParameters()
        updateOneHandedPanelVisibility()

        textInputManager.onRegisterInputView(inputView)
        mediaInputManager.onRegisterInputView(inputView)
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

        osHandler.removeCallbacksAndMessages(null)
        florisboardInstance = null

        super.onDestroy()
        textInputManager.onDestroy()
        mediaInputManager.onDestroy()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        currentInputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)

        super.onStartInputView(info, restarting)
        textInputManager.onStartInputView(info, restarting)
        mediaInputManager.onStartInputView(info, restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        currentInputConnection?.requestCursorUpdates(0)

        super.onFinishInputView(finishingInput)
        textInputManager.onFinishInputView(finishingInput)
        mediaInputManager.onFinishInputView(finishingInput)
    }

    override fun onWindowShown() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onWindowShown()")

        prefs.sync()
        updateThemeIfNecessary()
        updateOneHandedPanelVisibility()
        activeSubtype = prefs.keyboard.fetchActiveSubtype() ?: Subtype(-1, Locale.ENGLISH, "qwerty")
        onSubtypeChanged(activeSubtype)
        setActiveInput(R.id.text_input)

        super.onWindowShown()
        textInputManager.onWindowShown()
        mediaInputManager.onWindowShown()
    }

    override fun onWindowHidden() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onWindowHidden()")

        super.onWindowHidden()
        textInputManager.onWindowHidden()
        mediaInputManager.onWindowHidden()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (isInputViewShown) {
            updateOneHandedPanelVisibility()
        }

        super.onConfigurationChanged(newConfig)
        textInputManager.onConfigurationChanged(newConfig)
        mediaInputManager.onConfigurationChanged(newConfig)
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        textInputManager.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        mediaInputManager.onUpdateCursorAnchorInfo(cursorAnchorInfo)
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
        textInputManager.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
        mediaInputManager.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
    }

    /**
     * Checks the preferences if the selected theme res id has changed and updates the theme only
     * then by rebuilding the UI and setting the navigation bar theme manually.
     */
    private fun updateThemeIfNecessary() {
        val newThemeResId = prefs.theme.getSelectedThemeResId()
        if (newThemeResId != currentThemeResId) {
            currentThemeResId = newThemeResId
            setInputView(onCreateInputView())
            val w = window?.window ?: return
            w.navigationBarColor = getColorFromAttr(baseContext, android.R.attr.navigationBarColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                var flags = w.decorView.systemUiVisibility
                flags = if (getBooleanFromAttr(baseContext, android.R.attr.windowLightNavigationBar)) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
                w.decorView.systemUiVisibility = flags
            }
        }
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
        if (prefs.looknfeel.vibrationEnabled) {
            var vibrationStrength = prefs.looknfeel.vibrationStrength
            if (vibrationStrength == 0 && prefs.looknfeel.vibrationEnabledSystem) {
                vibrationStrength = 36
            }
            if (vibrationStrength > 0) {
                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(
                        VibrationEffect.createOneShot(
                            vibrationStrength.toLong(), VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(vibrationStrength.toLong())
                }
            }
        }
    }

    /**
     * Makes a key press sound if the user has this feature enabled in the preferences.
     */
    fun keyPressSound(keyData: KeyData? = null) {
        if (prefs.looknfeel.soundEnabled) {
            val soundVolume = prefs.looknfeel.soundVolume
            val effect = when (keyData?.code) {
                KeyCode.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                KeyCode.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                KeyCode.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            if (soundVolume == 0 && prefs.looknfeel.soundEnabledSystem) {
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
     * @return If the language switch should be shown
     */
    fun shouldShowLanguageSwitch(): Boolean {
        return prefs.keyboard.subtypes.size > 1
    }

    fun switchToNextSubtype() {
        activeSubtype = prefs.keyboard.switchToNextSubtype() ?: Subtype(-1, Locale.ENGLISH, "qwerty")
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
                prefs.looknfeel.oneHandedMode = "start"
            }
            R.id.one_handed_ctrl_move_end -> {
                prefs.looknfeel.oneHandedMode = "end"
            }
            R.id.one_handed_ctrl_close_start,
            R.id.one_handed_ctrl_close_end -> {
                prefs.looknfeel.oneHandedMode = "off"
            }
        }
        updateOneHandedPanelVisibility()
    }

    fun updateOneHandedPanelVisibility() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            inputView?.oneHandedCtrlPanelStart?.visibility = View.GONE
            inputView?.oneHandedCtrlPanelEnd?.visibility = View.GONE
        } else {
            when (prefs.looknfeel.oneHandedMode) {
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

    data class Subtype(
        var id: Int,
        var locale: Locale,
        var layoutName: String
    ) {
        companion object {
            fun fromString(string: String): Subtype {
                val data = string.split("/")
                if (data.size != 3) {
                    throw Exception("Given string is malformed...")
                } else {
                    val locale = LocaleUtils.stringToLocale(data[1])
                    return Subtype(data[0].toInt(), locale, data[2])
                }
            }
        }

        override fun toString(): String {
            return "$id/$locale/$layoutName"
        }
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

        fun onConfigurationChanged(newConfig: Configuration) {}

        fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {}
        fun onUpdateSelection(
            oldSelStart: Int,
            oldSelEnd: Int,
            newSelStart: Int,
            newSelEnd: Int,
            candidatesStart: Int,
            candidatesEnd: Int
        ) {}

        fun onSubtypeChanged(newSubtype: Subtype) {}
    }
}
