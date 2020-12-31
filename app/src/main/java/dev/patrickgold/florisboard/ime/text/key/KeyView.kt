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
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.ImeOptions
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.gestures.SwipeGesture
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.util.cancelAll
import dev.patrickgold.florisboard.util.postAtScheduledRate
import dev.patrickgold.florisboard.util.postDelayed
import dev.patrickgold.florisboard.util.setBackgroundTintColor2
import java.util.*

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
    val data: FlorisKeyData
) : View(keyboardView.context), SwipeGesture.Listener {
    private var isKeyPressed: Boolean = false
        set(value) {
            field = value
            updateKeyPressedBackground()
        }
    private var hasTriggeredGestureMove: Boolean = false
    private var keyHintMode: KeyHintMode = KeyHintMode.DISABLED
    private val longKeyPressHandler: Handler = Handler(context.mainLooper)
    private val repeatedKeyPressHandler: Handler = Handler(context.mainLooper)
    private val prefs: PrefHelper = PrefHelper.getDefaultInstance(context)
    private var shouldBlockNextKeyCode: Boolean = false

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
        typeface = Typeface.DEFAULT
    }
    private val tempRect: Rect = Rect()

    var florisboard: FlorisBoard? = null
    private val swipeGestureDetector = SwipeGesture.Detector(context, this)
    var touchHitBox: Rect = Rect(-1, -1, -1, -1)

    init {
        layoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                resources.getDimension((R.dimen.key_marginH)).toInt(),
                resources.getDimension(R.dimen.key_marginV).toInt(),
                resources.getDimension((R.dimen.key_marginH)).toInt(),
                resources.getDimension(R.dimen.key_marginV).toInt()
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

        background = getDrawable(context, R.drawable.shape_rect_rounded)
        elevation = 4.0f

        if (prefs.keyboard.hintedNumberRowMode != KeyHintMode.DISABLED && data.popup.hint?.type == KeyType.NUMERIC) {
            keyHintMode = prefs.keyboard.hintedNumberRowMode
        }
        if (prefs.keyboard.hintedSymbolsMode != KeyHintMode.DISABLED && data.popup.hint?.type == KeyType.CHARACTER) {
            keyHintMode = prefs.keyboard.hintedNumberRowMode
        }
        /*var hintKeyData: KeyData? = null
        var hintKeyMode: KeyHintMode = KeyHintMode.DISABLED
        val hintedNumber = data.hintedNumber
        if (prefs.keyboard.hintedNumberRowMode != KeyHintMode.DISABLED && hintedNumber != null) {
            hintKeyData = hintedNumber
            hintKeyMode = prefs.keyboard.hintedNumberRowMode
        }
        val hintedSymbol = data.hintedSymbol
        if (prefs.keyboard.hintedSymbolsMode != KeyHintMode.DISABLED && hintedSymbol != null) {
            hintKeyData = hintedSymbol
            hintKeyMode = prefs.keyboard.hintedSymbolsMode
        }
        dataPopupWithHint = if (hintKeyData == null) {
            data.popup.toMutableList()
        } else {
            val popupList = data.popup.toMutableList()
            if (hintKeyMode == KeyHintMode.ENABLED_HINT_PRIORITY) {
                popupList.add(0, hintKeyData)
            } else {
                popupList.add(hintKeyData)
            }
            popupList
        }*/

        updateKeyPressedBackground()
    }

    /**
     * Creates a label text from the given [keyData].
     *
     * @param keyData Optional. The key data to generate the label from. Defaults to [data].
     * @return The generated label.
     */
    fun getComputedLetter(keyData: KeyData = data): String {
        if (keyData.code == KeyCode.URI_COMPONENT_TLD) {
            return when (florisboard?.textInputManager?.caps) {
                true -> keyData.label.toUpperCase(Locale.getDefault())
                else -> keyData.label.toLowerCase(Locale.getDefault())
            }
        }
        val label = (keyData.code.toChar()).toString()
        return when {
            florisboard?.textInputManager?.caps ?: false -> label.toUpperCase(Locale.getDefault())
            else -> label
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
        if (swipeGestureDetector.onTouchEvent(event)) {
            isKeyPressed = false
            longKeyPressHandler.cancelAll()
            repeatedKeyPressHandler.cancelAll()
            keyboardView.popupManager.hide()
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val delayMillis = prefs.keyboard.longPressDelay.toLong()
                hasTriggeredGestureMove = false
                shouldBlockNextKeyCode = false
                florisboard?.prefs?.keyboard?.let {
                    if (it.popupEnabled){
                        keyboardView.popupManager.show(this, keyHintMode)
                    }
                }
                isKeyPressed = true
                florisboard?.keyPressVibrate()
                florisboard?.keyPressSound(data)
                when (data.code) {
                    KeyCode.ARROW_DOWN,
                    KeyCode.ARROW_LEFT,
                    KeyCode.ARROW_RIGHT,
                    KeyCode.ARROW_UP,
                    KeyCode.DELETE -> {
                        repeatedKeyPressHandler.postAtScheduledRate(delayMillis, 25) {
                            if (isKeyPressed) {
                                florisboard?.textInputManager?.sendKeyPress(data)
                            } else {
                                repeatedKeyPressHandler.cancelAll()
                            }
                        }
                    }
                }
                longKeyPressHandler.postDelayed(delayMillis) {
                    if (data.popup.isNotEmpty()) {
                        keyboardView.popupManager.extend(this, keyHintMode)
                    }
                    if (data.code == KeyCode.SPACE) {
                        florisboard?.textInputManager?.sendKeyPress(
                            KeyData(
                                type = KeyType.FUNCTION,
                                code = KeyCode.SHOW_INPUT_METHOD_PICKER,
                            )
                        )
                        shouldBlockNextKeyCode = true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (keyboardView.popupManager.isShowingExtendedPopup) {
                    val isPointerWithinBounds =
                        keyboardView.popupManager.propagateMotionEvent(this, event, keyHintMode)
                    if (!isPointerWithinBounds && !shouldBlockNextKeyCode) {
                        keyboardView.dismissActiveKeyViewReference()
                    }
                } else {
                    val parent = parent as ViewGroup
                    if ((event.x < -0.1f * measuredWidth && parent.children.first() != this)
                        || (event.x > 1.1f * measuredWidth && parent.children.last() != this)
                        || event.y < -0.35f * measuredHeight
                        || event.y > 1.35f * measuredHeight
                    ) {
                        if (!shouldBlockNextKeyCode) {
                            keyboardView.dismissActiveKeyViewReference()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isKeyPressed = false
                longKeyPressHandler.cancelAll()
                repeatedKeyPressHandler.cancelAll()
                if (hasTriggeredGestureMove && data.code == KeyCode.DELETE) {
                    hasTriggeredGestureMove = false
                    florisboard?.activeEditorInstance?.apply {
                        if (selection.isSelectionMode) {
                            deleteBackwards()
                        }
                    }
                } else {
                    val retData = keyboardView.popupManager.getActiveKeyData(this)
                    keyboardView.popupManager.hide()
                    if (event.actionMasked != MotionEvent.ACTION_CANCEL && !shouldBlockNextKeyCode && retData != null) {
                        florisboard?.textInputManager?.sendKeyPress(retData)
                    } else {
                        shouldBlockNextKeyCode = false
                    }
                }
            }
            else -> return false
        }
        return true
    }

    /**
     * Swipe event handler. Listens to touch_move left/right swipes and triggers the swipe action
     * defined in the prefs.
     */
    override fun onSwipe(direction: SwipeGesture.Direction, type: SwipeGesture.Type): Boolean {
        return when (data.code) {
            KeyCode.DELETE -> when (type) {
                SwipeGesture.Type.TOUCH_MOVE -> when (direction) {
                    SwipeGesture.Direction.LEFT -> when (prefs.gestures.deleteKeySwipeLeft) {
                        SwipeAction.DELETE_CHARACTERS_PRECISELY -> {
                            florisboard?.activeEditorInstance?.apply {
                                setSelection(
                                    if (selection.start > 0) { selection.start - 1 } else { selection.start },
                                    selection.end
                                )
                            }
                            hasTriggeredGestureMove = true
                            shouldBlockNextKeyCode = true
                            true
                        }
                        SwipeAction.DELETE_WORDS_PRECISELY -> {
                            florisboard?.activeEditorInstance?.apply {
                                leftAppendWordToSelection()
                            }

                            hasTriggeredGestureMove = true
                            shouldBlockNextKeyCode = true
                            true
                        }
                        else -> false
                    }
                    SwipeGesture.Direction.RIGHT -> when (prefs.gestures.deleteKeySwipeLeft) {
                        SwipeAction.DELETE_CHARACTERS_PRECISELY -> {
                            florisboard?.activeEditorInstance?.apply {
                                setSelection(
                                    if (selection.start < selection.end) { selection.start + 1 } else { selection.start },
                                    selection.end
                                )
                            }
                            shouldBlockNextKeyCode = true
                            true
                        }

                        SwipeAction.DELETE_WORDS_PRECISELY -> {
                            florisboard?.activeEditorInstance?.apply {
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
            KeyCode.SPACE -> when (type) {
                SwipeGesture.Type.TOUCH_MOVE -> when (direction) {
                    SwipeGesture.Direction.UP -> {
                        florisboard?.executeSwipeAction(prefs.gestures.spaceBarSwipeUp)
                        shouldBlockNextKeyCode = true
                        true
                    }
                    SwipeGesture.Direction.LEFT -> {
                        florisboard?.executeSwipeAction(prefs.gestures.spaceBarSwipeLeft)
                        shouldBlockNextKeyCode = true
                        true
                    }
                    SwipeGesture.Direction.RIGHT -> {
                        florisboard?.executeSwipeAction(prefs.gestures.spaceBarSwipeRight)
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
        desiredWidth = when (keyboardView.computedLayout?.mode) {
            KeyboardMode.NUMERIC,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> (keyboardView.desiredKeyWidth * 2.68f).toInt()
            KeyboardMode.NUMERIC_ADVANCED -> when (data.code) {
                44, 46 -> keyboardView.desiredKeyWidth
                KeyCode.VIEW_SYMBOLS, 61 -> (keyboardView.desiredKeyWidth * 1.34f).toInt()
                else -> (keyboardView.desiredKeyWidth * 1.56f).toInt()
            }
            else -> when (data.code) {
                KeyCode.SHIFT,
                KeyCode.VIEW_CHARACTERS,
                KeyCode.VIEW_SYMBOLS,
                KeyCode.VIEW_SYMBOLS2,
                KeyCode.DELETE,
                KeyCode.ENTER -> (keyboardView.desiredKeyWidth * 1.56f).toInt()
                else -> keyboardView.desiredKeyWidth
            }
        }
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
            KeyCode.CLIPBOARD_CUT -> {
                florisboard?.activeEditorInstance?.selection?.isSelectionMode == true &&
                        florisboard?.activeEditorInstance?.isRawInputEditor == false
            }
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

    /**
     * Updates the background depending on [isKeyPressed] and [data].
     */
    private fun updateKeyPressedBackground() {
        when {
            keyboardView.isSmartbarKeyboardView -> {
                elevation = 0.0f
                setBackgroundTintColor2(
                    this, when {
                        isKeyPressed && isEnabled -> prefs.theme.smartbarButtonBgColor
                        else -> prefs.theme.smartbarBgColor
                    }
                )
            }
            else -> {
                elevation = 4.0f
                when (data.code) {
                    KeyCode.ENTER -> {
                        setBackgroundTintColor2(
                            this, when {
                                isKeyPressed && isEnabled -> prefs.theme.keyEnterBgColorPressed
                                else -> prefs.theme.keyEnterBgColor
                            }
                        )
                    }
                    KeyCode.SHIFT -> {
                        setBackgroundTintColor2(
                            this, when {
                                isKeyPressed && isEnabled -> prefs.theme.keyShiftBgColorPressed
                                else -> prefs.theme.keyShiftBgColor
                            }
                        )
                    }
                    else -> {
                        setBackgroundTintColor2(
                            this, when {
                                isKeyPressed && isEnabled -> prefs.theme.keyBgColorPressed
                                else -> prefs.theme.keyBgColor
                            }
                        )
                    }
                }
            }
        }
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
            val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
            val keyMarginV = resources.getDimension((R.dimen.key_marginV)).toInt()

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
                visibility = if (florisboard?.shouldShowLanguageSwitch() == true) {
                    GONE
                } else {
                    VISIBLE
                }
            }
            KeyCode.LANGUAGE_SWITCH -> {
                visibility = if (florisboard?.shouldShowLanguageSwitch() == true) {
                    VISIBLE
                } else {
                    GONE
                }
            }
            else -> if (data.variation != KeyVariation.ALL) {
                val keyVariation = florisboard?.textInputManager?.keyVariation ?: KeyVariation.NORMAL
                visibility = if (data.variation == keyVariation) { VISIBLE } else { GONE }
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
     * Draw the key label / drawable.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        updateKeyPressedBackground()

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
                    drawableColor = prefs.theme.keyFgColor
                }
                KeyCode.ARROW_RIGHT -> {
                    drawable = getDrawable(context, R.drawable.ic_keyboard_arrow_right)
                    drawableColor = prefs.theme.keyFgColor
                }
                KeyCode.CLIPBOARD_COPY -> {
                    drawable = getDrawable(context, R.drawable.ic_content_copy)
                    drawableColor = prefs.theme.keyFgColor
                }
                KeyCode.CLIPBOARD_CUT -> {
                    drawable = getDrawable(context, R.drawable.ic_content_cut)
                    drawableColor = prefs.theme.keyFgColor
                }
                KeyCode.CLIPBOARD_PASTE -> {
                    drawable = getDrawable(context, R.drawable.ic_content_paste)
                    drawableColor = prefs.theme.keyFgColor
                }
                KeyCode.CLIPBOARD_SELECT_ALL -> {
                    drawable = getDrawable(context, R.drawable.ic_select_all)
                    drawableColor = prefs.theme.keyFgColor
                }
                KeyCode.DELETE -> {
                    drawable = getDrawable(context, R.drawable.ic_backspace)
                    drawableColor = prefs.theme.keyFgColor
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
                    drawableColor = prefs.theme.keyEnterFgColor
                    if (imeOptions.flagNoEnterAction) {
                        drawable = getDrawable(context, R.drawable.ic_keyboard_return)
                    }
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    drawable = getDrawable(context, R.drawable.ic_language)
                    drawableColor = prefs.theme.keyFgColor
                }
                KeyCode.PHONE_PAUSE -> label = resources.getString(R.string.key__phone_pause)
                KeyCode.PHONE_WAIT -> label = resources.getString(R.string.key__phone_wait)
                KeyCode.SHIFT -> {
                    drawable = getDrawable(context, when {
                        florisboard?.textInputManager?.caps ?: false && florisboard?.textInputManager?.capsLock ?: false -> {
                            drawableColor = prefs.theme.keyShiftFgColorCapsLock
                            R.drawable.ic_keyboard_capslock
                        }
                        florisboard?.textInputManager?.caps ?: false && !(florisboard?.textInputManager?.capsLock ?: false) -> {
                            drawableColor = prefs.theme.keyShiftFgColor
                            R.drawable.ic_keyboard_capslock
                        }
                        else -> {
                            drawableColor = prefs.theme.keyShiftFgColor
                            R.drawable.ic_keyboard_arrow_up
                        }
                    })
                }
                KeyCode.SPACE -> {
                    when (keyboardView.computedLayout?.mode) {
                        KeyboardMode.NUMERIC,
                        KeyboardMode.NUMERIC_ADVANCED,
                        KeyboardMode.PHONE,
                        KeyboardMode.PHONE2 -> {
                            drawable = getDrawable(context, R.drawable.ic_space_bar)
                            drawableColor = prefs.theme.keyFgColor
                        }
                        KeyboardMode.CHARACTERS -> {
                            label = florisboard?.activeSubtype?.locale?.displayName
                        }
                        else -> {}
                    }
                }
                KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                    drawable = getDrawable(context, R.drawable.ic_sentiment_satisfied)
                    drawableColor = prefs.theme.keyFgColor
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

        // Draw drawable
        val drawable = drawable
        if (drawable != null) {
            if (keyboardView.isSmartbarKeyboardView && !isEnabled) {
                drawableColor = prefs.theme.smartbarFgColorAlt
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
            drawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                drawableColor,
                BlendModeCompat.SRC_ATOP
            )
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
                            desiredWidth - (2.6f * drawablePaddingH),
                            desiredHeight - (3.4f * drawablePaddingV),
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
                        keyboardView.popupManager.keyPopupTextSize = cachedTextSize
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
            labelPaint.color = prefs.theme.keyFgColor
            labelPaint.alpha = if (keyboardView.computedLayout?.mode == KeyboardMode.CHARACTERS &&
                data.code == KeyCode.SPACE) { 120 } else { 255 }
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
                desiredWidth * 1.0f / 6.0f,
                desiredHeight * 1.0f / 6.0f,
                // Note: taking a "X" here because it is one of the biggest letters and
                //  the keys must have the same base character for calculation, else
                //  they will all look different and weird...
                "X"
            )
            hintedLabelPaint.color = prefs.theme.keyFgColor
            hintedLabelPaint.alpha = 120
            val centerX = measuredWidth * 5.0f / 6.0f
            val centerY = measuredHeight * 1.0f / 6.0f + (hintedLabelPaint.textSize - hintedLabelPaint.descent()) / 2
            canvas.drawText(hintedLabel, centerX, centerY, hintedLabelPaint)
        }
    }

    /**
     * Custom Outline Provider, needed for the [KeyView] elevation rendering.
     */
    private class KeyViewOutline(
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
