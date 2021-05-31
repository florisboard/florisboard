package dev.patrickgold.florisboard.ime.clip

import android.content.Context
import android.graphics.drawable.PaintDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.common.ViewUtils

class ClipboardPopupView: LinearLayout, ThemeManager.OnThemeUpdatedListener {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var backgroundDrawable: PaintDrawable = PaintDrawable().apply {
        setCornerRadius(ViewUtils.dp2px(6.0f))
    }
    private val themeManager: ThemeManager = ThemeManager.default()

    val properties: Properties = Properties(
        width = 0,
        height = 0,
        xOffset = 0,
        yOffset = 0
    )
    private val isShowing: Boolean
        get() = visibility == VISIBLE

    init {
        visibility = GONE
        background = backgroundDrawable
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        themeManager.registerOnThemeUpdatedListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        themeManager.unregisterOnThemeUpdatedListener(this)
    }

    override fun onThemeUpdated(theme: Theme) {
        backgroundDrawable.apply {
            setTint(theme.getAttr(Theme.Attr.POPUP_BACKGROUND).toSolidColor().color)
        }

        this.findViewById<ImageView>(R.id.pin_clip_item_icon).drawable.apply {
            setTint(theme.getAttr(Theme.Attr.WINDOW_TEXT_COLOR).toSolidColor().color)
        }


        this.findViewById<ImageView>(R.id.remove_from_history_icon).drawable.apply {
            setTint(theme.getAttr(Theme.Attr.WINDOW_TEXT_COLOR).toSolidColor().color)
        }

        this.findViewById<ImageView>(R.id.paste_clip_item_icon).drawable.apply {
            setTint(theme.getAttr(Theme.Attr.WINDOW_TEXT_COLOR).toSolidColor().color)
        }

        if (isShowing) {
            invalidate()
        }
    }

    private fun applyProperties(anchor: View) {
        val anchorCoords = IntArray(2)
        anchor.getLocationInWindow(anchorCoords)
        val anchorX = anchorCoords[0]
        val anchorY = anchorCoords[1] + anchor.measuredHeight
        when (val lp = layoutParams) {
            is FrameLayout.LayoutParams -> lp.apply {
                width = properties.width
                height = properties.height
                setMargins(
                    anchorX + properties.xOffset,
                    anchorY + properties.yOffset,
                    0,
                    0
                )
            }
            else -> {
                layoutParams = FrameLayout.LayoutParams(properties.width, properties.height).apply {
                    setMargins(
                        anchorX + properties.xOffset,
                        anchorY + properties.yOffset,
                        0,
                        0
                    )
                }
            }
        }
        if (isShowing) {
            requestLayout()
            invalidate()
        }
    }

    fun show(anchor: View) {
        applyProperties(anchor)
        visibility = VISIBLE
        requestLayout()
        invalidate()
    }

    fun hide() {
        visibility = GONE
        requestLayout()
        invalidate()
    }

    data class Properties(
        var width: Int,
        var height: Int,
        var xOffset: Int,
        var yOffset: Int
    )

}
