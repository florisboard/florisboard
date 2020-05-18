package dev.patrickgold.florisboard.ime.media

import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import com.google.android.material.tabs.TabLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyData
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyboardView
import dev.patrickgold.florisboard.ime.media.home.HomeView
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyType
import java.util.*

/**
 * MediaInputManager is responsible for managing everything which is related to media input. All of
 * the following count as media input: emojis, gifs, sticker, emoticons.
 * NOTE: if gifs and stickers end up to be implemented at all is not sure atm.
 *
 * All of the UI for the different media tabs are kept under the same container element and
 * are separated from text-related UI.
 *
 * Also within the scope of this class is a search bar for searching media content like stickers,
 * gifs and emojis.
 *
 * All events defined in [FlorisBoard.EventListener] will be passed through to this class by the
 * core.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 */
class MediaInputManager(private val florisboard: FlorisBoard) : FlorisBoard.EventListener {

    private var activeTab: Tab? = null
    private var mediaViewGroup: LinearLayout? = null
    private var tabLayout: TabLayout? = null
    private val tabViews = EnumMap<Tab, LinearLayout>(Tab::class.java)

    override fun onCreateInputView() {
        val rootViewGroup = florisboard.rootViewGroup ?: return
        mediaViewGroup = rootViewGroup.findViewById(R.id.media_input)

        // Init bottom buttons
        rootViewGroup.findViewById<Button>(R.id.media_input_switch_to_text_input_button)
            .setOnClickListener { v -> onBottomButtonClick(v) }
        rootViewGroup.findViewById<ImageButton>(R.id.media_input_backspace_button)
            .setOnClickListener { v -> onBottomButtonClick(v) }

        tabLayout = rootViewGroup.findViewById(R.id.media_input_tabs)
        tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> setActiveTab(Tab.HOME)
                    1 -> setActiveTab(Tab.EMOJIS)
                    2 -> setActiveTab(Tab.EMOTICONS)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        setActiveTab(Tab.HOME)
    }

    fun show() {
        mediaViewGroup?.visibility = View.VISIBLE
    }
    fun hide() {
        mediaViewGroup?.visibility = View.GONE
    }

    private fun onBottomButtonClick(v: View) {
        when (v.id) {
            R.id.media_input_switch_to_text_input_button -> {
                florisboard.setActiveInput(R.id.text_input)
            }
            R.id.media_input_backspace_button -> {
                florisboard.textInputManager.sendKeyPress(
                    KeyData(KeyCode.DELETE, type = KeyType.ENTER_EDITING)
                )
            }
        }
    }

    private fun createTabViewFor(tab: Tab): LinearLayout {
        return when (tab) {
            Tab.HOME -> HomeView(
                florisboard
            )
            Tab.EMOJIS -> EmojiKeyboardView(
                florisboard
            )
            else -> LinearLayout(florisboard)
        }
    }

    fun setActiveTab(newActiveTab: Tab) {
        if (!tabViews.containsKey(newActiveTab)) {
            val tabView = createTabViewFor(newActiveTab)
            tabViews[newActiveTab] = tabView
            mediaViewGroup?.addView(tabView, 1)
        }
        tabViews[activeTab]?.visibility = View.GONE
        tabViews[newActiveTab]?.visibility = View.VISIBLE
        activeTab = newActiveTab
    }

    fun sendEmojiKeyPress(emojiKeyData: EmojiKeyData) {
        val ic = florisboard.currentInputConnection
        ic.finishComposingText()
        ic.commitText(emojiKeyData.getCodePointsAsString(), 1)
    }

    enum class Tab {
        EMOJIS,
        EMOTICONS,
        GIFS,
        HOME,
        STICKERS
    }
}
