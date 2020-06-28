/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.media.emoji

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ViewFlipper
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.tabs.TabLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.popup.KeyPopupManager
import kotlinx.coroutines.*
import java.util.*

/**
 * Manages the layout creation and touch events for the emoji section of the media context. Parts
 * of the layout of this view will be generated in coroutines and will therefore not instantly be
 * visible.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 */
class EmojiKeyboardView : LinearLayout {

    private var activeCategory: EmojiCategory = EmojiCategory.SMILEYS_EMOTION
    private var emojiViewFlipper: ViewFlipper
    private val emojiKeyWidth = resources.getDimension(R.dimen.emoji_key_width).toInt()
    private val emojiKeyHeight = resources.getDimension(R.dimen.emoji_key_height).toInt()
    private val florisboard: FlorisBoard = FlorisBoard.getInstance()
    private var layouts: Deferred<EmojiLayoutDataMap>
    private val mainScope = MainScope()
    private val uiLayouts = EnumMap<EmojiCategory, HorizontalScrollView>(EmojiCategory::class.java)

    var isScrollBlocked: Boolean = false
    var popupManager = KeyPopupManager<EmojiKeyboardView, EmojiKeyView>(this)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        layouts = mainScope.async(Dispatchers.IO) {
            parseRawEmojiSpecsFile(context, "ime/media/emoji/emoji-test.txt")
        }
        orientation = VERTICAL

        emojiViewFlipper = ViewFlipper(context)
        emojiViewFlipper.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        emojiViewFlipper.measureAllChildren = false
        addView(emojiViewFlipper)

        val tabs =
            ViewGroup.inflate(context, R.layout.media_input_emoji_tabs, null) as TabLayout
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                setActiveCategory(when (tab?.position) {
                    0 -> EmojiCategory.SMILEYS_EMOTION
                    1 -> EmojiCategory.PEOPLE_BODY
                    2 -> EmojiCategory.ANIMALS_NATURE
                    3 -> EmojiCategory.FOOD_DRINK
                    4 -> EmojiCategory.TRAVEL_PLACES
                    5 -> EmojiCategory.ACTIVITIES
                    6 -> EmojiCategory.OBJECTS
                    7 -> EmojiCategory.SYMBOLS
                    8 -> EmojiCategory.FLAGS
                    else -> EmojiCategory.SMILEYS_EMOTION
                })
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
        addView(tabs)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mainScope.launch {
            layouts.await()
            buildLayout()
            setActiveCategory(EmojiCategory.SMILEYS_EMOTION)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainScope.cancel()
    }

    /**
     * Requests the layout for each category and attaches the built layout to [emojiViewFlipper].
     * This method runs in the [Dispatchers.Default] context and will block the main thread only
     * when it attaches a built category layout to the view hierarchy.
     */
    private suspend fun buildLayout() = withContext(Dispatchers.Default) {
        for (category in EmojiCategory.values()) {
            val hsv = buildLayoutForCategory(category)
            uiLayouts[category] = hsv
            withContext(Dispatchers.Main) {
                emojiViewFlipper.addView(hsv)
            }
        }
    }

    /**
     * Builds the layout for a given [category]. This function runs in the [Dispatchers.Default]
     * context and will not block the main UI thread.
     *
     * @param category The category for which a layout should be built.
     * @return The layout (top-most view is a [HorizontalScrollView]).
     */
    @SuppressLint("ClickableViewAccessibility")
    private suspend fun buildLayoutForCategory(
        category: EmojiCategory
    ): HorizontalScrollView = withContext(Dispatchers.Default) {
        val hsv = HorizontalScrollView(context)
        hsv.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val flexboxLayout = FlexboxLayout(context)
        flexboxLayout.layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, emojiKeyHeight * 3)
        flexboxLayout.flexDirection = FlexDirection.COLUMN
        flexboxLayout.flexWrap = FlexWrap.WRAP
        for (emojiKeyData in layouts.await()[category].orEmpty()) {
            val emojiKeyView =
                EmojiKeyView(florisboard, this@EmojiKeyboardView, emojiKeyData)
            emojiKeyView.layoutParams = FlexboxLayout.LayoutParams(
                emojiKeyWidth, emojiKeyHeight
            )
            flexboxLayout.addView(emojiKeyView)
        }
        hsv.setOnTouchListener { _, _ ->
            return@setOnTouchListener isScrollBlocked
        }
        hsv.addView(flexboxLayout)
        return@withContext hsv
    }

    /**
     * Sets the [activeCategory] and changes the active view of [emojiViewFlipper] accordingly.
     *
     * @param newActiveCategory The new active category.
     */
    fun setActiveCategory(newActiveCategory: EmojiCategory) {
        emojiViewFlipper.displayedChild =
            emojiViewFlipper.indexOfChild(uiLayouts[newActiveCategory])
        activeCategory = newActiveCategory
    }

    /**
     * Resets the [isScrollBlocked] flag whenever a [MotionEvent.ACTION_DOWN] occurs. This method
     * never intercepts any events and thus always returns false.
     *
     * @param ev The [MotionEvent] of the current touch event.
     * @return If this view wants to intercept the touch event. Always returns false here.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.actionMasked == MotionEvent.ACTION_DOWN) {
            isScrollBlocked = false
        }
        return false
    }

    /**
     * Invalidates the passed [keyView], sends a [MotionEvent.ACTION_CANCEL] to indicate the loss
     * of focus and prevents the HorizontalScrollView to scroll within this MotionEvent.
     */
    fun dismissKeyView(keyView: EmojiKeyView) {
        keyView.onTouchEvent(MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0
        ))
        isScrollBlocked = true
    }
}
