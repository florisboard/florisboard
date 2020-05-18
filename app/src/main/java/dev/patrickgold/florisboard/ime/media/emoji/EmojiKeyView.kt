package dev.patrickgold.florisboard.ime.media.emoji

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard

@SuppressLint("ViewConstructor")
class EmojiKeyView(
    private val florisboard: FlorisBoard,
    private val emojiKeyboardView: EmojiKeyboardView,
    private val emojiKeyData: EmojiKeyData
) : androidx.appcompat.widget.AppCompatButton(florisboard.context) {

    init {
        background = ContextCompat.getDrawable(context, R.drawable.button_transparent_bg_on_press)
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.emoji_key_textSize))

        text = emojiKeyData.getCodePointsAsString()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                florisboard.mediaInputManager.sendEmojiKeyPress(emojiKeyData)
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
