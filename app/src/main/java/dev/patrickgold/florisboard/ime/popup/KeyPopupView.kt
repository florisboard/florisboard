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
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.*

class KeyPopupView : LinearLayout {
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    private lateinit var text: TextView
    private lateinit var threedots: ImageView

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        text = findViewById(R.id.key_popup_text)
        threedots = findViewById(R.id.key_popup_threedots)
    }

    override fun onDraw(canvas: Canvas?) {
        setBackgroundTintColor2(this, prefs.theme.keyPopupBgColor)
        text.setTextColor(prefs.theme.keyPopupFgColor)
        setImageTintColor2(threedots, prefs.theme.keyPopupFgColor)
        super.onDraw(canvas)
    }
}
