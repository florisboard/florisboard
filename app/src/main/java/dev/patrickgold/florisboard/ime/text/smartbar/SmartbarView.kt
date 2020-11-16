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
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.core.view.children
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.setBackgroundTintColor2
import dev.patrickgold.florisboard.util.setDrawableTintColor2
import dev.patrickgold.florisboard.util.setImageTintColor2

/**
 * View class which keeps the references to important children and informs [SmartbarManager] that
 * it is now the active [SmartbarView] (useful when resetting the input view of FlorisBoard due to
 * a theme change).
 */
class SmartbarView : LinearLayout {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    private val smartbarManager = SmartbarManager.getInstance()

    private var variants: MutableList<ViewGroup> = mutableListOf()
    private var containers: MutableList<ViewGroup> = mutableListOf()

    var candidateViewList: MutableList<Button> = mutableListOf()
        private set

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onAttachedToWindow()")

        super.onAttachedToWindow()

        variants.add(findViewById(R.id.smartbar_variant_default))
        variants.add(findViewById(R.id.smartbar_variant_back_only))
        variants.add(findViewById(R.id.smartbar_variant_number_row))

        containers.add(findViewById(R.id.candidates))
        containers.add(findViewById(R.id.clipboard_suggestion_row))
        containers.add(findViewById(R.id.clipboard_cursor_row))
        containers.add(findViewById(R.id.quick_actions))

        candidateViewList.add(findViewById(R.id.candidate0))
        candidateViewList.add(findViewById(R.id.candidate1))
        candidateViewList.add(findViewById(R.id.candidate2))

        smartbarManager.registerSmartbarView(this)
    }

    /**
     * Sets the active Smartbar variant based on the given id. Pass null to hide all variants and
     * show an empty Smartbar.
     *
     * @param which Which variant to show. Pass null to hide all.
     */
    fun setActiveVariant(@IdRes which: Int?) {
        for (variant in variants) {
            if (variant.id == which) {
                variant.visibility = View.VISIBLE
            } else {
                variant.visibility = View.GONE
            }
        }
    }

    /**
     * Sets the active Smartbar container based on the given id. Does only work if the currently
     * shown Smartbar variant is [R.id.smartbar_variant_default]. Pass null to hide all containers
     * and show only the quick action toggle.
     *
     * @param which Which container to show. Pass null to hide all.
     */
    fun setActiveContainer(@IdRes which: Int?) {
        for (container in containers) {
            if (container.id == which) {
                container.visibility = View.VISIBLE
            } else {
                container.visibility = View.GONE
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                // Must be this size
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                // Can't be bigger than...
                (florisboard?.inputView?.desiredSmartbarHeight ?: 0).coerceAtMost(heightSize)
            }
            else -> {
                // Be whatever you want
                florisboard?.inputView?.desiredSmartbarHeight ?: 0
            }
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        setBackgroundColor(prefs.theme.smartbarBgColor)
        for (container in containers + variants) {
            when (container.id) {
                R.id.smartbar_variant_number_row -> {
                    for (button in container.children) {
                        if (button is Button) {
                            button.setTextColor(prefs.theme.smartbarFgColor)
                        }
                    }
                }
                R.id.clipboard_suggestion_row -> {
                    val clipboardSuggestion = findViewById<Button>(R.id.clipboard_suggestion)
                    setBackgroundTintColor2(clipboardSuggestion, prefs.theme.smartbarButtonBgColor)
                    setDrawableTintColor2(clipboardSuggestion, prefs.theme.smartbarButtonFgColor)
                    clipboardSuggestion.setTextColor(prefs.theme.smartbarButtonFgColor)
                }
                R.id.clipboard_cursor_row -> {
                    for (button in container.children) {
                        if (button is ImageButton) {
                            if (button.isEnabled) {
                                setImageTintColor2(button, prefs.theme.smartbarFgColor)
                            } else {
                                setImageTintColor2(button, prefs.theme.smartbarFgColorAlt)
                            }
                        }
                    }
                }
                R.id.candidates -> {
                    for (view in container.children) {
                        if (view is Button) {
                            view.setTextColor(prefs.theme.smartbarFgColor)
                        }
                    }
                }
            }
        }
    }
}
