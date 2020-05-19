package dev.patrickgold.florisboard.ime.media.emoji

import android.annotation.SuppressLint
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
import java.util.*

@SuppressLint("ViewConstructor")
class EmojiKeyboardView(
    private val florisboard: FlorisBoard
) : LinearLayout(florisboard.context) {

    private var activeCategory: EmojiCategory = EmojiCategory.SMILEYS_EMOTION
    private var emojiViewFlipper: ViewFlipper
    // TODO: run this task async (coroutines?) to avoid blocking the ui thread
    private val layouts =
        parseRawEmojiSpecsFile(context, "ime/emoji/emoji-test.txt")
    private val uiLayouts = EnumMap<EmojiCategory, HorizontalScrollView>(EmojiCategory::class.java)

    init {
        orientation = VERTICAL

        emojiViewFlipper = ViewFlipper(context)
        emojiViewFlipper.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        emojiViewFlipper.measureAllChildren = false
        addView(emojiViewFlipper)

        for (category in EmojiCategory.values()) {
            val hsv = buildLayoutForCategory(category)
            uiLayouts[category] = hsv
            emojiViewFlipper.addView(hsv)
        }

        val tabs = ViewGroup.inflate(context, R.layout.media_input_emoji_tabs, null) as TabLayout
        addView(tabs)
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

        setActiveCategory(EmojiCategory.SMILEYS_EMOTION)
    }

    private fun buildLayoutForCategory(category: EmojiCategory): HorizontalScrollView {
        val emojiKeyWidth = resources.getDimension(R.dimen.emoji_key_width).toInt()
        val emojiKeyHeight = resources.getDimension(R.dimen.emoji_key_height).toInt()
        val hsv = HorizontalScrollView(context)
        hsv.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val flexboxLayout = FlexboxLayout(context)
        flexboxLayout.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, emojiKeyHeight * 3)
        flexboxLayout.flexDirection = FlexDirection.COLUMN
        flexboxLayout.flexWrap = FlexWrap.WRAP
        for (emojiKeyData in layouts[category].orEmpty()) {
            val emojiKeyView = EmojiKeyView(florisboard, this, emojiKeyData)
            emojiKeyView.layoutParams = FlexboxLayout.LayoutParams(
                emojiKeyWidth, emojiKeyHeight
            )
            flexboxLayout.addView(emojiKeyView)
        }
        hsv.addView(flexboxLayout)
        return hsv
    }

    fun setActiveCategory(newActiveCategory: EmojiCategory) {
        emojiViewFlipper.displayedChild =
            emojiViewFlipper.indexOfChild(uiLayouts[newActiveCategory])
        activeCategory = newActiveCategory
    }
}
