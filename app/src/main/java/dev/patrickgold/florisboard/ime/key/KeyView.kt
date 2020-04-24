package dev.patrickgold.florisboard.ime.key

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat.getDrawable
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.popup.KeyPopupManager
import dev.patrickgold.florisboard.util.setBackgroundTintColor
import dev.patrickgold.florisboard.util.setDrawableTintColor
import java.util.*

@SuppressLint("ViewConstructor")
class KeyView(
    context: Context, val data: KeyData, private val florisboard: FlorisBoard, private val keyboardView: KeyboardView
) : AppCompatButton(
    context, null, R.attr.keyViewStyle
), View.OnTouchListener {

    private var isKeyPressed: Boolean = false
        set(value) {
            field = value
            updateKeyPressedBackground()
        }
    private val osHandler = Handler()
    private var osTimer: Timer? = null
    private val popupManager = KeyPopupManager(keyboardView, this)

    init {
        super.setOnTouchListener(this)

        val flexLayoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams = flexLayoutParams.apply {
            marginStart = resources.getDimension((R.dimen.key_marginH)).toInt()
            marginEnd = resources.getDimension((R.dimen.key_marginH)).toInt()
            flexShrink = when (data.code) {
                KeyCode.SHIFT,
                KeyCode.VIEW_CHARACTERS,
                KeyCode.VIEW_SYMBOLS,
                KeyCode.VIEW_SYMBOLS2,
                KeyCode.DELETE,
                KeyCode.ENTER -> 0.0f
                else -> 1.0f
            }
            flexGrow = when (data.code) {
                KeyCode.SPACE -> 1.0f
                else -> 0.0f
            }
        }
        setPadding(0, 0, 0, 0)

        if (data.code == KeyCode.VIEW_NUMERIC) {
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
    fun getComputedLetter(code: Int = data.code): String {
        val label = (code.toChar()).toString()
        return when {
            florisboard.caps -> label.toUpperCase(Locale.getDefault())
            else -> label
        }
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

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                popupManager.show()
                isKeyPressed = true
                if (data.code == KeyCode.DELETE && data.type == KeyType.ENTER_EDITING) {
                    osTimer = Timer()
                    osTimer?.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            florisboard.sendKeyPress(data)
                            if (!isKeyPressed) {
                                osTimer?.cancel()
                                osTimer = null
                            }
                        }
                    }, 500, 50)
                }
                val delayMillis = florisboard.prefs!!.getInt("keyboard__long_press_delay", 300)
                osHandler.postDelayed({
                    if (data.popup.isNotEmpty()) {
                        popupManager.extend()
                    }
                }, delayMillis.toLong())
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isKeyPressed = false
                osHandler.removeCallbacksAndMessages(null)
                osTimer?.cancel()
                osTimer = null
                val retData = popupManager.getActiveKeyData()
                popupManager.hide()
                florisboard.sendKeyPress(retData)
            }
            MotionEvent.ACTION_MOVE -> {
                // TODO: Add cancel event if pointer moves to far from key and popup window
                if (popupManager.isShowingExtendedPopup) {
                    popupManager.propagateMotionEvent(event)
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
        val desiredWidth = when (data.code) {
            KeyCode.SHIFT,
            KeyCode.VIEW_CHARACTERS,
            KeyCode.VIEW_SYMBOLS,
            KeyCode.VIEW_SYMBOLS2,
            KeyCode.DELETE,
            KeyCode.ENTER -> (keyboardView.desiredKeyWidth * 1.56f).toInt()
            else -> keyboardView.desiredKeyWidth
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

    override fun onDraw(canvas: Canvas?) {
        if (data.type == KeyType.CHARACTER) {
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
                KeyCode.SHIFT -> {
                    drawable = getDrawable(context, when {
                        florisboard.caps && florisboard.capsLock -> {
                            setDrawableTintColor(this, R.attr.app_colorAccentDark)
                            R.drawable.key_ic_capslock
                        }
                        florisboard.caps && !florisboard.capsLock -> {
                            setDrawableTintColor(this, R.attr.key_fgColor)
                            R.drawable.key_ic_capslock
                        }
                        else -> {
                            setDrawableTintColor(this, R.attr.key_fgColor)
                            R.drawable.key_ic_caps
                        }
                    })
                }
                KeyCode.VIEW_CHARACTERS -> {
                    setText(R.string.key__view_characters)
                }
                KeyCode.VIEW_NUMERIC -> {
                    setText(R.string.key__view_numeric)
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

        super.onDraw(canvas)
    }
}
