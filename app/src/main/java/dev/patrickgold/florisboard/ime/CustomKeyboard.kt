package dev.patrickgold.florisboard.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.google.android.flexbox.FlexboxLayout
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.kbd.KeyCode
import dev.patrickgold.florisboard.ime.layout.LayoutData

class CustomKeyboard : LinearLayout {

    private var hasCapsRecentlyChanged: Boolean = false
    private val osHandler = Handler()

    var caps: Boolean = false
    var capsLock: Boolean = false
    var layoutName: String? = null
    var inputMethodService: InputMethodService? = null
    val popupManager = KeyPopupManager(this)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.customKeyboardStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs)

    private fun buildLayout() {
        this.destroyLayout()
        val context = ContextThemeWrapper(context,
            R.style.KeyboardTheme_MaterialLight
        )
        this.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        val jsonRaw = resources.openRawResource(R.raw.kbd_qwerty)
            .bufferedReader().use { it.readText() }
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val layoutAdapter = moshi.adapter(LayoutData::class.java)
        val layout = layoutAdapter.fromJson(jsonRaw)
        if (layout != null) {
            for (row in layout.arrangement) {
                val rowView =
                    CustomKeyboardRow(context)
                val rowViewLP = FlexboxLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                rowView.layoutParams = rowViewLP
                rowView.setPadding(
                    resources.getDimension(R.dimen.keyboard_row_marginH).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginV).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginH).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginV).toInt()
                )
                for (key in row) {
                    val keyView =
                        CustomKey(context)
                    val keyViewLP = FlexboxLayout.LayoutParams(
                        resources.getDimension(R.dimen.key_width).toInt(),
                        resources.getDimension(R.dimen.key_height).toInt()
                    )
                    keyViewLP.setMargins(
                        resources.getDimension(R.dimen.key_marginH).toInt(), 0,
                        resources.getDimension(R.dimen.key_marginH).toInt(), 0
                    )
                    keyView.layoutParams = keyViewLP
                    keyView.code = key.code
                    keyView.keyboard = this
                    rowView.addView(keyView)
                }
                this.addView(rowView)
            }
        }
    }

    private fun destroyLayout() {
        this.removeAllViews()
    }

    fun setLayout(name: String): Boolean {
        this.layoutName = name
        this.buildLayout()
        return true
    }

    fun onKeyClicked(code: Int) {
        val ic = inputMethodService?.currentInputConnection ?: return
        when (code) {
            KeyCode.DELETE -> ic.deleteSurroundingText(1, 0)
            KeyCode.ENTER -> {
                val action = inputMethodService?.currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_NULL
                Log.d("imeOptions", action.toString())
                Log.d("imeOptions action only", (action and 0xFF).toString())
                if (action and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0) {
                    ic.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_ENTER
                        )
                    )
                } else {
                    when (action and EditorInfo.IME_MASK_ACTION) {
                        EditorInfo.IME_ACTION_DONE,
                        EditorInfo.IME_ACTION_GO,
                        EditorInfo.IME_ACTION_NEXT,
                        EditorInfo.IME_ACTION_PREVIOUS,
                        EditorInfo.IME_ACTION_SEARCH,
                        EditorInfo.IME_ACTION_SEND -> {
                            ic.performEditorAction(action)
                        }
                        else -> {
                            ic.sendKeyEvent(
                                KeyEvent(
                                    KeyEvent.ACTION_DOWN,
                                    KeyEvent.KEYCODE_ENTER
                                )
                            )
                        }
                    }
                }
            }
            KeyCode.LANGUAGE_SWITCH -> {}
            KeyCode.SHIFT -> {
                if (hasCapsRecentlyChanged) {
                    osHandler.removeCallbacksAndMessages(null)
                    caps = true
                    capsLock = true
                    hasCapsRecentlyChanged = false
                } else {
                    caps = !caps
                    capsLock = false
                    hasCapsRecentlyChanged = true
                    osHandler.postDelayed({
                        hasCapsRecentlyChanged = false
                    }, 300)
                }
            }
            KeyCode.VIEW_SYMOBLS -> {}
            else -> {
                var text = code.toChar()
                if (caps) {
                    text = Character.toUpperCase(text)
                }
                ic.commitText(text.toString(), 1)
                if (!capsLock) {
                    caps = false
                }
            }
        }
    }
}
