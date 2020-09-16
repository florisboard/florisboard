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

package dev.patrickgold.florisboard.ime.text.editing

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageButton
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import java.util.*

/**
 * View class for managing and rendering an editing key.
 */
class EditingKeyView : AppCompatImageButton {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    private val data: KeyData
    private var isKeyPressed: Boolean = false
    private var osTimer: Timer? = null

    private var label: String? = null
    private var labelPaint: Paint = Paint().apply {
        alpha = 255
        color = 0
        isAntiAlias = true
        isFakeBoldText = false
        textAlign = Paint.Align.CENTER
        textSize = Button(context).textSize
        typeface = Typeface.DEFAULT
    }

    var isHighlighted: Boolean = false
        set(value) { field = value; invalidate() }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.style.TextEditingButton)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val code = when (id) {
            R.id.arrow_down -> KeyCode.ARROW_DOWN
            R.id.arrow_left -> KeyCode.ARROW_LEFT
            R.id.arrow_right -> KeyCode.ARROW_RIGHT
            R.id.arrow_up -> KeyCode.ARROW_UP
            R.id.backspace -> KeyCode.DELETE
            R.id.clipboard_copy -> KeyCode.CLIPBOARD_COPY
            R.id.clipboard_cut -> KeyCode.CLIPBOARD_CUT
            R.id.clipboard_paste -> KeyCode.CLIPBOARD_PASTE
            R.id.move_home -> KeyCode.MOVE_HOME
            R.id.move_end -> KeyCode.MOVE_END
            R.id.select -> KeyCode.CLIPBOARD_SELECT
            R.id.select_all -> KeyCode.CLIPBOARD_SELECT_ALL
            else -> 0
        }
        data = KeyData(code)
        context.obtainStyledAttributes(attrs, R.styleable.EditingKeyView).apply {
            label = getString(R.styleable.EditingKeyView_android_text)
            recycle()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled || event == null) {
            return false
        }
        super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isKeyPressed = true
                florisboard?.keyPressVibrate()
                florisboard?.keyPressSound(data)
                when (data.code) {
                    KeyCode.ARROW_DOWN,
                    KeyCode.ARROW_LEFT,
                    KeyCode.ARROW_RIGHT,
                    KeyCode.ARROW_UP,
                    KeyCode.DELETE -> {
                        osTimer = Timer()
                        osTimer?.scheduleAtFixedRate(object : TimerTask() {
                            override fun run() {
                                florisboard?.textInputManager?.sendKeyPress(data)
                                if (!isKeyPressed) {
                                    osTimer?.cancel()
                                    osTimer = null
                                }
                            }
                        }, 500, 50)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isKeyPressed = false
                osTimer?.cancel()
                osTimer = null
                if (event.actionMasked != MotionEvent.ACTION_CANCEL) {
                    florisboard?.textInputManager?.sendKeyPress(data)
                }
            }
            else -> return false
        }
        return true
    }

    /**
     * Draw the key label / drawable.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        imageTintList = ColorStateList.valueOf(when {
            isEnabled -> prefs.theme.smartbarFgColor
            else -> prefs.theme.smartbarFgColorAlt
        })

        // Draw label
        val label = label
        if (label != null) {
            labelPaint.color = if (isHighlighted && isEnabled) {
                prefs.theme.colorPrimary
            } else if (!isEnabled) {
                prefs.theme.smartbarFgColorAlt
            } else {
                prefs.theme.smartbarFgColor
            }
            val isPortrait =
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            if (!isPortrait) {
                labelPaint.textSize *= 0.9f
            }
            val centerX = measuredWidth / 2.0f
            val centerY = measuredHeight / 2.0f + (labelPaint.textSize - labelPaint.descent()) / 2
            if (label.contains("\n")) {
                // Even if more lines may be existing only the first 2 are shown
                val labelLines = label.split("\n")
                canvas.drawText(labelLines[0], centerX, centerY * 0.70f, labelPaint)
                canvas.drawText(labelLines[1], centerX, centerY * 1.30f, labelPaint)
            } else {
                canvas.drawText(label, centerX, centerY, labelPaint)
            }
        }
    }
}
