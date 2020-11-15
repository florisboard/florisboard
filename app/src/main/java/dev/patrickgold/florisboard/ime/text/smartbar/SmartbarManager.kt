package dev.patrickgold.florisboard.ime.text.smartbar

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.children
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// TODO: Implement suggestion creation functionality
// TODO: Cleanup and reorganize SmartbarManager
class SmartbarManager private constructor() : CoroutineScope by MainScope(),
    FlorisBoard.EventListener {

    private val florisboard: FlorisBoard = FlorisBoard.getInstance()
    private var isComposingEnabled: Boolean = false
    private val textInputManager: TextInputManager = TextInputManager.getInstance()
    private var smartbarView: SmartbarView? = null

    var isQuickActionsVisible: Boolean = false
        set(value) { field = value; updateActiveContainerVisibility() }

    private val candidateViewOnClickListener = View.OnClickListener { v ->
        val view = v as Button
        val text = view.text.toString()
        if (text.isNotEmpty()) {
            florisboard.activeEditorInstance.commitCompletion(text)
        }
    }
    private val candidateViewOnLongClickListener = View.OnLongClickListener { v ->
        true
    }
    private val keyButtonOnClickListener = View.OnClickListener { v ->
        val keyData = when (v.id) {
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
        launch(Dispatchers.Default) {
            val numberRow = smartbarView.findViewById<KeyboardView>(R.id.smartbar_variant_number_row)
            numberRow.isSmartbarKeyboardView = true
            numberRow.computedLayout = textInputManager.layoutManager.fetchComputedLayoutAsync(KeyboardMode.NUMBER_ROW, Subtype.DEFAULT).await()
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

    override fun onUpdateSelection() {
        val ei = florisboard.activeEditorInstance
        val isSelectionActive = ei.selection.isSelectionMode
        smartbarView?.findViewById<View>(R.id.cc_select_all)?.isEnabled = !ei.isRawInputEditor
        smartbarView?.findViewById<View>(R.id.cc_cut)?.isEnabled = isSelectionActive && !ei.isRawInputEditor
        smartbarView?.findViewById<View>(R.id.cc_copy)?.isEnabled = isSelectionActive && !ei.isRawInputEditor
        smartbarView?.findViewById<View>(R.id.cc_paste)?.isEnabled =
            florisboard.clipboardManager?.hasPrimaryClip() ?: false
        smartbarView?.invalidate()
    }

    fun deleteCandidateFromDictionary(candidate: String) {
        //
    }

    fun resetCandidates() {
        //
    }

    fun generateCandidatesFromComposing(composingText: String) {
        val smartbarView = smartbarView ?: return

        if (composingText == "") {
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
            smartbarView.setActiveVariant(R.id.smartbar_variant_default)
            smartbarView.setActiveContainer(R.id.quick_actions)
            smartbarView.findViewById<View>(R.id.quick_action_toggle)?.rotation = -180.0f
        } else {
            when {
                isComposingEnabled -> {
                    smartbarView.setActiveVariant(R.id.smartbar_variant_default)
                    smartbarView.setActiveContainer(R.id.candidates)
                }
                textInputManager.getActiveKeyboardMode() == KeyboardMode.CHARACTERS -> {
                    when (florisboard.prefs.suggestion.showInstead) {
                        "number_row" -> {
                            smartbarView.setActiveVariant(R.id.smartbar_variant_number_row)
                            smartbarView.setActiveContainer(null)
                        }
                        "clipboard_cursor_tools" -> {
                            smartbarView.setActiveVariant(R.id.smartbar_variant_default)
                            smartbarView.setActiveContainer(R.id.clipboard_cursor_row)
                        }
                        else -> {
                            smartbarView.setActiveVariant(null)
                            smartbarView.setActiveContainer(null)
                        }
                    }
                }
                else -> {
                    smartbarView.setActiveVariant(null)
                    smartbarView.setActiveContainer(null)
                }
            }
            smartbarView.findViewById<View>(R.id.quick_action_toggle)?.rotation = 0.0f
        }
    }
}
