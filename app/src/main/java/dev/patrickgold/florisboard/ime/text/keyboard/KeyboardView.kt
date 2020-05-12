package dev.patrickgold.florisboard.ime.text.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.text.key.KeyView
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData

@SuppressLint("ViewConstructor")
class KeyboardView(
    val florisboard: FlorisBoard
) : LinearLayout(
    florisboard.context, null, R.attr.keyboardViewStyle
) {
    var computedLayout: ComputedLayoutData? = null
    var desiredKeyWidth: Int = resources.getDimension(R.dimen.key_width).toInt()
    var desiredKeyHeight: Int = resources.getDimension(R.dimen.key_height).toInt()
    var shouldStealMotionEvents: Boolean = false

    private fun buildLayout() {
        destroyLayout()
        val context = ContextThemeWrapper(context,
            R.style.KeyboardTheme_MaterialLight
        )
        this.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        val layout = computedLayout
        if (layout != null) {
            for (row in layout.arrangement) {
                val rowView = KeyboardRowView(context)
                for (key in row) {
                    val keyView = KeyView(florisboard, this, key)
                    rowView.addView(keyView)
                }
                this.addView(rowView)
            }
        }
    }

    private fun destroyLayout() {
        removeAllViews()
    }

    fun setKeyboardMode(keyboardMode: KeyboardMode) {
        computedLayout = florisboard.textInputManager.layoutManager.computeLayoutFor(keyboardMode)
        buildLayout()
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return shouldStealMotionEvents
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        shouldStealMotionEvents = false
        if (event != null && event.action == MotionEvent.ACTION_MOVE) {
            event.action = MotionEvent.ACTION_DOWN
            dispatchTouchEvent(event)
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        desiredKeyWidth = when (computedLayout?.mode) {
            KeyboardMode.NUMERIC,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> (widthSize / 4) - (2 * keyMarginH)
            else -> (widthSize / 10) - (2 * keyMarginH)
        }

        val factor = florisboard.prefs!!.heightFactor
        desiredKeyHeight = (resources.getDimension(R.dimen.key_height).toInt() * when (factor) {
            "short" -> 0.90f
            "mid_short" -> 0.95f
            "normal" -> 1.00f
            "mid_tall" -> 1.05f
            "tall" -> 1.10f
            else -> 1.00f
        }).toInt()
        if (florisboard.prefs?.oneHandedMode == "start" ||
            florisboard.prefs?.oneHandedMode == "end") {
            desiredKeyHeight = (desiredKeyHeight * 0.9f).toInt()
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun updateVariation() {
        for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        keyView.updateVariation()
                    }
                }
            }
        }
    }
}
