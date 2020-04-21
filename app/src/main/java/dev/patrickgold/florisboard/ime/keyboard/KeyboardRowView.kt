package dev.patrickgold.florisboard.ime.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.preference.PreferenceManager
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R

class KeyboardRowView : FlexboxLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs,
        R.attr.keyboardRowViewStyle
    )
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        layoutParams = layoutParams.apply {
            val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val factor = preferences.getString("keyboard__height_factor", "normal")
            height = (resources.getDimension(R.dimen.keyboard_row_height).toInt() * when (factor) {
                "small" -> 0.70f
                "mid_small" -> 0.85f
                "normal" -> 1.00f
                "mid_tall" -> 1.15f
                "tall" -> 1.30f
                else -> 1.00f
            }).toInt()
        }
    }
}
