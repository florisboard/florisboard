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
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.util.getColorFromAttr
import dev.patrickgold.florisboard.util.setBackgroundTintColor
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
    val data: KeyData
) : View(keyboardView.context) {

    private var isKeyPressed: Boolean = false
        set(value) {
            field = value
            updateKeyPressedBackground()
        }
    private var osHandler: Handler? = null
    private var osTimer: Timer? = null
    private var shouldBlockNextKeyCode: Boolean = false

    private var drawable: Drawable? = null
    private var drawableColor: Int = 0
    private var drawablePadding: Int = 0
    private var label: String? = null
    private var labelPaint: Paint = Paint().apply {
        alpha = 255
        color = 0
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.key_textSize)
        typeface = Typeface.DEFAULT
    }

    var florisboard: FlorisBoard? = null
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
        event ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                keyboardView.popupManager.show(this)
                isKeyPressed = true
                florisboard?.keyPressVibrate()
                florisboard?.keyPressSound(data)
                if (data.code == KeyCode.DELETE && data.type == KeyType.ENTER_EDITING) {
                    osTimer = Timer()
                    osTimer?.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            florisboard?.textInputManager?.sendKeyPress(data)
                            if (!isKeyPressed) {
                                osTimer?.cancel()
                                osTimer = null
                            }
                        }
                    }, 500, 50)
                }
                val delayMillis = keyboardView.prefs.looknfeel.longPressDelay
                if (osHandler == null) {
                    osHandler = Handler()
                }
                osHandler?.postDelayed({
                    if (data.popup.isNotEmpty()) {
                        keyboardView.popupManager.extend(this)
                    }
                    if (data.code == KeyCode.SPACE) {
                        florisboard?.textInputManager?.sendKeyPress(
                            KeyData(
                                KeyCode.SHOW_INPUT_METHOD_PICKER,
                                type = KeyType.FUNCTION
                            )
                        )
                        shouldBlockNextKeyCode = true
                    }
                }, delayMillis.toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (keyboardView.popupManager.isShowingExtendedPopup) {
                    val isPointerWithinBounds =
                        keyboardView.popupManager.propagateMotionEvent(this, event)
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
                osHandler?.removeCallbacksAndMessages(null)
                osTimer?.cancel()
                osTimer = null
                val retData = keyboardView.popupManager.getActiveKeyData(this)
                keyboardView.popupManager.hide()
                if (event.actionMasked != MotionEvent.ACTION_CANCEL && !shouldBlockNextKeyCode && retData != null) {
                    florisboard?.textInputManager?.sendKeyPress(retData)
                    performClick()
                } else {
                    shouldBlockNextKeyCode = false
                }
            }
            else -> return false
        }
        return true
    }

    /**
     * Solution base from this great StackOverflow answer which explained and helped a lot
     * for handling onMeasure():
     *  https://stackoverflow.com/a/12267248/6801193
     *  by Devunwired
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = when (keyboardView.computedLayout?.mode) {
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
        val desiredHeight = keyboardView.desiredKeyHeight

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

        drawablePadding = (0.2f * height).toInt()

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
     * Updates the background depending on [isKeyPressed] and [data].
     */
    private fun updateKeyPressedBackground() {
        if (data.code == KeyCode.ENTER) {
            setBackgroundTintColor(
                this, when {
                    isKeyPressed -> R.attr.colorPrimaryDark
                    else -> R.attr.colorPrimary
                }
            )
        } else {
            setBackgroundTintColor(
                this, when {
                    isKeyPressed -> R.attr.key_bgColorPressed
                    else -> R.attr.key_bgColor
                }
            )
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
                visibility =
                    if (data.variation == KeyVariation.NORMAL && (keyVariation == KeyVariation.NORMAL
                                || keyVariation == KeyVariation.PASSWORD)
                    ) {
                        VISIBLE
                    } else if (data.variation == keyVariation) {
                        VISIBLE
                    } else {
                        GONE
                    }
                updateTouchHitBox()
            }
        }
    }

    /**
     * Draw the key label / drawable.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        updateKeyPressedBackground()

        if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE
            || data.type == KeyType.NUMERIC
        ) {
            label = getComputedLetter()
        } else {
            when (data.code) {
                KeyCode.DELETE -> {
                    drawable = getDrawable(context, R.drawable.ic_backspace)
                    drawableColor = getColorFromAttr(context, R.attr.key_fgColor)
                }
                KeyCode.ENTER -> {
                    val action = florisboard?.currentInputEditorInfo?.imeOptions ?: 0
                    drawable = getDrawable(context, when (action and EditorInfo.IME_MASK_ACTION) {
                        EditorInfo.IME_ACTION_DONE -> R.drawable.ic_done
                        EditorInfo.IME_ACTION_GO -> R.drawable.ic_arrow_right_alt
                        EditorInfo.IME_ACTION_NEXT -> R.drawable.ic_arrow_right_alt
                        EditorInfo.IME_ACTION_NONE -> R.drawable.ic_keyboard_return
                        EditorInfo.IME_ACTION_PREVIOUS -> R.drawable.ic_arrow_right_alt
                        EditorInfo.IME_ACTION_SEARCH -> R.drawable.ic_search
                        EditorInfo.IME_ACTION_SEND -> R.drawable.ic_send
                        else -> R.drawable.ic_arrow_right_alt
                    })
                    drawableColor = getColorFromAttr(context, R.attr.key_enter_fgColor)
                    if (action and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0) {
                        drawable = getDrawable(context, R.drawable.ic_keyboard_return)
                    }
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    drawable = getDrawable(context, R.drawable.ic_language)
                    drawableColor = getColorFromAttr(context, R.attr.key_fgColor)
                }
                KeyCode.PHONE_PAUSE -> label = resources.getString(R.string.key__phone_pause)
                KeyCode.PHONE_WAIT -> label = resources.getString(R.string.key__phone_wait)
                KeyCode.SHIFT -> {
                    drawable = getDrawable(context, when {
                        florisboard?.textInputManager?.caps ?: false && florisboard?.textInputManager?.capsLock ?: false -> {
                            drawableColor = getColorFromAttr(context, R.attr.colorAccent)
                            R.drawable.ic_keyboard_capslock
                        }
                        florisboard?.textInputManager?.caps ?: false && !(florisboard?.textInputManager?.capsLock ?: false) -> {
                            drawableColor = getColorFromAttr(context, R.attr.key_fgColor)
                            R.drawable.ic_keyboard_capslock
                        }
                        else -> {
                            drawableColor = getColorFromAttr(context, R.attr.key_fgColor)
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
                            drawableColor = getColorFromAttr(context, R.attr.key_fgColor)
                        }
                        KeyboardMode.CHARACTERS -> {
                            label = florisboard?.activeSubtype?.locale?.displayName
                        }
                        else -> {}
                    }
                }
                KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                    drawable = getDrawable(context, R.drawable.ic_sentiment_satisfied)
                    drawableColor = getColorFromAttr(context, R.attr.key_fgColor)
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
            }
        }

        // Draw drawable
        val drawable = drawable
        if (drawable != null) {
            var marginV = 0
            var marginH = 0
            if (measuredWidth > measuredHeight) {
                marginH = (measuredWidth - measuredHeight) / 2
            } else {
                marginV = (measuredHeight - measuredWidth) / 2
            }
            drawable.setBounds(
                marginH + drawablePadding,
                marginV + drawablePadding,
                measuredWidth - marginH - drawablePadding,
                measuredHeight - marginV - drawablePadding)
            drawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                drawableColor,
                BlendModeCompat.SRC_ATOP
            )
            drawable.draw(canvas)
        }

        // Draw label
        val label = label
        if (label != null) {
            if (data.code == KeyCode.VIEW_NUMERIC || data.code == KeyCode.VIEW_NUMERIC_ADVANCED
                || data.code == KeyCode.SPACE) {
                labelPaint.textSize = resources.getDimension(R.dimen.key_numeric_textSize)
            } else {
                labelPaint.textSize = resources.getDimension(R.dimen.key_textSize)
            }
            labelPaint.color = getColorFromAttr(context, R.attr.key_fgColor)
            labelPaint.alpha = if (keyboardView.computedLayout?.mode == KeyboardMode.CHARACTERS &&
                data.code == KeyCode.SPACE) { 120 } else { 255 }
            val isPortrait =
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            if (keyboardView.prefs.looknfeel.oneHandedMode != "off" && isPortrait) {
                labelPaint.textSize *= 0.9f
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
