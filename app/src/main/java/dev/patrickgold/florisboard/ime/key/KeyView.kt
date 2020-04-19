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
import androidx.core.content.ContextCompat.getDrawable
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.keyboard.KeyboardRowView
import dev.patrickgold.florisboard.ime.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.popup.KeyPopupManager
import dev.patrickgold.florisboard.util.*
import java.util.*

@SuppressLint("ViewConstructor")
class KeyView(
    context: Context, val data: KeyData, private val florisboard: FlorisBoard, private val keyboardView: KeyboardView
) : androidx.appcompat.widget.AppCompatButton(
    context, null, R.attr.keyViewStyle
), View.OnTouchListener {

    private var isKeyPressed: Boolean = false
    private val osHandler = Handler()
    private var osTimer: Timer? = null
    private val popupManager = KeyPopupManager(keyboardView, this)

    init {
        super.setOnTouchListener(this)
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

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isKeyPressed = true
                popupManager.show()
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
                // TODO: Fetch delayMillis from preferences
                osHandler.postDelayed({
                    if (data.popup.isNotEmpty()) {
                        popupManager.extend()
                    }
                }, 300)
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val flexLayoutParams = layoutParams as FlexboxLayout.LayoutParams

        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        val keyboardRowWidth = (parent as KeyboardRowView).measuredWidth
        val keyWidth = (keyboardRowWidth / 10) - (2 * keyMarginH)

        layoutParams = flexLayoutParams.apply {
            width = keyWidth
            height = FlexboxLayout.LayoutParams.MATCH_PARENT
            marginStart = keyMarginH
            marginEnd = keyMarginH
            flexGrow = when (data.code) {
                KeyCode.DELETE -> 1.0f
                KeyCode.ENTER -> 1.0f
                KeyCode.SHIFT -> 1.0f
                KeyCode.SPACE -> 6.0f
                KeyCode.VIEW_CHARACTERS -> 1.0f
                KeyCode.VIEW_NUMERIC -> 0.0f
                KeyCode.VIEW_SYMBOLS -> 1.0f
                KeyCode.VIEW_SYMBOLS2 -> 1.0f
                else -> 0.0f
            }
        }

        if (data.code == KeyCode.VIEW_NUMERIC) {
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX, resources.getDimension(
                    R.dimen.key_numeric_textSize
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (data.type == KeyType.CHARACTER) {
            super.setText(getComputedLetter())
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
                    super.setText(R.string.key__view_characters)
                }
                KeyCode.VIEW_NUMERIC -> {
                    super.setText(R.string.key__view_numeric)
                }
                KeyCode.VIEW_SYMBOLS -> {
                    super.setText(R.string.key__view_symbols)
                }
                KeyCode.VIEW_SYMBOLS2 -> {
                    super.setText(R.string.key__view_symbols2)
                }
            }
            if (drawable != null) {
                if (measuredWidth > measuredHeight) {
                    drawable.setBounds(0, 0, measuredHeight, measuredHeight)
                    super.setCompoundDrawables(null, drawable, null, null)
                } else {
                    drawable.setBounds(0, 0, measuredWidth, measuredWidth)
                    super.setCompoundDrawables(drawable, null, null, null)
                }
            }
        }

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
}
