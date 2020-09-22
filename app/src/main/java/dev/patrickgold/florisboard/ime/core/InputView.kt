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
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import android.widget.ViewFlipper
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import kotlin.math.roundToInt

/**
 * Root view of the keyboard. Notifies [FlorisBoard] when it has been attached to a window.
 */
class InputView : LinearLayout {
    private var florisboard: FlorisBoard = FlorisBoard.getInstance()
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)

    var desiredInputViewHeight: Int = resources.getDimension(R.dimen.inputView_baseHeight).roundToInt()
        private set
    var desiredSmartbarHeight: Int = resources.getDimension(R.dimen.smartbar_baseHeight).roundToInt()
        private set
    var desiredTextKeyboardViewHeight: Int = resources.getDimension(R.dimen.textKeyboardView_baseHeight).roundToInt()
        private set
    var desiredMediaKeyboardViewHeight: Int = resources.getDimension(R.dimen.mediaKeyboardView_baseHeight).roundToInt()
        private set

    var mainViewFlipper: ViewFlipper? = null
        private set
    var oneHandedCtrlPanelStart: LinearLayout? = null
        private set
    var oneHandedCtrlPanelEnd: LinearLayout? = null
        private set

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onAttachedToWindow()")

        super.onAttachedToWindow()

        mainViewFlipper = findViewById(R.id.main_view_flipper)
        oneHandedCtrlPanelStart = findViewById(R.id.one_handed_ctrl_panel_start)
        oneHandedCtrlPanelEnd = findViewById(R.id.one_handed_ctrl_panel_end)

        florisboard.registerInputView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightFactor = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 0.85f
            else -> if (prefs.keyboard.oneHandedMode != "off") {
                0.9f
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
            else -> 1.00f
        }
        val height = (resources.getDimension(R.dimen.inputView_baseHeight) * heightFactor).roundToInt()
        desiredInputViewHeight = height
        desiredSmartbarHeight = (0.16129 * height).roundToInt()
        desiredTextKeyboardViewHeight = height - desiredSmartbarHeight
        desiredMediaKeyboardViewHeight = height

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
}
