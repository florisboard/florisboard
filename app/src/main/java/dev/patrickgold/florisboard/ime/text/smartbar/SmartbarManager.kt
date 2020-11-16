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
import dev.patrickgold.florisboard.ime.core.EditorInstance
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
    private val activeEditorInstance: EditorInstance
        get() = florisboard.activeEditorInstance

    private val textInputManager: TextInputManager = TextInputManager.getInstance()
    private var shouldSuggestClipboardContents: Boolean = false
    private var smartbarView: SmartbarView? = null

    var isQuickActionsVisible: Boolean = false
        //set(value) { field = value; updateActiveContainerVisibility() }

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
    private val clipboardSuggestionViewOnClickListener = View.OnClickListener { v ->
        val view = v as Button
        val text = view.text.toString()
        if (text.isNotEmpty()) {
            florisboard.activeEditorInstance.commitText(text)
            shouldSuggestClipboardContents = false
        }
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
        updateSmartbarUI()
    }
    private val quickActionToggleOnClickListener = View.OnClickListener {
        isQuickActionsVisible = !isQuickActionsVisible
        updateSmartbarUI()
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
        val clipboardSuggestion = smartbarView.findViewById<Button>(R.id.clipboard_suggestion)
        clipboardSuggestion.setOnClickListener(clipboardSuggestionViewOnClickListener)
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

        updateSmartbarUI()
    }

    override fun onWindowShown() {
        isQuickActionsVisible = false
        updateActiveContainerVisibility()
    }

    // TODO: clean up resources here
    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

        instance = null
    }

    fun onStartInputView(keyboardMode: KeyboardMode) {
        when (keyboardMode) {
            KeyboardMode.NUMERIC, KeyboardMode.PHONE, KeyboardMode.PHONE2 -> {
                smartbarView?.setActiveVariant(null)
            }
            else -> {
                smartbarView?.setActiveVariant(R.id.smartbar_variant_default)
                isQuickActionsVisible = false
            }
        }
        updateSmartbarUI()
    }

    fun onFinishInputView() {
        //spellCheckerSession?.close()
    }

    override fun onUpdateSelection() {
        shouldSuggestClipboardContents = false
        updateSmartbarUI()
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
    }

    override fun onPrimaryClipChanged() {
        shouldSuggestClipboardContents = true
        updateActiveContainerVisibility()
    }

    private fun updateSmartbarUI() {
        val ei = activeEditorInstance
        val isCursorMode = ei.selection.isCursorMode
        val isSelectionMode = ei.selection.isSelectionMode
        if (isCursorMode && ei.isComposingEnabled) {
            generateCandidatesFromComposing(ei.currentWord.text)
        }
        updateActiveContainerVisibility()
        smartbarView?.findViewById<View>(R.id.cc_select_all)?.isEnabled = !ei.isRawInputEditor
        smartbarView?.findViewById<View>(R.id.cc_cut)?.isEnabled = isSelectionMode && !ei.isRawInputEditor
        smartbarView?.findViewById<View>(R.id.cc_copy)?.isEnabled = isSelectionMode && !ei.isRawInputEditor
        smartbarView?.findViewById<View>(R.id.cc_paste)?.isEnabled =
            florisboard.clipboardManager?.hasPrimaryClip() ?: false
        smartbarView?.invalidate()
    }

    private fun updateActiveContainerVisibility() {
        val smartbarView = smartbarView ?: return

        if (isQuickActionsVisible) {
            smartbarView.setActiveVariant(R.id.smartbar_variant_default)
            smartbarView.setActiveContainer(R.id.quick_actions)
            smartbarView.findViewById<View>(R.id.quick_action_toggle)?.rotation = -180.0f
        } else {
            when {
                activeEditorInstance.isComposingEnabled -> {
                    smartbarView.setActiveVariant(R.id.smartbar_variant_default)
                    val containerId = if (shouldSuggestClipboardContents && florisboard.clipboardManager?.hasPrimaryClip() == true) {
                        val clipboardSuggestion = smartbarView.findViewById<Button>(R.id.clipboard_suggestion)
                        val item = florisboard.clipboardManager?.primaryClip?.getItemAt(0)
                        clipboardSuggestion?.text = item?.text ?: "(error while retrieving clipboard data)"
                        R.id.clipboard_suggestion_row
                    } else {
                        R.id.candidates
                    }
                    smartbarView.setActiveContainer(containerId)
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
