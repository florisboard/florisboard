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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import androidx.core.view.isVisible
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
import dev.patrickgold.florisboard.common.Pointer
import dev.patrickgold.florisboard.common.PointerMap
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.ime.keyboard.ImeOptions
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Suppress("UNUSED_PARAMETER")
class TextKeyboardView : KeyboardView, SwipeGesture.Listener, GlideTypingGesture.Listener {
    private var computedKeyboard: TextKeyboard? = null
    private var iconSet: TextKeyboardIconSet? = null

    // IS ONLY USED IF KEYBOARD IS IN PREVIEW MODE
    private var cachedTheme: Theme? = null

    private var cachedState: KeyboardState = KeyboardState.new(maskOfInterest = KeyboardState.INTEREST_TEXT)

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

    private var keyHintConfiguration: KeyHintConfiguration = KeyHintConfiguration.HINTS_DISABLED
    private val pointerMap: PointerMap<TouchPointer> = PointerMap { TouchPointer() }
    private val popupManager: PopupManager<TextKeyboardView>

    private var initSelectionStart: Int = 0
    private var initSelectionEnd: Int = 0
    private var isGliding: Boolean = false
    private val swipeGestureDetector = SwipeGesture.Detector(context, this)

    private val glideTypingDetector = GlideTypingGesture.Detector(context)
    private val glideTypingManager: GlideTypingManager
        get() = GlideTypingManager.getInstance()
    private val glideDataForDrawing: MutableList<GlideTypingGesture.Detector.Position> = mutableListOf()
    private val fadingGlide: MutableList<GlideTypingGesture.Detector.Position> = mutableListOf()
    private var fadingGlideRadius: Float = 0.0f

    val desiredKey: TextKey = TextKey(data = TextKeyData.UNSPECIFIED)
    private var keyMarginH: Int = 0
    private var keyMarginV: Int = 0

    private var keyBackgroundPaint = Paint().also {
        it.isAntiAlias = true
        it.style = Paint.Style.FILL
    }
    private var keyShadowBackgroundPaint = Paint().also {
        it.isAntiAlias = true
        it.style = Paint.Style.STROKE
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = 6.0f
    }
    private val keyBackgroundCornerSize: Float = ViewUtils.dp2px(6.0f)

    private var backgroundDrawable: PaintDrawable = PaintDrawable()
    private val baselineTextSize = resources.getDimension(R.dimen.key_textSize)
    var fontSizeMultiplier: Double = 1.0
        private set
    private val glideTrailPaint: Paint = Paint()
    private var labelPaintTextSize: Float = resources.getDimension(R.dimen.key_textSize)
    private var labelPaintSpaceTextSize: Float = resources.getDimension(R.dimen.key_textSize)
    private val labelPaint: Paint = Paint().also {
        it.isAntiAlias = true
        it.isFakeBoldText = false
        it.textAlign = Paint.Align.CENTER
        it.textSize = labelPaintTextSize
        it.typeface = Typeface.DEFAULT
    }
    private var hintedLabelPaintTextSize: Float = resources.getDimension(R.dimen.key_textHintSize)
    private val hintedLabelPaint: Paint = Paint().also {
        it.isAntiAlias = true
        it.isFakeBoldText = false
        it.textAlign = Paint.Align.CENTER
        it.textSize = hintedLabelPaintTextSize
        it.typeface = Typeface.MONOSPACE
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
        swipeGestureDetector.isEnabled = !isSmartbarKeyboardView

        setWillNotDraw(false)
    }

    fun setComputingEvaluator(evaluator: TextComputingEvaluator?) {
        externalComputingEvaluator = evaluator
    }

    fun setComputedKeyboard(keyboard: TextKeyboard) {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { keyboard.mode.toString() }
        computedKeyboard = keyboard
        initGlideClassifier(keyboard)
        if (isMeasured) {
            computeDesiredDimensions()
            computeKeyboard()
            invalidate()
        }
    }

    fun setIconSet(textKeyboardIconSet: TextKeyboardIconSet) {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { computedKeyboard?.mode?.toString() ?: "" }
        iconSet = textKeyboardIconSet
    }

    override fun onUpdateKeyboardState(newState: KeyboardState) {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { computedKeyboard?.mode?.toString() ?: "" }
        if (isMeasured) {
            if (newState != cachedState) {
                // Something within the defined interest has changed
                cachedState.reset(newState)
                computeKeyboard()
                invalidate()
            }
        }
    }

    override fun sync() {
        swipeGestureDetector.apply {
            distanceThreshold = prefs.gestures.swipeDistanceThreshold
            velocityThreshold = prefs.gestures.swipeVelocityThreshold
        }
        if (isSmartbarKeyboardView) {
            keyMarginH = resources.getDimension(R.dimen.key_marginH).toInt()
            keyMarginV = resources.getDimension(R.dimen.key_marginV).toInt()
        } else {
            keyMarginH = ViewUtils.dp2px(prefs.keyboard.keySpacingHorizontal).toInt()
            keyMarginV = ViewUtils.dp2px(prefs.keyboard.keySpacingVertical).toInt()
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
        keyHintConfiguration = prefs.keyboard.keyHintConfiguration()
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
        flogDebug { "event=$event" }
        val dispatcher = florisboard?.textInputManager?.inputEventDispatcher ?: return
        swipeGestureDetector.onTouchEvent(event)

        if (prefs.glide.enabled && computedKeyboard?.mode == KeyboardMode.CHARACTERS) {
            val glidePointer = pointerMap.findById(0)
            if (glideTypingDetector.onTouchEvent(event, glidePointer?.initialKey)) {
                for (pointer in pointerMap) {
                    if (pointer.activeKey != null) {
                        onTouchCancelInternal(event, pointer)
                    }
                }
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    pointerMap.clear()
                }
                isGliding = true
                return
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dispatcher.send(InputKeyEvent.down(TextKeyData.INTERNAL_BATCH_EDIT))
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val pointer = pointerMap.add(pointerId, pointerIndex)
                if (pointer != null) {
                    swipeGestureDetector.onTouchDown(event, pointer)
                    onTouchDownInternal(event, pointer)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val oldPointer = pointerMap.findById(pointerId)
                if (oldPointer != null) {
                    swipeGestureDetector.onTouchCancel(event, oldPointer)
                    onTouchCancelInternal(event, oldPointer)
                    pointerMap.removeById(oldPointer.id)
                }
                // Search for active character keys and cancel them
                for (pointer in pointerMap) {
                    val activeKey = pointer.activeKey
                    if (activeKey != null && popupManager.isSuitableForPopups(activeKey)) {
                        swipeGestureDetector.onTouchCancel(event, pointer)
                        onTouchUpInternal(event, pointer)
                    }
                }
                val pointer = pointerMap.add(pointerId, pointerIndex)
                if (pointer != null) {
                    swipeGestureDetector.onTouchDown(event, pointer)
                    onTouchDownInternal(event, pointer)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (pointerIndex in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(pointerIndex)
                    val pointer = pointerMap.findById(pointerId)
                    if (pointer != null) {
                        pointer.index = pointerIndex
                        val alwaysTriggerOnMove = (pointer.hasTriggeredGestureMove
                            && (pointer.initialKey?.computedData?.code == KeyCode.DELETE
                            && prefs.gestures.deleteKeySwipeLeft == SwipeAction.DELETE_CHARACTERS_PRECISELY
                            || pointer.initialKey?.computedData?.code == KeyCode.SPACE))
                        if (swipeGestureDetector.onTouchMove(event, pointer, alwaysTriggerOnMove) || pointer.hasTriggeredGestureMove) {
                            pointer.longPressJob?.cancel()
                            pointer.longPressJob = null
                            pointer.hasTriggeredGestureMove = true
                            pointer.activeKey?.let { activeKey ->
                                activeKey.setPressed(false) { invalidate() }
                                florisboard!!.textInputManager.inputEventDispatcher.let { dispatcher ->
                                    if (dispatcher.isPressed(activeKey.computedData.code)) {
                                        dispatcher.send(InputKeyEvent.cancel(activeKey.computedData))
                                    }
                                }
                            }
                        } else {
                            onTouchMoveInternal(event, pointer)
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val pointer = pointerMap.findById(pointerId)
                if (pointer != null) {
                    if (swipeGestureDetector.onTouchUp(event, pointer) || pointer.hasTriggeredGestureMove || pointer.shouldBlockNextUp) {
                        if (pointer.hasTriggeredGestureMove && pointer.initialKey?.computedData?.code == KeyCode.DELETE) {
                            florisboard!!.textInputManager.isGlidePostEffect = false
                            florisboard!!.activeEditorInstance.apply {
                                if (selection.isSelectionMode) {
                                    deleteBackwards()
                                }
                            }
                        }
                        onTouchCancelInternal(event, pointer)
                    } else {
                        onTouchUpInternal(event, pointer)
                    }
                    pointerMap.removeById(pointer.id)
                }
            }
            MotionEvent.ACTION_UP -> {
                dispatcher.send(InputKeyEvent.up(TextKeyData.INTERNAL_BATCH_EDIT))
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                for (pointer in pointerMap) {
                    if (pointer.id == pointerId) {
                        pointer.index = pointerIndex
                        if (swipeGestureDetector.onTouchUp(event, pointer) || pointer.hasTriggeredGestureMove || pointer.shouldBlockNextUp) {
                            if (pointer.hasTriggeredGestureMove && pointer.initialKey?.computedData?.code == KeyCode.DELETE) {
                                florisboard!!.textInputManager.isGlidePostEffect = false
                                florisboard!!.activeEditorInstance.apply {
                                    if (selection.isSelectionMode) {
                                        deleteBackwards()
                                    }
                                }
                            }
                            onTouchCancelInternal(event, pointer)
                        } else {
                            onTouchUpInternal(event, pointer)
                        }
                    } else {
                        swipeGestureDetector.onTouchCancel(event, pointer)
                        onTouchCancelInternal(event, pointer)
                    }
                }
                pointerMap.clear()
            }
            MotionEvent.ACTION_CANCEL -> {
                for (pointer in pointerMap) {
                    swipeGestureDetector.onTouchCancel(event, pointer)
                    onTouchCancelInternal(event, pointer)
                }
                pointerMap.clear()
                dispatcher.send(InputKeyEvent.up(TextKeyData.INTERNAL_BATCH_EDIT))
            }
        }
    }

    private fun onTouchDownInternal(event: MotionEvent, pointer: TouchPointer) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "pointer=$pointer" }

        val key = computedKeyboard?.getKeyForPos(
            event.getX(pointer.index).roundToInt(), event.getY(pointer.index).roundToInt()
        )
        if (key != null && key.isEnabled) {
            florisboard!!.textInputManager.inputEventDispatcher.let { dispatcher ->
                dispatcher.send(InputKeyEvent.down(key.computedData))
            }
            if (prefs.keyboard.popupEnabled && popupManager.isSuitableForPopups(key)) {
                popupManager.show(key, keyHintConfiguration)
            }
            florisboard!!.keyPressVibrate()
            florisboard!!.keyPressSound(key.computedData)
            key.setPressed(true) { invalidate() }
            if (pointer.initialKey == null) {
                pointer.initialKey = key
            }
            pointer.activeKey = key
            pointer.longPressJob = mainScope.launch {
                val delayMillis = prefs.keyboard.longPressDelay.toLong()
                when (key.computedData.code) {
                    KeyCode.SPACE -> {
                        initSelectionStart = florisboard!!.activeEditorInstance.selection.start
                        initSelectionEnd = florisboard!!.activeEditorInstance.selection.end
                        delay((delayMillis * 2.5f).toLong())
                        when (prefs.gestures.spaceBarLongPress) {
                            SwipeAction.NO_ACTION,
                            SwipeAction.INSERT_SPACE -> {
                            }
                            else -> {
                                florisboard!!.executeSwipeAction(prefs.gestures.spaceBarLongPress)
                                pointer.shouldBlockNextUp = true
                            }
                        }
                    }
                    KeyCode.SHIFT -> {
                        delay((delayMillis * 2.5f).toLong())
                        florisboard!!.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(TextKeyData.SHIFT_LOCK))
                        florisboard!!.keyPressVibrate()
                        florisboard!!.keyPressSound(key.computedData)
                    }
                    KeyCode.LANGUAGE_SWITCH -> {
                        delay((delayMillis * 2.0f).toLong())
                        pointer.shouldBlockNextUp = true
                        florisboard!!.textInputManager.inputEventDispatcher.let { dispatcher ->
                            dispatcher.send(InputKeyEvent.downUp(TextKeyData.SHOW_INPUT_METHOD_PICKER))
                        }
                    }
                    else -> {
                        delay(delayMillis)
                        if (popupManager.isSuitableForPopups(key) && key.computedPopups.getPopupKeys(keyHintConfiguration).isNotEmpty()) {
                            popupManager.extend(key, keyHintConfiguration)
                            florisboard!!.keyPressVibrate()
                            florisboard!!.keyPressSound(key.computedData)
                        }
                    }
                }
            }
        } else {
            pointer.activeKey = null
        }
    }

    private fun onTouchMoveInternal(event: MotionEvent, pointer: TouchPointer) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "pointer=$pointer" }

        val initialKey = pointer.initialKey
        val activeKey = pointer.activeKey
        if (initialKey != null && activeKey != null) {
            if (popupManager.isShowingExtendedPopup) {
                if (!popupManager.propagateMotionEvent(activeKey, event, pointer.index)) {
                    onTouchCancelInternal(event, pointer)
                    onTouchDownInternal(event, pointer)
                }
            } else {
                if ((event.getX(pointer.index) < activeKey.visibleBounds.left - 0.1f * activeKey.visibleBounds.width())
                    || (event.getX(pointer.index) > activeKey.visibleBounds.right + 0.1f * activeKey.visibleBounds.width())
                    || (event.getY(pointer.index) < activeKey.visibleBounds.top - 0.35f * activeKey.visibleBounds.height())
                    || (event.getY(pointer.index) > activeKey.visibleBounds.bottom + 0.35f * activeKey.visibleBounds.height())
                ) {
                    onTouchCancelInternal(event, pointer)
                    onTouchDownInternal(event, pointer)
                }
            }
        }
    }

    private fun onTouchUpInternal(event: MotionEvent, pointer: TouchPointer) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "pointer=$pointer" }
        pointer.longPressJob?.cancel()
        pointer.longPressJob = null

        val initialKey = pointer.initialKey
        val activeKey = pointer.activeKey
        if (initialKey != null && activeKey != null) {
            activeKey.setPressed(false) { invalidate() }
            florisboard!!.textInputManager.inputEventDispatcher.let { dispatcher ->
                if (popupManager.isSuitableForPopups(activeKey)) {
                    val retData = popupManager.getActiveKeyData(activeKey, keyHintConfiguration)
                    if (retData != null && !pointer.hasTriggeredGestureMove) {
                        if (retData == activeKey.computedData) {
                            dispatcher.send(InputKeyEvent.up(activeKey.computedData))
                        } else {
                            if (dispatcher.isPressed(activeKey.computedData.code)) {
                                dispatcher.send(InputKeyEvent.cancel(activeKey.computedData))
                            }
                            dispatcher.send(InputKeyEvent.downUp(retData))
                        }
                    } else {
                        if (dispatcher.isPressed(activeKey.computedData.code)) {
                            dispatcher.send(InputKeyEvent.cancel(activeKey.computedData))
                        }
                    }
                    popupManager.hide()
                } else {
                    if (dispatcher.isPressed(activeKey.computedData.code)) {
                        if (pointer.hasTriggeredGestureMove) {
                            dispatcher.send(InputKeyEvent.cancel(activeKey.computedData))
                        } else {
                            dispatcher.send(InputKeyEvent.up(activeKey.computedData))
                        }
                    }
                }
            }
            pointer.activeKey = null
        }
        pointer.hasTriggeredGestureMove = false
        pointer.shouldBlockNextUp = false
    }

    private fun onTouchCancelInternal(event: MotionEvent, pointer: TouchPointer) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "pointer=$pointer" }
        val florisboard = florisboard ?: return
        pointer.longPressJob?.cancel()
        pointer.longPressJob = null

        val activeKey = pointer.activeKey
        if (activeKey != null) {
            activeKey.setPressed(false) { invalidate() }
            florisboard.textInputManager.inputEventDispatcher.let { dispatcher ->
                dispatcher.send(InputKeyEvent.cancel(activeKey.computedData))
            }
            if (popupManager.isSuitableForPopups(activeKey)) {
                popupManager.hide()
            }
            pointer.activeKey = null
        }
        pointer.hasTriggeredGestureMove = false
        pointer.shouldBlockNextUp = false
    }

    override fun onSwipe(event: SwipeGesture.Event): Boolean {
        val florisboard = florisboard ?: return false
        val pointer = pointerMap.findById(event.pointerId) ?: return false
        val initialKey = pointer.initialKey ?: return false
        val activeKey = pointer.activeKey ?: return false
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW)

        return when (initialKey.computedData.code) {
            KeyCode.DELETE -> handleDeleteSwipe(event)
            KeyCode.SPACE -> handleSpaceSwipe(event)
            else -> when {
                initialKey.computedData.code == KeyCode.SHIFT && activeKey.computedData.code == KeyCode.SPACE &&
                    event.type == SwipeGesture.Type.TOUCH_MOVE -> handleSpaceSwipe(event)
                initialKey.computedData.code == KeyCode.SHIFT && activeKey.computedData.code != KeyCode.SHIFT &&
                    event.type == SwipeGesture.Type.TOUCH_UP -> {
                    activeKey.let {
                        florisboard.textInputManager.inputEventDispatcher.send(
                            InputKeyEvent.up(popupManager.getActiveKeyData(it, keyHintConfiguration) ?: it.computedData)
                        )
                    }
                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(TextKeyData.SHIFT))
                    true
                }
                initialKey.computedData.code > KeyCode.SPACE && !popupManager.isShowingExtendedPopup -> when {
                    !prefs.glide.enabled && !pointer.hasTriggeredGestureMove -> when (event.type) {
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
        if (cachedState.isRawInputEditor) return false
        val pointer = pointerMap.findById(event.pointerId) ?: return false

        return when (event.type) {
            SwipeGesture.Type.TOUCH_MOVE -> when (prefs.gestures.deleteKeySwipeLeft) {
                SwipeAction.DELETE_CHARACTERS_PRECISELY -> {
                    florisboard.activeEditorInstance.apply {
                        if (abs(event.relUnitCountX) > 0) {
                            florisboard.keyPressVibrate(isMovingGestureEffect = true)
                        }
                        markComposingRegion(null)
                        selection.updateAndNotify(
                            (selection.end + event.absUnitCountX + 1).coerceIn(0, selection.end),
                            selection.end
                        )
                        pointer.shouldBlockNextUp = true
                    }
                    true
                }
                SwipeAction.DELETE_WORDS_PRECISELY -> when (event.direction) {
                    SwipeGesture.Direction.LEFT -> {
                        florisboard.keyPressVibrate(isMovingGestureEffect = true)
                        florisboard.activeEditorInstance.apply {
                            leftAppendWordToSelection()
                        }
                        pointer.shouldBlockNextUp = true
                        true
                    }
                    SwipeGesture.Direction.RIGHT -> {
                        florisboard.keyPressVibrate(isMovingGestureEffect = true)
                        florisboard.activeEditorInstance.apply {
                            leftPopWordFromSelection()
                        }
                        pointer.shouldBlockNextUp = true
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
        val pointer = pointerMap.findById(event.pointerId) ?: return false

        return when (event.type) {
            SwipeGesture.Type.TOUCH_MOVE -> when (event.direction) {
                SwipeGesture.Direction.LEFT -> {
                    if (prefs.gestures.spaceBarSwipeLeft == SwipeAction.MOVE_CURSOR_LEFT) {
                        abs(event.relUnitCountX).let {
                            val count = if (!pointer.hasTriggeredGestureMove) { it - 1 } else { it }
                            if (count > 0) {
                                florisboard.keyPressVibrate(isMovingGestureEffect = true)
                                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(
                                    TextKeyData.ARROW_LEFT, count))
                            }
                        }
                    }
                    true
                }
                SwipeGesture.Direction.RIGHT -> {
                    if (prefs.gestures.spaceBarSwipeRight == SwipeAction.MOVE_CURSOR_RIGHT) {
                        abs(event.relUnitCountX).let {
                            val count = if (!pointer.hasTriggeredGestureMove) { it - 1 } else { it }
                            if (count > 0) {
                                florisboard.keyPressVibrate(isMovingGestureEffect = true)
                                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(
                                    TextKeyData.ARROW_RIGHT, count))
                            }
                        }
                    }
                    true
                }
                else -> true // To prevent the popup display of nearby keys
            }
            SwipeGesture.Type.TOUCH_UP -> when (event.direction) {
                SwipeGesture.Direction.LEFT -> {
                    prefs.gestures.spaceBarSwipeLeft.let {
                        if (it != SwipeAction.MOVE_CURSOR_LEFT) {
                            florisboard.executeSwipeAction(it)
                            true
                        } else {
                            false
                        }
                    }
                }
                SwipeGesture.Direction.RIGHT -> {
                    prefs.gestures.spaceBarSwipeRight.let {
                        if (it != SwipeAction.MOVE_CURSOR_RIGHT) {
                            florisboard.executeSwipeAction(it)
                            true
                        } else {
                            false
                        }
                    }
                }
                else -> {
                    if (event.absUnitCountY < -6) {
                        florisboard.executeSwipeAction(prefs.gestures.spaceBarSwipeUp)
                        true
                    } else {
                        false
                    }
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { computedKeyboard?.mode?.toString() ?: "" }
        super.onLayout(changed, left, top, right, bottom)
        computeDesiredDimensions()
        computeKeyboard()
    }

    private fun computeDesiredDimensions() {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { computedKeyboard?.mode?.toString() ?: "" }
        val keyboard = computedKeyboard ?: return
        desiredKey.touchBounds.let { bounds ->
            bounds.right = if (isSmartbarKeyboardView) {
                measuredWidth / 6
            } else {
                measuredWidth / 10
            }
            bounds.bottom = when {
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

        setTextSizeFor(
            labelPaint,
            desiredKey.visibleLabelBounds.width().toFloat(),
            desiredKey.visibleLabelBounds.height().toFloat(),
            "X",
            fontSizeMultiplier
        )
        labelPaintTextSize = labelPaint.textSize
        labelPaintSpaceTextSize = labelPaint.textSize.coerceAtMost(resources.getDimension(R.dimen.key_space_textSize))

        setTextSizeFor(
            hintedLabelPaint,
            desiredKey.visibleBounds.width() * 1.0f / 5.0f,
            desiredKey.visibleBounds.height() * 1.0f / 5.0f,
            "X",
            fontSizeMultiplier
        )
        hintedLabelPaintTextSize = hintedLabelPaint.textSize
    }

    private fun computeKeyboard() {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { computedKeyboard?.mode?.toString() ?: "" }
        val keyboard = computedKeyboard ?: return
        for (key in keyboard.keys()) {
            key.compute(internalComputingEvaluator)
            computeLabelsAndDrawables(key, keyHintConfiguration)
        }
        keyboard.layout(this)
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
        keyShadowBackgroundPaint.color = Color.argb(32, 0, 0, 0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!isVisible) return
        if (canvas == null) {
            flogWarning(LogTopic.TEXT_KEYBOARD_VIEW) { "${computedKeyboard?.mode} Cannot draw: 'canvas' is null!" }
            return
        } else {
            flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { computedKeyboard?.mode.toString() }
        }
        if (isPreviewMode) {
            backgroundDrawable.let {
                it.setBounds(0, 0, measuredWidth, measuredHeight)
                it.draw(canvas)
            }
        }
        onDrawComputedKeyboard(canvas)
    }

    private fun onDrawComputedKeyboard(canvas: Canvas) {
        flogInfo(LogTopic.TEXT_KEYBOARD_VIEW) { computedKeyboard?.mode.toString() }
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
                    cachedState.capsLock -> {
                        "capslock"
                    }
                    cachedState.caps -> {
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

        val bgBoundsLeft = key.visibleBounds.left.toFloat()
        val bgBoundsTop = (if (isBorderless) {
            (key.visibleBounds.top + key.visibleBounds.height() * 0.12).toInt()
        } else {
            key.visibleBounds.top
        }).toFloat()
        val bgBoundsRight = key.visibleBounds.right.toFloat()
        val bgBoundsBottom = (if (isBorderless) {
            (key.visibleBounds.bottom - key.visibleBounds.height() * 0.12).toInt()
        } else {
            key.visibleBounds.bottom
        }).toFloat()

        if (shouldShowBorder) {
            keyShadowBackgroundPaint.let {
                canvas.drawRoundRect(
                    bgBoundsLeft, bgBoundsTop, bgBoundsRight, bgBoundsBottom,
                    keyBackgroundCornerSize,
                    keyBackgroundCornerSize,
                    it
                )
            }
        }
        keyBackgroundPaint.let {
            it.color = keyBackground.toSolidColor().color
            canvas.drawRoundRect(
                bgBoundsLeft, bgBoundsTop, bgBoundsRight, bgBoundsBottom,
                keyBackgroundCornerSize,
                keyBackgroundCornerSize,
                it
            )
        }

        val label = key.label
        if (label != null) {
            labelPaint.let {
                it.color = keyForeground.toSolidColor().color
                if (computedKeyboard?.mode == KeyboardMode.CHARACTERS && key.computedData.code == KeyCode.SPACE) {
                    it.alpha = 120
                }
                it.textSize = when (key.computedData.code) {
                    KeyCode.SPACE -> labelPaintSpaceTextSize
                    KeyCode.VIEW_CHARACTERS,
                    KeyCode.VIEW_SYMBOLS,
                    KeyCode.VIEW_SYMBOLS2 -> labelPaintTextSize * 0.80f
                    KeyCode.VIEW_NUMERIC,
                    KeyCode.VIEW_NUMERIC_ADVANCED -> labelPaintTextSize * 0.55f
                    else -> labelPaintTextSize
                }
                val centerX = key.visibleLabelBounds.exactCenterX()
                val centerY = key.visibleLabelBounds.exactCenterY() + (it.textSize - it.descent()) / 2
                if (label.contains("\n")) {
                    // Even if more lines may be existing only the first 2 are shown
                    val labelLines = label.split("\n")
                    val verticalAdjustment = key.visibleBounds.height() * 0.18f
                    canvas.drawText(labelLines[0], centerX, centerY - verticalAdjustment, it)
                    canvas.drawText(labelLines[1], centerX, centerY + verticalAdjustment, it)
                } else {
                    canvas.drawText(label, centerX, centerY, it)
                }
            }
        }

        val hintedLabel = key.hintedLabel
        if (hintedLabel != null) {
            hintedLabelPaint.let {
                it.color = keyForeground.toSolidColor().color
                it.alpha = 170
                it.textSize = hintedLabelPaintTextSize
                val centerX = key.visibleBounds.left + key.visibleBounds.width() * 5.0f / 6.0f
                val centerY = key.visibleBounds.top + key.visibleBounds.height() * 1.0f / 6.0f + (it.textSize - it.descent()) / 2
                canvas.drawText(hintedLabel, centerX, centerY, it)
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
    private fun computeLabelsAndDrawables(key: TextKey, keyHintConfiguration: KeyHintConfiguration) {
        // Reset attributes first to avoid invalid states if not updated
        key.label = null
        key.hintedLabel = null
        key.foregroundDrawableId = null

        val data = key.computedData
        if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE
            && data.code != KeyCode.HALF_SPACE && data.code != KeyCode.KESHIDA || data.type == KeyType.NUMERIC
        ) {
            key.label = data.asString(isForDisplay = true)
            key.computedPopups.getPopupKeys(keyHintConfiguration).hint?.asString(isForDisplay = true).let {
                key.hintedLabel = it
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
                    val imeOptions = cachedState.imeOptions
                    key.foregroundDrawableId = when (imeOptions.enterAction) {
                        ImeOptions.EnterAction.DONE -> R.drawable.ic_done
                        ImeOptions.EnterAction.GO -> R.drawable.ic_arrow_right_alt
                        ImeOptions.EnterAction.NEXT -> R.drawable.ic_arrow_right_alt
                        ImeOptions.EnterAction.NONE -> R.drawable.ic_keyboard_return
                        ImeOptions.EnterAction.PREVIOUS -> R.drawable.ic_arrow_right_alt
                        ImeOptions.EnterAction.SEARCH -> R.drawable.ic_search
                        ImeOptions.EnterAction.SEND -> R.drawable.ic_send
                        ImeOptions.EnterAction.UNSPECIFIED -> R.drawable.ic_keyboard_return
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
                    key.foregroundDrawableId = when (cachedState.caps) {
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
            invalidate()
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

    private class TouchPointer : Pointer() {
        var initialKey: TextKey? = null
        var activeKey: TextKey? = null
        var longPressJob: Job? = null
        var hasTriggeredGestureMove: Boolean = false
        var shouldBlockNextUp: Boolean = false

        override fun reset() {
            super.reset()
            initialKey = null
            activeKey = null
            longPressJob?.cancel()
            longPressJob = null
            hasTriggeredGestureMove = false
            shouldBlockNextUp = false
        }

        override fun toString(): String {
            return "${TouchPointer::class.simpleName} { id=$id, index=$index, initialKey=$initialKey, activeKey=$activeKey }"
        }
    }
}
