package dev.patrickgold.florisboard.ime.key

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat.getDrawable
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import dev.patrickgold.florisboard.KeyCodes
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.KeyboardRowView
import dev.patrickgold.florisboard.ime.keyboard.KeyboardView
import dev.patrickgold.florisboard.util.*
import java.util.*

class KeyView(
    context: Context, val data: KeyData, val keyboardView: KeyboardView
) : androidx.appcompat.widget.AppCompatButton(
    context, null, R.attr.customKeyStyle
), View.OnTouchListener {

    private var isKeyPressed: Boolean = false
    private val osHandler = Handler()
    private var osTimer: Timer? = null

    init {
        super.setOnTouchListener(this)
    }

    /**
     * Creates a label text from the key's code.
     */
    fun getComputedLetter(code: Int = data.code): String {
        val label = (code.toChar()).toString()
        return when {
            keyboardView.caps -> label.toUpperCase(Locale.getDefault())
            else -> label
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            isKeyPressed = true
            keyboardView.popupManager.show(this)
            if (data.code == KeyCode.DELETE && data.type == KeyType.ENTER_EDITING) {
                osTimer = Timer()
                osTimer?.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        keyboardView.onKeyClicked(data.code)
                        if (!isKeyPressed) {
                            osTimer?.cancel()
                            osTimer = null
                        }
                    }
                }, 500, 50)
            }
            osHandler.postDelayed({
                if (data.popup.isNotEmpty()) {
                    keyboardView.popupManager.extend(this)
                }
            }, 300)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            isKeyPressed = false
            osHandler.removeCallbacksAndMessages(null)
            osTimer?.cancel()
            osTimer = null
            keyboardView.popupManager.hide(this)
            keyboardView.onKeyClicked(data.code)
        }
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val flexLayoutParams = layoutParams as FlexboxLayout.LayoutParams

        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        val keyboardRowWidth = (parent as KeyboardRowView).measuredWidth
        val keyWidth = (keyboardRowWidth / 10) - (2 * keyMarginH)

        layoutParams = flexLayoutParams.apply {
            width = keyWidth
            height = resources.getDimension(R.dimen.key_height).toInt()
            marginStart = keyMarginH
            marginEnd = keyMarginH
            flexGrow = when (data.code) {
                KeyCode.DELETE -> 1.0f
                KeyCode.ENTER -> 1.0f
                KeyCode.SHIFT -> 1.0f
                KeyCode.SPACE -> 5.0f
                KeyCode.VIEW_SYMOBLS -> 1.0f
                else -> 0.0f
            }
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
                    val action = keyboardView.inputMethodService?.currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_NULL
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
                        keyboardView.caps && keyboardView.capsLock -> {
                            setDrawableTintColor(this, R.attr.app_colorAccentDark)
                            R.drawable.key_ic_capslock
                        }
                        keyboardView.caps && !keyboardView.capsLock -> {
                            setDrawableTintColor(this, R.attr.key_fgColor)
                            R.drawable.key_ic_capslock
                        }
                        else -> {
                            setDrawableTintColor(this, R.attr.key_fgColor)
                            R.drawable.key_ic_caps
                        }
                    })
                }
                KeyCode.VIEW_SYMOBLS -> {
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
        }

        if (data.code == KeyCodes.ENTER) {
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
