/*
 * Copyright (C) 2021 Patrick Goldinger
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
import android.graphics.*
import android.graphics.drawable.PaintDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import dev.patrickgold.florisboard.common.ViewUtils

class TextKeyView : View {
    private val parentKeyboardView get() = parent as? TextKeyboardView

    var key: TextKey? = null

    val bgDrawable = PaintDrawable().also {
        it.setCornerRadius(ViewUtils.dp2px(6.0f))
    }
    val labelPaint: Paint = Paint().also {
        it.isAntiAlias = true
        it.isFakeBoldText = false
        it.textAlign = Paint.Align.CENTER
        it.typeface = Typeface.DEFAULT
    }
    val hintedLabelPaint: Paint = Paint().also {
        it.isAntiAlias = true
        it.isFakeBoldText = false
        it.textAlign = Paint.Align.CENTER
        it.typeface = Typeface.MONOSPACE
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        background = bgDrawable
        setWillNotDraw(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onHoverEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        return // This view does not measure itself
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val key = key ?: return
        canvas ?: return
        canvas.save()
        canvas.translate(-x, -y)
        parentKeyboardView?.onDrawComputedKey(canvas, key, this)
        canvas.restore()
    }
}
