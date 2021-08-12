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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.text.key.KeyHintConfiguration
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/**
 * View class for managing the rendering and the events of a single emoji keyboard key.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 * @property emojiKeyboardView Reference to the parent [EmojiKeyboardView].
 * @property key The current key. Is used to determine rendering and possible behaviour when events occur.
 */
@SuppressLint("ViewConstructor")
class EmojiKeyView(
    private val emojiKeyboardView: EmojiKeyboardView,
    key: EmojiKey
) : androidx.appcompat.widget.AppCompatTextView(emojiKeyboardView.context), CoroutineScope by MainScope(),
    FlorisBoard.EventListener, ThemeManager.OnThemeUpdatedListener {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val prefs get() = Preferences.default()

    private var isCancelled: Boolean = false
    private var osHandler: Handler? = null
    private var triangleDrawable: Drawable? = null

    var key: EmojiKey = key
        set(value) {
            field = value
            text = value.data.asString(true)
        }

    init {
        background = null
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.emoji_key_textSize))

        triangleDrawable = ContextCompat.getDrawable(context, R.drawable.triangle_bottom_right)?.mutate()

        text = key.data.asString(isForDisplay = true)
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
                    (parent as RecyclerView)
                        .requestDisallowInterceptTouchEvent(true)
                    emojiKeyboardView.isScrollBlocked = true
                    emojiKeyboardView.popupManager.show(key, KeyHintConfiguration.HINTS_DISABLED)
                    emojiKeyboardView.popupManager.extend(key, KeyHintConfiguration.HINTS_DISABLED)
                    florisboard?.inputFeedbackManager?.keyPress(TextKeyData.UNSPECIFIED)
                }, delayMillis.toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (emojiKeyboardView.popupManager.isShowingExtendedPopup) {
                    val isPointerWithinBounds =
                        emojiKeyboardView.popupManager.propagateMotionEvent(key, event, 0)
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
                    emojiKeyboardView.popupManager.getActiveEmojiKeyData(key)
                emojiKeyboardView.popupManager.hide()
                if (event.actionMasked != MotionEvent.ACTION_CANCEL &&
                    retData != null && !isCancelled
                ) {
                    if (!emojiKeyboardView.isScrollBlocked) {
                        florisboard?.inputFeedbackManager?.keyPress(TextKeyData.UNSPECIFIED)
                    }
                    (retData as? EmojiKeyData)?.let { florisboard?.mediaInputManager?.sendEmojiKeyPress(it) }
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        key.visibleBounds.set(left, top, right, bottom)
        key.touchBounds.set(left, top, right, bottom)
    }

    override fun onThemeUpdated(theme: Theme) {
        triangleDrawable?.setTint(theme.getAttr(Theme.Attr.MEDIA_FOREGROUND_ALT).toSolidColor().color)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        if (key.computedPopups.getPopupKeys(KeyHintConfiguration.HINTS_DISABLED).isNotEmpty()) {
            triangleDrawable?.draw(canvas)
        }
    }
}
