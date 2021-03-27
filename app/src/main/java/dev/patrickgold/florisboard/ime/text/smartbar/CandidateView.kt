/*
 * Copyright (C) 2021 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.text.smartbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.OverScroller
import androidx.core.content.ContextCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.clip.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeValue
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class CandidateView : View, ThemeManager.OnThemeUpdatedListener {
    private var themeManager: ThemeManager? = null
    private var eventListener: WeakReference<SmartbarView.EventListener?> = WeakReference(null)
    private var clipboardContentTimeout: Int = 60_000
    private var displayMode: DisplayMode = DisplayMode.DYNAMIC_SCROLLABLE

    private val candidates: ArrayList<String> = ArrayList()
    private var clipboardCandidate: ClipboardItem? = null
    private var clipboardCandidateTime: Long = 0
    private var computedCandidates: ArrayList<ComputedCandidate> = ArrayList()
    private var computedCandidatesWidthPx: Int = 0
    private var selectedIndex: Int = -1

    private var backgroundPaint: Paint = Paint().apply { color = Color.BLACK }
    private var candidateBackground: ThemeValue = ThemeValue.SolidColor.TRANSPARENT
    private var candidateForeground: ThemeValue = ThemeValue.SolidColor.TRANSPARENT
    private val candidateMarginH: Int = resources.getDimensionPixelOffset(R.dimen.smartbar_candidate_marginH)
    private var dividerBackground: ThemeValue = ThemeValue.SolidColor.TRANSPARENT
    private var dividerWidth: Int = resources.getDimensionPixelSize(R.dimen.smartbar_divider_width)
    private val pasteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_content_paste)
    private var lastX: Float = 0.0f
    private val scroller: OverScroller = OverScroller(context, AccelerateDecelerateInterpolator())
    private val textPaint: TextPaint = TextPaint().apply {
        alpha = 255
        color = Color.BLACK
        isAntiAlias = true
        isFakeBoldText = false
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.smartbar_candidate_textSize)
        typeface = Typeface.DEFAULT
    }
    private var velocityTracker: VelocityTracker? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false
        scrollTo(0, 0)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        themeManager = ThemeManager.defaultOrNull()
        themeManager?.registerOnThemeUpdatedListener(this)
        updateCandidates(candidates)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        themeManager?.unregisterOnThemeUpdatedListener(this)
        themeManager = null
        candidates.clear()
        velocityTracker?.recycle()
        velocityTracker = null
    }

    fun updateCandidates(newCandidates: List<String>?) {
        candidates.clear()
        if (newCandidates != null) {
            candidates.addAll(newCandidates)
        }
        recomputeCandidates()
    }

    fun updateClipboardCandidate(newClipboardCandidate: ClipboardItem) {
        clipboardCandidate = newClipboardCandidate
        clipboardCandidateTime = System.currentTimeMillis()
        recomputeCandidates()
    }

    fun setEventListener(listener: SmartbarView.EventListener) {
        eventListener = WeakReference(listener)
    }

    fun updateDisplaySettings(newDisplayMode: DisplayMode, newClipboardContentTimeout: Int) {
        if (newClipboardContentTimeout != clipboardContentTimeout) {
            clipboardContentTimeout = newClipboardContentTimeout
        }
        if (newDisplayMode != displayMode) {
            displayMode = newDisplayMode
            scroller.abortAnimation()
            scrollTo(0, 0)
        }
    }

    private fun recomputeCandidates() {
        computedCandidates.clear()
        if (clipboardCandidate != null && System.currentTimeMillis() - clipboardCandidateTime > clipboardContentTimeout) {
            clipboardCandidate = null
        }
        val classicCandidateWidth = (measuredWidth - 2 * dividerWidth) / 3
        val maxDynamicCandidateWidth = (measuredWidth * 0.7).toInt()
        computedCandidatesWidthPx = 0
        if (candidates.isEmpty()) {
            if (clipboardCandidate != null) {
                computedCandidates.add(ComputedCandidate.Clip(clipboardCandidate!!, Rect(
                    0,
                    0,
                    measuredWidth,
                    measuredHeight
                )))
            } else if (displayMode == DisplayMode.CLASSIC) {
                for (n in 0 until 3) {
                    val left = (classicCandidateWidth + dividerWidth) * n
                    computedCandidates.add(ComputedCandidate.Empty(Rect(
                        left,
                        0,
                        left + classicCandidateWidth,
                        measuredHeight
                    )))
                }
            }
        } else if (candidates.size == 1 && clipboardCandidate == null) {
            computedCandidates.add(ComputedCandidate.Word(candidates[0], Rect(
                0,
                0,
                measuredWidth,
                measuredHeight
            )))
        } else {
            when (displayMode) {
                DisplayMode.CLASSIC -> {
                    if (clipboardCandidate == null) {
                        for (n in 0 until candidates.size.coerceAtMost(3)) {
                            val left = (classicCandidateWidth + dividerWidth) * n
                            computedCandidates.add(ComputedCandidate.Word(candidates[n], Rect(
                                left,
                                0,
                                left + classicCandidateWidth,
                                measuredHeight
                            )))
                        }
                    } else {
                        computedCandidates.add(ComputedCandidate.Clip(clipboardCandidate!!, Rect(
                            0,
                            0,
                            classicCandidateWidth,
                            measuredHeight
                        )))
                        for (n in 0 until candidates.size.coerceAtMost(2)) {
                            val left = (classicCandidateWidth + dividerWidth) * (n + 1)
                            computedCandidates.add(ComputedCandidate.Word(candidates[n], Rect(
                                left,
                                0,
                                left + classicCandidateWidth,
                                measuredHeight
                            )))
                        }
                    }
                    if (computedCandidates.size < 3) {
                        for (n in computedCandidates.size until 3) {
                            val left = (classicCandidateWidth + dividerWidth) * n
                            computedCandidates.add(ComputedCandidate.Empty(Rect(
                                left,
                                0,
                                left + classicCandidateWidth,
                                measuredHeight
                            )))
                        }
                    }
                }
                DisplayMode.DYNAMIC -> {
                    if (clipboardCandidate != null) {
                        val candidateWidth = (textPaint.measureText(clipboardCandidate!!.text).toInt() + candidateMarginH + measuredHeight * 4 / 6).coerceAtMost(maxDynamicCandidateWidth)
                        computedCandidates.add(ComputedCandidate.Clip(clipboardCandidate!!, Rect(
                            0,
                            0,
                            candidateWidth,
                            measuredHeight
                        )))
                        computedCandidatesWidthPx += candidateWidth
                    }
                    for (n in candidates.indices) {
                        var tmpWidthPx = computedCandidatesWidthPx
                        if (tmpWidthPx > 0) {
                            tmpWidthPx += dividerWidth
                        }
                        val candidateWidth = (textPaint.measureText(candidates[n]).toInt() + 2 * candidateMarginH).coerceAtMost(maxDynamicCandidateWidth)
                        tmpWidthPx += candidateWidth
                        if (tmpWidthPx > measuredWidth) {
                            break
                        } else {
                            computedCandidates.add(ComputedCandidate.Word(candidates[n], Rect(
                                computedCandidatesWidthPx,
                                0,
                                computedCandidatesWidthPx + candidateWidth,
                                measuredHeight
                            )))
                            computedCandidatesWidthPx = tmpWidthPx
                        }
                    }
                    val widthToIncreasePerItem = (measuredWidth - computedCandidatesWidthPx) / computedCandidates.size
                    for (n in computedCandidates.indices) {
                        computedCandidates[n].geometry.let {
                            it.left += n * widthToIncreasePerItem
                            it.right += (n + 1) * widthToIncreasePerItem
                        }
                    }
                }
                DisplayMode.DYNAMIC_SCROLLABLE -> {
                    if (clipboardCandidate != null) {
                        val candidateWidth = (textPaint.measureText(clipboardCandidate!!.text).toInt() + candidateMarginH + measuredHeight * 4 / 6).coerceAtMost(maxDynamicCandidateWidth)
                        computedCandidates.add(ComputedCandidate.Clip(clipboardCandidate!!, Rect(
                            0,
                            0,
                            candidateWidth,
                            measuredHeight
                        )))
                        computedCandidatesWidthPx += candidateWidth
                    }
                    for (n in candidates.indices) {
                        if (computedCandidatesWidthPx > 0) {
                            computedCandidatesWidthPx += dividerWidth
                        }
                        val candidateWidth = (textPaint.measureText(candidates[n]).toInt() + 2 * candidateMarginH).coerceAtMost(maxDynamicCandidateWidth)
                        computedCandidates.add(ComputedCandidate.Word(candidates[n], Rect(
                            computedCandidatesWidthPx,
                            0,
                            computedCandidatesWidthPx + candidateWidth,
                            measuredHeight
                        )))
                        computedCandidatesWidthPx += candidateWidth
                    }
                }
            }
        }

        selectedIndex = -1
        scroller.abortAnimation()
        scrollTo(0, 0)
        invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            invalidate()
        }
    }

    private fun determineSelectedIndex(relX: Int, relY: Int): Int {
        val absX = relX + scrollX
        val absY = relY + scrollY
        var retIndex = -1
        for ((n, computedCandidate) in computedCandidates.withIndex()) {
            if (computedCandidate.geometry.contains(absX, absY)) {
                retIndex = n
                break
            }
        }
        return retIndex
    }

    private fun onCandidateClick(index: Int) {
        computedCandidates.getOrNull(index)?.let { candidate ->
            when (candidate) {
                is ComputedCandidate.Word -> {
                    eventListener.get()?.onSmartbarCandidatePressed(candidate.word)
                }
                is ComputedCandidate.Clip -> {
                    eventListener.get()?.onSmartbarClipboardCandidatePressed(candidate.clipboardItem)
                }
                is ComputedCandidate.Empty -> {
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        event ?: return false

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                selectedIndex = determineSelectedIndex(event.x.toInt(), event.y.toInt())
                if (displayMode == DisplayMode.DYNAMIC_SCROLLABLE) {
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(event)
                    lastX = event.x
                }
                invalidate()
                true
            }
            MotionEvent.ACTION_MOVE -> when (displayMode) {
                DisplayMode.CLASSIC,
                DisplayMode.DYNAMIC -> {
                    computedCandidates.getOrNull(selectedIndex)?.let { candidate ->
                        if (!candidate.geometry.contains(scrollX + event.x.toInt(), scrollY + event.y.toInt())) {
                            selectedIndex = -1
                            invalidate()
                        }
                    }
                    true
                }
                DisplayMode.DYNAMIC_SCROLLABLE -> {
                    velocityTracker?.addMovement(event)
                    selectedIndex = -1
                    if (computedCandidatesWidthPx > measuredWidth) {
                        scrollTo((scrollX + lastX - event.x).toInt().coerceIn(0, computedCandidatesWidthPx - measuredWidth), 0)
                        lastX = event.x
                    }
                    invalidate()
                    true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    onCandidateClick(selectedIndex)
                }
                selectedIndex = -1
                if (displayMode == DisplayMode.DYNAMIC_SCROLLABLE) {
                    velocityTracker?.let {
                        it.addMovement(event)
                        it.computeCurrentVelocity(1000)

                        if (computedCandidatesWidthPx > measuredWidth) {
                            scroller.fling(
                                scrollX, 0,
                                -it.xVelocity.toInt(), 0,
                                0, computedCandidatesWidthPx - measuredWidth,
                                0, 0,
                                0, 0
                            )
                        }
                        it.recycle()
                        velocityTracker = null
                    }
                }
                invalidate()
                true
            }
            else -> false
        }
    }

    override fun onThemeUpdated(theme: Theme) {
        candidateBackground = theme.getAttr(Theme.Attr.WINDOW_SEMI_TRANSPARENT_COLOR)
        candidateForeground = theme.getAttr(Theme.Attr.SMARTBAR_FOREGROUND)
        dividerBackground = theme.getAttr(Theme.Attr.SMARTBAR_FOREGROUND_ALT)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        Timber.i(computedCandidates.toString())
        Timber.i(selectedIndex.toString())
        textPaint.apply { color = candidateForeground.toSolidColor().color }
        for ((n, computedCandidate) in computedCandidates.withIndex()) {
            with(computedCandidate) {
                if (n == selectedIndex) {
                    backgroundPaint.apply { color = candidateBackground.toSolidColor().color }
                    canvas.drawRect(geometry, backgroundPaint)
                }
                when (this) {
                    is ComputedCandidate.Word -> {
                        val ellipsizedWord = TextUtils.ellipsize(
                            word, textPaint, geometry.width().toFloat() - candidateMarginH, TextUtils.TruncateAt.MIDDLE
                        ).toString()
                        canvas.drawText(
                            ellipsizedWord,
                            geometry.left + geometry.width() / 2.0f,
                            geometry.top + geometry.height() / 2.0f - (textPaint.descent() + textPaint.ascent()) / 2.0f,
                            textPaint
                        )
                    }
                    is ComputedCandidate.Clip -> {
                        pasteDrawable?.setTint(candidateForeground.toSolidColor().color)
                        pasteDrawable?.setBounds(
                            geometry.left + geometry.height() / 3,
                            geometry.height() / 3,
                            geometry.left + geometry.height() * 2 / 3,
                            geometry.height() * 2 / 3
                        )
                        pasteDrawable?.draw(canvas)
                        val pdWidth = geometry.height().toFloat()
                        val ellipsizedWord = TextUtils.ellipsize(
                            clipboardItem.text, textPaint, geometry.width().toFloat() - pdWidth, TextUtils.TruncateAt.MIDDLE
                        ).toString()
                        canvas.drawText(
                            ellipsizedWord,
                            geometry.left + geometry.width() / 2.0f + pdWidth / 2.0f - candidateMarginH,
                            geometry.top + geometry.height() / 2.0f - (textPaint.descent() + textPaint.ascent()) / 2.0f,
                            textPaint
                        )
                    }
                    is ComputedCandidate.Empty -> {
                    }
                }
                if (n + 1 < computedCandidates.size) {
                    backgroundPaint.apply { color = dividerBackground.toSolidColor().color }
                    canvas.drawRect(
                        geometry.right.toFloat(),
                        (geometry.height() / 6).toFloat(),
                        (geometry.right + dividerWidth).toFloat(),
                        (geometry.height() * 5 / 6).toFloat(),
                        backgroundPaint
                    )
                }
            }
        }
    }

    private sealed class ComputedCandidate(val geometry: Rect) {
        class Word(
            val word: String,
            geometry: Rect
        ) : ComputedCandidate(geometry) {
            override fun toString(): String {
                return "Word { word=\"$word\", geometry=$geometry }"
            }
        }

        class Empty(
            geometry: Rect
        ) : ComputedCandidate(geometry) {
            override fun toString(): String {
                return "Empty { geometry=$geometry }"
            }
        }

        class Clip(
            val clipboardItem: ClipboardItem,
            geometry: Rect
        ) : ComputedCandidate(geometry) {
            override fun toString(): String {
                return "Word { clipboardItem=$clipboardItem, geometry=$geometry }"
            }
        }
    }

    enum class DisplayMode {
        CLASSIC,
        DYNAMIC,
        DYNAMIC_SCROLLABLE;

        companion object {
            fun fromString(string: String): DisplayMode {
                return valueOf(string.toUpperCase(Locale.ENGLISH))
            }
        }

        override fun toString(): String {
            return super.toString().toLowerCase(Locale.ENGLISH)
        }
    }
}
