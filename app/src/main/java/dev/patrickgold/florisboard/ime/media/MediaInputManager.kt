package dev.patrickgold.florisboard.ime.media

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ViewFlipper
import com.google.android.material.tabs.TabLayout
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputView
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyData
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyboardView
import dev.patrickgold.florisboard.ime.media.home.HomeView
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyType
import kotlinx.coroutines.*
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
 */
class MediaInputManager private constructor() : CoroutineScope by MainScope(),
    FlorisBoard.EventListener {

    private val florisboard = FlorisBoard.getInstance()

    private var activeTab: Tab? = null
    private var mediaViewFlipper: ViewFlipper? = null
    private var tabLayout: TabLayout? = null
    private val tabViews = EnumMap<Tab, LinearLayout>(Tab::class.java)

    var mediaViewGroup: LinearLayout? = null

    companion object {
        private var instance: MediaInputManager? = null

        @Synchronized
        fun getInstance(): MediaInputManager {
            if (instance == null) {
                instance = MediaInputManager()
            }
            return instance!!
        }
    }

    override fun onRegisterInputView(inputView: InputView) {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onRegisterInputView(inputView)")

        launch(Dispatchers.Default) {
            mediaViewGroup = inputView.findViewById(R.id.media_input)
            mediaViewFlipper = inputView.findViewById(R.id.media_input_view_flipper)

            // Init bottom buttons
            inputView.findViewById<Button>(R.id.media_input_switch_to_text_input_button)
                .setOnClickListener { v -> onBottomButtonClick(v) }
            inputView.findViewById<ImageButton>(R.id.media_input_backspace_button)
                .setOnClickListener { v -> onBottomButtonClick(v) }

            tabLayout = inputView.findViewById(R.id.media_input_tabs)
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

            for (tab in Tab.values()) {
                val tabView = createTabViewFor(tab)
                tabViews[tab] = tabView
                withContext(Dispatchers.Main) {
                    mediaViewFlipper?.addView(tabView)
                }
            }

            withContext(Dispatchers.Main) {
                tabLayout?.selectTab(tabLayout?.getTabAt(1))
            }
        }
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

        val emojiKeyboardView = tabViews[Tab.EMOJIS]
        if (emojiKeyboardView is EmojiKeyboardView) {
            emojiKeyboardView.onDestroy()
        }
        cancel()
        instance = null
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
            Tab.HOME -> HomeView(florisboard)
            Tab.EMOJIS -> EmojiKeyboardView(florisboard)
            else -> LinearLayout(florisboard)
        }
    }

    fun setActiveTab(newActiveTab: Tab) {
        mediaViewFlipper?.displayedChild =
            mediaViewFlipper?.indexOfChild(tabViews[newActiveTab]) ?: 0
        activeTab = newActiveTab
    }

    fun sendEmojiKeyPress(emojiKeyData: EmojiKeyData) {
        val ic = florisboard.currentInputConnection
        ic?.finishComposingText()
        ic?.commitText(emojiKeyData.getCodePointsAsString(), 1)
    }

    enum class Tab {
        EMOJIS,
        EMOTICONS,
        GIFS,
        HOME,
        STICKERS
    }
}
