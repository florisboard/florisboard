package dev.patrickgold.florisboard.ime.keyboard

import android.content.Context
import android.util.AttributeSet
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R

class KeyboardRowView : FlexboxLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs,
        R.attr.keyboardRowViewStyle
    )
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs)
}
