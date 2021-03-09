package dev.patrickgold.florisboard.ime.clip

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeValue
import timber.log.Timber
import kotlin.math.roundToInt

class ClipboardHistoryView : LinearLayout, FlorisBoard.EventListener,
    ThemeManager.OnThemeUpdatedListener {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()
    private val themeManager: ThemeManager = ThemeManager.default()

    var backButton: Button? = null
        private set

    var clipText: TextView? = null
        private set

    var clipboardBar: LinearLayout? = null
        private set

    var clipboardHistory: RecyclerView? = null
        private set

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        florisboard?.addEventListener(this)
        themeManager.registerOnThemeUpdatedListener(this)
        backButton = findViewById(R.id.back_to_keyboard_button)
        clipText = findViewById(R.id.clipboard_text)
        clipboardBar = findViewById(R.id.clipboard_bar)
        clipboardHistory = findViewById(R.id.clipboard_history_items)

       // val layoutManager = StaggeredGridLayoutManager(2, VERTICAL)
       // clipboardHistory!!.layoutManager = layoutManager
        Timber.d("help me pls")
        onApplyThemeAttributes()
    }

    override fun onDetachedFromWindow() {
        themeManager.unregisterOnThemeUpdatedListener(this)
        florisboard?.removeEventListener(this)
        super.onDetachedFromWindow()
    }

    override fun onThemeUpdated(theme: Theme) {
        val fgColor = theme.getAttr(Theme.Attr.MEDIA_FOREGROUND).toSolidColor().color
        clipText?.setTextColor(fgColor)
        backButton?.setTextColor(fgColor)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = florisboard?.inputView?.desiredMediaKeyboardViewHeight ?: 0.0f
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height.roundToInt(), MeasureSpec.EXACTLY))
    }

}
