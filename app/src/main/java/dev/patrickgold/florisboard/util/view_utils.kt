package dev.patrickgold.florisboard.util

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children

fun getColorFromAttr(
    context: Context,
    attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    context.theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun getBooleanFromAttr(
    context: Context,
    attrBoolean: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Boolean {
    context.theme.resolveAttribute(attrBoolean, typedValue, resolveRefs)
    return typedValue.data != 0
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

fun refreshLayoutOf(view: View?) {
    if (view is ViewGroup) {
        view.invalidate()
        view.requestLayout()
        for (childView in view.children) {
            refreshLayoutOf(childView)
        }
    } else {
        view?.invalidate()
        view?.requestLayout()
    }
}
