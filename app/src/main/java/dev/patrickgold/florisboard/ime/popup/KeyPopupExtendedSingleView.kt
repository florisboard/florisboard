package dev.patrickgold.florisboard.ime.popup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import androidx.core.content.ContextCompat.getDrawable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.util.*

@SuppressLint("ViewConstructor")
class KeyPopupExtendedSingleView(
    context: Context, var isActive: Boolean = false
) : androidx.appcompat.widget.AppCompatTextView(
    context, null, 0
) {

    init {
        background = getDrawable(context, R.drawable.shape_rect_rounded)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        setBackgroundTintColor(this, when {
            isActive -> R.attr.key_popup_extended_bgColorActive
            else -> R.attr.key_popup_extended_bgColor
        })
    }
}
