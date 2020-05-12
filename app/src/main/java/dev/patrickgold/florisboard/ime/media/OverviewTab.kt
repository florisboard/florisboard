package dev.patrickgold.florisboard.ime.media

import android.annotation.SuppressLint
import android.widget.LinearLayout
import android.widget.TextView
import dev.patrickgold.florisboard.ime.core.FlorisBoard

@SuppressLint("ViewConstructor")
class OverviewTab(
    private val florisboard: FlorisBoard
) : LinearLayout(florisboard.context) {

    init {
        val textView = TextView(florisboard.context)
        textView.text = "OVERVIEW"
        addView(textView)
    }
}
