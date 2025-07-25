/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.glideTypingManager
import dev.patrickgold.florisboard.ime.input.InputEventDispatcher
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.keyboard.SpaceBarMode
import dev.patrickgold.florisboard.ime.popup.ExceptionsForKeyCodes
import dev.patrickgold.florisboard.ime.popup.PopupUiController
import dev.patrickgold.florisboard.ime.popup.rememberPopupUiController
import dev.patrickgold.florisboard.ime.text.gestures.GlideTypingGesture
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.gestures.SwipeGesture
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.FlorisRect
import dev.patrickgold.florisboard.lib.Pointer
import dev.patrickgold.florisboard.lib.PointerMap
import dev.patrickgold.florisboard.lib.compose.DisposableLifecycleEffect
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.observeAsTransformingState
import dev.patrickgold.florisboard.lib.toIntOffset
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.isActive
import org.florisboard.lib.android.isOrientationLandscape
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import kotlin.math.abs
import kotlin.math.sqrt

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextKeyboardLayout(
    modifier: Modifier = Modifier,
    evaluator: ComputingEvaluator,
    isPreview: Boolean = false,
): Unit = with(LocalDensity.current) {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val glideTypingManager by context.glideTypingManager()

    val keyboard = evaluator.keyboard as TextKeyboard
    val glideEnabledInternal by prefs.glide.enabled.observeAsState()
    val glideEnabled = glideEnabledInternal && evaluator.editorInfo.isRichInputEditor &&
        evaluator.state.keyVariation != KeyVariation.PASSWORD
    val glideShowTrail by prefs.glide.showTrail.observeAsState()
    val glideTrailStyle = rememberSnyggThemeQuery(FlorisImeUi.GlideTrail.elementName)
    val glideTrailColor = glideTrailStyle.foreground(default = Color.Green)

    val controller = remember { TextKeyboardLayoutController(context) }.also {
        it.keyboard = keyboard
        if (glideEnabled && !isPreview && keyboard.mode == KeyboardMode.CHARACTERS) {
            val keys = keyboard.keys().asSequence().toList()
            glideTypingManager.setLayout(keys)
        }
    }
    val touchEventChannel = remember { Channel<MotionEvent>(64) }

    fun resetAllKeys() {
        try {
            val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
            controller.onTouchEventInternal(event)
            controller.popupUiController.hide()
            event.recycle()
        } catch (e: Throwable) {
            // Ignore
        }
    }

    DisposableEffect(Unit) {
        controller.glideTypingDetector.registerListener(controller)
        controller.glideTypingDetector.registerListener(glideTypingManager)
        onDispose {
            controller.glideTypingDetector.unregisterListener(controller)
            controller.glideTypingDetector.unregisterListener(glideTypingManager)
            resetAllKeys()
        }
    }

    DisposableLifecycleEffect(
        onResume = { /* Do nothing */ },
        onPause = { resetAllKeys() },
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.keyboardUiHeight())
            .onGloballyPositioned { coords ->
                controller.size = coords.size.toSize()
            }
            .pointerInteropFilter { event ->
                if (isPreview) return@pointerInteropFilter false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN,
                    MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_POINTER_UP,
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                        -> {
                        val clonedEvent = MotionEvent.obtain(event)
                        touchEventChannel
                            .trySend(clonedEvent)
                            .onFailure {
                                // Make sure to prevent MotionEvent memory leakage
                                // in case the input channel is full
                                clonedEvent.recycle()
                            }
                        return@pointerInteropFilter true
                    }
                }
                return@pointerInteropFilter false
            }
            .drawWithContent {
                drawContent()
                if (glideEnabled && glideShowTrail) {
                    val targetDist = 3.0f
                    val radius = 20.0f

                    val radiusReductionFactor = 0.99f
                    if (controller.fadingGlideRadius > 0) {
                        controller.drawGlideTrail(
                            this,
                            controller.fadingGlide,
                            targetDist,
                            controller.fadingGlideRadius,
                            radiusReductionFactor,
                            glideTrailColor,
                        )
                    }
                    if (controller.isGliding && controller.glideDataForDrawing.isNotEmpty()) {
                        controller.drawGlideTrail(
                            this, controller.glideDataForDrawing, targetDist, radius,
                            radiusReductionFactor, glideTrailColor,
                        )
                    }
                }
            },
    ) {
        val keyboardWidth = constraints.maxWidth.toFloat()
        val keyboardHeight = constraints.maxHeight.toFloat()
        val keyMarginH by prefs.keyboard.keySpacingHorizontal.observeAsTransformingState { it.dp.toPx() }
        val keyMarginV by prefs.keyboard.keySpacingVertical.observeAsTransformingState { it.dp.toPx() }
        val keyboardRowBaseHeight = FlorisImeSizing.keyboardRowBaseHeight

        val desiredKey = remember(
            keyboard, keyboardWidth, keyboardHeight, keyMarginH, keyMarginV,
            keyboardRowBaseHeight, evaluator
        ) {
            TextKey(data = TextKeyData.UNSPECIFIED).also { desiredKey ->
                desiredKey.touchBounds.apply {
                    width = keyboardWidth / 10f
                    height = when (keyboard.mode) {
                        KeyboardMode.CHARACTERS,
                        KeyboardMode.NUMERIC_ADVANCED,
                        KeyboardMode.SYMBOLS,
                        KeyboardMode.SYMBOLS2 -> {
                            (keyboardHeight / keyboard.rowCount)
                                .coerceAtMost(keyboardRowBaseHeight.toPx() * 1.12f)
                        }
                        else -> keyboardRowBaseHeight.toPx()
                    }
                }
                desiredKey.visibleBounds.applyFrom(desiredKey.touchBounds).deflateBy(keyMarginH, keyMarginV)
                keyboard.layout(keyboardWidth, keyboardHeight, desiredKey, true)
            }
        }

        val popupUiController = rememberPopupUiController(
            key1 = keyboard,
            key2 = desiredKey,
            boundsProvider = { key ->
                val keyPopupWidth: Float
                val keyPopupHeight: Float
                when {
                    configuration.isOrientationLandscape() -> {
                        keyPopupWidth = desiredKey.visibleBounds.width * 1.0f
                        keyPopupHeight = desiredKey.visibleBounds.height * 3.0f
                    }
                    else -> {
                        keyPopupWidth = desiredKey.visibleBounds.width * 1.1f
                        keyPopupHeight = desiredKey.visibleBounds.height * 2.5f
                    }
                }
                val keyPopupDiffX = (key.visibleBounds.width - keyPopupWidth) / 2.0f
                FlorisRect.new().apply {
                    left = key.visibleBounds.left + keyPopupDiffX
                    top = key.visibleBounds.bottom - keyPopupHeight
                    right = left + keyPopupWidth
                    bottom = top + keyPopupHeight
                }
            },
            isSuitableForBasicPopup = { key ->
                if (key is TextKey) {
                    val keyCode = key.computedData.code
                    val keyType = key.computedData.type
                    val numeric = keyboard.mode == KeyboardMode.NUMERIC ||
                        keyboard.mode == KeyboardMode.PHONE || keyboard.mode == KeyboardMode.PHONE2 ||
                        keyboard.mode == KeyboardMode.NUMERIC_ADVANCED && keyType == KeyType.NUMERIC
                    keyCode > KeyCode.SPACE && keyCode != KeyCode.CJK_SPACE && !numeric
                } else {
                    true
                }
            },
            isSuitableForExtendedPopup = { key ->
                if (key is TextKey) {
                    val keyCode = key.computedData.code
                    keyCode > KeyCode.SPACE && keyCode != KeyCode.CJK_SPACE || ExceptionsForKeyCodes.contains(keyCode)
                } else {
                    true
                }
            },
        )
        popupUiController.evaluator = evaluator
        popupUiController.keyHintConfiguration = prefs.keyboard.keyHintConfiguration()
        controller.popupUiController = popupUiController
        val debugShowTouchBoundaries by prefs.devtools.showKeyTouchBoundaries.observeAsState()
        for (textKey in keyboard.keys()) {
            TextKeyButton(
                textKey, evaluator, desiredKey,
                debugShowTouchBoundaries,
            )
        }

        popupUiController.RenderPopups()
    }

    LaunchedEffect(Unit) {
        for (event in touchEventChannel) {
            if (!isActive) break
            controller.onTouchEventInternal(event)
            event.recycle()
        }
    }
}

@Composable
private fun TextKeyButton(
    key: TextKey,
    evaluator: ComputingEvaluator,
    desiredKey: TextKey,
    debugShowTouchBoundaries: Boolean,
) = with(LocalDensity.current) {
    val attributes = mapOf(
        FlorisImeUi.Attr.Code to key.computedData.code,
        FlorisImeUi.Attr.Mode to evaluator.keyboard.mode.toString(),
        FlorisImeUi.Attr.ShiftState to evaluator.state.inputShiftState.toString(),
    )
    val selector = when {
        !key.isEnabled -> SnyggSelector.DISABLED
        key.isPressed -> SnyggSelector.PRESSED
        else -> SnyggSelector.NONE
    }
    val size = remember(key, desiredKey) {
        key.visibleBounds.size.toDpSize()
    }
    SnyggBox(
        FlorisImeUi.Key.elementName,
        attributes = attributes,
        selector = selector,
        modifier = Modifier
            .requiredSize(size)
            .absoluteOffset { key.visibleBounds.topLeft.toIntOffset() },
    ) {
        val isTelPadKey = key.computedData.type == KeyType.NUMERIC && evaluator.keyboard.mode == KeyboardMode.PHONE
        key.label?.let { label ->
            var customLabel = label
            if (key.computedData.code == KeyCode.SPACE) {
                val prefs by florisPreferenceModel()
                val spaceBarMode by prefs.keyboard.spaceBarMode.observeAsState()
                when (spaceBarMode) {
                    SpaceBarMode.NOTHING -> return@let
                    SpaceBarMode.CURRENT_LANGUAGE -> {}
                    SpaceBarMode.SPACE_BAR_KEY -> customLabel = "␣"
                }
            }
            SnyggText(
                modifier = Modifier
                    .wrapContentSize()
                    .align(if (isTelPadKey) BiasAlignment(-0.5f, 0f) else Alignment.Center),
                text = customLabel,
            )
        }
        key.hintedLabel?.let { hintedLabel ->
            SnyggText(
                elementName = FlorisImeUi.KeyHint.elementName,
                attributes = attributes,
                selector = selector,
                modifier = Modifier
                    .wrapContentSize()
                    .align(if (isTelPadKey) BiasAlignment(0.5f, 0f) else Alignment.TopEnd),
                text = hintedLabel,
            )
        }
        key.foregroundImageVector?.let { imageVector ->
            SnyggIcon(
                modifier = Modifier.align(Alignment.Center),
                imageVector = imageVector,
                contentDescription = null,
            )
        }
    }
    if (debugShowTouchBoundaries) {
        Box(
            modifier = Modifier
                .requiredSize(key.touchBounds.size.toDpSize())
                .absoluteOffset { key.touchBounds.topLeft.toIntOffset() }
                .border(Dp.Hairline, Color.Red),
        )
    }
}

@Suppress("unused_parameter")
private class TextKeyboardLayoutController(
    context: Context,
) : SwipeGesture.Listener, GlideTypingGesture.Listener {
    private val prefs by florisPreferenceModel()
    private val editorInstance by context.editorInstance()
    private val keyboardManager by context.keyboardManager()

    private val inputEventDispatcher get() = keyboardManager.inputEventDispatcher
    private val inputFeedbackController get() = FlorisImeService.inputFeedbackController()
    private val keyHintConfiguration = prefs.keyboard.keyHintConfiguration()
    private val pointerMap: PointerMap<TouchPointer> = PointerMap { TouchPointer() }
    lateinit var popupUiController: PopupUiController

    private var initSelectionStart: Int = 0
    private var initSelectionEnd: Int = 0
    var isGliding by mutableStateOf(false)

    val glideTypingDetector = GlideTypingGesture.Detector(context)
    val glideDataForDrawing = mutableStateListOf<Pair<GlideTypingGesture.Detector.Position, Long>>()
    val fadingGlide = mutableStateListOf<Pair<GlideTypingGesture.Detector.Position, Long>>()
    var fadingGlideRadius by mutableFloatStateOf(0.0f)
    private val swipeGestureDetector = SwipeGesture.Detector(this)

    lateinit var keyboard: TextKeyboard
    var size = Size.Zero

    val isGlideEnabled: Boolean get() = prefs.glide.enabled.get() && editorInstance.activeInfo.isRichInputEditor &&
        keyboardManager.activeState.keyVariation != KeyVariation.PASSWORD

    fun onTouchEventInternal(event: MotionEvent) {
        flogDebug { "event=$event" }
        swipeGestureDetector.onTouchEvent(event)
        if (isGlideEnabled && keyboard.mode == KeyboardMode.CHARACTERS) {
            val glidePointer = pointerMap.findById(0)
            val isNotBlocked = glidePointer?.hasTriggeredLongPress != true
            if (isNotBlocked && glideTypingDetector.onTouchEvent(event, glidePointer?.initialKey)) {
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
                    if (activeKey != null && popupUiController.isSuitableForPopups(activeKey)) {
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
                            && prefs.gestures.deleteKeySwipeLeft.get().let {
                                it == SwipeAction.DELETE_CHARACTERS_PRECISELY || it == SwipeAction.SELECT_CHARACTERS_PRECISELY
                            }
                            || pointer.initialKey?.computedData?.code == KeyCode.SPACE
                            || pointer.initialKey?.computedData?.code == KeyCode.CJK_SPACE))
                        if (swipeGestureDetector.onTouchMove(event, pointer, alwaysTriggerOnMove) || pointer.hasTriggeredGestureMove) {
                            pointer.hasTriggeredGestureMove = true
                            pointer.activeKey?.let { activeKey ->
                                inputEventDispatcher.sendCancel(activeKey.computedDataOnDown)
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
                    pointer.index = pointerIndex
                    if (swipeGestureDetector.onTouchUp(event, pointer) || pointer.hasTriggeredGestureMove) {
                        if (pointer.hasTriggeredGestureMove && pointer.initialKey?.computedData?.code == KeyCode.DELETE) {
                            val selection = editorInstance.activeContent.selection
                            if (selection.isSelectionMode) {
                                editorInstance.deleteBackwards()
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
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                for (pointer in pointerMap) {
                    if (pointer.id == pointerId) {
                        pointer.index = pointerIndex
                        if (swipeGestureDetector.onTouchUp(event, pointer) || pointer.hasTriggeredGestureMove) {
                            if (pointer.hasTriggeredGestureMove &&
                                pointer.initialKey?.computedData?.code == KeyCode.DELETE &&
                                prefs.gestures.deleteKeySwipeLeft.get() != SwipeAction.SELECT_CHARACTERS_PRECISELY &&
                                prefs.gestures.deleteKeySwipeLeft.get() != SwipeAction.SELECT_WORDS_PRECISELY) {
                                val selection = editorInstance.activeContent.selection
                                if (selection.isSelectionMode) {
                                    editorInstance.deleteBackwards()
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
            }
        }
    }

    private fun onTouchDownInternal(event: MotionEvent, pointer: TouchPointer) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "pointer=$pointer" }

        val key = keyboard.getKeyForPos(event.getX(pointer.index), event.getY(pointer.index))
        if (key != null && key.isEnabled) {
            key.computedDataOnDown = key.computedData
            pointer.pressedKeyInfo = inputEventDispatcher.sendDown(
                data = key.computedData,
                onLongPress = onLongPress@ {
                    pointer.hasTriggeredLongPress = true
                    when (key.computedData.code) {
                        KeyCode.SPACE, KeyCode.CJK_SPACE -> {
                            when (prefs.gestures.spaceBarLongPress.get()) {
                                SwipeAction.NO_ACTION,
                                SwipeAction.INSERT_SPACE -> {
                                }
                                else -> {
                                    keyboardManager.executeSwipeAction(prefs.gestures.spaceBarLongPress.get())
                                }
                            }
                            true
                        }
                        KeyCode.SHIFT -> {
                            if (inputEventDispatcher.isUninterruptedEventSequence(key.computedData)) {
                                inputEventDispatcher.sendDownUp(TextKeyData.CAPS_LOCK)
                                inputFeedbackController?.keyLongPress(key.computedData)
                            }
                            // We always return false here to prevent blockade for the up touch event
                            false
                        }
                        KeyCode.LANGUAGE_SWITCH -> {
                            inputEventDispatcher.sendDownUp(TextKeyData.SYSTEM_INPUT_METHOD_PICKER)
                            true
                        }
                        else -> {
                            if (popupUiController.isSuitableForPopups(key) && key.computedPopups.getPopupKeys(
                                    keyHintConfiguration
                                ).isNotEmpty()
                            ) {
                                popupUiController.extend(key, size)
                                inputFeedbackController?.keyLongPress(key.computedData)
                                true
                            } else {
                                false
                            }
                        }
                    }
                },
            )
            if (prefs.keyboard.popupEnabled.get() && popupUiController.isSuitableForPopups(key)) {
                popupUiController.show(key)
            }
            inputFeedbackController?.keyPress(key.computedData)
            key.isPressed = true
            if (pointer.initialKey == null) {
                pointer.initialKey = key
            }
            pointer.activeKey = key
            initSelectionStart = editorInstance.activeContent.selection.start
            initSelectionEnd = editorInstance.activeContent.selection.end
        } else {
            pointer.activeKey = null
        }
    }

    private fun onTouchMoveInternal(event: MotionEvent, pointer: TouchPointer) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "pointer=$pointer" }

        val initialKey = pointer.initialKey
        val activeKey = pointer.activeKey
        if (initialKey != null && activeKey != null) {
            if (popupUiController.isShowingExtendedPopup) {
                val x = event.getX(pointer.index)
                val y = event.getY(pointer.index)
                if (!popupUiController.propagateMotionEvent(activeKey, x, y)) {
                    onTouchCancelInternal(event, pointer)
                    onTouchDownInternal(event, pointer)
                }
            } else {
                if ((event.getX(pointer.index) < activeKey.visibleBounds.left - 0.1f * activeKey.visibleBounds.width)
                    || (event.getX(pointer.index) > activeKey.visibleBounds.right + 0.1f * activeKey.visibleBounds.width)
                    || (event.getY(pointer.index) < activeKey.visibleBounds.top - 0.35f * activeKey.visibleBounds.height)
                    || (event.getY(pointer.index) > activeKey.visibleBounds.bottom + 0.35f * activeKey.visibleBounds.height)
                ) {
                    onTouchCancelInternal(event, pointer)
                    onTouchDownInternal(event, pointer)
                }
            }
        }
    }

    private fun onTouchUpInternal(event: MotionEvent, pointer: TouchPointer) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "pointer=$pointer" }
        pointer.pressedKeyInfo?.cancelJobs()
        pointer.pressedKeyInfo = null

        if (pointer.hasTriggeredMassSelection) {
            pointer.hasTriggeredMassSelection = false
            editorInstance.massSelection.end()
        }

        val initialKey = pointer.initialKey
        val activeKey = pointer.activeKey
        if (initialKey != null && activeKey != null) {
            activeKey.isPressed = false
            if (popupUiController.isSuitableForPopups(activeKey)) {
                val retData = popupUiController.getActiveKeyData(activeKey)
                if (retData != null && !pointer.hasTriggeredGestureMove) {
                    if (retData == activeKey.computedData) {
                        if (activeKey.computedData != activeKey.computedDataOnDown) {
                            inputEventDispatcher.sendCancel(activeKey.computedDataOnDown)
                            inputEventDispatcher.sendDownUp(activeKey.computedData)
                        } else {
                            inputEventDispatcher.sendUp(activeKey.computedDataOnDown)
                        }
                    } else {
                        inputEventDispatcher.sendCancel(activeKey.computedDataOnDown)
                        inputEventDispatcher.sendDownUp(retData)
                    }
                } else {
                    inputEventDispatcher.sendCancel(activeKey.computedDataOnDown)
                }
                popupUiController.hide()
            } else {
                if (pointer.hasTriggeredGestureMove) {
                    inputEventDispatcher.sendCancel(activeKey.computedDataOnDown)
                } else {
                    if (activeKey.computedData != activeKey.computedDataOnDown) {
                        inputEventDispatcher.sendCancel(activeKey.computedDataOnDown)
                        inputEventDispatcher.sendDownUp(activeKey.computedData)
                    } else {
                        inputEventDispatcher.sendUp(activeKey.computedDataOnDown)
                    }
                }
            }
            pointer.activeKey = null
        }
        pointer.hasTriggeredGestureMove = false
    }

    private fun onTouchCancelInternal(event: MotionEvent, pointer: TouchPointer) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "pointer=$pointer" }
        pointer.pressedKeyInfo?.cancelJobs()
        pointer.pressedKeyInfo = null

        if (pointer.hasTriggeredMassSelection) {
            pointer.hasTriggeredMassSelection = false
            editorInstance.massSelection.end()
        }

        val activeKey = pointer.activeKey
        if (activeKey != null) {
            activeKey.isPressed = false
            inputEventDispatcher.sendCancel(activeKey.computedDataOnDown)
            if (popupUiController.isSuitableForPopups(activeKey)) {
                popupUiController.hide()
            }
            pointer.activeKey = null
        }
        pointer.hasTriggeredGestureMove = false
    }

    override fun onSwipe(event: SwipeGesture.Event): Boolean {
        val pointer = pointerMap.findById(event.pointerId) ?: return false
        val initialKey = pointer.initialKey ?: return false
        val activeKey = pointer.activeKey
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW)

        return when (initialKey.computedData.code) {
            KeyCode.DELETE -> handleDeleteSwipe(event)
            KeyCode.SPACE, KeyCode.CJK_SPACE -> handleSpaceSwipe(event)
            else -> when {
                (initialKey.computedData.code == KeyCode.SHIFT && activeKey?.computedData?.code == KeyCode.SPACE ||
                    initialKey.computedData.code == KeyCode.SHIFT && activeKey?.computedData?.code == KeyCode.CJK_SPACE) &&
                    event.type == SwipeGesture.Type.TOUCH_MOVE -> handleSpaceSwipe(event)
                initialKey.computedData.code == KeyCode.SHIFT && activeKey?.computedData?.code != KeyCode.SHIFT &&
                    event.type == SwipeGesture.Type.TOUCH_UP -> {
                    activeKey?.let {
                        inputEventDispatcher.sendUp(popupUiController.getActiveKeyData(it) ?: it.computedDataOnDown)
                    }
                    inputEventDispatcher.sendCancel(TextKeyData.SHIFT)
                    true
                }
                initialKey.computedData.code > KeyCode.SPACE && !popupUiController.isShowingExtendedPopup -> when {
                    !isGlideEnabled && !pointer.hasTriggeredGestureMove -> when (event.type) {
                        SwipeGesture.Type.TOUCH_UP -> {
                            val swipeAction = when (event.direction) {
                                SwipeGesture.Direction.UP -> prefs.gestures.swipeUp.get()
                                SwipeGesture.Direction.DOWN -> prefs.gestures.swipeDown.get()
                                SwipeGesture.Direction.LEFT -> prefs.gestures.swipeLeft.get()
                                SwipeGesture.Direction.RIGHT -> prefs.gestures.swipeRight.get()
                                else -> SwipeAction.NO_ACTION
                            }
                            if (swipeAction != SwipeAction.NO_ACTION) {
                                keyboardManager.executeSwipeAction(swipeAction)
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
        if (editorInstance.activeInfo.isRawInputEditor) return false

        return when (event.type) {
            SwipeGesture.Type.TOUCH_MOVE -> when (prefs.gestures.deleteKeySwipeLeft.get()) {
                SwipeAction.DELETE_CHARACTERS_PRECISELY, SwipeAction.SELECT_CHARACTERS_PRECISELY -> {
                    if (abs(event.relUnitCountX) > 0) {
                        inputFeedbackController?.gestureMovingSwipe(TextKeyData.DELETE)
                    }
                    val activeSelection = editorInstance.activeContent.selection
                    if (activeSelection.isValid) {
                        editorInstance.setSelection(
                            (activeSelection.end + event.absUnitCountX + 1).coerceIn(0, activeSelection.end),
                            activeSelection.end,
                        )
                    }
                    true
                }
                SwipeAction.DELETE_WORDS_PRECISELY, SwipeAction.SELECT_WORDS_PRECISELY -> {
                    if (abs(event.relUnitCountX) > 0) {
                        inputFeedbackController?.gestureMovingSwipe(TextKeyData.DELETE)
                    }
                    val activeSelection = editorInstance.activeContent.selection
                    if (activeSelection.isValid && event.absUnitCountX <= 0) {
                        editorInstance.selectionSetNWordsLeft(abs(event.absUnitCountX / 2) - 1)
                    }
                    true
                }
                else -> false
            }
            SwipeGesture.Type.TOUCH_UP -> {
                if (event.direction == SwipeGesture.Direction.LEFT &&
                    prefs.gestures.deleteKeySwipeLeft.get() == SwipeAction.DELETE_WORD
                ) {
                    keyboardManager.executeSwipeAction(prefs.gestures.deleteKeySwipeLeft.get())
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun handleSpaceSwipe(event: SwipeGesture.Event): Boolean {
        val pointer = pointerMap.findById(event.pointerId) ?: return false

        return when (event.type) {
            SwipeGesture.Type.TOUCH_MOVE -> when (event.direction) {
                SwipeGesture.Direction.LEFT -> {
                    val action = prefs.gestures.spaceBarSwipeLeft.get()
                    if (action == SwipeAction.MOVE_CURSOR_LEFT) {
                        abs(event.relUnitCountX).let {
                            val count = if (!pointer.hasTriggeredGestureMove) it - 1 else it
                            if (count > 0) {
                                inputFeedbackController?.gestureMovingSwipe(TextKeyData.SPACE)
                                if (!pointer.hasTriggeredMassSelection) {
                                    pointer.hasTriggeredMassSelection = true
                                    editorInstance.massSelection.begin()
                                }
                                keyboardManager.handleArrow(KeyCode.ARROW_LEFT, count)
                            }
                        }
                        true
                    } else {
                        action != SwipeAction.NO_ACTION
                    }
                }
                SwipeGesture.Direction.RIGHT -> {
                    val action = prefs.gestures.spaceBarSwipeRight.get()
                    if (action == SwipeAction.MOVE_CURSOR_RIGHT) {
                        abs(event.relUnitCountX).let {
                            val count = if (!pointer.hasTriggeredGestureMove) it - 1 else it
                            if (count > 0) {
                                inputFeedbackController?.gestureMovingSwipe(TextKeyData.SPACE)
                                if (!pointer.hasTriggeredMassSelection) {
                                    pointer.hasTriggeredMassSelection = true
                                    editorInstance.massSelection.begin()
                                }
                                keyboardManager.handleArrow(KeyCode.ARROW_RIGHT, count)
                            }
                        }
                        true
                    } else {
                        action != SwipeAction.NO_ACTION
                    }
                }
                else -> false
            }
            SwipeGesture.Type.TOUCH_UP -> when (event.direction) {
                SwipeGesture.Direction.LEFT -> {
                    prefs.gestures.spaceBarSwipeLeft.get().let {
                        when {
                            it == SwipeAction.NO_ACTION -> {
                                false
                            }
                            it != SwipeAction.MOVE_CURSOR_LEFT -> {
                                keyboardManager.executeSwipeAction(it)
                                true
                            }
                            else -> {
                                false
                            }
                        }
                    }
                }
                SwipeGesture.Direction.RIGHT -> {
                    prefs.gestures.spaceBarSwipeRight.get().let {
                        when {
                            it == SwipeAction.NO_ACTION -> {
                                false
                            }
                            it != SwipeAction.MOVE_CURSOR_RIGHT -> {
                                keyboardManager.executeSwipeAction(it)
                                true
                            }
                            else -> {
                                false
                            }
                        }
                    }
                }
                else -> {
                    if (event.absUnitCountY < -6) {
                        keyboardManager.executeSwipeAction(prefs.gestures.spaceBarSwipeUp.get())
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    override fun onGlideAddPoint(point: GlideTypingGesture.Detector.Position) {
        if (isGlideEnabled) {
            glideDataForDrawing.add(point to System.currentTimeMillis())
        }
    }

    override fun onGlideComplete(data: GlideTypingGesture.Detector.PointerData) {
        onGlideCancelled()
    }

    override fun onGlideCancelled() {
        if (prefs.glide.showTrail.get()) {
            fadingGlide.clear()
            fadingGlide.addAll(glideDataForDrawing)

            val animator = ValueAnimator.ofFloat(20.0f, 0.0f)
            animator.interpolator = AccelerateInterpolator()
            animator.duration = prefs.glide.trailDuration.get().toLong()
            animator.addUpdateListener {
                fadingGlideRadius = it.animatedValue as Float
            }
            animator.start()

            glideDataForDrawing.clear()
            isGliding = false
        }
    }

    fun drawGlideTrail(
        drawScope: ContentDrawScope,
        gestureData: MutableList<Pair<GlideTypingGesture.Detector.Position, Long>>,
        targetDist: Float,
        initialRadius: Float,
        radiusReductionFactor: Float,
        color: Color,
    ) {
        var radius = initialRadius
        var drawnPoints = 0
        var prevX = gestureData.lastOrNull()?.first?.x ?: 0.0f
        var prevY = gestureData.lastOrNull()?.first?.y ?: 0.0f
        val time = System.currentTimeMillis()

        outer@ for (i in gestureData.size - 1 downTo 1) {
            if (time - gestureData[i - 1].second > prefs.glide.trailDuration.get()) break

            val dx = prevX - gestureData[i - 1].first.x
            val dy = prevY - gestureData[i - 1].first.y
            val dist = sqrt(dx * dx + dy * dy)

            val numPoints = (dist / targetDist).toInt()
            for (j in 0 until numPoints) {
                radius *= radiusReductionFactor
                val intermediateX =
                    gestureData[i].first.x * (1 - j.toFloat() / numPoints) + gestureData[i - 1].first.x * (j.toFloat() / numPoints)
                val intermediateY =
                    gestureData[i].first.y * (1 - j.toFloat() / numPoints) + gestureData[i - 1].first.y * (j.toFloat() / numPoints)
                drawScope.drawCircle(color, radius, center = Offset(intermediateX, intermediateY))
                drawnPoints += 1
                prevX = intermediateX
                prevY = intermediateY
            }
        }
    }

    private class TouchPointer : Pointer() {
        var initialKey: TextKey? = null
        var activeKey: TextKey? = null
        var hasTriggeredGestureMove: Boolean = false
        var hasTriggeredLongPress: Boolean = false
        var hasTriggeredMassSelection: Boolean = false
        var pressedKeyInfo: InputEventDispatcher.PressedKeyInfo? = null

        override fun reset() {
            super.reset()
            initialKey = null
            activeKey = null
            hasTriggeredGestureMove = false
            hasTriggeredLongPress = false
            hasTriggeredMassSelection = false
            pressedKeyInfo = null
        }

        override fun toString(): String {
            return "${TouchPointer::class.simpleName} { id=$id, index=$index, initialKey=$initialKey, activeKey=$activeKey }"
        }
    }
}
