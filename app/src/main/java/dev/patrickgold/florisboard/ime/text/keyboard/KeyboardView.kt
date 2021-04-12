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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.children
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.text.gestures.*
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyView
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayout
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.util.ViewLayoutUtils
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Manages the layout of the keyboard, key measurement, key selection and all touch events.
 * Supports multi touch events. Note that the keyboard's background is transparent. The 'real'
 * background of this keyboard is the background of the underlying mainViewFlipper. This prevents
 * rendering issues when a keyboard is being loaded for the first time.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 */
class KeyboardView : FlexboxLayout, FlorisBoard.EventListener, SwipeGesture.Listener, GlideTypingGesture.Listener,
    ThemeManager.OnThemeUpdatedListener {
    private var gesturing: Boolean = false
    private var activeKeyViews: MutableMap<Int, KeyView> = mutableMapOf()
    private var initialKeyCodes: MutableMap<Int, Int> = mutableMapOf()

    var computedLayout: ComputedLayout? = null
        set(v) {
            field = v
            buildLayout()
        }
    var desiredKeyWidth: Int = resources.getDimension(R.dimen.key_width).toInt()
    var desiredKeyHeight: Int = resources.getDimension(R.dimen.key_height).toInt()
    var florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val isPreviewMode: Boolean
    val isSmartbarKeyboardView: Boolean
    val isLoadingPlaceholderKeyboard: Boolean
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    private val themeManager: ThemeManager = ThemeManager.default()
    private val swipeGestureDetector = SwipeGesture.Detector(context, this)
    private lateinit var gestureDetector: GlideTypingGesture.Detector
    private val gestureDataForDrawing: MutableList<GlideTypingGesture.Detector.Position> = mutableListOf()
    private val fadingGesture: MutableList<GlideTypingGesture.Detector.Position> = mutableListOf()
    private var fadingGestureRadius: Float = 0f

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.obtainStyledAttributes(attrs, R.styleable.KeyboardView).apply {
            isPreviewMode = getBoolean(R.styleable.KeyboardView_isPreviewKeyboard, false)
            isSmartbarKeyboardView = getBoolean(R.styleable.KeyboardView_isSmartbarKeyboard, false)
            isLoadingPlaceholderKeyboard = getBoolean(R.styleable.KeyboardView_isLoadingPlaceholderKeyboard, false)
            recycle()
        }
        flexDirection = FlexDirection.COLUMN
        justifyContent = JustifyContent.SPACE_BETWEEN
        layoutParams = layoutParams ?: FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        onWindowShown()
        if (isLoadingPlaceholderKeyboard) {
            computedLayout = ComputedLayout.PRE_GENERATED_LOADING_KEYBOARD
        }

        if (!this.isSmartbarKeyboardView) {
            setWillNotDraw(false)
        }
    }

    /**
     * Builds the UI layout based on the [computedLayout].
     */
    private fun buildLayout() {
        destroyLayout()
        val computedLayout = computedLayout ?: return
        for (row in computedLayout.arrangement) {
            val rowView = KeyboardRowView(context, this)
            for (key in row) {
                val keyView = KeyView(this, key, florisboard)
                rowView.addView(keyView)
            }
            addView(rowView)
        }
        if (!isPreviewMode) {
            themeManager.requestThemeUpdate(this)
            onWindowShown()

            if (!isLoadingPlaceholderKeyboard)
                initGestureClassifier()
        } else {
            updateVisibility()
        }
    }

    private fun initGestureClassifier() {
        if (this.isSmartbarKeyboardView || this.computedLayout?.mode != KeyboardMode.CHARACTERS) {
            return
        }
        this.post {
            val dimensions = Dimensions(
                width.toFloat(),
                height.toFloat(),
            )

            val keys = children.map { (it as FlexboxLayout).children }.flatten().map { it as KeyView }
            GlideTypingManager.getInstance().setLayout(keys, dimensions)
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
        florisboard?.addEventListener(this)
        if (!isPreviewMode) {
            themeManager.registerOnThemeUpdatedListener(this)
        }
    }

    /**
     * Dismisses all shown key popups when keyboard is detached from window.
     */
    override fun onDetachedFromWindow() {
        if (!isPreviewMode) {
            themeManager.unregisterOnThemeUpdatedListener(this)
        }
        florisboard?.removeEventListener(this)
        super.onDetachedFromWindow()
    }

    override fun onWindowShown() {
        if (prefs.glide.enabled && !isPreviewMode && !isSmartbarKeyboardView) {
            this.gestureDetector = GlideTypingGesture.Detector(context).let {
                it.registerListener(this)
                it.registerListener(GlideTypingManager.getInstance())
                it.velocityThreshold = prefs.gestures.swipeVelocityThreshold
                it
            }
        }
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
        if (event == null || isPreviewMode || isLoadingPlaceholderKeyboard) return false
        if (!isSmartbarKeyboardView && swipeGestureDetector.onTouchEvent(event)) {
            for (pointerIndex in 0 until event.pointerCount) {
                val pointerId = event.getPointerId(pointerIndex)
                sendFlorisTouchEvent(event, pointerIndex, pointerId, MotionEvent.ACTION_CANCEL)
                activeKeyViews.remove(pointerId)
            }
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                florisboard?.textInputManager?.inputEventDispatcher?.send(InputKeyEvent.up(KeyData.INTERNAL_BATCH_EDIT))
            }
            return true
        }

        val dimensions = Dimensions(
            width.toFloat(),
            height.toFloat(),
        )
        GlideTypingManager.getInstance().updateDimensions(dimensions)

        // Gesture typing only works on character keyboard, if gesture detector says it's a gesture,
        if (prefs.glide.enabled &&
            computedLayout?.mode == KeyboardMode.CHARACTERS &&
            gestureDetector.onTouchEvent(event, initialKeyCodes) &&
            event.actionMasked != MotionEvent.ACTION_UP
        ) {
            // cancel all other button presses
            for (pointerIndex in 0 until event.pointerCount) {
                val pointerId = event.getPointerId(pointerIndex)
                sendFlorisTouchEvent(event, pointerIndex, pointerId, MotionEvent.ACTION_CANCEL)
                activeKeyViews.remove(pointerId)
            }
            invalidate()
            this.gesturing = true
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                florisboard?.textInputManager?.inputEventDispatcher?.send(InputKeyEvent.down(KeyData.INTERNAL_BATCH_EDIT))
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                searchForActiveKeyView(event, pointerIndex, pointerId)
                initialKeyCodes[pointerId] = activeKeyViews[pointerId]?.data?.code ?: 0
                sendFlorisTouchEvent(event, pointerIndex, pointerId, MotionEvent.ACTION_DOWN)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                searchForActiveKeyView(event, pointerIndex, pointerId)
                initialKeyCodes[pointerId] = activeKeyViews[pointerId]?.data?.code ?: 0
                sendFlorisTouchEvent(event, pointerIndex, pointerId, MotionEvent.ACTION_DOWN)
            }
            MotionEvent.ACTION_MOVE -> {
                for (pointerIndex in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(pointerIndex)
                    if (!activeKeyViews.containsKey(pointerId)) {
                        searchForActiveKeyView(event, pointerIndex, pointerId)
                        sendFlorisTouchEvent(event, pointerIndex, pointerId, MotionEvent.ACTION_DOWN)
                    } else {
                        sendFlorisTouchEvent(event, pointerIndex, pointerId, MotionEvent.ACTION_MOVE)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                sendFlorisTouchEvent(event, pointerIndex, pointerId, event.actionMasked)
                activeKeyViews.remove(pointerId)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                sendFlorisTouchEvent(event, pointerIndex, pointerId, event.actionMasked)
                activeKeyViews.remove(pointerId)
                florisboard?.textInputManager?.inputEventDispatcher?.send(InputKeyEvent.up(KeyData.INTERNAL_BATCH_EDIT))
            }
            else -> return false
        }
        return true
    }

    /**
     * Sends a touch [event] to the active key view which is associated with given [pointerId]. The action of the
     * event is set to [actionParam]. Normalizes passed actions (ACTION_POINTER_* will be converted to ACTION_*).
     * Translates the absolute coords of a passed [event] to relative ones so the active key view can work with it.
     *
     * @param event The event to pass to the active key view.
     * @param pointerIndex The index of the pointer, used for getting coordinates.
     * @param pointerId The unique ID of the pointer, used to reference the active key view.
     * @param actionParam The action to set the [event] to.
     */
    private fun sendFlorisTouchEvent(event: MotionEvent, pointerIndex: Int, pointerId: Int, actionParam: Int) {
        val keyView = activeKeyViews[pointerId] ?: return
        val keyViewParent = keyView.parent as? ViewGroup ?: return
        val eventToSend = MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            when (actionParam) {
                MotionEvent.ACTION_POINTER_DOWN -> MotionEvent.ACTION_DOWN
                MotionEvent.ACTION_POINTER_UP -> MotionEvent.ACTION_UP
                else -> actionParam
            },
            event.getX(pointerIndex) - keyViewParent.x - keyView.x,
            event.getY(pointerIndex) - keyViewParent.y - keyView.y,
            0
        )
        keyView.onFlorisTouchEvent(eventToSend)
        eventToSend.recycle()
    }

    /**
     * Swipe event handler. Listens to touch_up swipes and executes the swipe action defined for it
     * in the prefs.
     */
    override fun onSwipe(event: SwipeGesture.Event): Boolean {
        return when {
            initialKeyCodes[event.pointerId] == KeyCode.DELETE -> {
                if (event.type == SwipeGesture.Type.TOUCH_UP && event.direction == SwipeGesture.Direction.LEFT &&
                    prefs.gestures.deleteKeySwipeLeft == SwipeAction.DELETE_WORD
                ) {
                    florisboard?.executeSwipeAction(prefs.gestures.deleteKeySwipeLeft)
                    true
                } else {
                    false
                }
            }
            initialKeyCodes[event.pointerId] == KeyCode.SHIFT && activeKeyViews[event.pointerId]?.data?.code != KeyCode.SHIFT &&
                event.type == SwipeGesture.Type.TOUCH_UP -> {
                activeKeyViews[event.pointerId]?.let {
                    florisboard?.textInputManager?.inputEventDispatcher?.send(
                        InputKeyEvent.up(
                            it.popupManager.getActiveKeyData(it)
                                ?: it.data
                        )
                    )
                    florisboard?.textInputManager?.inputEventDispatcher?.send(InputKeyEvent.cancel(KeyData.SHIFT))
                }
                true
            }
            initialKeyCodes[event.pointerId] == KeyCode.SPACE && event.type == SwipeGesture.Type.TOUCH_UP
                && event.absUnitCountY.times(-1) > 6 -> {
                florisboard?.executeSwipeAction(prefs.gestures.spaceBarSwipeUp)
                true
            }
            initialKeyCodes[event.pointerId] ?: 0 > KeyCode.SPACE &&
                activeKeyViews[event.pointerId]?.popupManager?.isShowingExtendedPopup == false -> when {
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
     * Searches for an active key view at the passed pointer location.
     */
    private fun searchForActiveKeyView(event: MotionEvent, pointerIndex: Int, pointerId: Int) {
        val activeX = event.getX(pointerIndex)
        val activeY = event.getY(pointerIndex)
        loop@ for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        if (keyView.touchHitBox.contains(activeX.toInt(), activeY.toInt())) {
                            activeKeyViews[pointerId] = keyView
                            break@loop
                        }
                    }
                }
            }
        }
    }

    /**
     * Invalidates the current active key view and sends a [MotionEvent.ACTION_CANCEL] to indicate the loss of focus.
     */
    fun dismissActiveKeyViewReference(pointerId: Int) {
        activeKeyViews.remove(pointerId)?.onFlorisTouchEvent(
            MotionEvent.obtain(
                0, 0, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0
            )
        )
    }

    /**
     * The desired key heights/widths are being calculated here.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val keyMarginH: Int
        val keyMarginV: Int

        if (isSmartbarKeyboardView) {
            keyMarginH = resources.getDimension(R.dimen.key_marginH).toInt()
            keyMarginV = resources.getDimension(R.dimen.key_marginV).toInt()
        } else {
            keyMarginV = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingVertical, context).toInt()
            keyMarginH = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingHorizontal, context).toInt()
        }

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
        } * if (isPreviewMode) {
            0.90f
        } else {
            1.00f
        }
        val layoutSize = computedLayout?.arrangement?.size?.toFloat() ?: 4.0f
        desiredKeyHeight = when {
            isSmartbarKeyboardView -> {
                desiredHeight - 1.5f * keyMarginV
            }
            florisboard?.inputView?.shouldGiveAdditionalSpace == true -> {
                desiredHeight / (layoutSize + 0.5f).coerceAtMost(5.0f) - 2.0f * keyMarginV
            }
            else -> {
                desiredHeight / layoutSize - 2.0f * keyMarginV
            }
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
        if (theme.getAttr(Theme.Attr.GLIDE_TRAIL_COLOR).toSolidColor().color == 0) {
            this.glideTrailPaint.color = theme.getAttr(Theme.Attr.WINDOW_COLOR_PRIMARY).toSolidColor().color
            this.glideTrailPaint.alpha = 32
        } else {
            this.glideTrailPaint.color = theme.getAttr(Theme.Attr.GLIDE_TRAIL_COLOR).toSolidColor().color
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

    private val glideTrailPaint = Paint().apply {
        color = Color.GREEN
        alpha = 40
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)

        if (prefs.glide.enabled && prefs.glide.showTrail && !isSmartbarKeyboardView) {
            val targetDist = 5f
            val maxPoints = prefs.glide.trailMaxLength
            val radius = 20f
            // the tip of the trail will be 1px
            val radiusReductionFactor = (1/radius).pow(1f/maxPoints)
            if (this.fadingGestureRadius > 0) {
                drawGesture(fadingGesture, maxPoints, targetDist, fadingGestureRadius, canvas, radiusReductionFactor)
            }
            if (gesturing && gestureDataForDrawing.isNotEmpty()) {
                drawGesture(gestureDataForDrawing, maxPoints, targetDist, radius, canvas, radiusReductionFactor)
            }
        }
    }

    private fun drawGesture(
        gestureData: MutableList<GlideTypingGesture.Detector.Position>,
        maxPoints: Int,
        targetDist: Float,
        initialRadius: Float,
        canvas: Canvas?,
        radiusReductionFactor: Float
    ) {
        var radius = initialRadius
        var drawnPoints = 0
        var prevX = gestureData.lastOrNull()?.x ?: 0f
        var prevY = gestureData.lastOrNull()?.y ?: 0f

        outer@ for (i in gestureData.size - 1 downTo 1) {
            val dx = prevX - gestureData[i - 1].x
            val dy = prevY - gestureData[i - 1].y
            val dist = sqrt(dx * dx + dy * dy)

            val numPoints = (dist / targetDist).toInt()
            for (j in 0 until numPoints) {
                if (drawnPoints > maxPoints) break@outer
                radius *= radiusReductionFactor
                val intermediateX =
                    gestureData[i].x * (1 - j.toFloat() / numPoints) + gestureData[i - 1].x * (j.toFloat() / numPoints)
                val intermediateY =
                    gestureData[i].y * (1 - j.toFloat() / numPoints) + gestureData[i - 1].y * (j.toFloat() / numPoints)
                canvas?.drawCircle(intermediateX, intermediateY, radius,glideTrailPaint)
                drawnPoints += 1
                prevX = intermediateX
                prevY = intermediateY
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

    override fun onGestureComplete(data: GlideTypingGesture.Detector.PointerData) {
        onGestureCancelled()
    }

    override fun onGestureAdd(point: GlideTypingGesture.Detector.Position) {
        val initialRadius = 25f
        val gestureData = this.gestureDataForDrawing
        val targetDist = 5f
        val maxLength = 1000f
        if (prefs.glide.enabled) {
            this.gestureDataForDrawing.add(point)
        }
    }

    override fun onGestureCancelled() {
        if (prefs.glide.showTrail) {
            this.fadingGesture.clear()
            this.fadingGesture.addAll(gestureDataForDrawing)

            val animator = ValueAnimator.ofFloat(20f, 0f)
            animator.interpolator = AccelerateInterpolator()
            animator.duration = prefs.glide.trailDuration.toLong()
            animator.addUpdateListener {
                this.fadingGestureRadius = it.animatedValue as Float
                this.invalidate()
            }
            animator.start()

            this.gestureDataForDrawing.clear()
            gesturing = false
            invalidate()
        }
    }
}
