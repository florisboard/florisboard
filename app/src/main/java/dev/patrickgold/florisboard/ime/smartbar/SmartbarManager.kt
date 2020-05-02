package dev.patrickgold.florisboard.ime.smartbar

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.textservice.*
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.core.view.children
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.settings.SettingsMainActivity
import org.w3c.dom.Text
import java.util.*


class SmartbarManager(
    private val florisboard: FlorisBoard
) : SpellCheckerSession.SpellCheckerSessionListener {
    private val candidateViewList: MutableList<TextView> = mutableListOf()
    private var candidatesView: LinearLayout? = null
    private var quickActionsView: LinearLayout? = null
    private var quickActionToggle: ToggleButton? = null
    private var spellCheckerSession: SpellCheckerSession? = null

    var composingText: String = ""
    var isQuickActionsViewVisible: Boolean = false
        set(value) { field = value; updateQuickActionVisibility() }

    private val candidateViewOnClickListener = View.OnClickListener { v ->
        //
    }
    private val candidateViewOnLongClickListener = View.OnLongClickListener { v ->
        true
    }
    private val quickActionOnClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.quick_action_open_settings -> {
                val i = Intent(florisboard, SettingsMainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                florisboard.startActivity(i)
            }
            R.id.quick_action_one_handed_toggle -> {
                when (florisboard.prefs?.oneHandedMode) {
                    "off" -> {
                        florisboard.prefs?.oneHandedMode = "end"
                    }
                    else -> {
                        florisboard.prefs?.oneHandedMode = "off"
                    }
                }
                florisboard.updateOneHandedPanelVisibility()
            }
            else -> return@OnClickListener
        }
    }
    private val quickActionToggleOnClickListener = View.OnClickListener {
        isQuickActionsViewVisible = !isQuickActionsViewVisible

    }

    @SuppressLint("InflateParams")
    fun createSmartbarView(): LinearLayout {
        val smartbarView = florisboard.layoutInflater.inflate(R.layout.smartbar, null) as LinearLayout

        candidateViewList.add(smartbarView.findViewById(R.id.candidate0))
        candidateViewList.add(smartbarView.findViewById(R.id.candidate1))
        candidateViewList.add(smartbarView.findViewById(R.id.candidate2))

        candidatesView = smartbarView.findViewById(R.id.candidates)
        quickActionsView = smartbarView.findViewById(R.id.quick_actions)

        quickActionToggle = smartbarView.findViewById(R.id.quick_action_toggle)
        quickActionToggle?.setOnClickListener(quickActionToggleOnClickListener)

        val quickActions = smartbarView.findViewById<LinearLayout>(R.id.quick_actions)
        for (quickAction in quickActions.children) {
            if (quickAction is ImageButton) {
                quickAction.setOnClickListener(quickActionOnClickListener)
            }
        }

        return smartbarView
    }

    override fun onGetSuggestions(arr: Array<out SuggestionsInfo>?) {
        if (arr == null || arr.isEmpty()) {
            return
        }
        val suggestions = arr[0]
        for (i in 0 until suggestions.suggestionsCount) {
            candidateViewList[i].text = suggestions.getSuggestionAt(i)
            if (i == 2) {
                break
            }
        }
    }

    override fun onGetSentenceSuggestions(arr: Array<out SentenceSuggestionsInfo>?) {
        if (arr == null || arr.isEmpty()) {
            return
        }
        val suggestions = arr[0].getSuggestionsInfoAt(0)
        for (i in 0 until suggestions.suggestionsCount) {
            candidateViewList[i].text = suggestions.getSuggestionAt(i)
            if (i == 2) {
                break
            }
        }
    }

    fun onStartInputView() {
        val tsm = florisboard.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        spellCheckerSession = tsm.newSpellCheckerSession(null, null, this, true)
    }

    fun onFinishInputView() {
        spellCheckerSession?.close()
    }

    fun deleteCandidateFromDictionary(candidate: String) {
        //
    }

    fun resetCandidates() {
        //
    }

    fun generateCandidatesFromSuggestions(composing: String = composingText) {
        spellCheckerSession?.getSentenceSuggestions(arrayOf(TextInfo(composing)), 3)
        android.util.Log.i("SPELL", "GEN")
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

    private fun updateQuickActionVisibility() {
        if (isQuickActionsViewVisible) {
            candidatesView?.visibility = View.GONE
            quickActionsView?.visibility = View.VISIBLE
            quickActionToggle?.rotation = -180.0f
        } else {
            candidatesView?.visibility = View.VISIBLE
            quickActionsView?.visibility = View.GONE
            quickActionToggle?.rotation = 0.0f
        }
    }
}
