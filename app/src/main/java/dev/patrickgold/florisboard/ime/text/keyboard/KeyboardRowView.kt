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

package dev.patrickgold.florisboard.ime.text.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.ViewLayoutUtils

/**
 * This class' sole purpose is to manage the layout within a row of [KeyboardView]. No logic is
 * handled in this class.
 */
class KeyboardRowView(context: Context, val keyboardView: KeyboardView) : FlexboxLayout(context) {
    init {
        val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
        val keyMarginH = if (keyboardView.isSmartbarKeyboardView){
            resources.getDimension(R.dimen.key_marginH).toInt()
        }else{
            ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingHorizontal, context).toInt()
        }
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(
                keyMarginH, 0,
                keyMarginH, 0
            )
        }
        flexDirection = FlexDirection.ROW
        justifyContent = JustifyContent.CENTER
        setPadding(0, 0, 0, 0)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
        val keyMarginH = if (keyboardView.isSmartbarKeyboardView){
            resources.getDimension(R.dimen.key_marginH).toInt()
        }else{
            ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingHorizontal, context).toInt()
        }
        (layoutParams as MarginLayoutParams).setMargins(
            keyMarginH, 0,
            keyMarginH, 0
        )

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
