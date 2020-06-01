/*
 * Copyright (C) 2020 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.media.emoji

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.util.getColorFromAttr

/**
 * View class for managing the rendering and the events of a single emoji keyboard key.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 * @property emojiKeyboardView Reference to the parent [EmojiKeyboardView].
 * @property data The data the current key represents. Is used to determine rendering and possible
 *  behaviour when events occur.
 */
@SuppressLint("ViewConstructor")
class EmojiKeyView(
    private val florisboard: FlorisBoard,
    private val emojiKeyboardView: EmojiKeyboardView,
    val data: EmojiKeyData
) : androidx.appcompat.widget.AppCompatTextView(florisboard.context) {

    private var isCancelled: Boolean = false
    private var osHandler: Handler? = null
    private var triangleDrawable: Drawable? = null

    init {
        background = null
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.emoji_key_textSize))

        triangleDrawable = resources.getDrawable(
            R.drawable.triangle_bottom_right, context.theme
        )
        triangleDrawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorFromAttr(context, R.attr.emoji_key_fgColor), BlendModeCompat.SRC_ATOP
        )

        text = data.getCodePointsAsString()
    }

    /**
     * Logic for handling a touch event. Cancels the touch event if the pointer moves to far from
     * popup and/or key.
     *
     * @param event The [MotionEvent] that should be processed by this view.
     * @return If this view has handled the touch event.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isCancelled = false
                val delayMillis = florisboard.prefs.looknfeel.longPressDelay
                if (osHandler == null) {
                    osHandler = Handler()
                }
                osHandler?.postDelayed({
                    (parent.parent as HorizontalScrollView)
                        .requestDisallowInterceptTouchEvent(true)
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
                val retData =
                    emojiKeyboardView.popupManager.getActiveEmojiKeyData(this)
                emojiKeyboardView.popupManager.hide()
                if (event.actionMasked != MotionEvent.ACTION_CANCEL &&
                    retData != null && !isCancelled) {
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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        if (data.popup.isNotEmpty()) {
            triangleDrawable?.setBounds(
                (measuredWidth * 0.75f).toInt(),
                (measuredHeight * 0.75f).toInt(),
                (measuredWidth * 0.85f).toInt(),
                (measuredHeight * 0.85f).toInt()
            )
            triangleDrawable?.draw(canvas)
        }
    }
}
