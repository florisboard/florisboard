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

package dev.patrickgold.florisboard.ime.editing

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageButton
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.util.getColorFromAttr

class EditingKeyView : AppCompatImageButton {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val data: KeyData

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

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.style.TextEditingButton)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val code = when (id) {
            R.id.arrow_down -> KeyCode.ARROW_DOWN
            R.id.arrow_left -> KeyCode.ARROW_LEFT
            R.id.arrow_right -> KeyCode.ARROW_RIGHT
            R.id.arrow_up -> KeyCode.ARROW_UP
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

    /**
     * Draw the key label / drawable.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        // Draw label
        val label = label
        if (label != null) {
            labelPaint.color = if (data.code == KeyCode.CLIPBOARD_SELECT && false) {
                getColorFromAttr(context, R.attr.colorAccent)
            } else {
                getColorFromAttr(context, R.attr.key_fgColor)
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
