package dev.patrickgold.florisboard

import android.inputmethodservice.InputMethodService
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.LinearLayout
import com.google.android.flexbox.FlexboxLayout
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class FlorisBoard : InputMethodService() {

    data class JKey(
        val cmd: String?,
        val code: Int?,
        val popup: List<Int>?,
        val isRepeatable: Boolean?
    )

    data class JLayout(
        val name: String,
        val direction: String,
        val layout: List<List<JKey>>
    )

    override fun onCreateInputView(): View? {
        val florisboard = layoutInflater.inflate(R.layout.florisboard, null) as LinearLayout
        val keyboardContainer = florisboard.findViewById<LinearLayout>(R.id.keyboard_container)
        /*val keyboard = layoutInflater.inflate(R.layout.qwerty, null) as CustomKeyboard
        keyboard.inputMethodService = this
        for (i in 0 until keyboard.childCount) {
            val row = keyboard.getChildAt(i)
            if (row is CustomKeyboardRow) {
                for (j in 0 until row.childCount) {
                    val vv = row.getChildAt(j)
                    if (vv is CustomKey) {
                        vv.keyboard = keyboard
                    }
                }
            }
        }
        keyboardContainer.addView(keyboard)*/
        val keyboard = buildLayout("qwerty")
        keyboardContainer.addView(keyboard)
        return florisboard
    }

    private fun buildLayout(layoutName: String): CustomKeyboard {
        val context = ContextThemeWrapper(baseContext, R.style.KeyboardTheme_MaterialLight)
        val keyboard = CustomKeyboard(context)
        keyboard.layoutName = layoutName
        keyboard.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        keyboard.inputMethodService = this
        val jsonRaw = resources.openRawResource(R.raw.kbd_qwerty)
            .bufferedReader().use { it.readText() }
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val layoutAdapter = moshi.adapter(JLayout::class.java)
        val layout = layoutAdapter.fromJson(jsonRaw)
        if (layout != null) {
            for (row in layout.layout) {
                val rowView = CustomKeyboardRow(context)
                val rowViewLP = FlexboxLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                rowViewLP.setMargins(
                    0, resources.getDimension(R.dimen.keyboard_row_marginV).toInt(),
                    0, resources.getDimension(R.dimen.keyboard_row_marginV).toInt()
                )
                rowView.layoutParams = rowViewLP
                for (key in row) {
                    val keyView = CustomKey(context)
                    val keyViewLP = FlexboxLayout.LayoutParams(
                        resources.getDimension(R.dimen.key_width).toInt(),
                        resources.getDimension(R.dimen.key_height).toInt()
                    )
                    keyViewLP.setMargins(
                        resources.getDimension(R.dimen.key_marginH).toInt(), 0,
                        resources.getDimension(R.dimen.key_marginH).toInt(), 0
                    )
                    keyView.layoutParams = keyViewLP
                    if (key.code != null) {
                        keyView.code = key.code
                    }
                    if (key.cmd != null) {
                        keyView.cmd = KeyCodes.fromString(key.cmd)
                    }
                    if (key.popup != null) {
                        keyView.popupCodes = key.popup
                    }
                    if (key.isRepeatable != null) {
                        keyView.isRepeatable = key.isRepeatable
                    }
                    keyView.keyboard = keyboard
                    rowView.addView(keyView)
                }
                keyboard.addView(rowView)
            }
        }
        return keyboard
    }
}