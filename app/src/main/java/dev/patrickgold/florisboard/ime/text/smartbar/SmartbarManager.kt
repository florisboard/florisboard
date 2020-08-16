package dev.patrickgold.florisboard.ime.text.smartbar

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CursorAnchorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.children
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode

// TODO: Implement suggestion creation functionality
// TODO: Cleanup and reorganize SmartbarManager
class SmartbarManager private constructor() : FlorisBoard.EventListener {

    private val florisboard: FlorisBoard = FlorisBoard.getInstance()
    private var isComposingEnabled: Boolean = false
    private val textInputManager: TextInputManager = TextInputManager.getInstance()
    var smartbarView: SmartbarView? = null
        private set

    var isQuickActionsVisible: Boolean = false
        set(value) { field = value; updateActiveContainerVisibility() }

    private val candidateViewOnClickListener = View.OnClickListener { v ->
        val view = v as Button
        val text = view.text.toString()
        if (text.isNotEmpty()) {
            textInputManager.commitCandidate(text)
        }
    }
    private val candidateViewOnLongClickListener = View.OnLongClickListener { v ->
        true
    }
    private val keyButtonOnClickListener = View.OnClickListener { v ->
        val keyData = when (v.id) {
            R.id.number_row_0 -> KeyData(48, "0")
            R.id.number_row_1 -> KeyData(49, "1")
            R.id.number_row_2 -> KeyData(50, "2")
            R.id.number_row_3 -> KeyData(51, "3")
            R.id.number_row_4 -> KeyData(52, "4")
            R.id.number_row_5 -> KeyData(53, "5")
            R.id.number_row_6 -> KeyData(54, "6")
            R.id.number_row_7 -> KeyData(55, "7")
            R.id.number_row_8 -> KeyData(56, "8")
            R.id.number_row_9 -> KeyData(57, "9")
            R.id.cc_select_all -> KeyData(KeyCode.CLIPBOARD_SELECT_ALL)
            R.id.cc_copy -> KeyData(KeyCode.CLIPBOARD_COPY)
            R.id.cc_arrow_left -> KeyData(KeyCode.ARROW_LEFT)
            R.id.cc_arrow_right -> KeyData(KeyCode.ARROW_RIGHT)
            R.id.cc_cut -> KeyData(KeyCode.CLIPBOARD_CUT)
            R.id.cc_paste -> KeyData(KeyCode.CLIPBOARD_PASTE)
            else -> KeyData(0)
        }
        florisboard.textInputManager.sendKeyPress(keyData)
    }
    private val quickActionOnClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.back_button -> {
                florisboard.textInputManager.setActiveKeyboardMode(KeyboardMode.CHARACTERS)
                smartbarView?.setActiveVariant(R.id.smartbar_variant_default)
            }
            R.id.quick_action_switch_to_editing_context -> {
                if (florisboard.textInputManager.getActiveKeyboardMode() == KeyboardMode.EDITING) {
                    florisboard.textInputManager.setActiveKeyboardMode(KeyboardMode.CHARACTERS)
                    smartbarView?.setActiveVariant(R.id.smartbar_variant_default)
                } else {
                    florisboard.textInputManager.setActiveKeyboardMode(KeyboardMode.EDITING)
                    smartbarView?.setActiveVariant(R.id.smartbar_variant_back_only)
                }
            }
            R.id.quick_action_switch_to_media_context -> florisboard.setActiveInput(R.id.media_input)
            R.id.quick_action_open_settings -> florisboard.launchSettings()
            R.id.quick_action_one_handed_toggle -> florisboard.toggleOneHandedMode()
            else -> return@OnClickListener
        }
        isQuickActionsVisible = false
    }
    private val quickActionToggleOnClickListener = View.OnClickListener {
        isQuickActionsVisible = !isQuickActionsVisible
    }

    companion object {
        private var instance: SmartbarManager? = null

        @Synchronized
        fun getInstance(): SmartbarManager {
            if (instance == null) {
                instance = SmartbarManager()
            }
            return instance!!
        }
    }

    fun registerSmartbarView(smartbarView: SmartbarView) {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "registerSmartbarView(smartbarView)")

        this.smartbarView = smartbarView

        smartbarView.findViewById<View>(R.id.quick_action_toggle)?.setOnClickListener(quickActionToggleOnClickListener)
        val quickActions = smartbarView.findViewById<LinearLayout>(R.id.quick_actions)
        for (quickAction in quickActions.children) {
            if (quickAction is ImageButton) {
                quickAction.setOnClickListener(quickActionOnClickListener)
            }
        }
        val numberRow = smartbarView.findViewById<LinearLayout>(R.id.number_row)
        for (numberRowButton in numberRow.children) {
            if (numberRowButton is Button) {
                numberRowButton.setOnClickListener(keyButtonOnClickListener)
            }
        }
        val clipboardCursorRow = smartbarView.findViewById<ViewGroup>(R.id.clipboard_cursor_row)
        for (clipboardCursorRowButton in clipboardCursorRow.children) {
            if (clipboardCursorRowButton is ImageButton) {
                clipboardCursorRowButton.setOnClickListener(keyButtonOnClickListener)
            }
        }
        val backButton = smartbarView.findViewById<View>(R.id.back_button)
        backButton.setOnClickListener(quickActionOnClickListener)
        for (candidateView in smartbarView.candidateViewList) {
            candidateView.setOnClickListener(candidateViewOnClickListener)
            candidateView.setOnLongClickListener(candidateViewOnLongClickListener)
        }

        smartbarView.setActiveVariant(R.id.smartbar_variant_default)
    }

    override fun onWindowShown() {
        isQuickActionsVisible = false
    }

    // TODO: clean up resources here
    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

        instance = null
    }

    fun onStartInputView(keyboardMode: KeyboardMode, isComposingEnabled: Boolean) {
        this.isComposingEnabled = isComposingEnabled
        when (keyboardMode) {
            KeyboardMode.NUMERIC, KeyboardMode.PHONE, KeyboardMode.PHONE2 -> {
                smartbarView?.setActiveVariant(null)
            }
            else -> {
                smartbarView?.setActiveVariant(R.id.smartbar_variant_default)
                isQuickActionsVisible = false
            }
        }
    }

    fun onFinishInputView() {
        //spellCheckerSession?.close()
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        val isSelectionActive = florisboard.textInputManager.isTextSelected
        smartbarView?.findViewById<View>(R.id.cc_cut)?.isEnabled = isSelectionActive
        smartbarView?.findViewById<View>(R.id.cc_copy)?.isEnabled = isSelectionActive
        smartbarView?.findViewById<View>(R.id.cc_paste)?.isEnabled =
            florisboard.clipboardManager?.hasPrimaryClip() ?: false
    }

    fun deleteCandidateFromDictionary(candidate: String) {
        //
    }

    fun resetCandidates() {
        //
    }

    fun generateCandidatesFromComposing(composingText: String?) {
        val smartbarView = smartbarView ?: return

        if (composingText == null) {
            smartbarView.candidateViewList[0].text = "candidate"
            smartbarView.candidateViewList[1].text = "suggestions"
            smartbarView.candidateViewList[2].text = "nyi"
        } else {
            smartbarView.candidateViewList[0].text = ""
            smartbarView.candidateViewList[1].text = composingText + "test"
            smartbarView.candidateViewList[2].text = ""
        }
        //spellCheckerSession?.getSentenceSuggestions(arrayOf(TextInfo(composing)), 3)
        //android.util.Log.i("SPELL", "GEN")
        /*val dic: Uri = UserDictionary.Words.CONTENT_URI
        val resolver: ContentResolver = florisboard.contentResolver
        val cursor: Cursor = resolver.query(dic, null, null, null, null) ?: return
        var count = 0
        while (cursor.moveToNext()) {
            val word = cursor.getString(cursor.getColumnIndex(UserDictionary.Words.WORD))
            candidateViewList[count].text = word
            if (count++ > 2) {
                break
            }
        }
        cursor.close()*/
    }

    fun writeCandidate(candidate: String) {
        //
    }

    private fun updateActiveContainerVisibility() {
        val smartbarView = smartbarView ?: return

        if (isQuickActionsVisible) {
            smartbarView.setActiveContainer(R.id.quick_actions)
            smartbarView.findViewById<View>(R.id.quick_action_toggle)?.rotation = -180.0f
        } else {
            if (isComposingEnabled) {
                smartbarView.setActiveContainer(R.id.candidates)
            } else if (textInputManager.getActiveKeyboardMode() == KeyboardMode.CHARACTERS) {
                smartbarView.setActiveContainer(when (florisboard.prefs.suggestion.showInstead) {
                    "number_row" -> R.id.number_row
                    "clipboard_cursor_tools" -> R.id.clipboard_cursor_row
                    else -> null
                })
            } else {
                smartbarView.setActiveContainer(null)
            }
            smartbarView.findViewById<View>(R.id.quick_action_toggle)?.rotation = 0.0f
        }
    }
}
