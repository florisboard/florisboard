package dev.patrickgold.florisboard.ime.key

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat.getDrawable
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.KeyCodes
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.keyboard.KeyboardRowView
import dev.patrickgold.florisboard.ime.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.popup.KeyPopupManager
import dev.patrickgold.florisboard.util.*
import java.util.*

@SuppressLint("ViewConstructor")
class KeyPopupExtendedSingleView(
    context: Context, val data: KeyData, var isActive: Boolean = false
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
