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

package dev.patrickgold.florisboard.ime.text.smartbar

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R

/**
 * View class which keeps the references to important children and informs [SmartbarManager] that
 * it is now the active [SmartbarView] (useful when resetting the input view of FlorisBoard due to
 * a theme change).
 */
class SmartbarView : LinearLayout {

    private val smartbarManager = SmartbarManager.getInstance()

    var candidatesView: LinearLayout? = null
        private set
    var candidateViewList: MutableList<Button> = mutableListOf()
        private set
    var numberRowView: LinearLayout? = null
        private set
    var quickActionsView: LinearLayout? = null
        private set
    var quickActionToggle: ImageButton? = null
        private set

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onAttachedToWindow()")

        super.onAttachedToWindow()

        candidatesView = findViewById(R.id.candidates)
        candidateViewList.add(findViewById(R.id.candidate0))
        candidateViewList.add(findViewById(R.id.candidate1))
        candidateViewList.add(findViewById(R.id.candidate2))

        numberRowView = findViewById(R.id.number_row)
        quickActionsView = findViewById(R.id.quick_actions)
        quickActionToggle = findViewById(R.id.quick_action_toggle)

        smartbarManager.registerSmartbarView(this)
    }

    /**
     * Multiplies the default smartbar height with the given [factor] and sets it.
     */
    fun setHeightFactor(factor: Float) {
        val baseSize = resources.getDimension(R.dimen.smartbar_height)
        val size = (baseSize * factor).toInt()
        layoutParams?.height = size
    }
}
