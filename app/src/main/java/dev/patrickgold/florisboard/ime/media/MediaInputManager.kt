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

    /**
     * Called when a new input view has been registered. Used to initialize all media-relevant
     * views and layouts.
     * TODO: evaluate if the view initializing process can be optimized.
     */
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

    /**
     * Clean-up of resources and stopping all coroutines.
     */
    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i(this::class.simpleName, "onDestroy()")

        val emojiKeyboardView = tabViews[Tab.EMOJIS]
        if (emojiKeyboardView is EmojiKeyboardView) {
            emojiKeyboardView.onDestroy()
        }
        cancel()
        instance = null
    }

    /**
     * Handles clicks on the bottom buttons.
     */
    private fun onBottomButtonClick(v: View) {
        val keyData = when (v.id) {
            R.id.media_input_switch_to_text_input_button -> {
                val keyData = KeyData(KeyCode.SWITCH_TO_TEXT_CONTEXT)
                florisboard.setActiveInput(R.id.text_input)
                keyData
            }
            R.id.media_input_backspace_button -> {
                val keyData = KeyData(KeyCode.DELETE, type = KeyType.ENTER_EDITING)
                florisboard.textInputManager.sendKeyPress(keyData)
                keyData
            }
            else -> null
        }
        florisboard.keyPressVibrate()
        florisboard.keyPressSound(keyData)
    }

    /**
     * Creates and returns a tab view for the given [tab].
     */
    private fun createTabViewFor(tab: Tab): LinearLayout {
        return when (tab) {
            Tab.HOME -> HomeView(florisboard)
            Tab.EMOJIS -> EmojiKeyboardView(florisboard)
            else -> LinearLayout(florisboard)
        }
    }

    /**
     * Sets the actively shown tab.
     */
    fun setActiveTab(newActiveTab: Tab) {
        mediaViewFlipper?.displayedChild =
            mediaViewFlipper?.indexOfChild(tabViews[newActiveTab]) ?: 0
        activeTab = newActiveTab
    }

    /**
     * Sends a given [emojiKeyData] to the current input editor.
     */
    fun sendEmojiKeyPress(emojiKeyData: EmojiKeyData) {
        val ic = florisboard.currentInputConnection
        ic?.finishComposingText()
        ic?.commitText(emojiKeyData.getCodePointsAsString(), 1)
    }

    /**
     * Enum which defines the tabs for the media context.
     * TODO: evaluate if GIFs and stickers may become irrelevant and should be removed.
     * TODO: add kaomoji to the list.
     */
    enum class Tab {
        EMOJIS,
        EMOTICONS,
        GIFS,
        HOME,
        STICKERS
    }
}
