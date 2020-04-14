package dev.patrickgold.florisboard.ime.core

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.CustomKeyboard
import dev.patrickgold.florisboard.ime.key.KeyboardMode
import dev.patrickgold.florisboard.ime.layout.LayoutManager

class FlorisBoard : InputMethodService() {

    val layoutManager = LayoutManager(this)

    override fun onCreateInputView(): View? {
        val florisboard = layoutInflater.inflate(R.layout.florisboard, null) as LinearLayout
        layoutManager.autoFetchAssociationsFromPrefs()
        val keyboard = florisboard.findViewById<CustomKeyboard>(R.id.keyboard)
        keyboard.florisboard = this
        keyboard.inputMethodService = this
        keyboard.setKeyboardMode(KeyboardMode.CHARACTERS)
        return florisboard
    }
}
