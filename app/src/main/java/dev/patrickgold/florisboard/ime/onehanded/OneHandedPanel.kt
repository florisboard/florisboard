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

package dev.patrickgold.florisboard.ime.onehanded

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager

class OneHandedPanel : LinearLayout, ThemeManager.OnThemeUpdatedListener {
    private var florisboard: FlorisBoard? = null
    private var themeManager: ThemeManager? = null

    private var closeBtn: ImageButton? = null
    private var moveBtn: ImageButton? = null

    private val panelSide: String

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.obtainStyledAttributes(attrs, R.styleable.OneHandedPanel).apply {
            panelSide = getString(R.styleable.OneHandedPanel_panelSide) ?: OneHandedMode.START
            recycle()
        }
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        florisboard = FlorisBoard.getInstanceOrNull()
        themeManager = ThemeManager.defaultOrNull()

        closeBtn = findViewWithTag("one_handed_ctrl_close")
        closeBtn?.setOnClickListener {
            florisboard?.let {
                it.prefs.keyboard.oneHandedMode = OneHandedMode.OFF
                it.updateOneHandedPanelVisibility()
            }
        }
        moveBtn = findViewWithTag("one_handed_ctrl_move")
        moveBtn?.setOnClickListener {
            florisboard?.let {
                it.prefs.keyboard.oneHandedMode = panelSide
                it.updateOneHandedPanelVisibility()
            }
        }

        themeManager?.registerOnThemeUpdatedListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        florisboard = null
        themeManager?.unregisterOnThemeUpdatedListener(this)
        themeManager = null

        closeBtn?.setOnClickListener(null)
        closeBtn = null
        moveBtn?.setOnClickListener(null)
        moveBtn = null
    }

    override fun onThemeUpdated(theme: Theme) {
        setBackgroundColor(theme.getAttr(Theme.Attr.ONE_HANDED_BACKGROUND).toSolidColor().color)
        ColorStateList.valueOf(theme.getAttr(Theme.Attr.ONE_HANDED_FOREGROUND).toSolidColor().color).also {
            closeBtn?.imageTintList = it
            moveBtn?.imageTintList = it
        }
        closeBtn?.invalidate()
        moveBtn?.invalidate()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val florisboard = florisboard ?: return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = (florisboard.inputView?.measuredWidth ?: 0) *
            ((100 - florisboard.prefs.keyboard.oneHandedModeScaleFactor) / 100.0f)
        super.onMeasure(MeasureSpec.makeMeasureSpec(width.toInt(),  MeasureSpec.EXACTLY), heightMeasureSpec)
    }
}
