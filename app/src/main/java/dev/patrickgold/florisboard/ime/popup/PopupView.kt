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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.common.ViewUtils

class PopupView : View, ThemeManager.OnThemeUpdatedListener {
    private val themeManager: ThemeManager = ThemeManager.default()

    private var backgroundDrawable: PaintDrawable = PaintDrawable().apply {
        setCornerRadius(ViewUtils.dp2px(6.0f))
    }
    private val labelPaint: Paint = Paint().apply {
        alpha = 255
        color = 0
        isAntiAlias = true
        isFakeBoldText = false
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.key_textSize)
        typeface = Typeface.DEFAULT
    }
    private val threeDotsDrawable: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.ic_more_horiz)?.mutate()

    val properties: Properties = Properties(
        width = resources.getDimension(R.dimen.key_width).toInt(),
        height = resources.getDimension(R.dimen.key_height).toInt(),
        xOffset = 0,
        yOffset = 0,
        innerLabelFactor = 0.4f,
        label = "",
        labelTextSize = resources.getDimension(R.dimen.key_popup_textSize),
        shouldIndicateExtendedPopups = false
    )
    val isShowing: Boolean
        get() = visibility == VISIBLE

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        layoutDirection = LAYOUT_DIRECTION_LOCALE
        visibility = GONE
        background = backgroundDrawable
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        themeManager.registerOnThemeUpdatedListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        themeManager.unregisterOnThemeUpdatedListener(this)
    }

    override fun onThemeUpdated(theme: Theme) {
        backgroundDrawable.apply {
            setTint(theme.getAttr(Theme.Attr.POPUP_BACKGROUND).toSolidColor().color)
        }
        elevation = ViewUtils.dp2px(4.0f)
        threeDotsDrawable?.apply {
            setTint(theme.getAttr(Theme.Attr.POPUP_FOREGROUND).toSolidColor().color)
        }
        labelPaint.color = theme.getAttr(Theme.Attr.POPUP_FOREGROUND).toSolidColor().color
        if (isShowing) {
            invalidate()
        }
    }

    private fun applyProperties(keyboardView: View, anchor: Key) {
        val anchorCoords = IntArray(2)
        keyboardView.getLocationInWindow(anchorCoords)
        val anchorX = anchorCoords[0] + anchor.visibleBounds.left
        val anchorY = anchorCoords[1] + anchor.visibleBounds.top + anchor.visibleBounds.height()
        when (val lp = layoutParams) {
            is FrameLayout.LayoutParams -> lp.apply {
                width = properties.width
                height = properties.height
                setMargins(
                    anchorX + properties.xOffset,
                    anchorY + properties.yOffset,
                    0,
                    0
                )
            }
            else -> {
                layoutParams = FrameLayout.LayoutParams(properties.width, properties.height).apply {
                    setMargins(
                        anchorX + properties.xOffset,
                        anchorY + properties.yOffset,
                        0,
                        0
                    )
                }
            }
        }
        labelPaint.textSize = properties.labelTextSize
        if (isShowing) {
            requestLayout()
            invalidate()
        }
    }

    fun show(keyboardView: View, anchor: Key) {
        applyProperties(keyboardView, anchor)
        visibility = VISIBLE
        requestLayout()
        invalidate()
    }

    fun hide() {
        visibility = GONE
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        // Draw label
        val label = properties.label
        if (label.isNotEmpty()) {
            val centerX = measuredWidth / 2.0f
            val centerY =
                (measuredHeight * properties.innerLabelFactor) / 2.0f + (labelPaint.textSize - labelPaint.descent()) / 2
            canvas.drawText(label, centerX, centerY, labelPaint)
        }

        // Draw drawable
        val drawable = threeDotsDrawable
        if (drawable != null && properties.shouldIndicateExtendedPopups) {
            val marginTop = measuredHeight * properties.innerLabelFactor
            val drawableSize = marginTop * 0.25f
            if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                drawable.setBounds(
                    (measuredWidth * 0.10f).toInt(),
                    marginTop.toInt(),
                    (measuredWidth * 0.10f + drawableSize).toInt(),
                    (marginTop + drawableSize).toInt()
                )
            } else {
                drawable.setBounds(
                    (measuredWidth * 0.95f - drawableSize).toInt(),
                    marginTop.toInt(),
                    (measuredWidth * 0.95f).toInt(),
                    (marginTop + drawableSize).toInt()
                )
            }
            drawable.draw(canvas)
        }
    }

    data class Properties(
        var width: Int,
        var height: Int,
        var xOffset: Int,
        var yOffset: Int,
        var innerLabelFactor: Float,
        var label: String,
        var labelTextSize: Float,
        var shouldIndicateExtendedPopups: Boolean
    )
}
