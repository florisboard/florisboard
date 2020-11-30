package dev.patrickgold.florisboard.ime.text.smartbar

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.children
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.EditorInstance
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.TextInputManager
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
    private val prefs: PrefHelper
        get() = florisboard.prefs

    private val textInputManager: TextInputManager = TextInputManager.getInstance()
    private var shouldSuggestClipboardContents: Boolean = false
    private var smartbarView: SmartbarView? = null

    var isQuickActionsVisible: Boolean = false

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
    private val clipboardSuggestionViewOnClickListener = View.OnClickListener {
        activeEditorInstance.performClipboardPaste()
        shouldSuggestClipboardContents = false
        updateActiveContainerVisibility()
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
            val layout = textInputManager.layoutManager.fetchComputedLayoutAsync(KeyboardMode.SMARTBAR_NUMBER_ROW, Subtype.DEFAULT).await()
            launch(Dispatchers.Main) {
                numberRow.computedLayout = layout
                numberRow.updateVisibility()
            }
        }
        val clipboardSuggestion = smartbarView.findViewById<Button>(R.id.clipboard_suggestion)
        clipboardSuggestion.setOnClickListener(clipboardSuggestionViewOnClickListener)
        launch(Dispatchers.Default) {
            val ccRow = smartbarView.findViewById<KeyboardView>(R.id.clipboard_cursor_row)
            ccRow.isSmartbarKeyboardView = true
            val layout = textInputManager.layoutManager.fetchComputedLayoutAsync(KeyboardMode.SMARTBAR_CLIPBOARD_CURSOR_ROW, Subtype.DEFAULT).await()
            launch(Dispatchers.Main) {
                ccRow.computedLayout = layout
                ccRow.updateVisibility()
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
        if (prefs.suggestion.enabled && prefs.suggestion.suggestClipboardContent) {
            shouldSuggestClipboardContents = true
            updateActiveContainerVisibility()
        }
    }

    fun resetClipboardSuggestion() {
        if (prefs.suggestion.enabled && prefs.suggestion.suggestClipboardContent) {
            shouldSuggestClipboardContents = false
            updateActiveContainerVisibility()
        }
    }

    private fun updateSmartbarUI() {
        val ei = activeEditorInstance
        if (ei.selection.isCursorMode && ei.isComposingEnabled) {
            generateCandidatesFromComposing(ei.currentWord.text)
        }
        updateActiveContainerVisibility()
        val ccRow = smartbarView?.findViewById<KeyboardView>(R.id.clipboard_cursor_row)
        ccRow?.updateVisibility()
    }

    fun updateActiveContainerVisibility() {
        val smartbarView = smartbarView ?: return

        if (isQuickActionsVisible) {
            smartbarView.setActiveVariant(R.id.smartbar_variant_default)
            smartbarView.setActiveContainer(R.id.quick_actions)
            smartbarView.findViewById<View>(R.id.quick_action_toggle)?.rotation = -180.0f
        } else {
            when {
                textInputManager.getActiveKeyboardMode() == KeyboardMode.EDITING -> {
                    smartbarView.setActiveVariant(R.id.smartbar_variant_back_only)
                    smartbarView.setActiveContainer(null)
                }
                activeEditorInstance.isComposingEnabled -> {
                    smartbarView.setActiveVariant(R.id.smartbar_variant_default)
                    val containerId = if (shouldSuggestClipboardContents && florisboard.clipboardManager?.hasPrimaryClip() == true) {
                        val clipboardSuggestion = smartbarView.findViewById<Button>(R.id.clipboard_suggestion)
                        val item = florisboard.clipboardManager?.primaryClip?.getItemAt(0)
                        when {
                            item?.text != null -> {
                                clipboardSuggestion?.text = item.text
                            }
                            item?.uri != null -> {
                                clipboardSuggestion?.text = "(Image) " + item.uri.toString()
                            }
                            else -> {
                                clipboardSuggestion?.text = item?.text ?: "(Error while retrieving clipboard data)"
                            }
                        }
                        R.id.clipboard_suggestion_row
                    } else {
                        R.id.candidates
                    }
                    smartbarView.setActiveContainer(containerId)
                }
                textInputManager.getActiveKeyboardMode() == KeyboardMode.CHARACTERS -> {
                    when (prefs.suggestion.showInstead) {
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
