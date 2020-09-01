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
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.setBackgroundTintColor2

/**
 * View class for updating the key views depending on the current selection and clipboard state.
 */
class EditingKeyboardView : ConstraintLayout, FlorisBoard.EventListener {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)

    private var arrowUpKey: EditingKeyView? = null
    private var arrowDownKey: EditingKeyView? = null
    private var selectKey: EditingKeyView? = null
    private var selectAllKey: EditingKeyView? = null
    private var cutKey: EditingKeyView? = null
    private var copyKey: EditingKeyView? = null
    private var pasteKey: EditingKeyView? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        florisboard?.addEventListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        arrowUpKey = findViewById(R.id.arrow_up)
        arrowDownKey = findViewById(R.id.arrow_down)
        selectKey = findViewById(R.id.select)
        selectAllKey = findViewById(R.id.select_all)
        cutKey = findViewById(R.id.clipboard_cut)
        copyKey = findViewById(R.id.clipboard_copy)
        pasteKey = findViewById(R.id.clipboard_paste)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        florisboard?.removeEventListener(this)
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        val isSelectionActive = florisboard?.textInputManager?.isTextSelected ?: false
        val isSelectionMode = florisboard?.textInputManager?.isManualSelectionMode ?: false
        arrowUpKey?.isEnabled = !(isSelectionActive || isSelectionMode)
        arrowDownKey?.isEnabled = !(isSelectionActive || isSelectionMode)
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
        pasteKey?.isEnabled = florisboard?.clipboardManager?.hasPrimaryClip() ?: false
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        setBackgroundTintColor2(this, prefs.theme.smartbarBgColor)
    }
}
