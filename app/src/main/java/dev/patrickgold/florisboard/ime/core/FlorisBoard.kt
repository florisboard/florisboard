package dev.patrickgold.florisboard.ime.core

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.CustomKeyboard
import dev.patrickgold.florisboard.ime.layout.LayoutManager

class FlorisBoard : InputMethodService() {

    //val layoutManager = LayoutManager(baseContext)

    override fun onCreateInputView(): View? {
        val florisboard = layoutInflater.inflate(R.layout.florisboard, null) as LinearLayout
        val keyboard = florisboard.findViewById<CustomKeyboard>(R.id.keyboard)
        keyboard.inputMethodService = this
        keyboard.setLayout("qwerty")
        return florisboard
    }


}
