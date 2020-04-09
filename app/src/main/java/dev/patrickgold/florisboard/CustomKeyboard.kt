package dev.patrickgold.florisboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.LinearLayout


class CustomKeyboard : LinearLayout {

    private val layoutName: String
    var inputMethodService: InputMethodService? = null
    private val showLanguageButton: Boolean

    var caps: Boolean = false

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, R.attr.customKeyboardStyle)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.CustomKeyboard,
            defStyle, R.style.CustomKeyboardStyle).apply {
            try {
                layoutName = getNonResourceString(R.styleable.CustomKeyboard_layoutName)
                showLanguageButton = getBoolean(R.styleable.CustomKeyboard_showLanguageButton, false)
            } finally {}
        }.recycle()
    }

    fun onKeyClicked(code: Int) {
        val ic = inputMethodService?.currentInputConnection ?: return
        //playClick(primaryCode)
        when (code) {
            CustomKeySpecialCMD.DEL.value -> ic.deleteSurroundingText(1, 0)
            CustomKeySpecialCMD.SHIFT.value -> {
                caps = !caps
            }
            CustomKeySpecialCMD.CONFIRM.value -> ic.sendKeyEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER
                )
            )
            else -> {
                var text = code.toChar()
                if (caps) {
                    text = Character.toUpperCase(text)
                }
                ic.commitText(text.toString(), 1)
            }
        }
    }
}
