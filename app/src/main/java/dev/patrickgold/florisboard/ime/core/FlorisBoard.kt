package dev.patrickgold.florisboard.ime.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.ImageButton
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.media.MediaInputManager
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.settings.SettingsMainActivity

/**
 * Variable which holds the current [FlorisBoard] instance. To get this instance from another
 * package, see [FlorisBoard.getInstance].
 */
private var florisboardInstance: FlorisBoard? = null

/**
 * Core class responsible to link together both the text and media input managers as well as
 * managing the one-handed UI.
 *
 * TODO: fix layout issue when turning one-handed mode on or off
 */
class FlorisBoard : InputMethodService() {

    private var audioManager: AudioManager? = null
    val context: Context
        get() = inputView.context
    private var currentThemeResId: Int = 0
    lateinit var prefs: PrefHelper
        private set
    private lateinit var inputView: InputView

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
        updateOneHandedPanelVisibility()

        textInputManager.onRegisterInputView(inputView)
        mediaInputManager.onRegisterInputView(inputView)
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

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
        updateOneHandedPanelVisibility()
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

    /*fun applyTheme(theme: Int, shouldRecreateInputView: Boolean = true) {
        if (shouldRecreateInputView) {
            setInputView(onCreateInputView())
        } else {
            if (currentThemeResId == theme) {
                return
            }
            currentThemeResId = theme
            baseContext.setTheme(theme)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val w = window?.window ?: return
                w.navigationBarColor = getColorFromAttr(baseContext, android.R.attr.navigationBarColor)
                val flags = w.decorView.systemUiVisibility
                val isLight = getBooleanFromAttr(baseContext, android.R.attr.windowLightNavigationBar)
                if (isLight) {
                    w.decorView.systemUiVisibility = flags
                }
            }
        }
    }*/

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
     * TODO: evaluate the boolean based on the language prefs
     * @return If the language switch should be shown
     */
    fun shouldShowLanguageSwitch(): Boolean {
        return false
    }

    /*override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (!isFullscreenMode && outInsets != null) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets
        }
    }*/

    fun setActiveInput(type: Int) {
        when (type) {
            R.id.text_input -> {
                inputView.mainViewFlipper?.displayedChild =
                    inputView.mainViewFlipper?.indexOfChild(textInputManager.textViewGroup) ?: 0
            }
            R.id.media_input -> {
                inputView.mainViewFlipper?.displayedChild =
                    inputView.mainViewFlipper?.indexOfChild(mediaInputManager.mediaViewGroup) ?: 0
            }
        }
    }

    private fun initializeOneHandedEnvironment() {
        inputView.findViewById<ImageButton>(R.id.one_handed_ctrl_move_start)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        inputView.findViewById<ImageButton>(R.id.one_handed_ctrl_move_end)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        inputView.findViewById<ImageButton>(R.id.one_handed_ctrl_close_start)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        inputView.findViewById<ImageButton>(R.id.one_handed_ctrl_close_end)
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
            inputView.oneHandedCtrlPanelStart?.visibility = View.GONE
            inputView.oneHandedCtrlPanelEnd?.visibility = View.GONE
        } else {
            when (prefs.looknfeel.oneHandedMode) {
                "off" -> {
                    inputView.oneHandedCtrlPanelStart?.visibility = View.GONE
                    inputView.oneHandedCtrlPanelEnd?.visibility = View.GONE
                }
                "start" -> {
                    inputView.oneHandedCtrlPanelStart?.visibility = View.GONE
                    inputView.oneHandedCtrlPanelEnd?.visibility = View.VISIBLE
                }
                "end" -> {
                    inputView.oneHandedCtrlPanelStart?.visibility = View.VISIBLE
                    inputView.oneHandedCtrlPanelEnd?.visibility = View.GONE
                }
            }
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
    }
}
