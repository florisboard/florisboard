package dev.patrickgold.florisboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.LinearLayout

class FlorisBoard : InputMethodService() {

    data class JKey(
        val cmd: String?,
        val code: Int?,
        val popup: List<Int>?,
        val isRepeatable: Boolean?
    )

    data class JLayout(
        val name: String,
        val direction: String,
        val layout: List<List<JKey>>
    )

    override fun onCreateInputView(): View? {
        val florisboard = layoutInflater.inflate(R.layout.florisboard, null) as LinearLayout
        val keyboard = florisboard.findViewById<CustomKeyboard>(R.id.keyboard)
        keyboard.inputMethodService = this
        keyboard.setLayout("qwerty")
        return florisboard
    }
}