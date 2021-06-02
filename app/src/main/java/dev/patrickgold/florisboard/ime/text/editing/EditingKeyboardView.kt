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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.clip.FlorisClipboardManager
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.util.setBackgroundTintColor2
import kotlin.math.roundToInt

/**
 * View class for updating the key views depending on the current selection and clipboard state.
 */
class EditingKeyboardView : ConstraintLayout, FlorisBoard.EventListener,
    ThemeManager.OnThemeUpdatedListener {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val themeManager: ThemeManager = ThemeManager.default()

    private var selectKey: EditingKeyView? = null
    private var selectAllKey: EditingKeyView? = null
    private var cutKey: EditingKeyView? = null
    private var copyKey: EditingKeyView? = null
    private var pasteKey: EditingKeyView? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        florisboard?.addEventListener(this)
        themeManager.registerOnThemeUpdatedListener(this)

        selectKey = findViewById(R.id.select)
        selectAllKey = findViewById(R.id.select_all)
        cutKey = findViewById(R.id.clipboard_cut)
        copyKey = findViewById(R.id.clipboard_copy)
        pasteKey = findViewById(R.id.clipboard_paste)

        val clipboardManager = FlorisClipboardManager.getInstance()
        pasteKey?.isEnabled = clipboardManager.canBePasted(clipboardManager.primaryClip)
    }

    override fun onDetachedFromWindow() {
        themeManager.unregisterOnThemeUpdatedListener(this)
        florisboard?.removeEventListener(this)
        super.onDetachedFromWindow()
    }

    override fun onThemeUpdated(theme: Theme) {
        setBackgroundTintColor2(this, theme.getAttr(Theme.Attr.SMARTBAR_BACKGROUND).toSolidColor().color)
    }

    override fun onUpdateSelection() {
        val isSelectionActive = florisboard?.activeEditorInstance?.selection?.isSelectionMode ?: false
        val isSelectionMode = florisboard?.textInputManager?.isManualSelectionMode ?: false
        selectKey?.isHighlighted = isSelectionActive || isSelectionMode
        selectAllKey?.visibility = when {
            isSelectionActive -> View.GONE
            else -> View.VISIBLE
        }
        cutKey?.visibility = when {
            isSelectionActive -> View.VISIBLE
            else -> View.GONE
        }
        copyKey?.isEnabled = isSelectionActive
        val clipboardManager = FlorisClipboardManager.getInstance()
        pasteKey?.isEnabled = clipboardManager.canBePasted(clipboardManager.primaryClip)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                // Must be this size
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                // Can't be bigger than...
                (florisboard?.uiBinding?.inputView?.desiredTextKeyboardViewHeight ?: 0.0f).coerceAtMost(heightSize)
            }
            else -> {
                // Be whatever you want
                florisboard?.uiBinding?.inputView?.desiredTextKeyboardViewHeight ?: 0.0f
            }
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height.roundToInt(), MeasureSpec.EXACTLY))
    }
}
