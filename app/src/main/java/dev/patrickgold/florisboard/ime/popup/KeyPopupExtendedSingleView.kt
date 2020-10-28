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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.*

@SuppressLint("ViewConstructor")
class KeyPopupExtendedSingleView(
    context: Context, val adjustedIndex: Int, var isActive: Boolean = false
) : androidx.appcompat.widget.AppCompatTextView(
    context, null, 0
) {
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    var iconDrawable: Drawable? = null

    init {
        background = getDrawable(context, R.drawable.shape_rect_rounded)
    }

    override fun onDraw(canvas: Canvas?) {
        setBackgroundTintColor2(this, when {
            isActive -> prefs.theme.keyPopupBgColorActive
            else -> Color.TRANSPARENT
        })
        setTextColor(prefs.theme.keyPopupFgColor)

        super.onDraw(canvas)

        canvas ?: return

        val drawable = iconDrawable
        val drawablePadding = (0.2f * measuredHeight).toInt()
        if (drawable != null) {
            var marginV = 0
            var marginH = 0
            if (measuredWidth > measuredHeight) {
                marginH = (measuredWidth - measuredHeight) / 2
            } else {
                marginV = (measuredHeight - measuredWidth) / 2
            }
            drawable.setBounds(
                marginH + drawablePadding,
                marginV + drawablePadding,
                measuredWidth - marginH - drawablePadding,
                measuredHeight - marginV - drawablePadding)
            drawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                prefs.theme.keyPopupFgColor,
                BlendModeCompat.SRC_ATOP
            )
            drawable.draw(canvas)
        }
    }
}
