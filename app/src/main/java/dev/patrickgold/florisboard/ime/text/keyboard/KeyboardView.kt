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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.ime.text.gestures.*
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyView
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.util.ViewLayoutUtils
import org.json.JSONObject
import timber.log.Timber
import kotlin.math.roundToInt

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
    private var gestureClassifierInitialized: Boolean = false
    private var gesturing: Boolean = false
    private var activeKeyViews: MutableMap<Int, KeyView> = mutableMapOf()
    private var initialKeyCodes: MutableMap<Int, Int> = mutableMapOf()

    var computedLayout: ComputedLayoutData? = null
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
    private val gestureDetector = GlideTypingGesture.Detector(context, this)
    internal var gestureTypingClassifier: GestureTypingClassifier = StatisticalGestureTypingClassifier()
    private val gestureDataForDrawing: MutableList<GlideTypingGesture.Detector.Position> = mutableListOf()

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
            computedLayout = ComputedLayoutData.PRE_GENERATED_LOADING_KEYBOARD
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


            if (!gestureClassifierInitialized) {
                initGestureClassifier()
            }
        } else {
            updateVisibility()
        }
    }

    internal fun initGestureClassifier() {
        if (this.isSmartbarKeyboardView ||
            this.computedLayout == ComputedLayoutData.PRE_GENERATED_LOADING_KEYBOARD ||
            this.computedLayout?.mode != KeyboardMode.CHARACTERS)
            return

        Thread.dumpStack()

        computedLayout!!.arrangement.zip(this.children.asIterable()).forEach { pairOfRows ->
            pairOfRows.first.zip((pairOfRows.second as ViewGroup).children.asIterable()).forEach { pairOfKeys ->
                pairOfKeys.second.post {
                    pairOfKeys.first.x = pairOfRows.second.x + pairOfKeys.second.x + pairOfKeys.second.width / 2
                    pairOfKeys.first.y = pairOfRows.second.y + pairOfKeys.second.y + pairOfKeys.second.height / 2
                    pairOfKeys.first.height = pairOfKeys.second.height
                    pairOfKeys.first.width = pairOfKeys.second.width
                }
            }
        }


        this.post {
            gestureTypingClassifier.setLayout(computedLayout!!)
            // FIXME: get this info from dictionary.
            val data = AssetManager.default().loadAssetRaw(AssetRef(AssetSource.Assets, "ime/dict/data.json")).getOrThrow()
            val json = JSONObject(data)
            val map = hashMapOf<String, Int>()
            map.putAll(json.keys().asSequence().map { Pair(it, json.getInt(it)) })
            gestureTypingClassifier.setWordData(map)
            Timber.d("Rebuild gesture detector info.")
        }

        // set so that once keyboard layout is loaded, it can set up
        this.gestureClassifierInitialized = true
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
        swipeGestureDetector.apply {
            distanceThreshold = prefs.gestures.swipeDistanceThreshold
            velocityThreshold = prefs.gestures.swipeVelocityThreshold
        }
        gestureDetector.apply {
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

        if (computedLayout?.mode == KeyboardMode.CHARACTERS &&
            gestureDetector.onTouchEvent(event) &&
            event.actionMasked != MotionEvent.ACTION_UP) {
            for (pointerIndex in 0 until event.pointerCount) {
                val pointerId = event.getPointerId(pointerIndex)
                sendFlorisTouchEvent(event, pointerIndex, pointerId, MotionEvent.ACTION_CANCEL)
                activeKeyViews.remove(pointerId)
            }
            invalidate()
            this.gesturing = true
            return true
        } else {
            this.gesturing = false
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
                    prefs.gestures.deleteKeySwipeLeft == SwipeAction.DELETE_WORD) {
                    florisboard?.executeSwipeAction(prefs.gestures.deleteKeySwipeLeft)
                    true
                } else {
                    false
                }
            }
            initialKeyCodes[event.pointerId] == KeyCode.SHIFT && activeKeyViews[event.pointerId]?.data?.code != KeyCode.SHIFT &&
                event.type == SwipeGesture.Type.TOUCH_UP -> {
                activeKeyViews[event.pointerId]?.let {
                    florisboard?.textInputManager?.inputEventDispatcher?.send(InputKeyEvent.up(it.popupManager.getActiveKeyData(it)
                        ?: it.data))
                    florisboard?.textInputManager?.inputEventDispatcher?.send(InputKeyEvent.cancel(KeyData.SHIFT))
                }
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
        activeKeyViews.remove(pointerId)?.onFlorisTouchEvent(MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0
        ))
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

    private val paint = Paint().apply {
        color = Color.GREEN
        alpha = 100
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        // draw a line if gesturing:
        val gestureData = gestureDataForDrawing
        if (gesturing && gestureData.isNotEmpty()) {
            for (i in gestureData.size - 2 downTo 0) {
                paint.strokeWidth = paint.strokeWidth * 0.95f
                canvas?.drawLine(gestureData[i].x, gestureData[i].y, gestureData[i + 1].x, gestureData[i + 1].y, paint)
            }
        }
        paint.strokeWidth = 40.0f
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

    override fun onGestureComplete(event: GlideTypingGesture.Event): Boolean {
        val suggestion = gestureTypingClassifier.getSuggestions(1, true)
        Timber.d("Predictions: $suggestion")
        gestureTypingClassifier.clear()
        this.gestureDataForDrawing.clear()

        gesturing = false
        invalidate()
        return true
    }

    override fun onGestureAdd(gesture: MutableList<GlideTypingGesture.Detector.Position>) {
        this.gestureDataForDrawing.add(gesture.last())
        this.gestureTypingClassifier.addGesturePoint(gesture.last())
    }
}
