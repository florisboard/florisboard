package dev.patrickgold.florisboard.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import kotlin.reflect.KClass

fun getColorFromAttr(
    context: Context,
    attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    context.theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun setBackgroundTintColor2(view: View, colorInt: Int) {
    view.backgroundTintList = ColorStateList.valueOf(colorInt)
}

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.findViewWithType(type: KClass<T>): T? {
    for (child in this.children) {
        if (type.isInstance(child)) {
            return child as T
        } else if (child is ViewGroup) {
            child.findViewWithType(type)?.let { return it }
        }
    }
    return null
}

/**
 * Context extension function to get the Activity from the Context. Originally written by Vlad as
 * an SO answer. Modified to return an AppCompatActivity, as FlorisBoard relies on some compat
 * stuff.
 *
 * Original source: https://stackoverflow.com/a/58249983/6801193
 */
tailrec fun Context?.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    else -> (this as? ContextWrapper)?.baseContext?.getActivity()
}
