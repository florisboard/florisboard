package dev.patrickgold.florisboard.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.key.KeyView
import dev.patrickgold.florisboard.ime.layout.ComputedLayoutData

@SuppressLint("ViewConstructor")
class KeyboardView(
    context: Context, val florisboard: FlorisBoard
) : LinearLayout(
    context, null, R.attr.keyboardViewStyle
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
                    val keyView = KeyView(context, key, florisboard, this)
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
        computedLayout = florisboard.layoutManager.computeLayoutFor(keyboardMode)
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
            KeyboardMode.NUMERIC -> (widthSize / 4) - (2 * keyMarginH)
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
}
