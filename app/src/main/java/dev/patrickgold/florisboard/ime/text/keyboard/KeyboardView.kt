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
import dev.patrickgold.florisboard.ime.popup.PopupManager
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyView
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData
import dev.patrickgold.florisboard.ime.text.gestures.SwipeGesture
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import kotlin.math.roundToInt

/**
 * Manages the layout of the keyboard, key measurement, key selection and all touch events.
 * Supports multi touch events. Note that the keyboard's background is transparent. The 'real'
 * background of this keyboard is the background of the underlying mainViewFlipper. This prevents
 * rendering issues when a keyboard is being loaded for the first time.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 */
class KeyboardView : LinearLayout, FlorisBoard.EventListener, SwipeGesture.Listener,
    ThemeManager.OnThemeUpdatedListener {
    private var activeKeyView: KeyView? = null
    private var activePointerId: Int? = null
    private var activeX: Float = 0.0f
    private var activeY: Float = 0.0f

    var computedLayout: ComputedLayoutData? = null
        set(v) {
            field = v
            buildLayout()
        }
    var desiredKeyWidth: Int = resources.getDimension(R.dimen.key_width).toInt()
    var desiredKeyHeight: Int = resources.getDimension(R.dimen.key_height).toInt()
    var florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private var initialKeyCode: Int = 0
    private val isPreviewMode: Boolean
    val isSmartbarKeyboardView: Boolean
    val isLoadingPlaceholderKeyboard: Boolean
    var popupManager = PopupManager<KeyboardView, KeyView>(this, florisboard?.popupLayerView)
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    private val themeManager: ThemeManager = ThemeManager.default()
    private val swipeGestureDetector = SwipeGesture.Detector(context, this)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.obtainStyledAttributes(attrs, R.styleable.KeyboardView).apply {
            isPreviewMode = getBoolean(R.styleable.KeyboardView_isPreviewKeyboard, false)
            isSmartbarKeyboardView = getBoolean(R.styleable.KeyboardView_isSmartbarKeyboard, false)
            isLoadingPlaceholderKeyboard = getBoolean(R.styleable.KeyboardView_isLoadingPlaceholderKeyboard, false)
            recycle()
        }
        orientation = VERTICAL
        layoutParams = layoutParams ?: FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        florisboard?.addEventListener(this)
        onWindowShown()
        if (isLoadingPlaceholderKeyboard) {
            computedLayout = ComputedLayoutData.PRE_GENERATED_LOADING_KEYBOARD
            /*for ((i, row) in children.withIndex()) {
                row.alpha = (i + 1) * 0.25f
            }*/
        }
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
        if (!isPreviewMode) {
            themeManager.requestThemeUpdate(this)
            onWindowShown()
        } else {
            updateVisibility()
        }
    }

    /**
     * Removes all keys.
     */
    private fun destroyLayout() {
        removeAllViews()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isPreviewMode) {
            themeManager.registerOnThemeUpdatedListener(this)
        }
    }

    /**
     * Dismisses all shown key popups when keyboard is detached from window.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        popupManager.dismissAllPopups()
        if (!isPreviewMode) {
            themeManager.unregisterOnThemeUpdatedListener(this)
        }
    }

    override fun onWindowShown() {
        swipeGestureDetector.apply {
            distanceThreshold = prefs.gestures.swipeDistanceThreshold
            velocityThreshold = prefs.gestures.swipeVelocityThreshold
        }
        for (row in children) {
            if (row is ViewGroup) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        keyView.swipeGestureDetector.apply {
                            distanceThreshold = prefs.gestures.swipeDistanceThreshold
                            velocityThreshold = prefs.gestures.swipeVelocityThreshold
                        }
                    }
                }
            }
        }
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
        if (isPreviewMode || isLoadingPlaceholderKeyboard) {
            return false
        }
        val eventFloris = MotionEvent.obtainNoHistory(event)
        if (!isSmartbarKeyboardView && swipeGestureDetector.onTouchEvent(event)) {
            sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_CANCEL)
            activeKeyView = null
            activePointerId = null
            return true
        }
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
                    initialKeyCode = activeKeyView?.data?.code ?: 0
                    sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_DOWN)
                } else if (activePointerId != pointerId) {
                    // New pointer arrived. Send ACTION_UP to current active view and move on
                    sendFlorisTouchEvent(eventFloris, MotionEvent.ACTION_UP)
                    activePointerId = pointerId
                    activeX = event.getX(pointerIndex)
                    activeY = event.getY(pointerIndex)
                    searchForActiveKeyView()
                    initialKeyCode = activeKeyView?.data?.code ?: 0
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
     * Swipe event handler. Listens to touch_up swipes and executes the swipe action defined for it
     * in the prefs.
     */
    override fun onSwipe(event: SwipeGesture.Event): Boolean {
        return when {
            initialKeyCode == KeyCode.DELETE -> {
                if (event.type == SwipeGesture.Type.TOUCH_UP && event.direction == SwipeGesture.Direction.LEFT &&
                    prefs.gestures.deleteKeySwipeLeft == SwipeAction.DELETE_WORD) {
                    florisboard?.executeSwipeAction(prefs.gestures.deleteKeySwipeLeft)
                    true
                } else {
                    false
                }
            }
            initialKeyCode > KeyCode.SPACE && !popupManager.isShowingExtendedPopup -> when {
                !prefs.glide.enabled -> when (event.type) {
                    SwipeGesture.Type.TOUCH_UP -> {
                        val swipeAction = when (event.direction) {
                            SwipeGesture.Direction.UP -> prefs.gestures.swipeUp
                            SwipeGesture.Direction.DOWN -> prefs.gestures.swipeDown
                            SwipeGesture.Direction.LEFT -> prefs.gestures.swipeLeft
                            SwipeGesture.Direction.RIGHT -> prefs.gestures.swipeRight
                            else -> SwipeAction.NO_ACTION
                        }
                        if (swipeAction != SwipeAction.NO_ACTION) {
                            florisboard?.executeSwipeAction(swipeAction)
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
                else -> false
            }
            else -> false
        }
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
        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        val keyMarginV = resources.getDimension((R.dimen.key_marginV)).toInt()

        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        desiredKeyWidth = if (isSmartbarKeyboardView) {
            (desiredWidth / 6.0f - 2.0f * keyMarginH).roundToInt()
        } else {
            (desiredWidth / 10.0f - 2.0f * keyMarginH).roundToInt()
        }
        val desiredHeight = if (isSmartbarKeyboardView || isPreviewMode) {
            MeasureSpec.getSize(heightMeasureSpec).toFloat()
        } else {
            (florisboard?.inputView?.desiredTextKeyboardViewHeight ?: MeasureSpec.getSize(heightMeasureSpec).toFloat())
        } * if (isPreviewMode) { 0.90f } else { 1.00f }
        desiredKeyHeight = when {
            isSmartbarKeyboardView -> desiredHeight - 1.5f * keyMarginV
            else -> desiredHeight / (computedLayout?.arrangement?.size?.toFloat() ?: 4.0f) - 2.0f * keyMarginV
        }.roundToInt()

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(desiredWidth.roundToInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(desiredHeight.roundToInt(), MeasureSpec.EXACTLY)
        )
    }

    override fun onThemeUpdated(theme: Theme) {
        if (isPreviewMode) {
            setBackgroundColor(theme.getAttr(Theme.Attr.KEYBOARD_BACKGROUND).toSolidColor().color)
        }
        for (row in children) {
            if (row is ViewGroup) {
                for (keyView in row.children) {
                    if (keyView is ThemeManager.OnThemeUpdatedListener) {
                        keyView.onThemeUpdated(theme)
                    }
                }
            }
        }
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
     * Queues a redraw for all keys. The ThemeManager's event automatically triggers an invalidate
     * call on the KeyView's, so no need to manually loop through all KeyViews here.
     */
    fun invalidateAllKeys() {
        themeManager.requestThemeUpdate(this)
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
}
