package dev.patrickgold.florisboard.ime.text.key

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat.getDrawable
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.popup.KeyPopupManager
import dev.patrickgold.florisboard.util.setBackgroundTintColor
import dev.patrickgold.florisboard.util.setDrawableTintColor
import java.util.*

@SuppressLint("ViewConstructor")
class KeyView(
    private val florisboard: FlorisBoard, private val keyboardView: KeyboardView, val data: KeyData
) : AppCompatButton(
    florisboard.context, null, R.attr.keyViewStyle
) {
    private var isKeyPressed: Boolean = false
        set(value) {
            field = value
            updateKeyPressedBackground()
        }
    private val osHandler = Handler()
    private var osTimer: Timer? = null
    private val popupManager = KeyPopupManager(keyboardView, this)
    private var shouldBlockNextKeyCode: Boolean = false

    init {
        val flexLayoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams = flexLayoutParams.apply {
            marginStart = resources.getDimension((R.dimen.key_marginH)).toInt()
            marginEnd = resources.getDimension((R.dimen.key_marginH)).toInt()
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

        if (data.code == KeyCode.VIEW_NUMERIC || data.code == KeyCode.VIEW_NUMERIC_ADVANCED) {
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX, resources.getDimension(
                    R.dimen.key_numeric_textSize
                )
            )
        }

        updateKeyPressedBackground()
    }

    /**
     * Creates a label text from the key's code.
     */
    fun getComputedLetter(keyData: KeyData = data): String {
        if (keyData.code == KeyCode.URI_COMPONENT_TLD) {
            return when (florisboard.textInputManager.caps) {
                true -> keyData.label.toUpperCase(Locale.getDefault())
                false -> keyData.label.toLowerCase(Locale.getDefault())
            }
        }
        val label = (keyData.code.toChar()).toString()
        return when {
            florisboard.textInputManager.caps -> label.toUpperCase(Locale.getDefault())
            else -> label
        }
    }

    private fun keyPressVibrate() {
        if (florisboard.prefs!!.vibrationEnabled) {
            var vibrationStrength = florisboard.prefs!!.vibrationStrength
            if (vibrationStrength == 0 && florisboard.prefs!!.vibrationEnabledSystem) {
                vibrationStrength = 36
            }
            if (vibrationStrength > 0) {
                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(
                        vibrationStrength.toLong(), VibrationEffect.DEFAULT_AMPLITUDE
                    ))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(vibrationStrength.toLong())
                }
            }
        }
    }

    private fun keyPressSound() {
        if (florisboard.prefs!!.soundEnabled) {
            val soundVolume = florisboard.prefs!!.soundVolume
            val effect = when (data.code) {
                KeyCode.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                KeyCode.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                KeyCode.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            if (soundVolume == 0 && florisboard.prefs!!.soundEnabledSystem) {
                florisboard.audioManager!!.playSoundEffect(effect)
            } else if (soundVolume > 0) {
                florisboard.audioManager!!.playSoundEffect(effect, soundVolume / 100f)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("NAME_SHADOWING")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val event = event ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                popupManager.show()
                isKeyPressed = true
                keyPressVibrate()
                keyPressSound()
                if (data.code == KeyCode.DELETE && data.type == KeyType.ENTER_EDITING) {
                    osTimer = Timer()
                    osTimer?.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            florisboard.textInputManager.sendKeyPress(data)
                            if (!isKeyPressed) {
                                osTimer?.cancel()
                                osTimer = null
                            }
                        }
                    }, 500, 50)
                }
                val delayMillis = florisboard.prefs!!.longPressDelay
                osHandler.postDelayed({
                    if (data.popup.isNotEmpty()) {
                        popupManager.extend()
                    }
                    if (data.code == KeyCode.SPACE) {
                        florisboard.textInputManager.sendKeyPress(KeyData(KeyCode.SHOW_INPUT_METHOD_PICKER, type = KeyType.FUNCTION))
                        shouldBlockNextKeyCode = true
                    }
                }, delayMillis.toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (popupManager.isShowingExtendedPopup) {
                    val isPointerWithinBounds = popupManager.propagateMotionEvent(event)
                    if (!isPointerWithinBounds && !shouldBlockNextKeyCode) {
                        keyboardView.shouldStealMotionEvents = true
                    }
                } else {
                    if (event.x < -0.1f * measuredWidth || event.x > 1.1f * measuredWidth
                        || event.y < -0.35f * measuredHeight || event.y > 1.35f * measuredHeight) {
                        if (!shouldBlockNextKeyCode) {
                            keyboardView.shouldStealMotionEvents = true
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isKeyPressed = false
                osHandler.removeCallbacksAndMessages(null)
                osTimer?.cancel()
                osTimer = null
                val retData = popupManager.getActiveKeyData()
                popupManager.hide()
                if (event.actionMasked != MotionEvent.ACTION_CANCEL && !shouldBlockNextKeyCode) {
                    florisboard.textInputManager.sendKeyPress(retData)
                } else {
                    shouldBlockNextKeyCode = false
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    performClick()
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
            KeyboardMode.PHONE2 -> keyboardView.desiredKeyWidth
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

        // MUST CALL THIS
        setMeasuredDimension(width, height)
    }

    private fun updateKeyPressedBackground() {
        if (data.code == KeyCode.ENTER) {
            setBackgroundTintColor(this, when {
                isKeyPressed -> R.attr.app_colorPrimaryDark
                else -> R.attr.app_colorPrimary
            })
        } else {
            setBackgroundTintColor(this, when {
                isKeyPressed -> R.attr.key_bgColorPressed
                else -> R.attr.key_bgColor
            })
        }
    }

    fun updateVariation() {
        if (data.variation != KeyVariation.ALL) {
            val keyVariation = florisboard.textInputManager.keyVariation
            visibility =
                if (data.variation == KeyVariation.NORMAL && (keyVariation == KeyVariation.NORMAL || keyVariation == KeyVariation.PASSWORD)) {
                    View.VISIBLE
                } else if (data.variation == keyVariation) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE
            || data.type == KeyType.NUMERIC) {
            text = getComputedLetter()
        } else {
            var drawable: Drawable? = null
            when (data.code) {
                KeyCode.DELETE -> {
                    drawable = getDrawable(context,
                        R.drawable.key_ic_backspace
                    )
                }
                KeyCode.ENTER -> {
                    setDrawableTintColor(this, R.attr.key_bgColor)
                    val action = florisboard.currentInputEditorInfo.imeOptions
                    drawable = getDrawable(context, when (action and EditorInfo.IME_MASK_ACTION) {
                        EditorInfo.IME_ACTION_DONE -> R.drawable.key_ic_action_done
                        EditorInfo.IME_ACTION_GO -> R.drawable.key_ic_action_go
                        EditorInfo.IME_ACTION_NEXT -> R.drawable.key_ic_action_next
                        EditorInfo.IME_ACTION_NONE -> R.drawable.key_ic_action_none
                        EditorInfo.IME_ACTION_PREVIOUS -> R.drawable.key_ic_action_previous
                        EditorInfo.IME_ACTION_SEARCH -> R.drawable.key_ic_action_search
                        EditorInfo.IME_ACTION_SEND -> R.drawable.key_ic_action_send
                        else -> R.drawable.key_ic_action_next
                    })
                    if (action and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0) {
                        drawable = getDrawable(context,
                            R.drawable.key_ic_action_none
                        )
                    }
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    drawable = getDrawable(context,
                        R.drawable.key_ic_language
                    )
                }
                KeyCode.PHONE_PAUSE -> setText(R.string.key__phone_pause)
                KeyCode.PHONE_WAIT -> setText(R.string.key__phone_wait)
                KeyCode.SHIFT -> {
                    drawable = getDrawable(context, when {
                        florisboard.textInputManager.caps && florisboard.textInputManager.capsLock -> {
                            setDrawableTintColor(this, R.attr.app_colorAccentDark)
                            R.drawable.key_ic_capslock
                        }
                        florisboard.textInputManager.caps && !florisboard.textInputManager.capsLock -> {
                            setDrawableTintColor(this, R.attr.key_fgColor)
                            R.drawable.key_ic_capslock
                        }
                        else -> {
                            setDrawableTintColor(this, R.attr.key_fgColor)
                            R.drawable.key_ic_caps
                        }
                    })
                }
                KeyCode.SPACE -> {
                    when (keyboardView.computedLayout?.mode) {
                        KeyboardMode.NUMERIC,
                        KeyboardMode.NUMERIC_ADVANCED,
                        KeyboardMode.PHONE,
                        KeyboardMode.PHONE2 -> {
                            drawable = getDrawable(context,
                                R.drawable.key_ic_space_bar
                            )
                        }
                        else -> {}
                    }
                }
                KeyCode.VIEW_CHARACTERS -> {
                    setText(R.string.key__view_characters)
                }
                KeyCode.VIEW_NUMERIC,
                KeyCode.VIEW_NUMERIC_ADVANCED -> {
                    setText(R.string.key__view_numeric)
                }
                KeyCode.VIEW_PHONE -> {
                    setText(R.string.key__view_phone)
                }
                KeyCode.VIEW_PHONE2 -> {
                    setText(R.string.key__view_phone2)
                }
                KeyCode.VIEW_SYMBOLS -> {
                    setText(R.string.key__view_symbols)
                }
                KeyCode.VIEW_SYMBOLS2 -> {
                    setText(R.string.key__view_symbols2)
                }
            }
            if (drawable != null) {
                if (measuredWidth > measuredHeight) {
                    drawable.setBounds(0, 0, measuredHeight, measuredHeight)
                    setCompoundDrawables(null, drawable, null, null)
                } else {
                    drawable.setBounds(0, 0, measuredWidth, measuredWidth)
                    setCompoundDrawables(drawable, null, null, null)
                }
            }
        }
    }
}
