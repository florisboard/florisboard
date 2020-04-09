package dev.patrickgold.florisboard

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.*

class CustomKey : androidx.appcompat.widget.AppCompatButton, View.OnClickListener {

    private val code: Int
    var keyboard: CustomKeyboard? = null

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, R.attr.customKeyStyle)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.CustomKey,
            defStyle, R.style.CustomKeyStyle).apply {
            try {
                code = getInteger(R.styleable.CustomKey_code, 0)
            } finally {}
        }.recycle()

        super.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        keyboard?.onKeyClicked(code)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val label = code.toChar().toString()
        super.setText(when {
            keyboard?.caps ?: false -> label.toUpperCase(Locale.getDefault())
            else -> label
        })
    }
}
