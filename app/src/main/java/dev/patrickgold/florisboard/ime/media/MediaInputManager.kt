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

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.google.android.material.tabs.TabLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.EditorInstance
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.core.InputView
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyData
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyboardView
import dev.patrickgold.florisboard.ime.media.emoticon.EmoticonKeyData
import dev.patrickgold.florisboard.ime.media.emoticon.EmoticonKeyboardView
import dev.patrickgold.florisboard.ime.text.key.KeyData
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

/**
 * MediaInputManager is responsible for managing everything which is related to media input. All of
 * the following count as media input: emojis, emoticons, kaomoji.
 *
 * All of the UI for the different media tabs are kept under the same container element and
 * are separated from text-related UI.
 *
 * All events defined in [FlorisBoard.EventListener] will be passed through to this class by the
 * core.
 */
class MediaInputManager private constructor() : CoroutineScope by MainScope(),
    FlorisBoard.EventListener {

    private val florisboard = FlorisBoard.getInstance()
    private val activeEditorInstance: EditorInstance
        get() = florisboard.activeEditorInstance

    private var activeTab: Tab? = null
    private var mediaViewFlipper: ViewFlipper? = null
    private var tabLayout: TabLayout? = null
    private val tabViews = EnumMap<Tab, LinearLayout>(Tab::class.java)

    private var mediaViewGroup: LinearLayout? = null

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

    init {
        florisboard.addEventListener(this)
    }

    /**
     * Called when a new input view has been registered. Used to initialize all media-relevant
     * views and layouts.
     * TODO: evaluate if the view initializing process can be optimized.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onRegisterInputView(inputView: InputView) {
        Timber.i("onRegisterInputView(inputView)")

        launch(Dispatchers.Default) {
            mediaViewGroup = inputView.findViewById(R.id.media_input)
            mediaViewFlipper = inputView.findViewById(R.id.media_input_view_flipper)

            // Init bottom buttons
            inputView.findViewById<Button>(R.id.media_input_switch_to_text_input_button)
                .setOnTouchListener { view, event -> onBottomButtonEvent(view, event) }
            inputView.findViewById<ImageButton>(R.id.media_input_backspace_button)
                .setOnTouchListener { view, event -> onBottomButtonEvent(view, event) }

            tabLayout = inputView.findViewById(R.id.media_input_tabs)
            tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> setActiveTab(Tab.EMOJI)
                        1 -> setActiveTab(Tab.EMOTICON)
                        2 -> setActiveTab(Tab.KAOMOJI)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            withContext(Dispatchers.Main) {
                for (tab in Tab.values()) {
                    val tabView = createTabViewFor(tab)
                    tabViews[tab] = tabView
                    mediaViewFlipper?.addView(tabView)
                }
                tabLayout?.selectTab(tabLayout?.getTabAt(0))
            }
        }
    }

    /**
     * Clean-up of resources and stopping all coroutines.
     */
    override fun onDestroy() {
        Timber.i("onDestroy()")

        cancel()
        instance = null
    }

    /**
     * Handles clicks on the bottom buttons.
     */
    private fun onBottomButtonEvent(view: View, event: MotionEvent?): Boolean {
        event ?: return false
        val data = when (view.id) {
            R.id.media_input_switch_to_text_input_button -> KeyData.SWITCH_TO_TEXT_CONTEXT
            R.id.media_input_backspace_button -> KeyData.DELETE
            else -> return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                florisboard.keyPressVibrate()
                florisboard.keyPressSound(data)
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.down(data))
            }
            MotionEvent.ACTION_UP -> {
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.up(data))
            }
            MotionEvent.ACTION_CANCEL -> {
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(data))
            }
        }
        // MUST return false here so the background selector for showing a transparent bg works
        return false
    }

    /**
     * Creates and returns a tab view for the given [tab].
     */
    private fun createTabViewFor(tab: Tab): LinearLayout {
        return when (tab) {
            Tab.EMOJI -> EmojiKeyboardView(florisboard)
            Tab.EMOTICON -> EmoticonKeyboardView(florisboard.context)
            else -> LinearLayout(florisboard.context).apply {
                addView(TextView(context).apply {
                    text = "not yet implemented"
                })
            }
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
        activeEditorInstance.commitText(emojiKeyData.getCodePointsAsString())
    }

    /**
     * Sends a given [emoticonKeyData] to the current input editor.
     */
    fun sendEmoticonKeyPress(emoticonKeyData: EmoticonKeyData) {
        activeEditorInstance.commitText(emoticonKeyData.icon)
    }

    /**
     * Enum which defines the tabs for the media context.
     */
    enum class Tab {
        EMOJI,
        EMOTICON,
        KAOMOJI
    }
}
