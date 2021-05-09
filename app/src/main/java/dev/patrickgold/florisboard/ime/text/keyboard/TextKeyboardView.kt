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

package dev.patrickgold.florisboard.ime.text.keyboard

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.PaintDrawable
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.debug.*
import dev.patrickgold.florisboard.ime.core.*
import dev.patrickgold.florisboard.ime.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.popup.PopupManager
import dev.patrickgold.florisboard.ime.text.gestures.GlideTypingGesture
import dev.patrickgold.florisboard.ime.text.gestures.GlideTypingManager
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.gestures.SwipeGesture
import dev.patrickgold.florisboard.ime.text.key.*
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeValue
import dev.patrickgold.florisboard.util.ViewLayoutUtils
import dev.patrickgold.florisboard.util.cancelAll
import dev.patrickgold.florisboard.util.postDelayed
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class TextKeyboardView : KeyboardView, SwipeGesture.Listener, GlideTypingGesture.Listener {
    private var computedKeyboard: TextKeyboard? = null
    private var iconSet: TextKeyboardIconSet? = null

    // IS ONLY USED IF KEYBOARD IS IN PREVIEW MODE
    private var cachedTheme: Theme? = null

    private var isRecomputingRequested: Boolean = true
    private var externalComputingEvaluator: TextComputingEvaluator? = null
    private val internalComputingEvaluator = object : TextComputingEvaluator {
        override fun evaluateCaps(): Boolean {
            return externalComputingEvaluator?.evaluateCaps()
                ?: DefaultTextComputingEvaluator.evaluateCaps()
        }

        override fun evaluateCaps(data: TextKeyData): Boolean {
            return externalComputingEvaluator?.evaluateCaps(data)
                ?: DefaultTextComputingEvaluator.evaluateCaps(data)
        }

        override fun evaluateEnabled(data: TextKeyData): Boolean {
            return externalComputingEvaluator?.evaluateEnabled(data)
                ?: DefaultTextComputingEvaluator.evaluateEnabled(data)
        }

        override fun evaluateVisible(data: TextKeyData): Boolean {
            return externalComputingEvaluator?.evaluateVisible(data)
                ?: DefaultTextComputingEvaluator.evaluateVisible(data)
        }

        override fun getActiveSubtype(): Subtype {
            return externalComputingEvaluator?.getActiveSubtype()
                ?: DefaultTextComputingEvaluator.getActiveSubtype()
        }

        override fun getKeyVariation(): KeyVariation {
            return externalComputingEvaluator?.getKeyVariation()
                ?: DefaultTextComputingEvaluator.getKeyVariation()
        }

        override fun getKeyboard(): TextKeyboard {
            return computedKeyboard // Purposely not calling the external evaluator!
                ?: DefaultTextComputingEvaluator.getKeyboard()
        }

        override fun isSlot(data: TextKeyData): Boolean {
            return externalComputingEvaluator?.isSlot(data)
                ?: DefaultTextComputingEvaluator.isSlot(data)
        }

        override fun getSlotData(data: TextKeyData): TextKeyData? {
            return externalComputingEvaluator?.getSlotData(data)
                ?: DefaultTextComputingEvaluator.getSlotData(data)
        }
    }

    internal var isSmartbarKeyboardView: Boolean = false
    private var isPreviewMode: Boolean = false
    private var isLoadingPlaceholderKeyboard: Boolean = false

    private var initialKey: TextKey? = null
    private var activeKey: TextKey? = null
    private var activePointerId: Int? = null
    private val longPressHandler: Handler = Handler(context.mainLooper)
    private val popupManager: PopupManager<TextKeyboardView>

    private var initSelectionStart: Int = 0
    private var initSelectionEnd: Int = 0
    private var isGliding: Boolean = false
    private var hasTriggeredGestureMove: Boolean = false
    private var shouldBlockNextUp: Boolean = false
    private val swipeGestureDetector = SwipeGesture.Detector(context, this)

    private val glideTypingDetector = GlideTypingGesture.Detector(context)
    private val glideTypingManager: GlideTypingManager
        get() = GlideTypingManager.getInstance()
    private val glideDataForDrawing: MutableList<GlideTypingGesture.Detector.Position> = mutableListOf()
    private val fadingGlide: MutableList<GlideTypingGesture.Detector.Position> = mutableListOf()
    private var fadingGlideRadius: Float = 0.0f

    val desiredKey: TextKey = TextKey(data = TextKeyData.UNSPECIFIED)

    private var keyBackgroundDrawable: PaintDrawable = PaintDrawable().apply {
        setCornerRadius(ViewLayoutUtils.convertDpToPixel(6.0f, context))
    }

    private var backgroundDrawable: PaintDrawable = PaintDrawable()
    private val baselineTextSize = resources.getDimension(R.dimen.key_textSize)
    var fontSizeMultiplier: Double = 1.0
        private set
    private val glideTrailPaint: Paint = Paint()
    private var labelPaintTextSize: Float = resources.getDimension(R.dimen.key_textSize)
    private var labelPaintSpaceTextSize: Float = resources.getDimension(R.dimen.key_textSize)
    private val labelPaint: Paint = Paint().apply {
        isAntiAlias = true
        isFakeBoldText = false
        textAlign = Paint.Align.CENTER
        textSize = labelPaintTextSize
        typeface = Typeface.DEFAULT
    }
    private var hintedLabelPaintTextSize: Float = resources.getDimension(R.dimen.key_textHintSize)
    private val hintedLabelPaint: Paint = Paint().apply {
        isAntiAlias = true
        isFakeBoldText = false
        textAlign = Paint.Align.CENTER
        textSize = hintedLabelPaintTextSize
        typeface = Typeface.MONOSPACE
    }
    private val tempRect: Rect = Rect()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.obtainStyledAttributes(attrs, R.styleable.TextKeyboardView).apply {
            isPreviewMode = getBoolean(R.styleable.TextKeyboardView_isPreviewKeyboard, false)
            isTouchable = !isPreviewMode
            isSmartbarKeyboardView = getBoolean(R.styleable.TextKeyboardView_isSmartbarKeyboard, false)
            isLoadingPlaceholderKeyboard = getBoolean(R.styleable.TextKeyboardView_isLoadingPlaceholderKeyboard, false)
            recycle()
        }

        val popupLayerView = florisboard?.popupLayerView
        if (popupLayerView == null) {
            flogError(LogTopic.TEXT_KEYBOARD_VIEW) { "PopupLayerView is null, cannot show popups!" }
        }
        popupManager = PopupManager(this, popupLayerView)

        setWillNotDraw(false)
    }

    fun setComputingEvaluator(evaluator: TextComputingEvaluator?) {
        externalComputingEvaluator = evaluator
    }

    fun setComputedKeyboard(keyboard: TextKeyboard) {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { keyboard.toString() }
        computedKeyboard = keyboard
        initGlideClassifier(keyboard)
        notifyStateChanged()
    }

    fun setIconSet(textKeyboardIconSet: TextKeyboardIconSet) {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW)
        iconSet = textKeyboardIconSet
    }

    fun notifyStateChanged() {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW)
        isRecomputingRequested = true
        swipeGestureDetector.apply {
            distanceThreshold = prefs.gestures.swipeDistanceThreshold
            velocityThreshold = prefs.gestures.swipeVelocityThreshold
        }
        if (isMeasured) {
            onLayoutInternal()
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        glideTypingDetector.let {
            it.registerListener(this)
            it.registerListener(glideTypingManager)
            it.velocityThreshold = prefs.gestures.swipeVelocityThreshold
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cachedTheme = null
        glideTypingDetector.let {
            it.unregisterListener(this)
            it.unregisterListener(glideTypingManager)
        }
    }

    override fun onTouchEventInternal(event: MotionEvent) {
        if (prefs.glide.enabled &&
            computedKeyboard?.mode == KeyboardMode.CHARACTERS &&
            glideTypingDetector.onTouchEvent(event, initialKey) &&
            event.actionMasked != MotionEvent.ACTION_UP
        ) {
            if (activePointerId != null) {
                val pointerIndex = event.actionIndex
                onTouchCancelInternal(event, pointerIndex, activePointerId!!)
            }
            isGliding = true
            invalidate()
            return
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (activePointerId != null) {
                    onTouchUpInternal(event, pointerIndex, activePointerId!!)
                    onTouchDownInternal(event, pointerIndex, pointerId, resetInitialKey = false)
                } else {
                    onTouchDownInternal(event, pointerIndex, pointerId, resetInitialKey = true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (pointerIndex in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(pointerIndex)
                    if (activePointerId == pointerId) {
                        onTouchMoveInternal(event, pointerIndex, pointerId)
                        break // No need to continue looping at this point as multi-touch is not supported
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (activePointerId == pointerId) {
                    onTouchUpInternal(event, pointerIndex, pointerId)
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    florisboard?.let {
                        if (it.textInputManager.inputEventDispatcher.isPressed(KeyCode.SHIFT)) {
                            if (initialKey?.computedData?.code == KeyCode.SHIFT && activeKey?.computedData?.code == KeyCode.SHIFT) {
                                it.textInputManager.inputEventDispatcher.send(InputKeyEvent.up(TextKeyData.SHIFT))
                            } else {
                                it.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(TextKeyData.SHIFT))
                            }
                        }
                    }
                    activePointerId = null
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                onTouchCancelInternal(event, pointerIndex, pointerId)
                florisboard?.let {
                    if (it.textInputManager.inputEventDispatcher.isPressed(KeyCode.SHIFT)) {
                        it.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(TextKeyData.SHIFT))
                    }
                }
                activePointerId = null
            }
        }
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "initialKey: ${initialKey?.computedData?.label} activeKey: ${activeKey?.computedData?.label}" }
    }

    private fun onTouchDownInternal(event: MotionEvent, pointerIndex: Int, pointerId: Int, resetInitialKey: Boolean) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "index=$pointerIndex id=$pointerId event=$event" }
        val florisboard = florisboard ?: return

        activePointerId = pointerId
        val key = computedKeyboard?.getKeyForPos(
            event.getX(pointerIndex).roundToInt(), event.getY(pointerIndex).roundToInt()
        )
        if (key != null && key.isEnabled) {
            var keyHintMode = KeyHintMode.DISABLED
            if (prefs.keyboard.hintedNumberRowMode != KeyHintMode.DISABLED && key.computedPopups.hint?.type == KeyType.NUMERIC) {
                keyHintMode = prefs.keyboard.hintedNumberRowMode
            }
            if (prefs.keyboard.hintedSymbolsMode != KeyHintMode.DISABLED && key.computedPopups.hint?.type == KeyType.CHARACTER) {
                keyHintMode = prefs.keyboard.hintedSymbolsMode
            }

            florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.down(key.computedData))
            if (prefs.keyboard.popupEnabled) {
                popupManager.show(key, keyHintMode)
            }
            florisboard.keyPressVibrate()
            florisboard.keyPressSound(key.computedData)
            key.isPressed = true
            if (resetInitialKey) {
                initialKey = key
            }
            activeKey = key
            val delayMillis = prefs.keyboard.longPressDelay.toLong()
            when (key.computedData.code) {
                KeyCode.SPACE -> {
                    initSelectionStart = florisboard.activeEditorInstance.selection.start
                    initSelectionEnd = florisboard.activeEditorInstance.selection.end
                    longPressHandler.postDelayed((delayMillis * 2.5f).toLong()) {
                        when (prefs.gestures.spaceBarLongPress) {
                            SwipeAction.NO_ACTION,
                            SwipeAction.INSERT_SPACE -> {
                            }
                            else -> {
                                florisboard.executeSwipeAction(prefs.gestures.spaceBarLongPress)
                                shouldBlockNextUp = true
                            }
                        }
                    }
                }
                KeyCode.SHIFT -> {
                    longPressHandler.postDelayed((delayMillis * 2.5).toLong()) {
                        florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(TextKeyData.SHIFT_LOCK))
                        florisboard.keyPressVibrate()
                        florisboard.keyPressSound(key.computedData)
                    }
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    longPressHandler.postDelayed((delayMillis * 2.0).toLong()) {
                        shouldBlockNextUp = true
                        florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(TextKeyData.SHOW_INPUT_METHOD_PICKER))
                    }
                }
                else -> {
                    longPressHandler.postDelayed(delayMillis) {
                        if (key.computedPopups.isNotEmpty()) {
                            popupManager.extend(key, keyHintMode)
                            florisboard.keyPressVibrate()
                            florisboard.keyPressSound(key.computedData)
                        }
                    }
                }
            }
            if (!isSmartbarKeyboardView) {
                swipeGestureDetector.onTouchEvent(event)
            }
        } else {
            if (resetInitialKey) {
                initialKey = null
            }
            activeKey = null
        }
        invalidate()
    }

    private fun onTouchMoveInternal(event: MotionEvent, pointerIndex: Int, pointerId: Int) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "index=$pointerIndex id=$pointerId event=$event" }
        val florisboard = florisboard ?: return

        val key = activeKey
        if (key != null) {
            if (!isSmartbarKeyboardView) {
                val alwaysTriggerOnMove = (hasTriggeredGestureMove
                    && (initialKey?.computedData?.code == KeyCode.DELETE
                    && prefs.gestures.deleteKeySwipeLeft == SwipeAction.DELETE_CHARACTERS_PRECISELY
                    || initialKey?.computedData?.code == KeyCode.SPACE || (initialKey?.computedData?.code == KeyCode.SHIFT && activeKey?.computedData?.code == KeyCode.SPACE)))
                if (swipeGestureDetector.onTouchEvent(event, alwaysTriggerOnMove) || hasTriggeredGestureMove) {
                    longPressHandler.cancelAll()
                    hasTriggeredGestureMove = true
                    initialKey?.let {
                        if (it.computedData.code != KeyCode.SHIFT && florisboard.textInputManager.inputEventDispatcher.isPressed(it.computedData.code)) {
                            florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(it.computedData))
                        }
                    }
                    return
                }
            }
            if (popupManager.isShowingExtendedPopup) {
                if (!popupManager.propagateMotionEvent(key, event, pointerIndex)) {
                    onTouchCancelInternal(event, pointerIndex, pointerId)
                    onTouchDownInternal(event, pointerIndex, pointerId, resetInitialKey = false)
                }
            } else {
                if ((event.getX(pointerIndex) < key.visibleBounds.left - 0.1f * key.visibleBounds.width())
                    || (event.getX(pointerIndex) > key.visibleBounds.right + 0.1f * key.visibleBounds.width())
                    || (event.getY(pointerIndex) < key.visibleBounds.top - 0.35f * key.visibleBounds.height())
                    || (event.getY(pointerIndex) > key.visibleBounds.bottom + 0.35f * key.visibleBounds.height())
                ) {
                    onTouchCancelInternal(event, pointerIndex, pointerId)
                    onTouchDownInternal(event, pointerIndex, pointerId, resetInitialKey = false)
                }
            }
        }
        invalidate()
    }

    private fun onTouchUpInternal(event: MotionEvent, pointerIndex: Int, pointerId: Int) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "index=$pointerIndex id=$pointerId event=$event" }
        val florisboard = florisboard ?: return

        longPressHandler.cancelAll()
        val key = activeKey
        if (key != null) {
            if (swipeGestureDetector.onTouchEvent(event) || hasTriggeredGestureMove || shouldBlockNextUp) {
                if (hasTriggeredGestureMove && initialKey?.computedData?.code == KeyCode.DELETE) {
                    florisboard.textInputManager.isGlidePostEffect = false
                    florisboard.activeEditorInstance.apply {
                        if (selection.isSelectionMode) {
                            deleteBackwards()
                        }
                    }
                }
                onTouchCancelInternal(event, pointerIndex, pointerId)
                return
            }
            key.isPressed = false
            if (activeKey?.computedData?.code != KeyCode.SHIFT) {
                val retData = popupManager.getActiveKeyData(key)
                if (retData != null) {
                    if (retData == key.computedData) {
                        florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.up(key.computedData))
                    } else {
                        if (florisboard.textInputManager.inputEventDispatcher.isPressed(key.computedData.code)) {
                            florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(key.computedData))
                        }
                        florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(retData))
                    }
                } else {
                    if (florisboard.textInputManager.inputEventDispatcher.isPressed(key.computedData.code)) {
                        florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(key.computedData))
                    }
                }
            }
        }
        popupManager.hide()
        activePointerId = null
        hasTriggeredGestureMove = false
        shouldBlockNextUp = false
        invalidate()
    }

    private fun onTouchCancelInternal(event: MotionEvent, pointerIndex: Int, pointerId: Int) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "index=$pointerIndex id=$pointerId event=$event" }
        val florisboard = florisboard ?: return

        longPressHandler.cancelAll()
        val key = activeKey
        if (key != null) {
            key.isPressed = false
            if (activeKey?.computedData?.code != KeyCode.SHIFT) {
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(key.computedData))
            }
        }
        popupManager.hide()
        activePointerId = null
        hasTriggeredGestureMove = false
        shouldBlockNextUp = false
        invalidate()
    }

    override fun onSwipe(event: SwipeGesture.Event): Boolean {
        val florisboard = florisboard ?: return false
        val initialKey = initialKey ?: return false
        if (activePointerId != event.pointerId) return false
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW)

        return when (initialKey.computedData.code) {
            KeyCode.DELETE -> handleDeleteSwipe(event)
            KeyCode.SPACE -> handleSpaceSwipe(event)
            else -> when {
                initialKey.computedData.code == KeyCode.SHIFT && activeKey?.computedData?.code == KeyCode.SPACE &&
                    event.type == SwipeGesture.Type.TOUCH_MOVE -> handleSpaceSwipe(event)
                initialKey.computedData.code == KeyCode.SHIFT && activeKey?.computedData?.code != KeyCode.SHIFT &&
                    event.type == SwipeGesture.Type.TOUCH_UP -> {
                    activeKey?.let {
                        florisboard.textInputManager.inputEventDispatcher.send(
                            InputKeyEvent.up(popupManager.getActiveKeyData(it) ?: it.computedData)
                        )
                    }
                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(TextKeyData.SHIFT))
                    true
                }
                initialKey.computedData.code > KeyCode.SPACE && !popupManager.isShowingExtendedPopup -> when {
                    !prefs.glide.enabled && !hasTriggeredGestureMove -> when (event.type) {
                        SwipeGesture.Type.TOUCH_UP -> {
                            val swipeAction = when (event.direction) {
                                SwipeGesture.Direction.UP -> prefs.gestures.swipeUp
                                SwipeGesture.Direction.DOWN -> prefs.gestures.swipeDown
                                SwipeGesture.Direction.LEFT -> prefs.gestures.swipeLeft
                                SwipeGesture.Direction.RIGHT -> prefs.gestures.swipeRight
                                else -> SwipeAction.NO_ACTION
                            }
                            if (swipeAction != SwipeAction.NO_ACTION) {
                                florisboard.executeSwipeAction(swipeAction)
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
    }

    private fun handleDeleteSwipe(event: SwipeGesture.Event): Boolean {
        val florisboard = florisboard ?: return false

        return when (event.type) {
            SwipeGesture.Type.TOUCH_MOVE -> when (prefs.gestures.deleteKeySwipeLeft) {
                SwipeAction.DELETE_CHARACTERS_PRECISELY -> {
                    florisboard.activeEditorInstance.apply {
                        if (abs(event.relUnitCountX) > 0) {
                            florisboard.keyPressVibrate(isMovingGestureEffect = true)
                        }
                        selection.updateAndNotify(
                            (selection.end + event.absUnitCountX + 1).coerceIn(0, selection.end),
                            selection.end
                        )
                        shouldBlockNextUp = true
                    }
                    true
                }
                SwipeAction.DELETE_WORDS_PRECISELY -> when (event.direction) {
                    SwipeGesture.Direction.LEFT -> {
                        florisboard.keyPressVibrate(isMovingGestureEffect = true)
                        florisboard.activeEditorInstance.apply {
                            leftAppendWordToSelection()
                        }
                        shouldBlockNextUp = true
                        true
                    }
                    SwipeGesture.Direction.RIGHT -> {
                        florisboard.keyPressVibrate(isMovingGestureEffect = true)
                        florisboard.activeEditorInstance.apply {
                            leftPopWordFromSelection()
                        }
                        shouldBlockNextUp = true
                        true
                    }
                    else -> false
                }
                else -> false
            }
            SwipeGesture.Type.TOUCH_UP -> {
                if (event.direction == SwipeGesture.Direction.LEFT &&
                    prefs.gestures.deleteKeySwipeLeft == SwipeAction.DELETE_WORD
                ) {
                    florisboard.executeSwipeAction(prefs.gestures.deleteKeySwipeLeft)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun handleSpaceSwipe(event: SwipeGesture.Event): Boolean {
        val florisboard = florisboard ?: return false

        return when (event.type) {
            SwipeGesture.Type.TOUCH_MOVE -> when (event.direction) {
                SwipeGesture.Direction.LEFT -> {
                    if (prefs.gestures.spaceBarSwipeLeft == SwipeAction.MOVE_CURSOR_LEFT) {
                        abs(event.relUnitCountX).let {
                            val count = if (!hasTriggeredGestureMove) { it - 1 } else { it }
                            if (count > 0) {
                                florisboard.keyPressVibrate(isMovingGestureEffect = true)
                                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(
                                    TextKeyData.ARROW_LEFT, count))
                            }
                        }
                    } else {
                        florisboard.executeSwipeAction(prefs.gestures.spaceBarSwipeLeft)
                    }
                    true
                }
                SwipeGesture.Direction.RIGHT -> {
                    if (prefs.gestures.spaceBarSwipeRight == SwipeAction.MOVE_CURSOR_RIGHT) {
                        abs(event.relUnitCountX).let {
                            val count = if (!hasTriggeredGestureMove) { it - 1 } else { it }
                            if (count > 0) {
                                florisboard.keyPressVibrate(isMovingGestureEffect = true)
                                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(
                                    TextKeyData.ARROW_RIGHT, count))
                            }
                        }
                    } else {
                        florisboard.executeSwipeAction(prefs.gestures.spaceBarSwipeRight)
                    }
                    true
                }
                else -> true // To prevent the popup display of nearby keys
            }
            SwipeGesture.Type.TOUCH_UP -> {
                if (event.absUnitCountY.times(-1) > 6) {
                    florisboard.executeSwipeAction(prefs.gestures.spaceBarSwipeUp)
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val desiredHeight = if (isSmartbarKeyboardView || isPreviewMode) {
            MeasureSpec.getSize(heightMeasureSpec).toFloat()
        } else {
            (florisboard?.inputView?.desiredTextKeyboardViewHeight ?: MeasureSpec.getSize(heightMeasureSpec).toFloat())
        } * if (isPreviewMode) {
            0.90f
        } else {
            1.00f
        }

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(desiredWidth.roundToInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(desiredHeight.roundToInt(), MeasureSpec.EXACTLY)
        )
    }

    override fun onLayoutInternal() {
        val keyboard = computedKeyboard
        if (keyboard == null) {
            flogWarning(LogTopic.TEXT_KEYBOARD_VIEW) { "Computed keyboard is null!" }
            return
        } else {
            flogInfo(LogTopic.TEXT_KEYBOARD_VIEW)
        }

        val keyMarginH: Int
        val keyMarginV: Int

        if (isSmartbarKeyboardView) {
            keyMarginH = resources.getDimension(R.dimen.key_marginH).toInt()
            keyMarginV = resources.getDimension(R.dimen.key_marginV).toInt()
        } else {
            keyMarginH = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingHorizontal, context).toInt()
            keyMarginV = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingVertical, context).toInt()
        }

        desiredKey.touchBounds.apply {
            right = if (isSmartbarKeyboardView) {
                measuredWidth / 6
            } else {
                measuredWidth / 10
            }
            bottom = when {
                isSmartbarKeyboardView -> {
                    measuredHeight
                }
                florisboard?.inputView?.shouldGiveAdditionalSpace == true -> {
                    (measuredHeight / (keyboard.rowCount + 0.5f).coerceAtMost(5.0f)).toInt()
                }
                else -> {
                    measuredHeight / keyboard.rowCount.coerceAtLeast(1)
                }
            }
        }
        desiredKey.visibleBounds.apply {
            left = keyMarginH
            right = desiredKey.touchBounds.width() - keyMarginH
            when {
                isSmartbarKeyboardView -> {
                    top = (0.75 * keyMarginV).toInt()
                    bottom = (desiredKey.touchBounds.height() - 0.75 * keyMarginV).toInt()
                }
                else -> {
                    top = keyMarginV
                    bottom = desiredKey.touchBounds.height() - keyMarginV
                }
            }
        }
        TextKeyboard.layoutDrawableBounds(desiredKey, 1.0)
        TextKeyboard.layoutLabelBounds(desiredKey)

        var spaceKey: TextKey? = null
        if (isRecomputingRequested) {
            isRecomputingRequested = false
            for (key in keyboard.keys()) {
                key.compute(internalComputingEvaluator)
                computeLabelsAndDrawables(key)
                if (key.computedData.code == KeyCode.SPACE) {
                    spaceKey = key
                }
            }
        }

        fontSizeMultiplier = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                prefs.keyboard.fontSizeMultiplierPortrait.toFloat() / 100.0
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                prefs.keyboard.fontSizeMultiplierLandscape.toFloat() / 100.0
            }
            else -> 1.0
        }

        keyboard.layout(this)

        setTextSizeFor(
            labelPaint,
            desiredKey.visibleLabelBounds.width().toFloat(),
            desiredKey.visibleLabelBounds.height().toFloat(),
            "X",
            fontSizeMultiplier
        )
        labelPaintTextSize = labelPaint.textSize

        if (spaceKey != null) {
            setTextSizeFor(
                labelPaint,
                spaceKey.visibleLabelBounds.width().toFloat(),
                spaceKey.visibleLabelBounds.height().toFloat(),
                spaceKey.label ?: "X",
                fontSizeMultiplier.coerceAtMost(1.0)
            )
            labelPaintSpaceTextSize = labelPaint.textSize
        }

        setTextSizeFor(
            hintedLabelPaint,
            desiredKey.visibleBounds.width() * 1.0f / 5.0f,
            desiredKey.visibleBounds.height() * 1.0f / 5.0f,
            "X",
            fontSizeMultiplier
        )
        hintedLabelPaintTextSize = hintedLabelPaint.textSize
    }

    /**
     * Automatically sets the text size of [boxPaint] for given [text] so it fits within the given
     * bounds.
     *
     * Implementation based on this blog post by Lucas (SketchingDev), written on Aug 20, 2015
     *  https://sketchingdev.co.uk/blog/resizing-text-to-fit-into-a-container-on-android.html
     *
     * @param boxPaint The [Paint] object which the text size should be applied to.
     * @param boxWidth The max width for the surrounding box of [text].
     * @param boxHeight The max height for the surrounding box of [text].
     * @param text The text for which the size should be calculated.
     * @param multiplier The factor by which the resulting text size should be multiplied with.
     */
    private fun setTextSizeFor(boxPaint: Paint, boxWidth: Float, boxHeight: Float, text: String, multiplier: Double = 1.0): Float {
        var size = baselineTextSize
        boxPaint.textSize = size
        boxPaint.getTextBounds(text, 0, text.length, tempRect)
        val w = tempRect.width().toFloat()
        val h = tempRect.height().toFloat()
        val diffW = abs(boxWidth - w)
        val diffH = abs(boxHeight - h)
        if (w < boxWidth && h < boxHeight) {
            // Text fits, scale up on axis which has less room
            size *= if (diffW < diffH) {
                1.0f + diffW / w
            } else {
                1.0f + diffH / h
            }
        } else if (w >= boxWidth && h < boxHeight) {
            // Text does not fit on x-axis
            size *= (1.0f - diffW / w)
        } else if (w < boxWidth && h >= boxHeight) {
            // Text does not fit on y-axis
            size *= (1.0f - diffH / h)
        } else {
            // Text does not fit at all, scale down on axis which has most overshoot
            size *= if (diffW < diffH) {
                1.0f - diffH / h
            } else {
                1.0f - diffW / w
            }
        }
        size *= multiplier.toFloat()
        boxPaint.textSize = size
        return size
    }

    override fun onThemeUpdated(theme: Theme) {
        if (isPreviewMode) {
            cachedTheme = theme
            backgroundDrawable.apply {
                paint.color = theme.getAttr(Theme.Attr.KEYBOARD_BACKGROUND).toSolidColor().color
            }
        }
        if (theme.getAttr(Theme.Attr.GLIDE_TRAIL_COLOR).toSolidColor().color == 0) {
            glideTrailPaint.color = theme.getAttr(Theme.Attr.WINDOW_COLOR_PRIMARY).toSolidColor().color
            glideTrailPaint.alpha = 32
        } else {
            glideTrailPaint.color = theme.getAttr(Theme.Attr.GLIDE_TRAIL_COLOR).toSolidColor().color
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) {
            flogWarning(LogTopic.TEXT_KEYBOARD_VIEW) { "Cannot draw: 'canvas' is null!" }
            return
        } else {
            flogInfo(LogTopic.TEXT_KEYBOARD_VIEW)
        }

        if (isPreviewMode) {
            backgroundDrawable.apply {
                setBounds(0, 0, measuredWidth, measuredHeight)
                draw(canvas)
            }
        }

        onDrawComputedKeyboard(canvas)
    }

    private fun onDrawComputedKeyboard(canvas: Canvas) {
        val keyboard = computedKeyboard ?: return

        // SUPER JANK nyi message implementation for the editing layout
        if (keyboard.mode == KeyboardMode.EDITING) {
            val msg = "The Editing layout is currently not available, see #216."
            val msg2 = "Will be re-implemented in a later stage."
            labelPaint.apply {
                textSize = 30.0f
            }
            canvas.drawText(msg, measuredWidth / 2.0f, 100.0f, labelPaint)
            canvas.drawText(msg2, measuredWidth / 2.0f, 200.0f, labelPaint)
            return
        }

        val theme = cachedTheme ?: themeManager?.activeTheme ?: Theme.BASE_THEME
        val isBorderless = !theme.getAttr(Theme.Attr.KEY_SHOW_BORDER).toOnOff().state &&
            Color.alpha(theme.getAttr(Theme.Attr.KEY_BACKGROUND).toSolidColor().color) <= 0x0F
        for (key in keyboard.keys()) {
            onDrawComputedKey(canvas, key, theme, isBorderless)
        }
    }

    private fun onDrawComputedKey(canvas: Canvas, key: TextKey, theme: Theme, isBorderless: Boolean) {
        if (!key.isVisible) return

        val keyBackground: ThemeValue
        val keyForeground: ThemeValue
        val shouldShowBorder: Boolean
        val themeLabel = key.computedData.asString(isForDisplay = false)
        when {
            isLoadingPlaceholderKeyboard -> {
                shouldShowBorder = theme.getAttr(Theme.Attr.KEY_SHOW_BORDER, themeLabel).toOnOff().state
                if (key.isPressed && key.isEnabled) {
                    keyBackground = theme.getAttr(Theme.Attr.KEY_BACKGROUND_PRESSED, themeLabel)
                    keyForeground = theme.getAttr(Theme.Attr.KEY_FOREGROUND_PRESSED, themeLabel)
                } else {
                    keyBackground = if (shouldShowBorder) {
                        theme.getAttr(Theme.Attr.KEY_BACKGROUND, themeLabel)
                    } else {
                        theme.getAttr(Theme.Attr.SMARTBAR_BUTTON_BACKGROUND, themeLabel)
                    }
                    keyForeground = theme.getAttr(Theme.Attr.KEY_FOREGROUND, themeLabel)
                }
            }
            isSmartbarKeyboardView -> {
                shouldShowBorder = false
                if (key.isPressed && key.isEnabled) {
                    keyBackground = theme.getAttr(Theme.Attr.SMARTBAR_BUTTON_BACKGROUND)
                    keyForeground = theme.getAttr(Theme.Attr.SMARTBAR_FOREGROUND)
                } else {
                    keyBackground = theme.getAttr(Theme.Attr.SMARTBAR_BACKGROUND)
                    keyForeground = if (!key.isEnabled) {
                        theme.getAttr(Theme.Attr.SMARTBAR_FOREGROUND_ALT)
                    } else {
                        theme.getAttr(Theme.Attr.SMARTBAR_FOREGROUND)
                    }
                }
            }
            else -> {
                val capsSpecific = when {
                    florisboard?.textInputManager?.capsLock == true -> {
                        "capslock"
                    }
                    florisboard?.textInputManager?.caps == true -> {
                        "caps"
                    }
                    else -> {
                        null
                    }
                }
                shouldShowBorder = theme.getAttr(Theme.Attr.KEY_SHOW_BORDER, themeLabel, capsSpecific).toOnOff().state
                if (key.isPressed && key.isEnabled) {
                    keyBackground = theme.getAttr(Theme.Attr.KEY_BACKGROUND_PRESSED, themeLabel, capsSpecific)
                    keyForeground = theme.getAttr(Theme.Attr.KEY_FOREGROUND_PRESSED, themeLabel, capsSpecific)
                } else {
                    keyBackground = theme.getAttr(Theme.Attr.KEY_BACKGROUND, themeLabel, capsSpecific)
                    keyForeground = theme.getAttr(Theme.Attr.KEY_FOREGROUND, themeLabel, capsSpecific)
                }
            }
        }

        keyBackgroundDrawable.apply {
            setBounds(
                key.visibleBounds.left,
                if (isBorderless) {
                    (key.visibleBounds.top + key.visibleBounds.height() * 0.12).toInt()
                } else {
                    key.visibleBounds.top
                },
                key.visibleBounds.right,
                if (isBorderless) {
                    (key.visibleBounds.bottom - key.visibleBounds.height() * 0.12).toInt()
                } else {
                    key.visibleBounds.bottom
                }
            )
            elevation = if (shouldShowBorder) 4.0f else 0.0f
            paint.color = keyBackground.toSolidColor().color
            draw(canvas)
        }

        val label = key.label
        if (label != null) {
            labelPaint.apply {
                color = keyForeground.toSolidColor().color
                if (computedKeyboard?.mode == KeyboardMode.CHARACTERS && key.computedData.code == KeyCode.SPACE) {
                    alpha = 120
                }
                textSize = when (key.computedData.code) {
                    KeyCode.SPACE -> labelPaintSpaceTextSize
                    KeyCode.VIEW_CHARACTERS,
                    KeyCode.VIEW_SYMBOLS,
                    KeyCode.VIEW_SYMBOLS2 -> labelPaintTextSize * 0.80f
                    KeyCode.VIEW_NUMERIC,
                    KeyCode.VIEW_NUMERIC_ADVANCED -> labelPaintTextSize * 0.55f
                    else -> labelPaintTextSize
                }
                val centerX = key.visibleLabelBounds.exactCenterX()
                val centerY = key.visibleLabelBounds.exactCenterY() + (labelPaint.textSize - labelPaint.descent()) / 2
                if (label.contains("\n")) {
                    // Even if more lines may be existing only the first 2 are shown
                    val labelLines = label.split("\n")
                    val verticalAdjustment = key.visibleBounds.height() * 0.18f
                    canvas.drawText(labelLines[0], centerX, centerY - verticalAdjustment, labelPaint)
                    canvas.drawText(labelLines[1], centerX, centerY + verticalAdjustment, labelPaint)
                } else {
                    canvas.drawText(label, centerX, centerY, labelPaint)
                }
            }
        }

        val hintedLabel = key.hintedLabel
        if (hintedLabel != null) {
            hintedLabelPaint.apply {
                color = keyForeground.toSolidColor().color
                alpha = 170
                textSize = hintedLabelPaintTextSize
                val centerX = key.visibleBounds.left + key.visibleBounds.width() * 5.0f / 6.0f
                val centerY = key.visibleBounds.top + key.visibleBounds.height() * 1.0f / 6.0f + (hintedLabelPaint.textSize - hintedLabelPaint.descent()) / 2
                canvas.drawText(hintedLabel, centerX, centerY, hintedLabelPaint)
            }
        }

        val foregroundDrawableId = key.foregroundDrawableId
        if (foregroundDrawableId != null) {
            iconSet?.withDrawable(foregroundDrawableId) {
                bounds = key.visibleDrawableBounds
                setTint(keyForeground.toSolidColor().color)
                draw(canvas)
            }
        }
    }

    /**
     * Computes the labels and drawables needed to draw the key.
     */
    private fun computeLabelsAndDrawables(key: TextKey) {
        // Reset attributes first to avoid invalid states if not updated
        key.label = null
        key.hintedLabel = null
        key.foregroundDrawableId = null

        val data = key.computedData
        if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE
            && data.code != KeyCode.HALF_SPACE && data.code != KeyCode.KESHIDA || data.type == KeyType.NUMERIC
        ) {
            key.label = data.asString(isForDisplay = true)
            val hint = key.computedPopups.hint
            if (prefs.keyboard.hintedNumberRowMode != KeyHintMode.DISABLED && hint?.type == KeyType.NUMERIC) {
                key.hintedLabel = hint.asString(isForDisplay = true)
            }
            if (prefs.keyboard.hintedSymbolsMode != KeyHintMode.DISABLED && hint?.type == KeyType.CHARACTER) {
                key.hintedLabel = hint.asString(isForDisplay = true)
            }
        } else {
            when (data.code) {
                KeyCode.ARROW_LEFT -> {
                    key.foregroundDrawableId = R.drawable.ic_keyboard_arrow_left
                }
                KeyCode.ARROW_RIGHT -> {
                    key.foregroundDrawableId = R.drawable.ic_keyboard_arrow_right
                }
                KeyCode.CLIPBOARD_COPY -> {
                    key.foregroundDrawableId = R.drawable.ic_content_copy
                }
                KeyCode.CLIPBOARD_CUT -> {
                    key.foregroundDrawableId = R.drawable.ic_content_cut
                }
                KeyCode.CLIPBOARD_PASTE -> {
                    key.foregroundDrawableId = R.drawable.ic_content_paste
                }
                KeyCode.CLIPBOARD_SELECT_ALL -> {
                    key.foregroundDrawableId = R.drawable.ic_select_all
                }
                KeyCode.DELETE -> {
                    key.foregroundDrawableId = R.drawable.ic_backspace
                }
                KeyCode.ENTER -> {
                    val imeOptions = florisboard?.activeEditorInstance?.imeOptions ?: ImeOptions.default()
                    key.foregroundDrawableId = when (imeOptions.action) {
                        ImeOptions.Action.DONE -> R.drawable.ic_done
                        ImeOptions.Action.GO -> R.drawable.ic_arrow_right_alt
                        ImeOptions.Action.NEXT -> R.drawable.ic_arrow_right_alt
                        ImeOptions.Action.NONE -> R.drawable.ic_keyboard_return
                        ImeOptions.Action.PREVIOUS -> R.drawable.ic_arrow_right_alt
                        ImeOptions.Action.SEARCH -> R.drawable.ic_search
                        ImeOptions.Action.SEND -> R.drawable.ic_send
                        ImeOptions.Action.UNSPECIFIED -> R.drawable.ic_keyboard_return
                    }
                    if (imeOptions.flagNoEnterAction) {
                        key.foregroundDrawableId = R.drawable.ic_keyboard_return
                    }
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    key.foregroundDrawableId = R.drawable.ic_language
                }
                KeyCode.PHONE_PAUSE -> key.label = resources.getString(R.string.key__phone_pause)
                KeyCode.PHONE_WAIT -> key.label = resources.getString(R.string.key__phone_wait)
                KeyCode.SHIFT -> {
                    key.foregroundDrawableId = when (florisboard?.textInputManager?.caps) {
                        true -> R.drawable.ic_keyboard_capslock
                        else -> R.drawable.ic_keyboard_arrow_up
                    }
                }
                KeyCode.SPACE -> {
                    when (computedKeyboard?.mode) {
                        KeyboardMode.NUMERIC,
                        KeyboardMode.NUMERIC_ADVANCED,
                        KeyboardMode.PHONE,
                        KeyboardMode.PHONE2 -> {
                            key.foregroundDrawableId = R.drawable.ic_space_bar
                        }
                        KeyboardMode.CHARACTERS -> {
                            key.label = florisboard?.activeSubtype?.locale?.displayName
                        }
                        else -> {
                        }
                    }
                }
                KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                    key.foregroundDrawableId = R.drawable.ic_sentiment_satisfied
                }
                KeyCode.SWITCH_TO_CLIPBOARD_CONTEXT -> {
                    key.foregroundDrawableId = R.drawable.ic_assignment
                }
                KeyCode.SWITCH_TO_TEXT_CONTEXT,
                KeyCode.VIEW_CHARACTERS -> {
                    key.label = resources.getString(R.string.key__view_characters)
                }
                KeyCode.VIEW_NUMERIC,
                KeyCode.VIEW_NUMERIC_ADVANCED -> {
                    key.label = resources.getString(R.string.key__view_numeric)
                }
                KeyCode.VIEW_PHONE -> {
                    key.label = resources.getString(R.string.key__view_phone)
                }
                KeyCode.VIEW_PHONE2 -> {
                    key.label = resources.getString(R.string.key__view_phone2)
                }
                KeyCode.VIEW_SYMBOLS -> {
                    key.label = resources.getString(R.string.key__view_symbols)
                }
                KeyCode.VIEW_SYMBOLS2 -> {
                    key.label = resources.getString(R.string.key__view_symbols2)
                }
                KeyCode.HALF_SPACE -> {
                    key.label = resources.getString(R.string.key__view_half_space)
                }
                KeyCode.KESHIDA -> {
                    key.label = resources.getString(R.string.key__view_keshida)
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)

        if (prefs.glide.enabled && prefs.glide.showTrail && !isSmartbarKeyboardView) {
            val targetDist = 5.0f
            val maxPoints = prefs.glide.trailMaxLength
            val radius = 20.0f
            // the tip of the trail will be 1px
            val radiusReductionFactor = (1.0f /radius).pow(1.0f / maxPoints)
            if (fadingGlideRadius > 0) {
                drawGlideTrail(fadingGlide, maxPoints, targetDist, fadingGlideRadius, canvas, radiusReductionFactor)
            }
            if (isGliding && glideDataForDrawing.isNotEmpty()) {
                drawGlideTrail(glideDataForDrawing, maxPoints, targetDist, radius, canvas, radiusReductionFactor)
            }
        }
    }

    private fun drawGlideTrail(
        gestureData: MutableList<GlideTypingGesture.Detector.Position>,
        maxPoints: Int,
        targetDist: Float,
        initialRadius: Float,
        canvas: Canvas?,
        radiusReductionFactor: Float
    ) {
        var radius = initialRadius
        var drawnPoints = 0
        var prevX = gestureData.lastOrNull()?.x ?: 0.0f
        var prevY = gestureData.lastOrNull()?.y ?: 0.0f

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

    private fun initGlideClassifier(keyboard: TextKeyboard) {
        if (isSmartbarKeyboardView || isPreviewMode || keyboard.mode != KeyboardMode.CHARACTERS) {
            return
        }
        post {
            val keys = keyboard.keys().asSequence().toList()
            GlideTypingManager.getInstance().setLayout(keys)
        }
    }

    override fun onGlideAddPoint(point: GlideTypingGesture.Detector.Position) {
        if (prefs.glide.enabled) {
            glideDataForDrawing.add(point)
        }
    }

    override fun onGlideComplete(data: GlideTypingGesture.Detector.PointerData) {
        onGlideCancelled()
    }

    override fun onGlideCancelled() {
        if (prefs.glide.showTrail) {
            fadingGlide.clear()
            fadingGlide.addAll(glideDataForDrawing)

            val animator = ValueAnimator.ofFloat(20.0f, 0.0f)
            animator.interpolator = AccelerateInterpolator()
            animator.duration = prefs.glide.trailDuration.toLong()
            animator.addUpdateListener {
                fadingGlideRadius = it.animatedValue as Float
                invalidate()
            }
            animator.start()

            glideDataForDrawing.clear()
            isGliding = false
            invalidate()
        }
    }
}
