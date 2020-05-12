package dev.patrickgold.florisboard.ime.media

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.children
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.R
import java.util.*

enum class MediaInputTab {
    EMOJIS,
    EMOTICONS,
    GIFS,
    OVERVIEW,
    STICKERS
}

/**
 * MediaInputManager is responsible for managing everything which is related to media input. All of
 * the following count as media input: emojis, gifs, sticker, emoticons.
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

    private var activeTab: MediaInputTab? = null
    private var frameLayout: FrameLayout? = null
    private var mediaViewGroup: LinearLayout? = null
    private val tabViews = EnumMap<MediaInputTab, LinearLayout>(MediaInputTab::class.java)

    override fun onCreateInputView() {
        val rootViewGroup = florisboard.rootViewGroup ?: return

        // Init bottom buttons
        val bottom = rootViewGroup.findViewById<ViewGroup>(R.id.media_input_bottom)
        for (button in bottom.children) {
            if (button is Button) {
                button.setOnClickListener { v -> onBottomButtonClick(v) }
            }
        }

        frameLayout = florisboard.rootViewGroup?.findViewById(R.id.media_input_tab_container)
        mediaViewGroup = florisboard.rootViewGroup?.findViewById(R.id.media_input)

        setActiveTab(MediaInputTab.OVERVIEW)
    }

    fun show() {
        mediaViewGroup?.visibility = View.VISIBLE
    }
    fun hide() {
        mediaViewGroup?.visibility = View.GONE
    }

    private fun onBottomButtonClick(v: View) {
        when (v.id) {
            R.id.media_input_bottom_switch_to_text_input_button -> {
                florisboard.setActiveInput(R.id.text_input)
            }
        }
    }

    fun createTabViewFor(tab: MediaInputTab): LinearLayout {
        return when (tab) {
            MediaInputTab.OVERVIEW -> {
                OverviewTab(florisboard)
            }
            else -> {
                LinearLayout(florisboard)
            }
        }
    }

    fun setActiveTab(newActiveTab: MediaInputTab) {
        if (!tabViews.containsKey(newActiveTab)) {
            val tabView = createTabViewFor(newActiveTab)
            tabViews[newActiveTab] = tabView
            frameLayout?.addView(tabView)
        }
        tabViews[activeTab]?.visibility = View.GONE
        tabViews[newActiveTab]?.visibility = View.VISIBLE
        activeTab = newActiveTab
    }
}
