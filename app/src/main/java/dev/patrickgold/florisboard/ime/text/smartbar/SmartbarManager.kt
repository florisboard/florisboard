package dev.patrickgold.florisboard.ime.text.smartbar

import android.content.Context
import android.util.Log
import android.view.View
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextServicesManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.children
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode

// TODO: Implement suggestion creation functionality
// TODO: Cleanup and reorganize SmartbarManager
class SmartbarManager private constructor() :
    SpellCheckerSession.SpellCheckerSessionListener, FlorisBoard.EventListener {

    private val florisboard: FlorisBoard = FlorisBoard.getInstance()
    private var isComposingEnabled: Boolean = false
    private var spellCheckerSession: SpellCheckerSession? = null
    private val textInputManager: TextInputManager = TextInputManager.getInstance()
    var smartbarView: SmartbarView? = null
        private set

    var activeContainerId: Int = R.id.candidates
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
    private val numberRowButtonOnClickListener = View.OnClickListener { v ->
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
            else -> KeyData(0)
        }
        florisboard.textInputManager.sendKeyPress(keyData)
    }
    private val quickActionOnClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.quick_action_switch_to_media_context -> {
                activeContainerId = getPreferredContainerId()
                florisboard.setActiveInput(R.id.media_input)
            }
            R.id.quick_action_open_settings -> florisboard.launchSettings()
            R.id.quick_action_one_handed_toggle -> florisboard.toggleOneHandedMode()
            else -> return@OnClickListener
        }
    }
    private val quickActionToggleOnClickListener = View.OnClickListener {
        activeContainerId = when (activeContainerId) {
            R.id.quick_actions -> getPreferredContainerId()
            else -> R.id.quick_actions
        }
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

        smartbarView.quickActionToggle?.setOnClickListener(quickActionToggleOnClickListener)
        val quickActions = smartbarView.findViewById<LinearLayout>(R.id.quick_actions)
        for (quickAction in quickActions.children) {
            if (quickAction is ImageButton) {
                quickAction.setOnClickListener(quickActionOnClickListener)
            }
        }
        val numberRow = smartbarView.findViewById<LinearLayout>(R.id.number_row)
        for (numberRowButton in numberRow.children) {
            if (numberRowButton is Button) {
                numberRowButton.setOnClickListener(numberRowButtonOnClickListener)
            }
        }
        for (candidateView in smartbarView.candidateViewList) {
            candidateView.setOnClickListener(candidateViewOnClickListener)
            candidateView.setOnLongClickListener(candidateViewOnLongClickListener)
        }
    }

    // TODO: clean up resources here
    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

        instance = null
    }

    override fun onGetSuggestions(arr: Array<out SuggestionsInfo>?) {
        if (arr == null || arr.isEmpty()) {
            return
        }
        /*val suggestions = arr[0]
        for (i in 0 until suggestions.suggestionsCount) {
            candidateViewList[i].text = suggestions.getSuggestionAt(i)
            if (i == 2) {
                break
            }
        }*/
    }

    override fun onGetSentenceSuggestions(arr: Array<out SentenceSuggestionsInfo>?) {
        if (arr == null || arr.isEmpty()) {
            return
        }
        /*val suggestions = arr[0].getSuggestionsInfoAt(0)
        for (i in 0 until suggestions.suggestionsCount) {
            candidateViewList[i].text = suggestions.getSuggestionAt(i)
            if (i == 2) {
                break
            }
        }*/
    }

    fun onStartInputView(keyboardMode: KeyboardMode, isComposingEnabled: Boolean) {
        this.isComposingEnabled = isComposingEnabled
        when {
            keyboardMode == KeyboardMode.NUMERIC ||
            keyboardMode == KeyboardMode.PHONE ||
            keyboardMode == KeyboardMode.PHONE2 -> {
                smartbarView?.visibility = View.GONE
            }
            !isComposingEnabled -> {
                smartbarView?.visibility = View.VISIBLE
                activeContainerId = R.id.number_row
            }
            else -> {
                smartbarView?.visibility = View.VISIBLE
                activeContainerId = R.id.candidates
                //val tsm = florisboard.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
                //spellCheckerSession = tsm.newSpellCheckerSession(null, null, this, true)
            }
        }
    }

    fun onFinishInputView() {
        //spellCheckerSession?.close()
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
            activeContainerId = R.id.candidates
            updateActiveContainerVisibility()
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

    fun getPreferredContainerId(): Int {
        return when {
            !isComposingEnabled -> when(textInputManager.getActiveKeyboardMode()) {
                KeyboardMode.CHARACTERS -> R.id.number_row
                else -> 0
            }
            else -> R.id.candidates
        }
    }

    private fun updateActiveContainerVisibility() {
        val smartbarView = smartbarView ?: return

        when (activeContainerId) {
            R.id.quick_actions -> {
                smartbarView.candidatesView?.visibility = View.GONE
                smartbarView.numberRowView?.visibility = View.GONE
                smartbarView.quickActionsView?.visibility = View.VISIBLE
                smartbarView.quickActionToggle?.rotation = -180.0f
            }
            R.id.number_row -> {
                smartbarView.candidatesView?.visibility = View.GONE
                smartbarView.numberRowView?.visibility = View.VISIBLE
                smartbarView.quickActionsView?.visibility = View.GONE
                smartbarView.quickActionToggle?.rotation = 0.0f
            }
            R.id.candidates -> {
                smartbarView.candidatesView?.visibility = View.VISIBLE
                smartbarView.numberRowView?.visibility = View.GONE
                smartbarView.quickActionsView?.visibility = View.GONE
                smartbarView.quickActionToggle?.rotation = 0.0f
            }
            else -> {
                smartbarView.candidatesView?.visibility = View.GONE
                smartbarView.numberRowView?.visibility = View.GONE
                smartbarView.quickActionsView?.visibility = View.GONE
                smartbarView.quickActionToggle?.rotation = 0.0f
            }
        }
    }
}
