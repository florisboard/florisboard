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

package dev.patrickgold.florisboard.ime.core

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.common.ViewUtils
import kotlin.math.roundToInt

/**
 * Root view of the keyboard.
 */
class InputView : LinearLayout {
    private val florisboard get() = FlorisBoard.getInstance()
    private val prefs get() = Preferences.default()

    var desiredInputViewHeight: Float = resources.getDimension(R.dimen.inputView_baseHeight)
        private set
    var desiredSmartbarHeight: Float = resources.getDimension(R.dimen.smartbar_baseHeight)
        private set
    var desiredTextKeyboardViewHeight: Float = resources.getDimension(R.dimen.textKeyboardView_baseHeight)
        private set
    var desiredMediaKeyboardViewHeight: Float = resources.getDimension(R.dimen.mediaKeyboardView_baseHeight)
        private set
    var heightFactor: Float = 1.0f
        private set
    var shouldGiveAdditionalSpace: Boolean = false
        private set

    var desiredInlineSuggestionsMinWidth: Int = 0
        private set
    var desiredInlineSuggestionsMinHeight: Int = 0
        private set
    var desiredInlineSuggestionsMaxWidth: Int = 0
        private set
    var desiredInlineSuggestionsMaxHeight: Int = 0
        private set

    private val overlayTextPaint: TextPaint = TextPaint().apply {
        color = Color.GREEN
        textAlign = Paint.Align.RIGHT
        textSize = resources.getDimension(R.dimen.devtools_memory_overlay_textSize)
        typeface = Typeface.MONOSPACE
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        heightFactor = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 1.0f
            else -> if (prefs.keyboard.oneHandedMode != OneHandedMode.OFF) {
                prefs.keyboard.oneHandedModeScaleFactor / 100.0f
            } else {
                1.0f
            }
        } * when (prefs.keyboard.heightFactor) {
            "extra_short" -> 0.85f
            "short" -> 0.90f
            "mid_short" -> 0.95f
            "normal" -> 1.00f
            "mid_tall" -> 1.05f
            "tall" -> 1.10f
            "extra_tall" -> 1.15f
            "custom" -> prefs.keyboard.heightFactorCustom.toFloat() / 100.0f
            else -> 1.00f
        }
        var baseHeight = calcInputViewHeight() * heightFactor
        var baseSmartbarHeight = 0.16129f * baseHeight
        var baseTextInputHeight = baseHeight - baseSmartbarHeight
        val tim = florisboard.textInputManager
        shouldGiveAdditionalSpace = prefs.keyboard.numberRow &&
                !(tim.getActiveKeyboardMode() == KeyboardMode.NUMERIC ||
                tim.getActiveKeyboardMode() == KeyboardMode.PHONE ||
                tim.getActiveKeyboardMode() == KeyboardMode.PHONE2)
        if (shouldGiveAdditionalSpace) {
            val additionalHeight = baseTextInputHeight * 0.25f
            baseHeight += additionalHeight
            baseTextInputHeight += additionalHeight
        }
        val smartbarDisabled = !prefs.smartbar.enabled ||
                tim.activeState.keyVariation == KeyVariation.PASSWORD && prefs.keyboard.numberRow && !prefs.suggestion.api30InlineSuggestionsEnabled
        if (smartbarDisabled) {
            baseHeight = baseTextInputHeight
            baseSmartbarHeight = 0.0f
        }
        desiredInputViewHeight = baseHeight
        desiredSmartbarHeight = baseSmartbarHeight
        desiredTextKeyboardViewHeight = baseTextInputHeight
        desiredMediaKeyboardViewHeight = baseHeight
        // Add bottom offset for curved screens here. As the desired heights have already been set,
        //  adding a value to the height now will result in a bottom padding (aka offset).
        baseHeight += ViewUtils.dp2px(
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                prefs.keyboard.bottomOffsetLandscape.toFloat()
            } else {
                prefs.keyboard.bottomOffsetPortrait.toFloat()
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            desiredInlineSuggestionsMinWidth = width / 3
            desiredInlineSuggestionsMinHeight = desiredSmartbarHeight.toInt()
            desiredInlineSuggestionsMaxWidth = (width / 1.5).toInt()
            desiredInlineSuggestionsMaxHeight = desiredSmartbarHeight.toInt()
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(baseHeight.roundToInt(), MeasureSpec.EXACTLY))
    }

    /**
     * Calculates the input view height based on the current screen dimensions and the auto
     * selected dimension values.
     *
     * This method and the fraction values have been inspired by [OpenBoard](https://github.com/dslul/openboard)
     * but are not 1:1 the same. This implementation differs from the
     * [original](https://github.com/dslul/openboard/blob/90ae4c8aec034a8935e1fd02b441be25c7dba6ce/app/src/main/java/org/dslul/openboard/inputmethod/latin/utils/ResourceUtils.java)
     * by calculating the average of the min and max height values, then taking at least the input
     * view base height and return this resulting value.
     */
    private fun calcInputViewHeight(): Float {
        val dm: DisplayMetrics = resources.displayMetrics
        val minBaseSize: Float = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> resources.getFraction(
                R.fraction.inputView_minHeightFraction, dm.heightPixels, dm.heightPixels
            )
            else -> resources.getFraction(
                R.fraction.inputView_minHeightFraction, dm.widthPixels, dm.widthPixels
            )
        }
        val maxBaseSize: Float = resources.getFraction(
            R.fraction.inputView_maxHeightFraction, dm.heightPixels, dm.heightPixels
        )
        return ((minBaseSize + maxBaseSize) / 2.0f).coerceAtLeast(
            resources.getDimension(R.dimen.inputView_baseHeight)
        )
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas ?: return

        if (prefs.devtools.enabled && prefs.devtools.showHeapMemoryStats) {
            try {
                // Note: the below code only gets the heap size in MB, the actual RAM usage (native or others) can be
                //  a lot higher
                //  Source: https://stackoverflow.com/a/19267315/6801193
                val runtime = Runtime.getRuntime()
                val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
                val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
                val availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB
                val output = listOf(
                    "heap mem:",
                    String.format("used=%4dMB", usedMemInMB),
                    String.format("max=%4dMB", maxHeapSizeInMB),
                    String.format("avail=%4dMB", availHeapSizeInMB),
                )
                val x = measuredWidth.toFloat()
                var y = overlayTextPaint.descent() - overlayTextPaint.ascent()
                for (line in output) {
                    canvas.drawText(line, x, y, overlayTextPaint)
                    y += overlayTextPaint.descent() - overlayTextPaint.ascent()
                }
            } catch (_: Throwable) {
            }
        }
    }
}
