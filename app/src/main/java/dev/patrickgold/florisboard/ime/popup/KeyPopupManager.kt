package dev.patrickgold.florisboard.ime.popup

import android.annotation.SuppressLint
import android.opengl.Visibility
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.key.KeyView
import dev.patrickgold.florisboard.ime.keyboard.KeyboardView
import dev.patrickgold.florisboard.util.setTextTintColor
import kotlin.math.roundToInt


@SuppressLint("RtlHardcoded")
class KeyPopupManager(kbd: KeyboardView) {

    private val keyPopupWidth = kbd.resources.getDimension(R.dimen.key_popup_width).toInt()
    private val keyPopupHeight = kbd.resources.getDimension(R.dimen.key_popup_height).toInt()
    private val keyboardView: KeyboardView = kbd
    private var windows: HashMap<Int, PopupWindow> = hashMapOf()
    private var windowsExt: HashMap<Int, PopupWindow> = hashMapOf()

    private fun createTextView(keyView: KeyView, k: Int): TextView {
        val textView = TextView(keyboardView.context)
        textView.layoutParams = ViewGroup.LayoutParams(keyPopupWidth, keyView.measuredHeight)
        textView.gravity = Gravity.CENTER
        setTextTintColor(textView,
            R.attr.key_popup_fgColor
        )
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, keyboardView.resources.getDimension(
            R.dimen.key_popup_textSize
        ))
        textView.text = keyView.getComputedLetter(keyView.data.popup[k].code)
        return textView
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

    /*fun prepare(keyView: KeyView) {
        val code = keyView.data.code
        if (code <= 32) {
            return
        }
        if (windows.containsKey(code)) {
            return
        }
        val popupView = View.inflate(keyboardView.context,
            R.layout.key_popup, null) as ViewGroup
        val w = createPopupWindow(popupView, 0, 0)
        w.setTouchInterceptor { _, event ->
            keyView.dispatchTouchEvent(event)
            true
        }
        w.showAtLocation(
            keyboardView, Gravity.LEFT or Gravity.TOP,
            (keyboardView.x + (keyView.parent as ViewGroup).x + keyView.x - ((keyPopupWidth - keyView.measuredWidth).toFloat() / 2.0f)).toInt(),
            (keyboardView.y + (keyView.parent as ViewGroup).y + keyView.y - (keyPopupHeight - keyView.measuredHeight)).toInt()
        )
        windows[code] = w
    }

    fun updateLocation(keyView: KeyView) {
        val code = keyView.data.code
        if (code <= 32) {
            return
        }
        val w = windows[code] ?: return
        w.update(
            (keyboardView.x + (keyView.parent as ViewGroup).x + keyView.x - ((keyPopupWidth - keyView.measuredWidth).toFloat() / 2.0f)).toInt(),
            (keyboardView.y + (keyView.parent as ViewGroup).y + keyView.y - (keyPopupHeight - keyView.measuredHeight)).toInt(),
            -1, -1
        )
    }*/

    fun show(keyView: KeyView) {
        val code = keyView.data.code
        if (code <= 32) {
            return
        }
        if (windows.containsKey(code)) {
            return
        }
        val popupView = View.inflate(keyboardView.context,
            R.layout.key_popup, null) as ViewGroup
        popupView.findViewById<TextView>(R.id.key_popup_text).text = keyView.getComputedLetter()
        popupView.findViewById<ImageView>(R.id.key_popup_threedots).visibility = when {
            keyView.data.popup.isEmpty() -> View.INVISIBLE
            else -> View.VISIBLE
        }
        val w = createPopupWindow(popupView, keyPopupWidth, keyPopupHeight)
        w.setTouchInterceptor { _, event ->
            keyView.dispatchTouchEvent(event)
            true
        }
        w.showAtLocation(
            keyboardView, Gravity.LEFT or Gravity.TOP,
            (keyboardView.x + (keyView.parent as ViewGroup).x + keyView.x - ((keyPopupWidth - keyView.measuredWidth).toFloat() / 2.0f)).toInt(),
            (keyboardView.y + (keyView.parent as ViewGroup).y + keyView.y - (keyPopupHeight - keyView.measuredHeight)).toInt()
        )
        windows[code] = w
    }

    fun extend(keyView: KeyView) {
        val code = keyView.data.code
        if (code <= 32 || keyView.data.popup.isEmpty()) {
            return
        }
        if (windowsExt.containsKey(code)) {
            return
        }
        val popupViewExt = View.inflate(keyboardView.context,
            R.layout.key_popup_extended, null) as ViewGroup
        // Extended popup layout:
        // row 1
        // row 0 (has always items, takes all if size <= 5, when higher and uneven 1 more than row 1
        val row0count = when {
            keyView.data.popup.size > 5 -> (keyView.data.popup.size.toFloat() / 2.0f).roundToInt()
            else -> keyView.data.popup.size
        }
        val row1count = keyView.data.popup.size - row0count
        val row0 = popupViewExt.findViewById<LinearLayout>(R.id.key_popup_extended_row0)
        val row1 = popupViewExt.findViewById<LinearLayout>(R.id.key_popup_extended_row1)
        row0.removeAllViews()
        row1.removeAllViews()
        for (k in keyView.data.popup.indices) {
            if (row1count > 0 && k < row1count) {
                row1.addView(createTextView(keyView, k))
            } else {
                row0.addView(createTextView(keyView, k))
            }
        }
        windows[code]?.contentView?.findViewById<ImageView>(R.id.key_popup_threedots)?.visibility = View.INVISIBLE
        val w = createPopupWindow(popupViewExt, row0count * keyPopupWidth, when {
            row1count > 0 -> keyView.measuredHeight * 2
            else -> keyView.measuredHeight
        })
        w.showAtLocation(
            keyboardView, Gravity.LEFT or Gravity.TOP,
            (keyboardView.x + (keyView.parent as ViewGroup).x + keyView.x - ((keyPopupWidth - keyView.measuredWidth).toFloat() / 2.0f)).toInt(),
            (keyboardView.y + (keyView.parent as ViewGroup).y + keyView.y - (keyPopupHeight - keyView.measuredHeight) - when {
                row1count > 0 -> keyView.measuredHeight
                else -> 0
            }).toInt()
        )
        windowsExt[code] = w
    }

    fun hide(keyView: KeyView) {
        val code = keyView.data.code
        if (code < 32) {
            return
        }
        // Hide popup delayed so the popup can show for persons who type fast
        /*Handler().postDelayed({*/
        windows[code]?.dismiss()
        windows.remove(code)
        windowsExt[code]?.dismiss()
        windowsExt.remove(code)
        /*}, 10)*/
    }

    /*fun reset() {
        for ((code, window) in windows) {
            window.dismiss()
            windows.remove(code)
        }
        for ((code, windowExt) in windowsExt) {
            windowExt.dismiss()
            windowsExt.remove(code)
        }
    }*/
}
