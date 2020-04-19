package dev.patrickgold.florisboard.ime.popup

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.key.KeyData
import dev.patrickgold.florisboard.ime.key.KeyView
import dev.patrickgold.florisboard.ime.keyboard.KeyboardView
import dev.patrickgold.florisboard.util.setTextTintColor
import kotlin.math.roundToInt

@SuppressLint("RtlHardcoded")
class KeyPopupManager(
    private val keyboardView: KeyboardView,
    private val keyView: KeyView
) {

    private var anchorLeft: Boolean = false
    private var anchorRight: Boolean = false
    private var activeExtIndex: Int? = null
    private val keyPopupWidth = keyboardView.resources.getDimension(R.dimen.key_popup_width).toInt()
    private val keyPopupHeight = keyboardView.resources.getDimension(R.dimen.key_popup_height).toInt()
    private val popupView = View.inflate(keyboardView.context,
        R.layout.key_popup, null) as LinearLayout
    private val popupViewExt = View.inflate(keyboardView.context,
        R.layout.key_popup_extended, null) as FlexboxLayout
    private var row0count: Int = 0
    private var row1count: Int = 0
    private var window: PopupWindow? = null
    private var windowExt: PopupWindow? = null

    val isShowingPopup: Boolean
        get() = window != null
    val isShowingExtendedPopup: Boolean
        get() = windowExt != null

    private fun createTextView(
        keyView: KeyView,
        k: Int,
        isInitActive: Boolean = false,
        isWrapBefore: Boolean = false
    ): TextView {
        val textView = KeyPopupExtendedSingleView(keyboardView.context, isInitActive)
        val lp = FlexboxLayout.LayoutParams(keyPopupWidth, keyView.measuredHeight)
        lp.isWrapBefore = isWrapBefore
        textView.layoutParams = lp
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

    fun show() {
        val code = keyView.data.code
        if (code <= 32) {
            return
        }
        if (isShowingPopup) {
            return
        }
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
        window = w
    }

    fun extend() {
        // TODO: cleanup and spilt into multiple functions
        val code = keyView.data.code
        if (code <= 32 || keyView.data.popup.isEmpty()) {
            return
        }
        if (isShowingExtendedPopup) {
            return
        }
        anchorLeft = keyView.x < keyboardView.measuredWidth / 2
        anchorRight = !anchorLeft
        // Extended popup layout:
        // row 1
        // row 0 (has always items, takes all if size <= 5, when higher and uneven 1 more than row 1
        row0count = when {
            keyView.data.popup.size > 5 -> (keyView.data.popup.size.toFloat() / 2.0f).roundToInt()
            else -> keyView.data.popup.size
        }
        row1count = keyView.data.popup.size - row0count
        popupViewExt.removeAllViews()
        for (k in keyView.data.popup.indices) {
            val isInitActive = anchorLeft && (k - row1count == 0) || anchorRight && (k - row1count == row0count - 1)
            popupViewExt.addView(createTextView(
                keyView, k, isInitActive, (row1count > 0) && (k - row1count == 0)
            ))
            if (isInitActive) {
                activeExtIndex = k
            }
        }
        popupView.findViewById<ImageView>(R.id.key_popup_threedots)?.visibility = View.INVISIBLE
        val extWidth = row0count * keyPopupWidth
        val extHeight = when {
            row1count > 0 -> keyView.measuredHeight * 2
            else -> keyView.measuredHeight
        }
        popupViewExt.justifyContent = if (anchorLeft) { JustifyContent.FLEX_START } else { JustifyContent.FLEX_END }
        popupViewExt.layoutParams = FlexboxLayout.LayoutParams(extWidth, extHeight)
        val w = createPopupWindow(popupViewExt, extWidth, extHeight)
        val x = (keyboardView.x + (keyView.parent as ViewGroup).x + keyView.x - ((keyPopupWidth - keyView.measuredWidth).toFloat() / 2.0f)).toInt() + when {
            anchorLeft -> 0
            else -> -extWidth + keyPopupWidth
        }
        val y = (keyboardView.y + (keyView.parent as ViewGroup).y + keyView.y - (keyPopupHeight - keyView.measuredHeight) - when {
            row1count > 0 -> keyView.measuredHeight
            else -> 0
        }).toInt()
        w.showAtLocation(keyboardView, Gravity.LEFT or Gravity.TOP, x, y)
        windowExt = w
    }

    fun propagateMotionEvent(event: MotionEvent) {
        if (!isShowingExtendedPopup) {
            return
        }

        val kX: Float = event.x / keyPopupWidth.toFloat()

        activeExtIndex = when {
            anchorLeft -> when {
                // row 1
                event.y < 0 && row1count > 0 -> when {
                    kX >= row1count         -> row1count - 1
                    kX < 0                  -> 0
                    else                    -> kX.toInt()
                }
                // row 0
                else -> when {
                    kX >= row0count         -> row1count + row0count - 1
                    kX < 0                  -> row1count
                    else                    -> row1count + kX.toInt()
                }
            }
            anchorRight -> when {
                // row 1
                event.y < 0 && row1count > 0 -> when {
                    kX >= 0                 -> row1count - 1
                    kX < -(row1count - 1)   -> 0
                    else                    -> row1count - 2 + kX.toInt()
                }
                // row 0
                else -> when {
                    kX >= 0                 -> row1count + row0count - 1
                    kX < -(row0count - 1)   -> row1count
                    else                    -> row1count + row0count - 2 + kX.toInt()
                }
            }
            else -> -1
        }

        for (k in keyView.data.popup.indices) {
            val textView = popupViewExt.getChildAt(k) as KeyPopupExtendedSingleView
            textView.isActive = k == activeExtIndex
        }
    }

    fun getActiveKeyData(): KeyData {
        return keyView.data.popup.getOrNull(activeExtIndex ?: -1) ?: keyView.data
    }

    fun hide() {
        window?.dismiss()
        window = null
        windowExt?.dismiss()
        windowExt = null

        activeExtIndex = null
    }
}
