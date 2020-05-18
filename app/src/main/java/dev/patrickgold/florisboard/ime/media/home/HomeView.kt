package dev.patrickgold.florisboard.ime.media.home

import android.annotation.SuppressLint
import android.widget.LinearLayout
import android.widget.TextView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard

@SuppressLint("ViewConstructor")
class HomeView(
    private val florisboard: FlorisBoard
) : LinearLayout(florisboard.context) {

    init {
        val textView = TextView(context)
        textView.text = "HOME"
        addView(textView)
    }
}
