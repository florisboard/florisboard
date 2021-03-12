package dev.patrickgold.florisboard.ime.clip

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.popup.PopupLayerView
import timber.log.Timber
import kotlin.math.max

class ClipboardPopupManager (private val keyboardView: ClipboardHistoryView,
                             private val popupLayerView: PopupLayerView?,
                             private val clipboardHistoryItem: ClipboardHistoryItemView) {

    private val popupView: ClipboardPopupView = LayoutInflater.from(keyboardView.context).inflate(R.layout.clip_popup_layout, null) as ClipboardPopupView
    private var width = 0
    private var height = 0
    private var xOffset = 0
    private var yOffset = 0


    init {
        popupLayerView?.addView(popupView)
    }

    fun show(view: ClipboardHistoryItemView) {
        calc(view)

        popupView.properties.let {
            it.width   = this.width
            it.height  = this.height
            it.xOffset = this.xOffset
            it.yOffset = this.yOffset
        }
        popupView.show(keyboardView)


        popupView.findViewById<LinearLayout>(R.id.pin_clip_item).setOnClickListener{
            val pos = ClipboardInputManager.getInstance().getPositionOfView(clipboardHistoryItem)
            FlorisClipboardManager.getInstance().pinClip(pos)
            hide()
        }

        FlorisBoard.getInstance().isClipboardContextMenuShown = true
        popupLayerView?.clipboardPopupManager = this
        popupLayerView?.intercept = popupView
    }

    private fun calc(view: ClipboardHistoryItemView) {

        if (popupView.width == 0){
            val widthMeasureSpec: Int = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.AT_MOST)
            val heightMeasureSpec: Int = View.MeasureSpec.makeMeasureSpec(100000, View.MeasureSpec.AT_MOST)
            popupView.measure(widthMeasureSpec, heightMeasureSpec)
        }

        // TODO: extract to [dimens.xml]
        width =  popupView.measuredWidth
        height = popupView.measuredHeight
        xOffset = view.x.toInt() + (view.width - popupView.measuredWidth)/2
        yOffset = max(view.y.toInt() - keyboardView.height - height/2 - 20, keyboardView.y.toInt()  - keyboardView.height - height/2 - 20)

        Timber.d("$width $height $xOffset $yOffset")
    }

    fun hide() {
        popupView.hide()
        popupLayerView?.intercept = null
        popupLayerView?.clipboardPopupManager = null
        FlorisBoard.getInstance().isClipboardContextMenuShown = true

        popupView.apply {
            visibility = GONE
        }

    }
}
