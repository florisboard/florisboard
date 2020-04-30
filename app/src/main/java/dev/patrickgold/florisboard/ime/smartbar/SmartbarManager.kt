package dev.patrickgold.florisboard.ime.smartbar

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ToggleButton
import androidx.core.view.children
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.settings.SettingsMainActivity

class SmartbarManager(
    private val florisboard: FlorisBoard
) {
    private var candidatesView: LinearLayout? = null
    private var quickActionsView: LinearLayout? = null
    private var quickActionToggle: ToggleButton? = null

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
            else -> return@OnClickListener
        }
    }
    private val quickActionToggleOnClickListener = View.OnClickListener {
        isQuickActionsViewVisible = !isQuickActionsViewVisible

    }

    @SuppressLint("InflateParams")
    fun createSmartbarView(): LinearLayout {
        val smartbarView = florisboard.layoutInflater.inflate(R.layout.smartbar, null) as LinearLayout

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
