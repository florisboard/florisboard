package dev.patrickgold.florisboard.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import com.google.android.flexbox.FlexboxLayout
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
    var keyWidth: Int = resources.getDimension(R.dimen.key_height).toInt()

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
                val rowView =
                    KeyboardRowView(
                        context
                    )
                val rowViewLP = FlexboxLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                rowView.layoutParams = rowViewLP
                rowView.setPadding(
                    resources.getDimension(R.dimen.keyboard_row_marginH).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginV).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginH).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginV).toInt()
                )
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        keyWidth = (measuredWidth / 10) - (2 * keyMarginH)
    }
}
