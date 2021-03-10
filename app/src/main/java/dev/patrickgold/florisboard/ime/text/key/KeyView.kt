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

package dev.patrickgold.florisboard.ime.text.key

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.ImeOptions
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.popup.PopupManager
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.gestures.SwipeGesture
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeValue
import dev.patrickgold.florisboard.util.ViewLayoutUtils
import dev.patrickgold.florisboard.util.cancelAll
import dev.patrickgold.florisboard.util.postDelayed
import java.util.*
import kotlin.math.abs

/**
 * View class for managing the rendering and the events of a single keyboard key.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 * @property keyboardView Reference to the parent [KeyboardView].
 * @property data The data the current key represents. Is used to determine rendering and possible
 *  behaviour when events occur.
 */
@SuppressLint("ViewConstructor")
class KeyView(
    private val keyboardView: KeyboardView,
    val data: FlorisKeyData,
    private val florisboard: FlorisBoard?
) : View(keyboardView.context), SwipeGesture.Listener, ThemeManager.OnThemeUpdatedListener {
    private var isKeyPressed: Boolean = false
        set(value) {
            field = value
            updateKeyPressedBackground()
        }
    private var initSelectionStart: Int = 0
    private var initSelectionEnd: Int = 0
    private var hasTriggeredGestureMove: Boolean = false
    private var keyHintMode: KeyHintMode = KeyHintMode.DISABLED
    private val longKeyPressHandler: Handler = Handler(context.mainLooper)
    val popupManager = PopupManager<KeyboardView, KeyView>(keyboardView, florisboard?.popupLayerView)
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    private var shouldBlockNextKeyCode: Boolean = false

    private var backgroundDrawable: PaintDrawable = PaintDrawable().apply {
        setCornerRadius(ViewLayoutUtils.convertDpToPixel(6.0f, context))
    }
    private var desiredWidth: Int = 0
    private var desiredHeight: Int = 0
    private var drawable: Drawable? = null
    private var drawableColor: Int = 0
    private var drawablePaddingH: Int = 0
    private var drawablePaddingV: Int = 0
    private var label: String? = null
    private var labelPaint: Paint = Paint().apply {
        alpha = 255
        color = 0
        isAntiAlias = true
        isFakeBoldText = false
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.key_textSize)
        typeface = Typeface.DEFAULT
    }
    private var hintedLabel: String? = null
    private var hintedLabelPaint: Paint = Paint().apply {
        alpha = 120
        color = 0
        isAntiAlias = true
        isFakeBoldText = false
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.key_textHintSize)
        typeface = Typeface.MONOSPACE
    }
    private val tempRect: Rect = Rect()
    private var themeValueCache: ThemeValueCache = ThemeValueCache()

    val swipeGestureDetector = SwipeGesture.Detector(context, this)
    var touchHitBox: Rect = Rect(-1, -1, -1, -1)

    init {
        layoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            val keyMarginH: Int
            val keyMarginV: Int

            if (keyboardView.isSmartbarKeyboardView) {
                keyMarginH = resources.getDimension(R.dimen.key_marginH).toInt()
                keyMarginV = resources.getDimension(R.dimen.key_marginV).toInt()
            } else {
                keyMarginV = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingVertical, context).toInt()
                keyMarginH = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingHorizontal, context).toInt()
            }

            setMargins(
                keyMarginH,
                keyMarginV,
                keyMarginH,
                keyMarginV
            )
            flexShrink = when (keyboardView.computedLayout?.mode) {
                KeyboardMode.NUMERIC,
                KeyboardMode.NUMERIC_ADVANCED,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> 1.0f
                else -> when (data.code) {
                    KeyCode.SHIFT,
                    KeyCode.VIEW_CHARACTERS,
                    KeyCode.VIEW_SYMBOLS,
                    KeyCode.VIEW_SYMBOLS2,
                    KeyCode.DELETE,
                    KeyCode.ENTER -> 0.0f
                    else -> 1.0f
                }
            }
            flexGrow = when (keyboardView.computedLayout?.mode) {
                KeyboardMode.NUMERIC,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> 0.0f
                KeyboardMode.NUMERIC_ADVANCED -> when (data.type) {
                    KeyType.NUMERIC -> 1.0f
                    else -> 0.0f
                }
                else -> when (data.code) {
                    KeyCode.SPACE -> 1.0f
                    else -> 0.0f
                }
            }
        }
        setPadding(0, 0, 0, 0)

        background = backgroundDrawable
        elevation = if (themeValueCache.shouldShowBorder) 4.0f else 0.0f

        if (prefs.keyboard.hintedNumberRowMode != KeyHintMode.DISABLED && data.popup.hint?.type == KeyType.NUMERIC) {
            keyHintMode = prefs.keyboard.hintedNumberRowMode
        }
        if (prefs.keyboard.hintedSymbolsMode != KeyHintMode.DISABLED && data.popup.hint?.type == KeyType.CHARACTER) {
            keyHintMode = prefs.keyboard.hintedSymbolsMode
        }

        updateKeyPressedBackground()
    }

    /**
     * Creates a label text from the given [keyData].
     *
     * @param keyData Optional. The key data to generate the label from. Defaults to [data].
     * @param caps If the generated text should be uppercase (true) or in lowercase (false).
     *  Defaults to FlorisBoard's TextInputManager's caps state or false. Ignored when the passed
     *  [keyData] is a TLD, in which case always the lower case variant is returned.
     * @param subtype The subtype for which this label should be created. Defaults to
     *  [Subtype.DEFAULT]. Ignored when the passed [keyData] is a TLD.
     * @return The generated label ready for usage in the front-end UI.
     */
    fun getComputedLetter(
        keyData: KeyData = data,
        caps: Boolean = florisboard?.textInputManager?.caps ?: false,
        subtype: Subtype = florisboard?.activeSubtype ?: Subtype.DEFAULT
    ): String {
        return if (caps && keyData is FlorisKeyData && keyData.shift != null) {
            (keyData.shift!!.code.toChar()).toString()
        } else {
            when (data.code) {
                KeyCode.URI_COMPONENT_TLD -> keyData.label.toLowerCase(Locale.ENGLISH)
                else -> {
                    val labelText = (keyData.code.toChar()).toString()
                    if (caps) {
                        labelText.toUpperCase(subtype.locale)
                    } else {
                        labelText
                    }
                }
            }
        }
    }

    /**
     * Disable receiving touch events by the Android system. All touch events should be handled
     * only by the parent [KeyboardView].
     *
     * @see [onFlorisTouchEvent] for an explanation why.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    /**
     * Basically the same as [onTouchEvent], but is only called by the parent [KeyboardView].
     * The parent [KeyboardView] can at any time send an [MotionEvent.ACTION_CANCEL], which means
     * the pointer has lost interest in this key and thus this key should return back to the
     * default, non-pressed state. An [MotionEvent.ACTION_CANCEL] can also be requested by this
     * [KeyView] itself, if it notices that the pointer moved to far from the key and/or from the
     * eventually showing extended popup.
     *
     * The reason why a custom onTouch event listener is being used is that in the default
     * implementation of the Android touch system there isn't really a way for a child view to tell
     * its parent that it has lost interest in having the focus of the parent and the parent should
     * go look at which child the pointer is actually above.
     */
    fun onFlorisTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || !isEnabled) return false
        val florisboard = florisboard ?: return false

        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        val alwaysTriggerOnMove = (hasTriggeredGestureMove
            && (data.code == KeyCode.DELETE && prefs.gestures.deleteKeySwipeLeft == SwipeAction.DELETE_CHARACTERS_PRECISELY
            || data.code == KeyCode.SPACE))
        if (swipeGestureDetector.onTouchEvent(event, alwaysTriggerOnMove)) {
            isKeyPressed = false
            if (florisboard.textInputManager.inputEventDispatcher.isPressed(data.code)) {
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(data))
            }
            longKeyPressHandler.cancelAll()
            popupManager.hide()
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.down(data))
                isKeyPressed = true
                val delayMillis = prefs.keyboard.longPressDelay.toLong()
                hasTriggeredGestureMove = false
                shouldBlockNextKeyCode = false
                if (florisboard.prefs.keyboard.popupEnabled) {
                    popupManager.show(this, keyHintMode)
                }
                isKeyPressed = true
                florisboard.keyPressVibrate()
                florisboard.keyPressSound(data)

                when (data.code) {
                    KeyCode.SPACE -> {
                        initSelectionStart = florisboard.activeEditorInstance.selection.start
                        initSelectionEnd = florisboard.activeEditorInstance.selection.end
                        longKeyPressHandler.postDelayed((delayMillis * 2.5f).toLong()) {
                            when (prefs.gestures.spaceBarLongPress) {
                                SwipeAction.NO_ACTION,
                                SwipeAction.INSERT_SPACE -> {
                                }
                                else -> {
                                    this.florisboard.executeSwipeAction(prefs.gestures.spaceBarLongPress)
                                    shouldBlockNextKeyCode = true
                                }
                            }
                        }
                    }
                    KeyCode.SHIFT -> {
                        longKeyPressHandler.postDelayed((delayMillis * 2.5).toLong()) {
                            this.florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(KeyData.SHIFT_LOCK))
                        }
                    }
                    KeyCode.LANGUAGE_SWITCH -> {
                        longKeyPressHandler.postDelayed((delayMillis * 2.0).toLong()) {
                            shouldBlockNextKeyCode = true
                            this.florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(KeyData.SHOW_INPUT_METHOD_PICKER))
                        }
                    }
                    else -> {
                        longKeyPressHandler.postDelayed(delayMillis) {
                            if (data.popup.isNotEmpty()) {
                                popupManager.extend(this@KeyView, keyHintMode)
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (popupManager.isShowingExtendedPopup) {
                    val isPointerWithinBounds =
                        popupManager.propagateMotionEvent(this, event)
                    if (!isPointerWithinBounds && !shouldBlockNextKeyCode) {
                        keyboardView.dismissActiveKeyViewReference(pointerId)
                    }
                } else {
                    val parent = parent as ViewGroup
                    if ((event.x < -0.1f * measuredWidth && parent.children.first() != this)
                        || (event.x > 1.1f * measuredWidth && parent.children.last() != this)
                        || event.y < -0.35f * measuredHeight
                        || event.y > 1.35f * measuredHeight
                    ) {
                        if (!shouldBlockNextKeyCode) {
                            keyboardView.dismissActiveKeyViewReference(pointerId)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                longKeyPressHandler.cancelAll()
                if (hasTriggeredGestureMove && data.code == KeyCode.DELETE) {
                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(data))
                    florisboard.activeEditorInstance.apply {
                        if (selection.isSelectionMode) {
                            deleteBackwards()
                        }
                    }
                } else {
                    val retData = popupManager.getActiveKeyData(this)
                    if (!shouldBlockNextKeyCode && retData != null) {
                        if (retData == data) {
                            florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.up(data))
                        } else {
                            if (florisboard.textInputManager.inputEventDispatcher.isPressed(data.code)) {
                                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(data))
                            }
                            florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(retData))
                        }
                    } else {
                        if (florisboard.textInputManager.inputEventDispatcher.isPressed(data.code)) {
                            florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(data))
                        }
                    }
                    popupManager.hide()
                }
                shouldBlockNextKeyCode = false
                hasTriggeredGestureMove = false
                isKeyPressed = false
            }
            MotionEvent.ACTION_CANCEL -> {
                longKeyPressHandler.cancelAll()
                if (data.code != KeyCode.SHIFT) {
                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(data))
                }
                popupManager.hide()
                shouldBlockNextKeyCode = false
                hasTriggeredGestureMove = false
                isKeyPressed = false
            }
            else -> return false
        }
        return true
    }

    /**
     * Swipe event handler. Listens to touch_move left/right swipes and triggers the swipe action
     * defined in the prefs.
     */
    override fun onSwipe(event: SwipeGesture.Event): Boolean {
        val florisboard = florisboard ?: return false
        return when (data.code) {
            KeyCode.DELETE -> when (event.type) {
                SwipeGesture.Type.TOUCH_MOVE -> when (prefs.gestures.deleteKeySwipeLeft) {
                    SwipeAction.DELETE_CHARACTERS_PRECISELY -> {
                        florisboard.activeEditorInstance.apply {
                            selection.updateAndNotify(
                                (selection.end + event.absUnitCountX + 1).coerceIn(0, selection.end),
                                selection.end
                            )
                        }
                        hasTriggeredGestureMove = true
                        shouldBlockNextKeyCode = true
                        true
                    }
                    SwipeAction.DELETE_WORDS_PRECISELY -> when (event.direction) {
                        SwipeGesture.Direction.LEFT -> {
                            florisboard.activeEditorInstance.apply {
                                leftAppendWordToSelection()
                            }
                            hasTriggeredGestureMove = true
                            shouldBlockNextKeyCode = true
                            true
                        }
                        SwipeGesture.Direction.RIGHT -> {
                            florisboard.activeEditorInstance.apply {
                                leftPopWordFromSelection()
                            }
                            shouldBlockNextKeyCode = true
                            true
                        }
                        else -> false
                    }
                    else -> false
                }
                else -> false
            }
            KeyCode.SPACE -> when (event.type) {
                SwipeGesture.Type.TOUCH_MOVE -> when (event.direction) {
                    SwipeGesture.Direction.UP -> {
                        if (event.absUnitCountY.times(-1) >= 6) {
                            florisboard.executeSwipeAction(prefs.gestures.spaceBarSwipeUp)
                            hasTriggeredGestureMove = true
                            shouldBlockNextKeyCode = true
                            true
                        } else {
                            false
                        }
                    }
                    SwipeGesture.Direction.LEFT -> {
                        if (prefs.gestures.spaceBarSwipeLeft == SwipeAction.MOVE_CURSOR_LEFT) {
                            abs(event.relUnitCountX).let {
                                val count = if (!hasTriggeredGestureMove) { it - 1 } else { it }
                                if (count > 0) {
                                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(KeyData.ARROW_LEFT, count))
                                }
                            }
                        } else {
                            florisboard.executeSwipeAction(prefs.gestures.spaceBarSwipeLeft)
                        }
                        hasTriggeredGestureMove = true
                        shouldBlockNextKeyCode = true
                        true
                    }
                    SwipeGesture.Direction.RIGHT -> {
                        if (prefs.gestures.spaceBarSwipeRight == SwipeAction.MOVE_CURSOR_RIGHT) {
                            abs(event.relUnitCountX).let {
                                val count = if (!hasTriggeredGestureMove) { it - 1 } else { it }
                                if (count > 0) {
                                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(KeyData.ARROW_RIGHT, count))
                                }
                            }
                        } else {
                            florisboard.executeSwipeAction(prefs.gestures.spaceBarSwipeRight)
                        }
                        hasTriggeredGestureMove = true
                        shouldBlockNextKeyCode = true
                        true
                    }
                    else -> false
                }
                else -> false
            }
            else -> false
        }
    }

    /**
     * Solution base from this great StackOverflow answer which explained and helped a lot
     * for handling onMeasure():
     *  https://stackoverflow.com/a/12267248/6801193
     *  by Devunwired
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val keyMarginH: Int
        val keyMarginV: Int

        if (keyboardView.isSmartbarKeyboardView) {
            keyMarginH = resources.getDimension(R.dimen.key_marginH).toInt()
            keyMarginV = resources.getDimension(R.dimen.key_marginV).toInt()
        } else {
            keyMarginV = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingVertical, context).toInt()
            keyMarginH = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingHorizontal, context).toInt()
        }

        (layoutParams as ViewGroup.MarginLayoutParams).setMargins(
            keyMarginH,
            keyMarginV,
            keyMarginH,
            keyMarginV
        )

        desiredWidth = (keyboardView.desiredKeyWidth * when (keyboardView.computedLayout?.mode) {
            KeyboardMode.NUMERIC,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> 2.68f
            KeyboardMode.NUMERIC_ADVANCED -> when (data.code) {
                44, 46 -> 1.00f
                KeyCode.VIEW_SYMBOLS, 61 -> 1.34f
                else -> 1.56f
            }
            else -> when (data.code) {
                KeyCode.SHIFT,
                KeyCode.DELETE ->
                    if ((keyboardView.computedLayout?.arrangement?.get(2)?.size ?: 0) > 10) {
                        1.12f
                    } else {
                        1.56f
                    }
                KeyCode.VIEW_CHARACTERS,
                KeyCode.VIEW_SYMBOLS,
                KeyCode.VIEW_SYMBOLS2,
                KeyCode.ENTER -> 1.56f
                else -> 1.00f
            }
        }).toInt()
        desiredHeight = keyboardView.desiredKeyHeight

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        // Measure Width
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                // Must be this size
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                // Can't be bigger than...
                desiredWidth.coerceAtMost(widthSize)
            }
            else -> {
                // Be whatever you want
                desiredWidth
            }
        }

        // Measure Height
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                // Must be this size
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                // Can't be bigger than...
                desiredHeight.coerceAtMost(heightSize)
            }
            else -> {
                // Be whatever you want
                desiredHeight
            }
        }

        drawablePaddingH = (0.2f * width).toInt()
        drawablePaddingV = (0.2f * height).toInt()

        // MUST CALL THIS
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateTouchHitBox()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outlineProvider = KeyViewOutline(w, h)
    }

    /**
     * Updates the enabled state of a key depending on the [data] and its parameters.
     */
    private fun updateEnabledState() {
        isEnabled = when (data.code) {
            KeyCode.CLIPBOARD_COPY,
            KeyCode.CLIPBOARD_CUT -> (florisboard != null
                && florisboard.activeEditorInstance.selection.isSelectionMode
                && !florisboard.activeEditorInstance.isRawInputEditor)
            KeyCode.CLIPBOARD_PASTE -> florisboard?.clipboardManager?.hasPrimaryClip() == true
            KeyCode.CLIPBOARD_SELECT_ALL -> {
                florisboard?.activeEditorInstance?.isRawInputEditor == false
            }
            else -> true
        }
        if (!isEnabled) {
            isKeyPressed = false
        }
    }

    override fun onThemeUpdated(theme: Theme) {
        when {
            keyboardView.isLoadingPlaceholderKeyboard -> {
                val label = data.label
                themeValueCache.apply {
                    shouldShowBorder = theme.getAttr(Theme.Attr.KEY_SHOW_BORDER, label).toOnOff().state
                    keyBackground = if (shouldShowBorder) {
                        theme.getAttr(Theme.Attr.KEY_BACKGROUND, label)
                    } else {
                        theme.getAttr(Theme.Attr.SMARTBAR_BUTTON_BACKGROUND, label)
                    }
                    keyBackgroundPressed = theme.getAttr(Theme.Attr.KEY_BACKGROUND_PRESSED, label)
                    keyForeground = keyBackground
                    keyForegroundAlt = ThemeValue.SolidColor(0)
                    keyForegroundPressed = keyBackgroundPressed
                }
            }
            keyboardView.isSmartbarKeyboardView -> {
                themeValueCache.apply {
                    keyBackground = theme.getAttr(Theme.Attr.SMARTBAR_BACKGROUND)
                    keyBackgroundPressed = theme.getAttr(Theme.Attr.SMARTBAR_BUTTON_BACKGROUND)
                    keyForeground = theme.getAttr(Theme.Attr.SMARTBAR_FOREGROUND)
                    keyForegroundAlt = theme.getAttr(Theme.Attr.SMARTBAR_FOREGROUND_ALT)
                    keyForegroundPressed = theme.getAttr(Theme.Attr.SMARTBAR_FOREGROUND)
                    shouldShowBorder = false
                }
            }
            else -> {
                val label = data.label
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
                themeValueCache.apply {
                    keyBackground = theme.getAttr(Theme.Attr.KEY_BACKGROUND, label, capsSpecific)
                    keyBackgroundPressed = theme.getAttr(Theme.Attr.KEY_BACKGROUND_PRESSED, label, capsSpecific)
                    keyForeground = theme.getAttr(Theme.Attr.KEY_FOREGROUND, label, capsSpecific)
                    keyForegroundAlt = ThemeValue.SolidColor(0)
                    keyForegroundPressed = theme.getAttr(Theme.Attr.KEY_FOREGROUND_PRESSED, label, capsSpecific)
                    shouldShowBorder = theme.getAttr(Theme.Attr.KEY_SHOW_BORDER, label, capsSpecific).toOnOff().state
                }
            }
        }
        updateKeyPressedBackground()
    }

    /**
     * Updates the background depending on [isKeyPressed] and [data].
     */
    private fun updateKeyPressedBackground() {
        elevation = if (themeValueCache.shouldShowBorder) 4.0f else 0.0f
        backgroundDrawable.setTint(when {
            isKeyPressed && isEnabled -> themeValueCache.keyBackgroundPressed.toSolidColor().color
            else -> themeValueCache.keyBackground.toSolidColor().color
        })
        invalidate()
    }

    /**
     * Updates the touch hit box of this [KeyView] with its current absolute position within the
     * parent [KeyboardView].
     */
    private fun updateTouchHitBox() {
        if (visibility == GONE) {
            touchHitBox.set(-1, -1, -1, -1)
        } else {
            val parent = parent as ViewGroup

            val keyMarginH: Int
            val keyMarginV: Int

            if (keyboardView.isSmartbarKeyboardView) {
                keyMarginH = resources.getDimension(R.dimen.key_marginH).toInt()
                keyMarginV = resources.getDimension(R.dimen.key_marginV).toInt()
            } else {
                keyMarginV = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingVertical, context).toInt()
                keyMarginH = ViewLayoutUtils.convertDpToPixel(prefs.keyboard.keySpacingHorizontal, context).toInt()
            }

            touchHitBox.apply {
                left = when (this@KeyView) {
                    parent.children.first() -> 0
                    else -> (parent.x + x - keyMarginH).toInt()
                }
                right = when (this@KeyView) {
                    parent.children.last() -> keyboardView.measuredWidth
                    else -> (parent.x + x + measuredWidth + keyMarginH).toInt()
                }
                top = (parent.y + y - keyMarginV).toInt()
                bottom = (parent.y + y + measuredHeight + keyMarginV).toInt()
            }
        }
    }

    /**
     * Updates the visibility of this [KeyView] by checking the current key variation of the parent
     * TextInputManager.
     */
    fun updateVisibility() {
        updateEnabledState()
        when (data.code) {
            KeyCode.SWITCH_TO_TEXT_CONTEXT,
            KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                val tempUtilityKeyAction = when {
                    prefs.keyboard.utilityKeyEnabled -> prefs.keyboard.utilityKeyAction
                    else -> UtilityKeyAction.DISABLED
                }
                visibility = when (tempUtilityKeyAction) {
                    UtilityKeyAction.DISABLED,
                    UtilityKeyAction.SWITCH_LANGUAGE,
                    UtilityKeyAction.SWITCH_KEYBOARD_APP -> GONE
                    UtilityKeyAction.SWITCH_TO_EMOJIS -> VISIBLE
                    UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS ->
                        if (florisboard?.shouldShowLanguageSwitch() == true) {
                            GONE
                        } else {
                            VISIBLE
                        }
                }
            }
            KeyCode.LANGUAGE_SWITCH -> {
                val tempUtilityKeyAction = when {
                    prefs.keyboard.utilityKeyEnabled -> prefs.keyboard.utilityKeyAction
                    else -> UtilityKeyAction.DISABLED
                }
                visibility = when (tempUtilityKeyAction) {
                    UtilityKeyAction.DISABLED,
                    UtilityKeyAction.SWITCH_TO_EMOJIS -> GONE
                    UtilityKeyAction.SWITCH_LANGUAGE,
                    UtilityKeyAction.SWITCH_KEYBOARD_APP -> VISIBLE
                    UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS ->
                        if (florisboard?.shouldShowLanguageSwitch() == true) {
                            VISIBLE
                        } else {
                            GONE
                        }
                }
            }
            else -> if (data.variation != KeyVariation.ALL) {
                val keyVariation = florisboard?.textInputManager?.keyVariation ?: KeyVariation.NORMAL
                visibility = if (data.variation == keyVariation) {
                    VISIBLE
                } else {
                    GONE
                }
                updateTouchHitBox()
            }
        }
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
     */
    private fun setTextSizeFor(boxPaint: Paint, boxWidth: Float, boxHeight: Float, text: String, multiplier: Float = 1.0f): Float {
        var stage = 1
        var textSize = 0.0f
        while (stage < 3) {
            if (stage == 1) {
                textSize += 10.0f
            } else if (stage == 2) {
                textSize -= 1.0f
            }
            boxPaint.textSize = textSize
            boxPaint.getTextBounds(text, 0, text.length, tempRect)
            val fits = tempRect.width() < boxWidth && tempRect.height() < boxHeight
            if (stage == 1 && !fits || stage == 2 && fits) {
                stage++
            }
        }
        textSize *= multiplier
        boxPaint.textSize = textSize
        return textSize
    }

    /**
     * Computes the labels and drawables needed to draw the key.
     */
    private fun computeLabelsAndDrawables() {
        if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE
            && data.code != KeyCode.HALF_SPACE && data.code != KeyCode.KESHIDA || data.type == KeyType.NUMERIC
        ) {
            label = getComputedLetter()
            val hint = data.popup.hint
            if (prefs.keyboard.hintedNumberRowMode != KeyHintMode.DISABLED && hint?.type == KeyType.NUMERIC) {
                hintedLabel = getComputedLetter(hint)
            }
            if (prefs.keyboard.hintedSymbolsMode != KeyHintMode.DISABLED && hint?.type == KeyType.CHARACTER) {
                hintedLabel = getComputedLetter(hint)
            }

        } else {
            when (data.code) {
                KeyCode.ARROW_LEFT -> {
                    drawable = getDrawable(context, R.drawable.ic_keyboard_arrow_left)
                }
                KeyCode.ARROW_RIGHT -> {
                    drawable = getDrawable(context, R.drawable.ic_keyboard_arrow_right)
                }
                KeyCode.CLIPBOARD_COPY -> {
                    drawable = getDrawable(context, R.drawable.ic_content_copy)
                }
                KeyCode.CLIPBOARD_CUT -> {
                    drawable = getDrawable(context, R.drawable.ic_content_cut)
                }
                KeyCode.CLIPBOARD_PASTE -> {
                    drawable = getDrawable(context, R.drawable.ic_content_paste)
                }
                KeyCode.CLIPBOARD_SELECT_ALL -> {
                    drawable = getDrawable(context, R.drawable.ic_select_all)
                }
                KeyCode.DELETE -> {
                    drawable = getDrawable(context, R.drawable.ic_backspace)
                }
                KeyCode.ENTER -> {
                    val imeOptions = florisboard?.activeEditorInstance?.imeOptions ?: ImeOptions.default()
                    drawable = getDrawable(context, when (imeOptions.action) {
                        ImeOptions.Action.DONE -> R.drawable.ic_done
                        ImeOptions.Action.GO -> R.drawable.ic_arrow_right_alt
                        ImeOptions.Action.NEXT -> R.drawable.ic_arrow_right_alt
                        ImeOptions.Action.NONE -> R.drawable.ic_keyboard_return
                        ImeOptions.Action.PREVIOUS -> R.drawable.ic_arrow_right_alt
                        ImeOptions.Action.SEARCH -> R.drawable.ic_search
                        ImeOptions.Action.SEND -> R.drawable.ic_send
                        ImeOptions.Action.UNSPECIFIED -> R.drawable.ic_keyboard_return
                    })
                    if (imeOptions.flagNoEnterAction) {
                        drawable = getDrawable(context, R.drawable.ic_keyboard_return)
                    }
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    drawable = getDrawable(context, R.drawable.ic_language)
                }
                KeyCode.PHONE_PAUSE -> label = resources.getString(R.string.key__phone_pause)
                KeyCode.PHONE_WAIT -> label = resources.getString(R.string.key__phone_wait)
                KeyCode.SHIFT -> {
                    drawable = getDrawable(context, when (florisboard?.textInputManager?.caps) {
                        true -> R.drawable.ic_keyboard_capslock
                        else -> R.drawable.ic_keyboard_arrow_up
                    })
                }
                KeyCode.SPACE -> {
                    when (keyboardView.computedLayout?.mode) {
                        KeyboardMode.NUMERIC,
                        KeyboardMode.NUMERIC_ADVANCED,
                        KeyboardMode.PHONE,
                        KeyboardMode.PHONE2 -> {
                            drawable = getDrawable(context, R.drawable.ic_space_bar)
                        }
                        KeyboardMode.CHARACTERS -> {
                            label = florisboard?.activeSubtype?.locale?.displayName
                        }
                        else -> {
                        }
                    }
                }
                KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                    drawable = getDrawable(context, R.drawable.ic_sentiment_satisfied)
                }
                KeyCode.SWITCH_TO_TEXT_CONTEXT,
                KeyCode.VIEW_CHARACTERS -> {
                    label = resources.getString(R.string.key__view_characters)
                }
                KeyCode.VIEW_NUMERIC,
                KeyCode.VIEW_NUMERIC_ADVANCED -> {
                    label = resources.getString(R.string.key__view_numeric)
                }
                KeyCode.VIEW_PHONE -> {
                    label = resources.getString(R.string.key__view_phone)
                }
                KeyCode.VIEW_PHONE2 -> {
                    label = resources.getString(R.string.key__view_phone2)
                }
                KeyCode.VIEW_SYMBOLS -> {
                    label = resources.getString(R.string.key__view_symbols)
                }
                KeyCode.VIEW_SYMBOLS2 -> {
                    label = resources.getString(R.string.key__view_symbols2)
                }
                KeyCode.HALF_SPACE -> {
                    label = resources.getString(R.string.key__view_half_space)
                }
                KeyCode.KESHIDA -> {
                    label = resources.getString(R.string.key__view_keshida)
                }
            }
        }
    }

    /**
     * Draw the key label / drawable.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        computeLabelsAndDrawables()

        // Draw drawable
        val drawable = drawable
        if (drawable != null) {
            drawableColor = if (keyboardView.isSmartbarKeyboardView && !isEnabled) {
                themeValueCache.keyForegroundAlt.toSolidColor().color
            } else {
                themeValueCache.keyForeground.toSolidColor().color
            }
            var marginV = 0
            var marginH = 0
            if (measuredWidth > measuredHeight) {
                marginH = (measuredWidth - measuredHeight) / 2
            } else {
                marginV = (measuredHeight - measuredWidth) / 2
            }
            // Note: using the vertical padding for horizontal as well on purpose here
            drawable.setBounds(
                marginH + drawablePaddingV,
                marginV + drawablePaddingV,
                measuredWidth - marginH - drawablePaddingV,
                measuredHeight - marginV - drawablePaddingV)
            drawable.setTint(drawableColor)
            drawable.draw(canvas)
        }

        // Draw label
        val label = label
        if (label != null) {
            when (data.code) {
                KeyCode.VIEW_NUMERIC, KeyCode.VIEW_NUMERIC_ADVANCED -> {
                    labelPaint.textSize = resources.getDimension(R.dimen.key_numeric_textSize)
                }
                else -> when {
                    (data.type == KeyType.CHARACTER || data.type == KeyType.NUMERIC) &&
                        data.code != KeyCode.SPACE -> {
                        val cachedTextSize = setTextSizeFor(
                            labelPaint,
                            measuredWidth - (2.6f * drawablePaddingH),
                            measuredHeight - (3.4f * drawablePaddingV),
                            // Note: taking a "X" here because it is one of the biggest letters and
                            //  the keys must have the same base character for calculation, else
                            //  they will all look different and weird...
                            "X",
                            when (resources.configuration.orientation) {
                                Configuration.ORIENTATION_PORTRAIT -> {
                                    prefs.keyboard.fontSizeMultiplierPortrait.toFloat() / 100.0f
                                }
                                Configuration.ORIENTATION_LANDSCAPE -> {
                                    prefs.keyboard.fontSizeMultiplierLandscape.toFloat() / 100.0f
                                }
                                else -> 1.0f
                            }
                        )
                        popupManager.keyPopupTextSize = cachedTextSize
                    }
                    else -> {
                        setTextSizeFor(
                            labelPaint,
                            measuredWidth - (1.2f * drawablePaddingH),
                            measuredHeight - (3.6f * drawablePaddingV),
                            when (data.code) {
                                KeyCode.VIEW_CHARACTERS, KeyCode.VIEW_SYMBOLS, KeyCode.VIEW_SYMBOLS2 -> {
                                    resources.getString(R.string.key__view_symbols)
                                }
                                else -> label
                            }
                        )
                    }
                }
            }
            labelPaint.color = if (isKeyPressed && isEnabled) {
                themeValueCache.keyForegroundPressed.toSolidColor().color
            } else {
                themeValueCache.keyForeground.toSolidColor().color
            }
            labelPaint.alpha = if (keyboardView.computedLayout?.mode == KeyboardMode.CHARACTERS &&
                data.code == KeyCode.SPACE) {
                120
            } else {
                255
            }
            val centerX = measuredWidth / 2.0f
            val centerY = measuredHeight / 2.0f + (labelPaint.textSize - labelPaint.descent()) / 2
            if (label.contains("\n")) {
                // Even if more lines may be existing only the first 2 are shown
                val labelLines = label.split("\n")
                canvas.drawText(labelLines[0], centerX, centerY * 0.70f, labelPaint)
                canvas.drawText(labelLines[1], centerX, centerY * 1.30f, labelPaint)
            } else {
                canvas.drawText(label, centerX, centerY, labelPaint)
            }
        }

        // Draw hinted label
        val hintedLabel = hintedLabel
        if (hintedLabel != null) {
            setTextSizeFor(
                hintedLabelPaint,
                desiredWidth * 1.0f / 5.0f,
                desiredHeight * 1.0f / 5.0f,
                // Note: taking a "X" here because it is one of the biggest letters and
                //  the keys must have the same base character for calculation, else
                //  they will all look different and weird...
                "X"
            )
            hintedLabelPaint.color = labelPaint.color
            hintedLabelPaint.alpha = 170
            val centerX = measuredWidth * 5.0f / 6.0f
            val centerY = measuredHeight * 1.0f / 6.0f + (hintedLabelPaint.textSize - hintedLabelPaint.descent()) / 2
            canvas.drawText(hintedLabel, centerX, centerY, hintedLabelPaint)
        }
    }

    private data class ThemeValueCache(
        var keyBackground: ThemeValue = ThemeValue.SolidColor(0),
        var keyBackgroundPressed: ThemeValue = ThemeValue.SolidColor(0),
        var keyForeground: ThemeValue = ThemeValue.SolidColor(0),
        var keyForegroundAlt: ThemeValue = ThemeValue.SolidColor(0),
        var keyForegroundPressed: ThemeValue = ThemeValue.SolidColor(0),
        var shouldShowBorder: Boolean = true
    )

    /**
     * Custom Outline Provider, needed for the [KeyView] elevation rendering.
     */
    private inner class KeyViewOutline(
        private val width: Int,
        private val height: Int
    ) : ViewOutlineProvider() {

        override fun getOutline(view: View?, outline: Outline?) {
            view ?: return
            outline ?: return
            outline.setRoundRect(
                0,
                0,
                width,
                height,
                view.resources.getDimension(R.dimen.key_borderRadius)
            )
        }
    }
}
