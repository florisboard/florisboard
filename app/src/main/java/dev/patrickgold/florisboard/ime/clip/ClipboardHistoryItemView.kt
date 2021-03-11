package dev.patrickgold.florisboard.ime.clip

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import timber.log.Timber

class ClipboardHistoryItemView: ConstraintLayout{

    lateinit var keyboardView: ClipboardHistoryView
    constructor(context: Context) : this(context, null as AttributeSet?)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var type: Int = -1
    var popupManager: ClipboardPopupManager? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        popupManager = ClipboardPopupManager(keyboardView, FlorisBoard.getInstance().popupLayerView)

        setOnClickListener{
            onClickItem(this)
        }

        setOnLongClickListener{
            onLongClickItem()
        }
        findViewById<ImageView>(R.id.clipboard_pin).visibility = INVISIBLE

        if (type == ClipboardHistoryItemAdapter.TEXT){
            val view = findViewById<TextView>(R.id.clipboard_history_item_text)
            view?.run {
                val params = layoutParams as LayoutParams
                params.marginEnd = 0
                layoutParams = params
            }

            val themeManager = ThemeManager.default()
            themeManager.registerOnThemeUpdatedListener{
                background.setTint(it.getAttr(Theme.Attr.KEY_BACKGROUND).toSolidColor().color)
            }
        }
    }

    override fun onDetachedFromWindow() {
        Timber.d("DETACHED")
        super.onDetachedFromWindow()
    }


    private fun onLongClickItem() : Boolean {
        popupManager?.show(this)
        return true
    }

    private fun onClickItem(view: View){
        val florisBoard = FlorisBoard.getInstance()
        val position = florisBoard.clipInputManager.recyclerView?.getChildLayoutPosition(view)!!
        florisBoard.florisClipboardManager!!
            .changeCurrent(florisBoard.florisClipboardManager!!.peekHistory(position)!!)
    }
}
