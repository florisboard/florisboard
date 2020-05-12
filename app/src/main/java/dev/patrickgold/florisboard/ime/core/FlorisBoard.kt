package dev.patrickgold.florisboard.ime.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.media.MediaInputManager
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.util.initDefaultPreferences

/**
 * Core class responsible to link together both the text and media input managers as well as
 * managing the one-handed UI.
 */
class FlorisBoard : InputMethodService() {

    private var oneHandedCtrlPanelStart: LinearLayout? = null
    private var oneHandedCtrlPanelEnd: LinearLayout? = null

    var audioManager: AudioManager? = null
        private set
    val context: Context
        get() = rootViewGroup?.context ?: this
    var prefs: PrefHelper? = null
        private set
    var rootViewGroup: LinearLayout? = null
        private set

    val textInputManager: TextInputManager = TextInputManager(this)
    val mediaInputManager: MediaInputManager = MediaInputManager(this)

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View? {
        // Set default preference values if user has not used preferences screen
        initDefaultPreferences(this)

        rootViewGroup = layoutInflater.inflate(R.layout.florisboard, null) as LinearLayout

        prefs = PrefHelper(this, PreferenceManager.getDefaultSharedPreferences(this))
        prefs!!.sync()

        initializeOneHandedEnvironment()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        textInputManager.onCreateInputView()
        mediaInputManager.onCreateInputView()

        return rootViewGroup
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        textInputManager.onStartInputView(info, restarting)
        mediaInputManager.onStartInputView(info, restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        textInputManager.onFinishInputView(finishingInput)
        mediaInputManager.onFinishInputView(finishingInput)
    }

    override fun onWindowShown() {
        prefs!!.sync()
        updateOneHandedPanelVisibility()

        super.onWindowShown()
        textInputManager.onWindowShown()
        mediaInputManager.onWindowShown()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        textInputManager.onWindowHidden()
        mediaInputManager.onWindowHidden()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        updateOneHandedPanelVisibility()

        super.onConfigurationChanged(newConfig)
        textInputManager.onConfigurationChanged(newConfig)
        mediaInputManager.onConfigurationChanged(newConfig)
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
                textInputManager.show()
                mediaInputManager.hide()
            }
            R.id.media_input -> {
                textInputManager.hide()
                mediaInputManager.show()
            }
        }
    }

    private fun initializeOneHandedEnvironment() {
        oneHandedCtrlPanelStart = rootViewGroup?.findViewById(R.id.one_handed_ctrl_panel_start)
        oneHandedCtrlPanelEnd = rootViewGroup?.findViewById(R.id.one_handed_ctrl_panel_end)

        rootViewGroup?.findViewById<ImageButton>(R.id.one_handed_ctrl_move_start)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        rootViewGroup?.findViewById<ImageButton>(R.id.one_handed_ctrl_move_end)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        rootViewGroup?.findViewById<ImageButton>(R.id.one_handed_ctrl_close_start)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
        rootViewGroup?.findViewById<ImageButton>(R.id.one_handed_ctrl_close_end)
            ?.setOnClickListener { v -> onOneHandedPanelButtonClick(v) }
    }

    private fun onOneHandedPanelButtonClick(v: View) {
        when (v.id) {
            R.id.one_handed_ctrl_move_start -> {
                prefs?.oneHandedMode = "start"
            }
            R.id.one_handed_ctrl_move_end -> {
                prefs?.oneHandedMode = "end"
            }
            R.id.one_handed_ctrl_close_start,
            R.id.one_handed_ctrl_close_end -> {
                prefs?.oneHandedMode = "off"
            }
        }
        updateOneHandedPanelVisibility()
    }

    fun updateOneHandedPanelVisibility() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            oneHandedCtrlPanelStart?.visibility = View.GONE
            oneHandedCtrlPanelEnd?.visibility = View.GONE
        } else {
            when (prefs?.oneHandedMode) {
                "off" -> {
                    oneHandedCtrlPanelStart?.visibility = View.GONE
                    oneHandedCtrlPanelEnd?.visibility = View.GONE
                }
                "start" -> {
                    oneHandedCtrlPanelStart?.visibility = View.GONE
                    oneHandedCtrlPanelEnd?.visibility = View.VISIBLE
                }
                "end" -> {
                    oneHandedCtrlPanelStart?.visibility = View.VISIBLE
                    oneHandedCtrlPanelEnd?.visibility = View.GONE
                }
            }
        }
    }

    interface EventListener {
        fun onCreateInputView()

        fun onStartInputView(info: EditorInfo?, restarting: Boolean) {}
        fun onFinishInputView(finishingInput: Boolean) {}

        fun onWindowShown() {}
        fun onWindowHidden() {}

        fun onConfigurationChanged(newConfig: Configuration) {}
    }
}
