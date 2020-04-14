package dev.patrickgold.florisboard.ime

import android.annotation.SuppressLint
import android.os.Handler
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.key.KeyView
import dev.patrickgold.florisboard.util.*
import kotlin.math.roundToInt

@SuppressLint("RtlHardcoded")
class KeyPopupManager(kbd: CustomKeyboard) {

    private val keyPopupWidth = kbd.resources.getDimension(R.dimen.key_popup_width).toInt()
    private val keyPopupHeight = kbd.resources.getDimension(R.dimen.key_popup_height).toInt()
    private val keyboard: CustomKeyboard = kbd
    private var windows: HashMap<Int, PopupWindow> = hashMapOf()
    private var windowsExt: HashMap<Int, PopupWindow> = hashMapOf()

    private fun createTextView(keyView: KeyView, k: Int): TextView {
        val textView = TextView(keyboard.context)
        textView.layoutParams = ViewGroup.LayoutParams(keyPopupWidth, keyView.measuredHeight)
        textView.gravity = Gravity.CENTER
        setTextTintColor(textView,
            R.attr.key_popup_fgColor
        )
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, keyboard.resources.getDimension(
            R.dimen.key_popup_textSize
        ))
        textView.text = keyView.createPopupKeyText(keyView.data.popup[k].code)
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

    fun show(keyView: KeyView) {
        val code = keyView.data.code ?: 0
        if (code == 32) {
            return
        }
        if (windows.containsKey(code)) {
            return
        }
        val popupView = View.inflate(keyboard.context,
            R.layout.key_popup, null) as ViewGroup
        popupView.findViewById<TextView>(R.id.key_popup_text).text = keyView.createLabelText()
        popupView.findViewById<ImageView>(R.id.key_popup_threedots).visibility = when {
            keyView.data.popup.isEmpty() -> View.INVISIBLE
            else -> View.VISIBLE
        }
        val w = createPopupWindow(popupView, keyPopupWidth, keyPopupHeight)
        w.showAtLocation(
            keyboard, Gravity.LEFT or Gravity.TOP,
            (keyboard.x + (keyView.parent as ViewGroup).x + keyView.x - ((keyPopupWidth - keyView.measuredWidth).toFloat() / 2.0f)).toInt(),
            (keyboard.y + (keyView.parent as ViewGroup).y + keyView.y - (keyPopupHeight - keyView.measuredHeight)).toInt()
        )
        windows[code] = w
    }

    fun extend(keyView: KeyView) {
        val code = keyView.data.code ?: 0
        if (code == 32 || keyView.data.popup.isEmpty()) {
            return
        }
        if (windowsExt.containsKey(code)) {
            return
        }
        val popupViewExt = View.inflate(keyboard.context,
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
        //popupView.findViewById<ImageView>(R.id.key_popup_threedots).visibility = View.INVISIBLE
        val w = createPopupWindow(popupViewExt, row0count * keyPopupWidth, when {
            row1count > 0 -> keyView.measuredHeight * 2
            else -> keyView.measuredHeight
        })
        w.showAtLocation(
            keyboard, Gravity.LEFT or Gravity.TOP,
            (keyboard.x + (keyView.parent as ViewGroup).x + keyView.x - ((keyPopupWidth - keyView.measuredWidth).toFloat() / 2.0f)).toInt(),
            (keyboard.y + (keyView.parent as ViewGroup).y + keyView.y - (keyPopupHeight - keyView.measuredHeight) - when {
                row1count > 0 -> keyView.measuredHeight
                else -> 0
            }).toInt()
        )
        windowsExt[code] = w
    }

    fun hide(keyView: KeyView) {
        val code = keyView.data.code ?: 0
        if (code == 32 || keyView.data.code == null) {
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
