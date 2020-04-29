package dev.patrickgold.florisboard.ime.smartbar

import android.annotation.SuppressLint
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard

class SmartbarManager(
    private val florisboard: FlorisBoard
) {
    @SuppressLint("InflateParams")
    fun createSmartbarView(): LinearLayout {
        val smartbarView = florisboard.layoutInflater.inflate(R.layout.smartbar, null) as LinearLayout
        return smartbarView
    }
}
