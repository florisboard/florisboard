package dev.patrickgold.florisboard

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.opengl.Visibility
import android.os.Handler
import android.provider.Settings
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.roundToInt

@SuppressLint("RtlHardcoded")
class KeyPopupManager(kbd: CustomKeyboard) {

    private val keyPopupWidth = kbd.resources.getDimension(R.dimen.key_popup_width).toInt()
    private val keyPopupHeight = kbd.resources.getDimension(R.dimen.key_popup_height).toInt()
    private val keyboard: CustomKeyboard = kbd
    private var windows: HashMap<Int, PopupWindow> = hashMapOf()
    private var windowsExt: HashMap<Int, PopupWindow> = hashMapOf()

    private fun createTextView(key: CustomKey, k: Int): TextView {
        val textView = TextView(keyboard.context)
        textView.layoutParams = ViewGroup.LayoutParams(keyPopupWidth, key.measuredHeight)
        textView.gravity = Gravity.CENTER
        setTextTintColor(textView, R.attr.key_popup_fgColor)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, keyboard.resources.getDimension(R.dimen.key_popup_textSize))
        textView.text = key.createPopupKeyText(key.popupCodes[k])
        return textView
    }

    private fun getColorFromAttr(
        attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        keyboard.context.theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    private fun setBackgroundTintColor(textView: TextView, colorId: Int) {
        textView.backgroundTintList = ColorStateList.valueOf(
            getColorFromAttr(colorId)
        )
    }
    private fun setDrawableTintColor(textView: TextView, colorId: Int) {
        textView.compoundDrawableTintList = ColorStateList.valueOf(
            getColorFromAttr(colorId)
        )
    }
    private fun setTextTintColor(textView: TextView, colorId: Int) {
        textView.foregroundTintList = ColorStateList.valueOf(
            getColorFromAttr(colorId)
        )
    }

    private fun createPopupWindow(view: View, width: Int, height: Int): PopupWindow {
        val w = PopupWindow(view, width, height, false)
        w.animationStyle = 0
        w.enterTransition = null
        w.exitTransition = null
        w.isClippingEnabled = false
        w.isOutsideTouchable = true
        return w
    }

    fun show(key: CustomKey) {
        val code = key.code ?: 0
        if (code == 32 || key.code == null) {
            return
        }
        if (windows.containsKey(code)) {
            return
        }
        val popupView = View.inflate(keyboard.context, R.layout.key_popup, null) as ViewGroup
        popupView.findViewById<TextView>(R.id.key_popup_text).text = key.createLabelText()
        popupView.findViewById<ImageView>(R.id.key_popup_threedots).visibility = when {
            key.popupCodes.isEmpty() -> View.INVISIBLE
            else -> View.VISIBLE
        }
        val w = createPopupWindow(popupView, keyPopupWidth, keyPopupHeight)
        w.showAtLocation(
            keyboard, Gravity.LEFT or Gravity.TOP,
            (keyboard.x + (key.parent as ViewGroup).x + key.x - ((keyPopupWidth - key.measuredWidth).toFloat() / 2.0f)).toInt(),
            (keyboard.y + (key.parent as ViewGroup).y + key.y - (keyPopupHeight - key.measuredHeight)).toInt()
        )
        windows[code] = w
    }

    fun extend(key: CustomKey) {
        val code = key.code ?: 0
        if (code == 32 || key.code == null || key.popupCodes.isEmpty()) {
            return
        }
        if (windowsExt.containsKey(code)) {
            return
        }
        val popupViewExt = View.inflate(keyboard.context, R.layout.key_popup_extended, null) as ViewGroup
        // Extended popup layout:
        // row 1
        // row 0 (has always items, takes all if size <= 5, when higher and uneven 1 more than row 1
        val row0count = when {
            key.popupCodes.size > 5 -> (key.popupCodes.size.toFloat() / 2.0f).roundToInt()
            else -> key.popupCodes.size
        }
        val row1count = key.popupCodes.size - row0count
        val row0 = popupViewExt.findViewById<LinearLayout>(R.id.key_popup_extended_row0)
        val row1 = popupViewExt.findViewById<LinearLayout>(R.id.key_popup_extended_row1)
        row0.removeAllViews()
        row1.removeAllViews()
        for (k in key.popupCodes.indices) {
            if (row1count > 0 && k < row1count) {
                row1.addView(createTextView(key, k))
            } else {
                row0.addView(createTextView(key, k))
            }
        }
        //popupView.findViewById<ImageView>(R.id.key_popup_threedots).visibility = View.INVISIBLE
        val w = createPopupWindow(popupViewExt, row0count * keyPopupWidth, when {
            row1count > 0 -> key.measuredHeight * 2
            else -> key.measuredHeight
        })
        w.showAtLocation(
            keyboard, Gravity.LEFT or Gravity.TOP,
            (keyboard.x + (key.parent as ViewGroup).x + key.x - ((keyPopupWidth - key.measuredWidth).toFloat() / 2.0f)).toInt(),
            (keyboard.y + (key.parent as ViewGroup).y + key.y - (keyPopupHeight - key.measuredHeight) - when {
                row1count > 0 -> key.measuredHeight
                else -> 0
            }).toInt()
        )
        windowsExt[code] = w
    }

    fun hide(key: CustomKey) {
        val code = key.code ?: 0
        if (code == 32 || key.code == null) {
            return
        }
        // Hide popup delayed so the popup can show for persons who type fast
        Handler().postDelayed({
            windows[code]?.dismiss()
            windows.remove(code)
            windowsExt[code]?.dismiss()
            windowsExt.remove(code)
        }, 50)
    }
}
