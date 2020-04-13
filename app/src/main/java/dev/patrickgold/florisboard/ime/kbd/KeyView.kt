package dev.patrickgold.florisboard.ime.kbd

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat.getDrawable
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.KeyCodes
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.CustomKeyboard
import dev.patrickgold.florisboard.util.*
import java.util.*

class KeyView : androidx.appcompat.widget.AppCompatButton, View.OnTouchListener {

    private var isKeyPressed: Boolean = false
        set(v) {
            if (data.code == KeyCodes.ENTER) {
                setBackgroundTintColor(this, when {
                    v -> R.attr.app_colorPrimaryDark
                    else -> R.attr.app_colorPrimary
                })
            } else {
                setBackgroundTintColor(this, when {
                    v -> R.attr.key_bgColorPressed
                    else -> R.attr.key_bgColor
                })
            }
            field = v
        }
    private val osHandler = Handler()
    private var osTimer: Timer? = null
    var data: KeyData = KeyData(0)
        set(v) { field = v; updateUI() }
    var keyboard: CustomKeyboard? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.customKeyStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs) {
        super.setOnTouchListener(this)
    }

    private fun updateUI() {
        isKeyPressed = isKeyPressed
        super.setText(createLabelText())
        var drawable: Drawable? = null
        when (data.code) {
            KeyCode.DELETE -> {
                (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
                drawable = getDrawable(context,
                    R.drawable.key_ic_backspace
                )
            }
            KeyCode.ENTER -> {
                (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
                setDrawableTintColor(this, R.attr.key_bgColor)
                val action = keyboard?.inputMethodService?.currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_NULL
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
                (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
                drawable = getDrawable(context, when {
                    (keyboard?.caps ?: false) && (keyboard?.capsLock ?: false) -> {
                        setDrawableTintColor(this, R.attr.app_colorAccentDark)
                        R.drawable.key_ic_capslock
                    }
                    (keyboard?.caps ?: false) && !(keyboard?.capsLock ?: false) -> {
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
                (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 5.0f
            }
            KeyCode.VIEW_SYMOBLS -> {
                (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
                super.setText("?123")
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
        requestLayout()
        invalidate()
    }

    /**
     * Creates a label text from the key's code.
     */
    fun createLabelText(): String {
        if (data.type != KeyType.CHARACTER) {
            return ""
        }
        val label = (data.code.toChar()).toString()
        return when {
            keyboard?.caps ?: false -> label.toUpperCase(Locale.getDefault())
            else -> label
        }
    }

    /**
     * Creates a label text from the key's code.
     */
    fun createPopupKeyText(popupCode: Int): String {
        val label = popupCode.toChar().toString()
        return when {
            keyboard?.caps ?: false -> label.toUpperCase(Locale.getDefault())
            else -> label
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            isKeyPressed = true
            keyboard?.popupManager?.show(this)
            if (data.code == KeyCode.DELETE && data.type == KeyType.MODIFIER) {
                osTimer = Timer()
                osTimer?.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        keyboard?.onKeyClicked(data.code)
                        if (!isKeyPressed) {
                            osTimer?.cancel()
                            osTimer = null
                        }
                    }
                }, 500, 50)
            }
            osHandler.postDelayed({
                if (data.popup.isNotEmpty()) {
                    keyboard?.popupManager?.extend(this)
                }
            }, 300)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            isKeyPressed = false
            osHandler.removeCallbacksAndMessages(null)
            osTimer?.cancel()
            osTimer = null
            keyboard?.popupManager?.hide(this)
            keyboard?.onKeyClicked(data.code)
        }
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        val keyboardWidth = keyboard?.measuredWidth ?: 0
        val keyboardRowMarginH = resources.getDimension((R.dimen.keyboard_row_marginH)).toInt()
        val keyWidthPlusMargin = (keyboardWidth - 2 * keyboardRowMarginH) / 10
        val keyWidth = keyWidthPlusMargin - 2 * keyMarginH
        layoutParams = layoutParams.apply {
            width = keyWidth
        }
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        updateUI()
        super.onDraw(canvas)
    }
}
