package dev.patrickgold.florisboard.ime.clip

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.popup.PopupLayerView
import kotlin.math.max

class ClipboardPopupManager(private val keyboardView: ClipboardHistoryView,
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


    private fun pinButtonListener() {
        val pos = ClipboardInputManager.getInstance().getPositionOfView(clipboardHistoryItem)
        val pinned = FlorisClipboardManager.getInstance().isPinned(pos)
        if (pinned) {
            FlorisClipboardManager.getInstance().unpinClip(pos)
            hide()
        } else {
            FlorisClipboardManager.getInstance().pinClip(pos)
            hide()
        }
    }

    /**
     * Show a popup.
     */
    fun show(view: ClipboardHistoryItemView) {
        val pinButton = popupView.findViewById<LinearLayout>(R.id.pin_clip_item)
        pinButton.setOnClickListener {
            pinButtonListener()
        }

        val pos = ClipboardInputManager.getInstance().getPositionOfView(clipboardHistoryItem)
        val pinned = FlorisClipboardManager.getInstance().isPinned(pos)

        if (pinned) {
            pinButton.findViewById<TextView>(R.id.pin_clip_item_text).text = view.context.getString(R.string.clip__unpin_item)
        }

        val delete = popupView.findViewById<LinearLayout>(R.id.remove_from_history)
        delete.setOnClickListener {
            FlorisClipboardManager.getInstance().removeClip(pos)
            hide()
        }

        val clipboardManager = FlorisClipboardManager.getInstance()
        val clipItem = clipboardManager.peekHistoryOrPin(pos)
        val pasteShouldBeEnabled = FlorisClipboardManager.getInstance().canBePasted(clipItem)
        // the clipboard item has any of the supported mime types of the editor OR is plain text.

        val paste = popupView.findViewById<LinearLayout>(R.id.paste_clip_item)
        if (pasteShouldBeEnabled) {
            paste.setOnClickListener {
                FlorisClipboardManager.getInstance().pasteItem(pos)
                hide()
            }
            popupView.findViewById<Space>(R.id.paste_clip_item_space).visibility = VISIBLE
            paste.visibility = VISIBLE
        }else {
            popupView.findViewById<Space>(R.id.paste_clip_item_space).visibility = GONE
            paste.visibility = GONE
        }

        FlorisBoard.getInstance().isClipboardContextMenuShown = true
        popupLayerView?.clipboardPopupManager = this
        popupLayerView?.intercept = popupView
        calc(view)

        popupView.properties.let {
            it.width = this.width
            it.height = this.height
            it.xOffset = this.xOffset
            it.yOffset = this.yOffset
        }
        popupView.show(keyboardView)
    }

    /**
     * Calculate sizes of popup.
     */
    private fun calc(view: ClipboardHistoryItemView) {
        val widthMeasureSpec: Int = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.AT_MOST)
        val heightMeasureSpec: Int = View.MeasureSpec.makeMeasureSpec(100000, View.MeasureSpec.AT_MOST)
        popupView.invalidate()
        popupView.measure(widthMeasureSpec, heightMeasureSpec)

        width = view.width * 4 / 5
        height = popupView.measuredHeight
        xOffset = view.x.toInt() + (view.width - width) / 2
        // y offset is either where the top of the item is OR if the top is off screen, the top of the keyboard.
        yOffset = max(view.y.toInt() - keyboardView.height - height / 2 - 20, keyboardView.y.toInt() - keyboardView.height - height / 2 - 20)
    }

    /**
     * Hides a popup.
     */
    fun hide() {
        popupView.hide()
        popupLayerView?.intercept = null
        popupLayerView?.clipboardPopupManager = null
        FlorisBoard.getInstance().isClipboardContextMenuShown = false

        popupView.apply {
            visibility = GONE
        }

    }
}
