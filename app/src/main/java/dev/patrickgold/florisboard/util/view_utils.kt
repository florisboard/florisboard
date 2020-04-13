package dev.patrickgold.florisboard.util

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.View
import android.widget.Button

fun getColorFromAttr(
    context: Context,
    attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    context.theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun setBackgroundTintColor(view: View, colorId: Int) {
    view.backgroundTintList = ColorStateList.valueOf(
        getColorFromAttr(view.context, colorId)
    )
}
fun setDrawableTintColor(view: Button, colorId: Int) {
    view.compoundDrawableTintList = ColorStateList.valueOf(
        getColorFromAttr(view.context, colorId)
    )
}
fun setTextTintColor(view: View, colorId: Int) {
    view.foregroundTintList = ColorStateList.valueOf(
        getColorFromAttr(view.context, colorId)
    )
}
