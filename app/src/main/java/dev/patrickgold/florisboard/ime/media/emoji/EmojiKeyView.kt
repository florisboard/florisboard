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
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.text.key.KeyHintMode
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

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
    private val emojiKeyboardView: EmojiKeyboardView,
    val data: EmojiKeyData
) : androidx.appcompat.widget.AppCompatTextView(emojiKeyboardView.context), CoroutineScope by MainScope(),
    FlorisBoard.EventListener, ThemeManager.OnThemeUpdatedListener {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)

    private var isCancelled: Boolean = false
    private var osHandler: Handler? = null
    private var triangleDrawable: Drawable? = null

    init {
        background = null
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.emoji_key_textSize))

        triangleDrawable = ContextCompat.getDrawable(context, R.drawable.triangle_bottom_right)

        text = data.getCodePointsAsString()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        florisboard?.addEventListener(this)
    }

    override fun onDetachedFromWindow() {
        florisboard?.removeEventListener(this)
        super.onDetachedFromWindow()
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
                val delayMillis = prefs.keyboard.longPressDelay
                if (osHandler == null) {
                    osHandler = Handler()
                }
                osHandler?.postDelayed({
                    (parent.parent as ScrollView)
                        .requestDisallowInterceptTouchEvent(true)
                    emojiKeyboardView.isScrollBlocked = true
                    emojiKeyboardView.popupManager.show(this, KeyHintMode.DISABLED)
                    emojiKeyboardView.popupManager.extend(this, KeyHintMode.DISABLED)
                    florisboard?.keyPressVibrate()
                    florisboard?.keyPressSound()
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
                        florisboard?.keyPressVibrate()
                        florisboard?.keyPressSound()
                    }
                    florisboard?.mediaInputManager?.sendEmojiKeyPress(retData)
                    performClick()
                }
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    isCancelled = true
                }
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        triangleDrawable?.setBounds(
            (measuredWidth * 0.75f).toInt(),
            (measuredHeight * 0.75f).toInt(),
            (measuredWidth * 0.85f).toInt(),
            (measuredHeight * 0.85f).toInt()
        )
    }

    override fun onThemeUpdated(theme: Theme) {
        triangleDrawable?.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                theme.getAttr(Theme.Attr.MEDIA_FOREGROUND_ALT).toSolidColor().color, BlendModeCompat.SRC_ATOP
            )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        if (data.popup.isNotEmpty()) {
            triangleDrawable?.draw(canvas)
        }
    }
}
