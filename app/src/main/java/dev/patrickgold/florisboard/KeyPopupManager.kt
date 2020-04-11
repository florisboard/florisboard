package dev.patrickgold.florisboard

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

class KeyPopupManager(kbd: CustomKeyboard) {

    private val keyboard: CustomKeyboard = kbd
    private var window: PopupWindow? = null

    val isShowing: Boolean
        get() = this.window?.isShowing ?: false

    fun show(key: CustomKey) {
        if (key.code == 32 || key.code == null) {
            return
        }
        val keyPopupWidth = keyboard.resources.getDimension(R.dimen.key_popup_width).toInt()
        val keyPopupHeight = keyboard.resources.getDimension(R.dimen.key_popup_height).toInt()
        val popupView = View.inflate(key.context, R.layout.key_popup, null) as ViewGroup
        popupView.findViewById<TextView>(R.id.key_popup_text).text = key.getLabel()
        popupView.findViewById<ImageView>(R.id.key_popup_threedots).visibility = when {
            key.popupCodes.isEmpty() -> View.INVISIBLE
            else -> View.VISIBLE
        }
        window = PopupWindow(popupView, keyPopupWidth, keyPopupHeight, false)
        window?.isClippingEnabled = false
        window?.isOutsideTouchable = true
        window?.showAtLocation(
            keyboard, Gravity.LEFT or Gravity.TOP,
            (key.x - ((keyPopupWidth - key.measuredWidth).toFloat() / 2.0f)).toInt(),
            ((key.parent as ViewGroup).y - key.measuredHeight).toInt()
        )
    }

    fun hide(key: CustomKey) {
        if (key.code == 32 || key.code == null) {
            return
        }
        window?.dismiss()
        window = null
    }

}
