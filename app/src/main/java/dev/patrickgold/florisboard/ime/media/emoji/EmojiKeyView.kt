package dev.patrickgold.florisboard.ime.media.emoji

import android.annotation.SuppressLint
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

        text = emojiKeyData.getCodePointsAsString()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        android.util.Log.i("EKV", "HI")
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
