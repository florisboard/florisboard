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

package dev.patrickgold.florisboard.ime.text.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.popup.KeyPopupManager
import dev.patrickgold.florisboard.ime.text.key.KeyView
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData
import dev.patrickgold.florisboard.util.getColorFromAttr

/**
 * Manages the layout of the keyboard, key measurement, key selection and all touch events.
 * Supports multi touch events.
 *
 * TODO: Implement swipe gesture support
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 */
class KeyboardView : LinearLayout {
    private var activeKeyView: KeyView? = null
    private var activePointerId: Int? = null
    private var activeX: Float = 0.0f
    private var activeY: Float = 0.0f

    private var colorDrawable: ColorDrawable
    var computedLayout: ComputedLayoutData? = null
        set(v) {
            field = v
            buildLayout()
        }
    var desiredKeyWidth: Int = resources.getDimension(R.dimen.key_width).toInt()
    var desiredKeyHeight: Int = resources.getDimension(R.dimen.key_height).toInt()
    var florisboard: FlorisBoard? = null
    var isPreviewMode: Boolean = false
    var popupManager = KeyPopupManager<KeyboardView, KeyView>(this)
    lateinit var prefs: PrefHelper

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        colorDrawable = ColorDrawable(getColorFromAttr(context, R.attr.keyboard_bgColor))
        background = colorDrawable
        orientation = VERTICAL
        layoutParams = layoutParams ?: FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Builds the UI layout based on the [computedLayout].
     */
    private fun buildLayout() {
        destroyLayout()
        val computedLayout = computedLayout ?: return
        for (row in computedLayout.arrangement) {
            val rowView = KeyboardRowView(context)
            for (key in row) {
                val keyView = KeyView(this, key)
                keyView.florisboard = florisboard
                rowView.addView(keyView)
            }
            addView(rowView)
        }
    }

    /**
     * Removes all keys.
     */
    private fun destroyLayout() {
        removeAllViews()
    }

    /**
     * Dismisses all shown key popups when keyboard is detached from window.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        popupManager.dismissAllPopups()
    }

    /**
     * Catch all events which are designated for child views.
     */
    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return true
    }

    /**
     * This is the main logic for choosing which [KeyView] is the current active one.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        if (isPreviewMode) {
            return false
        }
        val eventFloris = MotionEvent.obtainNoHistory(event)
        val pointerIndex = event.actionIndex
        var pointerId = event.getPointerId(pointerIndex)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (activePointerId == null) {
                    activePointerId = pointerId
                    activeX = event.getX(pointerIndex)
                    activeY = event.getY(pointerIndex)
                    searchForActiveKeyView()
                    sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_DOWN)
                } else if (activePointerId != pointerId) {
                    // New pointer arrived. Send ACTION_UP to current active view and move on
                    sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_UP)
                    activePointerId = pointerId
                    activeX = event.getX(pointerIndex)
                    activeY = event.getY(pointerIndex)
                    searchForActiveKeyView()
                    sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_DOWN)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until event.pointerCount) {
                    pointerId = event.getPointerId(index)
                    if (activePointerId == pointerId) {
                        activeX = event.getX(index)
                        activeY = event.getY(index)
                        if (activeKeyView == null) {
                            searchForActiveKeyView()
                            sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_DOWN)
                        } else {
                            sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_MOVE)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_UP -> {
                if (activePointerId == pointerId) {
                    sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_UP)
                    activeKeyView = null
                    activePointerId = null
                }
            }
            else -> return false
        }
        eventFloris.recycle()
        return true
    }

    /**
     * Sends a touch [event] to [activeKeyView] with action set to [actionParam]. Normalizes passed
     * actions (ACTION_POINTER_* will be converted to ACTION_*). Translates the absolute coords of
     * a passed [event] to relative ones so the [activeKeyView] can work with it.
     *
     * @param event The event to pass to [activeKeyView].
     * @param actionParam The action to set the [event] to.
     */
    private fun sendFlorisTouchEvent(event: MotionEvent, actionParam: Int) {
        val keyView = activeKeyView ?: return
        val keyViewParent = keyView.parent as ViewGroup
        keyView.onFlorisTouchEvent(event.apply {
            action = when (actionParam) {
                MotionEvent.ACTION_POINTER_DOWN -> MotionEvent.ACTION_DOWN
                MotionEvent.ACTION_POINTER_UP -> MotionEvent.ACTION_UP
                else -> actionParam
            }
            setLocation(
                activeX - keyViewParent.x - keyView.x,
                activeY - keyViewParent.y - keyView.y
            )
        })
    }

    /**
     * Searches for an active key view at [activeX]/[activeY].
     */
    private fun searchForActiveKeyView() {
        loop@ for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        if (keyView.touchHitBox.contains(activeX.toInt(), activeY.toInt())) {
                            activeKeyView = keyView
                            break@loop
                        }
                    }
                }
            }
        }
    }

    /**
     * Invalidates the current [activeKeyView] and sends a [MotionEvent.ACTION_CANCEL] to indicate
     * the loss of focus.
     */
    fun dismissActiveKeyViewReference() {
        activeKeyView?.onFlorisTouchEvent(MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0
        ))
        activeKeyView = null
    }

    /**
     * The desired key heights/widths are being calculated here.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        desiredKeyWidth = (widthSize / 10) - (2 * keyMarginH)

        val factor = prefs.looknfeel.heightFactor
        val keyHeightFactor = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 0.85f
            else -> if (prefs.looknfeel.oneHandedMode == "start" ||
                prefs.looknfeel.oneHandedMode == "end") {
                0.9f
            } else {
                1.0f
            }
        } * when (factor) {
            "extra_short" -> 0.85f
            "short" -> 0.90f
            "mid_short" -> 0.95f
            "normal" -> 1.00f
            "mid_tall" -> 1.05f
            "tall" -> 1.10f
            "extra_tall" -> 1.15f
            else -> 1.00f
        } * when (isPreviewMode) {
            true -> 0.90f
            else -> 1.00f
        }
        desiredKeyHeight = (resources.getDimension(R.dimen.key_height) * keyHeightFactor).toInt()
        florisboard?.textInputManager?.smartbarManager?.smartbarView?.setHeightFactor(keyHeightFactor)

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Queues a layout request for all keys.
     */
    fun requestLayoutAllKeys() {
        for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        keyView.requestLayout()
                    }
                }
            }
        }
    }

    /**
     * Queues a redraw for all keys.
     */
    fun invalidateAllKeys() {
        for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        keyView.invalidate()
                    }
                }
            }
        }
    }

    /**
     * Syncs the current key variation with all keys and updates their visibility.
     */
    fun updateVisibility() {
        for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        keyView.updateVisibility()
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        colorDrawable.color = getColorFromAttr(context, R.attr.keyboard_bgColor)
    }
}
