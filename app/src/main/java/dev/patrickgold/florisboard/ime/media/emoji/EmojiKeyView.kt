package dev.patrickgold.florisboard.ime.media.emoji

import android.annotation.SuppressLint
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.util.setBackgroundTintColor

@SuppressLint("ViewConstructor")
class EmojiKeyView(
    private val florisboard: FlorisBoard,
    private val emojiKeyboardView: EmojiKeyboardView,
    val data: EmojiKeyData
) : androidx.appcompat.widget.AppCompatButton(florisboard.context) {

    private var isKeyPressed: Boolean = false
        set(value) {
            field = value
            updateKeyPressedBackground()
        }
    private val osHandler = Handler()

    init {
        background = ContextCompat.getDrawable(context, R.drawable.shape_rect)
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.emoji_key_textSize))

        text = data.getCodePointsAsString()

        updateKeyPressedBackground()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                emojiKeyboardView.popupManager.show(this)
                isKeyPressed = true
                florisboard.keyPressVibrate()
                florisboard.keyPressSound()
                val delayMillis = florisboard.prefs!!.longPressDelay
                osHandler.postDelayed({
                    if (data.popup.isNotEmpty()) {
                        emojiKeyboardView.popupManager.extend(this)
                    }
                }, delayMillis.toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (emojiKeyboardView.popupManager.isShowingExtendedPopup) {
                    val isPointerWithinBounds =
                        emojiKeyboardView.popupManager.propagateMotionEvent(this, event)
                    if (!isPointerWithinBounds) {
                        //emojiKeyboardView.dismissActiveKeyViewReference()
                    }
                } else {
                    if (event.x < -0.1f * measuredWidth || event.x > 1.1f * measuredWidth
                        || event.y < -0.35f * measuredHeight || event.y > 1.35f * measuredHeight
                    ) {
                        //emojiKeyboardView.dismissActiveKeyViewReference()
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isKeyPressed = false
                osHandler.removeCallbacksAndMessages(null)
                val retData = emojiKeyboardView.popupManager.getActiveEmojiKeyData(this)
                emojiKeyboardView.popupManager.hide()
                if (event.actionMasked != MotionEvent.ACTION_CANCEL && retData != null) {
                    florisboard.mediaInputManager.sendEmojiKeyPress(retData)
                    performClick()
                }
            }
        }
        return true
    }

    /**
     * Updates the background depending on [isKeyPressed].
     */
    private fun updateKeyPressedBackground() {
        setBackgroundTintColor(this, when {
            isKeyPressed -> R.attr.emoji_key_bgColorPressed
            else -> R.attr.emoji_key_bgColor
        })
    }
}
