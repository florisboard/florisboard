package dev.patrickgold.florisboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.LinearLayout
import com.google.android.flexbox.FlexboxLayout

class FlorisBoard : InputMethodService() {

    override fun onCreateInputView(): View? {
        val florisboard = layoutInflater.inflate(R.layout.florisboard, null) as LinearLayout
        val keyboardContainer = florisboard.findViewById<LinearLayout>(R.id.keyboard_container)
        val keyboard = layoutInflater.inflate(R.layout.qwerty, null) as CustomKeyboard
        keyboard.inputMethodService = this
        val childCount = keyboard.childCount
        for (i in 0 until childCount) {
            val v = keyboard.getChildAt(i)
            if (v is FlexboxLayout) {
                val row = v as FlexboxLayout
                val rowChildCount = row.childCount
                for (j in 0 until rowChildCount) {
                    val vv = row.getChildAt(j)
                    if (vv is CustomKey) {
                        vv.keyboard = keyboard
                    } else if (vv is CustomKeySpecial) {
                        vv.keyboard = keyboard
                    }
                }
            }
        }
        keyboardContainer.addView(keyboard)
        return florisboard
    }

}