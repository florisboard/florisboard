package dev.patrickgold.florisboard.ime.text.keyboard

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.popup.KeyPopupManager
import dev.patrickgold.florisboard.ime.text.key.KeyView
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData

/**
 * View class for managing the UI layout and desired key width/height.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 */
@SuppressLint("ViewConstructor")
class KeyboardView(
    val florisboard: FlorisBoard
) : LinearLayout(
    florisboard.context, null, R.attr.keyboardViewStyle
) {
    var activeKeyView: KeyView? = null
    var computedLayout: ComputedLayoutData? = null
    var desiredKeyWidth: Int = resources.getDimension(R.dimen.key_width).toInt()
    var desiredKeyHeight: Int = resources.getDimension(R.dimen.key_height).toInt()
    var popupManager: KeyPopupManager = KeyPopupManager(this)
    var shouldStealMotionEvents: Boolean = false

    private fun buildLayout() {
        destroyLayout()
        val context = ContextThemeWrapper(context,
            R.style.KeyboardTheme_MaterialLight
        )
        this.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        val layout = computedLayout
        if (layout != null) {
            for (row in layout.arrangement) {
                val rowView = KeyboardRowView(context)
                for (key in row) {
                    val keyView = KeyView(florisboard, this, key)
                    rowView.addView(keyView)
                }
                this.addView(rowView)
            }
        }
    }

    private fun destroyLayout() {
        removeAllViews()
    }

    fun setKeyboardMode(keyboardMode: KeyboardMode) {
        computedLayout = florisboard.textInputManager.layoutManager.computeLayoutFor(keyboardMode)
        buildLayout()
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return true
    }

    // TODO: Implement multi touch to prevent key loss when user taps fast and it occurs roughly at
    //       the same time
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val event = event ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                getActiveKeyViewFor(event)
                activeKeyView?.onFlorisTouchEvent(transformToKeyViewEvent(event))
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeKeyView == null) {
                    getActiveKeyViewFor(event)
                    activeKeyView?.onFlorisTouchEvent(transformToKeyViewEvent(event.apply {
                        action = MotionEvent.ACTION_DOWN
                    }))
                } else {
                    activeKeyView?.onFlorisTouchEvent(transformToKeyViewEvent(event))
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                activeKeyView?.onFlorisTouchEvent(transformToKeyViewEvent(event))
                activeKeyView = null
            }
            else -> return false
        }
        return true
    }

    private fun transformToKeyViewEvent(event: MotionEvent): MotionEvent {
        val keyView = activeKeyView ?: return event
        val keyViewParent = keyView.parent as ViewGroup
        return event.apply {
            setLocation(
                event.x - keyViewParent.x - keyView.x,
                event.y - keyViewParent.y - keyView.y
            )
        }
    }

    private fun getActiveKeyViewFor(event: MotionEvent) {
        loop@ for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        if (keyView.touchHitBox.contains(
                                event.x.toInt(), event.y.toInt()
                            )) {
                            activeKeyView = keyView
                            break@loop
                        }
                    }
                }
            }
        }
    }

    fun invalidateActiveKeyViewReference() {
        activeKeyView?.onFlorisTouchEvent(MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0
        ))
        activeKeyView = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        desiredKeyWidth = when (computedLayout?.mode) {
            KeyboardMode.NUMERIC,
            KeyboardMode.PHONE,
            KeyboardMode.PHONE2 -> (widthSize / 4) - (2 * keyMarginH)
            else -> (widthSize / 10) - (2 * keyMarginH)
        }

        val factor = florisboard.prefs!!.heightFactor
        val keyHeightNormal = resources.getDimension(R.dimen.key_height) * when(resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 0.85f
            else -> if (florisboard.prefs?.oneHandedMode == "start" ||
                florisboard.prefs?.oneHandedMode == "end") {
                0.9f
            } else {
                1.0f
            }
        }
        desiredKeyHeight = (keyHeightNormal * when (factor) {
            "extra_short" -> 0.85f
            "short" -> 0.90f
            "mid_short" -> 0.95f
            "normal" -> 1.00f
            "mid_tall" -> 1.05f
            "tall" -> 1.10f
            "extra_tall" -> 1.15f
            else -> 1.00f
        }).toInt()

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun invalidateAllKeys() {
        for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        keyView.invalidate()
                    }
                }
            }
        }
    }

    fun updateVariation() {
        for (row in children) {
            if (row is FlexboxLayout) {
                for (keyView in row.children) {
                    if (keyView is KeyView) {
                        keyView.updateVariation()
                    }
                }
            }
        }
    }
}
