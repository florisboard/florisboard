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

package dev.patrickgold.florisboard.ime.popup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import dev.patrickgold.florisboard.ime.clip.ClipboardPopupManager
import dev.patrickgold.florisboard.ime.clip.ClipboardPopupView

/**
 * Basic helper view class which acts as a non-interactive layer view, which sits above the whole
 * input UI. Automatically rejects any touch events and passes it through to the View below.
 */
class PopupLayerView : FrameLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        background = null
        isClickable = false
        isFocusable = false
        layoutDirection = LAYOUT_DIRECTION_LTR
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        setWillNotDraw(true)
    }

    var clipboardPopupManager: ClipboardPopupManager? = null
    var intercept: ClipboardPopupView? = null
    var shouldIntercept = true

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            intercept?.run {
                val viewRect = Rect()
                getGlobalVisibleRect(viewRect)
                return when {
                    !viewRect.contains(ev.x.toInt(), ev.y.toInt()) -> {
                        clipboardPopupManager?.hide()
                        true
                    }
                    else -> false
                }
            }
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }
}
