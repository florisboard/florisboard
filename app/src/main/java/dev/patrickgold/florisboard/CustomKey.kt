package dev.patrickgold.florisboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat.getDrawable
import java.util.*
import com.google.android.flexbox.FlexboxLayout

class CustomKey : androidx.appcompat.widget.AppCompatButton, View.OnClickListener {

    var cmd: Int?
        set(v) { updateUI(); field = v }
    var code: Int?
        set(v) { updateUI(); field = v }
    var keyboard: CustomKeyboard? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.customKeyStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs) {
        context.obtainStyledAttributes(
            attrs, R.styleable.CustomKey, defStyleAttrs,
            R.style.Widget_AppCompat_Button_Borderless_CustomKey).apply {
            try {
                cmd = KeyCodes.fromString(getString(R.styleable.CustomKey_cmd) ?: "")
                val tmp = getInteger(R.styleable.CustomKey_code, 0)
                code =  when (tmp) {
                    0 -> null
                    else -> tmp
                }
            } finally {}
        }.recycle()

        super.setOnClickListener(this)

    }

    private fun updateUI() {
        val label = (code?.toChar() ?: "").toString()
        super.setText(when {
            keyboard?.caps ?: false -> label.toUpperCase(Locale.getDefault())
            else -> label
        })
        val drawable: Drawable? = when (cmd) {
            KeyCodes.ENTER -> getDrawable(context, R.drawable.ic_arrow_forward)
            KeyCodes.DELETE -> getDrawable(context, R.drawable.ic_backspace)
            KeyCodes.LANGUAGE_SWITCH -> getDrawable(context, R.drawable.ic_language)
            KeyCodes.KEYBOARD_CYCLE -> getDrawable(context, R.drawable.ic_language)
            KeyCodes.SHIFT -> getDrawable(context, R.drawable.ic_capslock)
            else -> null
        }
        drawable?.setBounds(0, 0, resources.getDimension(R.dimen.key_width).toInt(), resources.getDimension(R.dimen.key_width).toInt())
        super.setCompoundDrawables(null, drawable, null, null)
        when (cmd) {
            KeyCodes.DELETE -> (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
            KeyCodes.ENTER -> (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
            KeyCodes.SHIFT -> (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
            KeyCodes.SPACE -> (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 5.0f
            KeyCodes.VIEW_SYMOBLS -> {
                (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
                super.setText("?123")
            }
        }
    }

    override fun onClick(v: View) {
        keyboard?.onKeyClicked(code ?: cmd ?: 0)
    }

    override fun onDraw(canvas: Canvas?) {
        updateUI()
        super.onDraw(canvas)
    }
}
