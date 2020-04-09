package dev.patrickgold.florisboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Icon
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.drawable.toDrawable

enum class CustomKeySpecialCMD(val value: Int) {
    CONFIRM(-4),
    DEL(-5),
    LANGSWITCH(-64),
    PAGESWITCH(-63),
    SHIFT(-1),
    SPACE(32),
    UNASSIGNED(0);
    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}

class CustomKeySpecial : androidx.appcompat.widget.AppCompatImageButton, View.OnClickListener {

    private val cmd: CustomKeySpecialCMD
    var keyboard: CustomKeyboard? = null

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, R.attr.customKeySpecialStyle)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.CustomKeySpecial,
            defStyle, R.style.CustomKeySpecialStyle).apply {
            try {
                cmd = CustomKeySpecialCMD.fromInt(getInt(R.styleable.CustomKeySpecial_cmd, 0))
            } finally {}
        }.recycle()

        super.setOnClickListener(this)

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (cmd) {
            CustomKeySpecialCMD.CONFIRM -> setImageResource(R.drawable.ic_arrow_forward)
            CustomKeySpecialCMD.DEL -> setImageResource(R.drawable.ic_backspace)
            CustomKeySpecialCMD.LANGSWITCH -> setImageResource(R.drawable.ic_language)
            CustomKeySpecialCMD.PAGESWITCH -> setImageResource(R.drawable.ic_language)
            CustomKeySpecialCMD.SHIFT -> setImageResource(R.drawable.ic_capslock)
        }
    }

    override fun onClick(v: View) {
        keyboard?.onKeyClicked(cmd.value)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }
}
