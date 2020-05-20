package dev.patrickgold.florisboard.ime.media.emoji

import android.annotation.SuppressLint
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard

@SuppressLint("ViewConstructor")
class EmojiKeyView(
    private val florisboard: FlorisBoard,
    private val emojiKeyboardView: EmojiKeyboardView,
    val data: EmojiKeyData
) : androidx.appcompat.widget.AppCompatTextView(florisboard.context) {

    private var isCancelled: Boolean = false
    private var osHandler: Handler? = null

    init {
        background = null
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.emoji_key_textSize))

        text = data.getCodePointsAsString()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isCancelled = false
                val delayMillis = florisboard.prefs!!.longPressDelay
                if (osHandler == null) {
                    osHandler = Handler()
                }
                osHandler?.postDelayed({
                    (parent.parent as HorizontalScrollView).requestDisallowInterceptTouchEvent(true)
                    emojiKeyboardView.isScrollBlocked = true
                    emojiKeyboardView.popupManager.show(this)
                    emojiKeyboardView.popupManager.extend(this)
                    florisboard.keyPressVibrate()
                    florisboard.keyPressSound()
                }, delayMillis.toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (emojiKeyboardView.popupManager.isShowingExtendedPopup) {
                    val isPointerWithinBounds =
                        emojiKeyboardView.popupManager.propagateMotionEvent(this, event)
                    if (!isPointerWithinBounds) {
                        emojiKeyboardView.dismissKeyView(this)
                    }
                } else {
                    if (event.x < -0.1f * measuredWidth || event.x > 1.1f * measuredWidth
                        || event.y < -0.1f * measuredHeight || event.y > 1.1f * measuredHeight
                    ) {
                        emojiKeyboardView.dismissKeyView(this)
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                osHandler?.removeCallbacksAndMessages(null)
                val retData = emojiKeyboardView.popupManager.getActiveEmojiKeyData(this)
                emojiKeyboardView.popupManager.hide()
                if (event.actionMasked != MotionEvent.ACTION_CANCEL && retData != null && !isCancelled) {
                    if (!emojiKeyboardView.isScrollBlocked) {
                        florisboard.keyPressVibrate()
                        florisboard.keyPressSound()
                    }
                    florisboard.mediaInputManager.sendEmojiKeyPress(retData)
                    performClick()
                }
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    isCancelled = true
                }
            }
        }
        return true
    }
}
