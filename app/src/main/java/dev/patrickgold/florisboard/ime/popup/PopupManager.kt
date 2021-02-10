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

package dev.patrickgold.florisboard.ime.popup

import android.content.res.Configuration
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat.getDrawable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyData
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyView
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyboardView
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyHintMode
import dev.patrickgold.florisboard.ime.text.key.KeyView
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView

/**
 * Manages the creation and dismissal of key popups as well as the checks if the pointer moved
 * out of the popup bound (only for extended popups).
 *
 * @property keyboardView Reference to the keyboard view to which this manager class belongs to.
 */
class PopupManager<T_KBD: View, T_KV: View>(
    private val keyboardView: T_KBD,
    private val popupLayerView: PopupLayerView?
) {
    private var anchorLeft: Boolean = false
    private var anchorRight: Boolean = false
    private var anchorOffset: Int = 0
    private val exceptionsForKeyCodes = listOf(
        KeyCode.ENTER,
        KeyCode.LANGUAGE_SWITCH,
        KeyCode.SWITCH_TO_TEXT_CONTEXT,
        KeyCode.SWITCH_TO_MEDIA_CONTEXT
    )
    private var keyPopupWidth: Int
    private var keyPopupHeight: Int
    var keyPopupTextSize: Float = keyboardView.resources.getDimension(R.dimen.key_popup_textSize)
    private var keyPopupDiffX: Int = 0
    private val popupView: PopupView
    private val popupViewExt: PopupExtendedView
    private var row0count: Int = 0
    private var row1count: Int = 0

    /** Is true if the preview popup is visible to the user, else false */
    val isShowingPopup: Boolean
        get() = popupView.isShowing
    /** Is true if the extended popup is visible to the user, else false */
    val isShowingExtendedPopup: Boolean
        get() = popupViewExt.isShowing

    companion object {
        const val POPUP_EXTENSION_PATH_REL: String = "ime/text/characters/extended_popups"
    }

    init {
        keyPopupWidth = keyboardView.resources.getDimension(R.dimen.key_width).toInt()
        keyPopupHeight = keyboardView.resources.getDimension(R.dimen.key_height).toInt()
        popupView = PopupView(keyboardView.context)
        popupViewExt = PopupExtendedView(keyboardView.context)
        popupLayerView?.addView(popupView)
        popupLayerView?.addView(popupViewExt)
    }

    /**
     * Helper function to create a element for the extended popup and preconfigure it.
     *
     * @param keyView Reference to the keyView currently controlling the popup.
     * @param adjustedIndex The index of the key in the key data popup array.
     * @return A preconfigured extended popup element.
     */
    private fun createElement(
        keyView: T_KV,
        adjustedIndex: Int
    ): PopupExtendedView.Element {
        return when (keyView) {
            is KeyView -> {
                when (keyView.data.popup[adjustedIndex].code) {
                    KeyCode.SETTINGS -> {
                        getDrawable(keyView.context, R.drawable.ic_settings)?.let {
                            PopupExtendedView.Element.Icon(it, adjustedIndex)
                        } ?: PopupExtendedView.Element.Undefined
                    }
                    KeyCode.SWITCH_TO_TEXT_CONTEXT -> {
                        PopupExtendedView.Element.Label(
                            keyView.resources.getString(R.string.key__view_characters), adjustedIndex
                        )
                    }
                    KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                        getDrawable(keyView.context, R.drawable.ic_sentiment_satisfied)?.let {
                            PopupExtendedView.Element.Icon(it, adjustedIndex)
                        } ?: PopupExtendedView.Element.Undefined
                    }
                    KeyCode.URI_COMPONENT_TLD -> {
                        PopupExtendedView.Element.Tld(
                            keyView.data.popup[adjustedIndex].label, adjustedIndex
                        )
                    }
                    KeyCode.TOGGLE_ONE_HANDED_MODE_LEFT,
                    KeyCode.TOGGLE_ONE_HANDED_MODE_RIGHT -> {
                        getDrawable(keyView.context, R.drawable.ic_smartphone)?.let {
                            PopupExtendedView.Element.Icon(it, adjustedIndex)
                        } ?: PopupExtendedView.Element.Undefined
                    }
                    else -> {
                        PopupExtendedView.Element.Label(
                            keyView.getComputedLetter(keyView.data.popup[adjustedIndex]), adjustedIndex
                        )
                    }
                }
            }
            is EmojiKeyView -> {
                PopupExtendedView.Element.Label(
                    keyView.data.popup[adjustedIndex].getCodePointsAsString(), adjustedIndex
                )
            }
            else -> {
                PopupExtendedView.Element.Undefined
            }
        }
    }

    /**
     * Calculates all attributes required by both the normal and the extended popup, regardless of
     * the passed [keyView]'s code.
     */
    private fun calc(keyView: T_KV) {
        if (keyboardView is KeyboardView) {
            when (keyboardView.resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    if (keyboardView.isSmartbarKeyboardView) {
                        keyPopupWidth = (keyView.measuredWidth * 0.6f).toInt()
                        keyPopupHeight = (keyboardView.desiredKeyHeight * 3.0f * 1.2f).toInt()
                    } else {
                        keyPopupWidth = (keyboardView.desiredKeyWidth * 0.6f).toInt()
                        keyPopupHeight = (keyboardView.desiredKeyHeight * 3.0f).toInt()
                    }
                }
                else -> {
                    if (keyboardView.isSmartbarKeyboardView) {
                        keyPopupWidth = (keyView.measuredWidth * 1.1f).toInt()
                        keyPopupHeight = (keyboardView.desiredKeyHeight * 2.5f * 1.2f).toInt()
                    } else {
                        keyPopupWidth = (keyboardView.desiredKeyWidth * 1.1f).toInt()
                        keyPopupHeight = (keyboardView.desiredKeyHeight * 2.5f).toInt()
                    }
                }
            }
        } else if (keyboardView is EmojiKeyboardView) {
            keyPopupWidth = keyView.measuredWidth
            keyPopupHeight = (keyView.measuredHeight * 2.5f).toInt()
        }
        keyPopupDiffX = (keyView.measuredWidth - keyPopupWidth) / 2
    }

    /**
     * Shows a preview popup for the passed [keyView]. Ignores show requests for key views which
     * key code is equal to or less than [KeyCode.SPACE].
     *
     * @param keyView Reference to the keyView currently controlling the popup.
     */
    fun show(keyView: T_KV, keyHintMode: KeyHintMode) {
        if (keyView is KeyView && keyView.data.code <= KeyCode.SPACE) {
            return
        }

        calc(keyView)

        popupView.properties.apply {
            width = keyPopupWidth
            height = keyPopupHeight
            xOffset = keyPopupDiffX
            yOffset = -keyPopupHeight
            innerLabelFactor = 0.4f
            label = when (keyView) {
                is KeyView -> keyView.getComputedLetter()
                is EmojiKeyView -> keyView.data.getCodePointsAsString()
                else -> ""
            }
            labelTextSize = keyPopupTextSize
            shouldIndicateExtendedPopups = when (keyView) {
                is KeyView -> keyView.data.popup.size(keyHintMode) > 0
                is EmojiKeyView -> keyView.data.popup.isNotEmpty()
                else -> false
            }
        }
        popupView.show(keyView)
    }

    /**
     * Extends the currently showing key preview popup if there are popup keys defined in the
     * key data of the passed [keyView]. Ignores extend requests for key views which key code
     * is equal to or less than [KeyCode.SPACE]. An exception is made for the codes defined in
     * [exceptionsForKeyCodes], as they most likely have special keys bound to them.
     *
     * Layout of the extended key popup: (n = keyView.data.popup.size)
     *   when n <= 5: single line, row0 only
     *     _ _ _ _ _
     *     K K K K K
     *   when n > 5 && n % 2 == 1: multi line, row0 has 1 more key than row1, empty space position
     *     is depending on the current anchor
     *     anchorLeft           anchorRight
     *     K K ... K _         _ K ... K K
     *     K K ... K K         K K ... K K
     *   when n > 5 && n % 2 == 0: multi line, both same length
     *     K K ... K K
     *     K K ... K K
     *
     * @param keyView Reference to the keyView currently controlling the popup.
     */
    fun extend(keyView: T_KV, keyHintMode: KeyHintMode) {
        if (keyView is KeyView && keyView.data.code <= KeyCode.SPACE
            && !exceptionsForKeyCodes.contains(keyView.data.code)) {
            return
        }

        if (!isShowingPopup) {
            calc(keyView)
        }

        // Anchor left if keyView is in left half of keyboardView, else anchor right
        anchorLeft = keyView.x < keyboardView.measuredWidth / 2
        anchorRight = !anchorLeft

        // Determine key counts for each row
        val n = when (keyView) {
            is KeyView -> keyView.data.popup.size(keyHintMode)
            is EmojiKeyView -> keyView.data.popup.size
            else -> 0
        }
        when {
            n <= 0 -> return
            n <= 5 -> {
                row1count = 0
                row0count = n
            }
            n > 5 && n % 2 == 1 -> {
                row1count = (n - 1) / 2
                row0count = (n + 1) / 2
            }
            else -> {
                row1count = n / 2
                row0count = n / 2
            }
        }

        // Calculate anchor offset (always positive int, direction depends on anchorLeft and
        // anchorRight state)
        anchorOffset = when {
            row0count <= 1 -> 0
            else -> {
                var offset = when {
                    row0count % 2 == 1 -> (row0count - 1) / 2
                    row0count % 2 == 0 -> (row0count / 2) - 1
                    else -> 0
                }
                val availableSpace = when {
                    anchorLeft -> keyView.x.toInt() + keyPopupDiffX
                    anchorRight -> keyboardView.measuredWidth -
                            (keyView.x.toInt() + keyPopupDiffX + keyPopupWidth)
                    else -> 0
                }
                while (offset > 0) {
                    if (availableSpace >= offset * keyPopupWidth) {
                        break
                    } else {
                        offset -= 1
                    }
                }
                offset
            }
        }

        // Build UI
        popupViewExt.properties.elements.clear()
        val initUiIndex = when {
            anchorLeft -> anchorOffset + row1count
            anchorRight -> row0count - 1 - anchorOffset + row1count
            else -> 0
        }
        val popupIndices: IntArray
        val uiIndices = IntRange(0, (n - 1).coerceAtLeast(0))
        if (keyView is KeyView) {
            popupIndices = IntArray(n) { 0 }
            when (keyHintMode) {
                KeyHintMode.ENABLED_ACCENT_PRIORITY -> when {
                    keyView.data.popup.main != null -> {
                        popupIndices[initUiIndex] = PopupSet.MAIN_INDEX
                        if (keyView.data.popup.hint != null) when {
                            initUiIndex + 1 < n -> popupIndices[initUiIndex + 1] = PopupSet.HINT_INDEX
                            initUiIndex - 1 >= 0 -> popupIndices[initUiIndex - 1] = PopupSet.HINT_INDEX
                        }
                    }
                    keyView.data.popup.hint != null -> when {
                        initUiIndex + 1 < n -> popupIndices[initUiIndex + 1] = PopupSet.HINT_INDEX
                        initUiIndex - 1 >= 0 -> popupIndices[initUiIndex - 1] = PopupSet.HINT_INDEX
                        else -> popupIndices[initUiIndex] = PopupSet.HINT_INDEX
                    }
                }
                KeyHintMode.ENABLED_HINT_PRIORITY -> when {
                    keyView.data.popup.hint != null -> {
                        popupIndices[initUiIndex] = PopupSet.HINT_INDEX
                        if (keyView.data.popup.main != null) when {
                            initUiIndex + 1 < n -> popupIndices[initUiIndex + 1] = PopupSet.MAIN_INDEX
                            initUiIndex - 1 >= 0 -> popupIndices[initUiIndex - 1] = PopupSet.MAIN_INDEX
                        }
                    }
                    keyView.data.popup.main != null -> popupIndices[initUiIndex] = PopupSet.MAIN_INDEX
                }
                KeyHintMode.ENABLED_SMART_PRIORITY -> when {
                    keyView.data.popup.main != null -> {
                        popupIndices[initUiIndex] = PopupSet.MAIN_INDEX
                        if (keyView.data.popup.hint != null) when {
                            initUiIndex + 1 < n -> popupIndices[initUiIndex + 1] = PopupSet.HINT_INDEX
                            initUiIndex - 1 >= 0 -> popupIndices[initUiIndex - 1] = PopupSet.HINT_INDEX
                        }
                    }
                    keyView.data.popup.hint != null -> popupIndices[initUiIndex] = PopupSet.HINT_INDEX
                }
                KeyHintMode.DISABLED -> when {
                    keyView.data.popup.main != null -> popupIndices[initUiIndex] = PopupSet.MAIN_INDEX
                }
            }
            var offset = 0
            for (uiIndex in uiIndices) {
                if (popupIndices[uiIndex] < 0) {
                    offset++
                } else {
                    popupIndices[uiIndex] = uiIndex - offset
                }
            }
        } else {
            popupIndices = IntArray(n) { it }
        }
        if (row1count > 0) {
            popupViewExt.properties.elements.add(mutableListOf())
        }
        popupViewExt.properties.elements.add(mutableListOf())
        for (uiIndex in uiIndices) {
            val rowIndex = if (uiIndex < row1count && row1count > 0) { 1 } else { 0 }
            popupViewExt.properties.elements[rowIndex].add(
                createElement(keyView, popupIndices[uiIndex])
            )
        }

        // Calculate layout params
        val extWidth = row0count * keyPopupWidth
        val extHeight = when {
            row1count > 0 -> keyPopupHeight * 0.4f * 2.0f
            else -> keyPopupHeight * 0.4f
        }.toInt()
        val x = ((keyView.measuredWidth - keyPopupWidth) / 2) + when {
            anchorLeft -> -anchorOffset * keyPopupWidth
            anchorRight -> -extWidth + keyPopupWidth + anchorOffset * keyPopupWidth
            else -> 0
        }
        val y = -keyPopupHeight - when {
            row1count > 0 -> (keyPopupHeight * 0.4f).toInt()
            else -> 0
        }

        popupViewExt.properties.apply {
            width = extWidth
            height = extHeight
            xOffset = x
            yOffset = y
            gravity = if (anchorLeft) { Gravity.START } else { Gravity.END }
            labelTextSize = keyPopupTextSize
            activeElementIndex = initUiIndex
        }
        popupViewExt.show(keyView)

        popupView.properties.shouldIndicateExtendedPopups = false
        popupView.invalidate()
    }

    /**
     * Updates the current selected key in extended popup according to the passed [event].
     * This function does nothing if the extended popup is not showing and will return false.
     *
     * @param keyView Reference to the keyView currently controlling the popup.
     * @param event The [MotionEvent] passed from the parent keyboard view's onTouch event.
     * @return True if the pointer movement is within the elements bounds, false otherwise.
     */
    fun propagateMotionEvent(keyView: T_KV, event: MotionEvent): Boolean {
        if (!isShowingExtendedPopup) {
            return false
        }

        val kX: Float = event.x / keyPopupWidth.toFloat()

        // Check if out of boundary on y-axis
        if (event.y < -keyPopupHeight || event.y > 0.9f * keyPopupHeight) {
            return false
        }

        popupViewExt.properties.activeElementIndex = when {
            anchorLeft -> when {
                // check if out of boundary on x-axis
                event.x < keyPopupDiffX - (anchorOffset + 1) * keyPopupWidth ||
                event.x > (keyPopupDiffX + (row0count + 1 - anchorOffset) * keyPopupWidth) -> {
                    return false
                }
                // row 1
                event.y < 0 && row1count > 0 -> when {
                    kX >= row1count - anchorOffset -> row1count - 1
                    kX < -anchorOffset -> 0
                    kX < 0 -> kX.toInt() - 1 + anchorOffset
                    else -> kX.toInt() + anchorOffset
                }
                // row 0
                else -> when {
                    kX >= row0count - anchorOffset -> row1count + row0count - 1
                    kX < -anchorOffset -> row1count
                    kX < 0 -> row1count + kX.toInt() - 1 + anchorOffset
                    else -> row1count + kX.toInt() + anchorOffset
                }
            }
            anchorRight -> when {
                // check if out of boundary on x-axis
                event.x > keyView.measuredWidth - keyPopupDiffX + (anchorOffset + 1) * keyPopupWidth ||
                event.x < (keyView.measuredWidth -
                        keyPopupDiffX - (row0count + 1 - anchorOffset) * keyPopupWidth) -> {
                    return false
                }
                // row 1
                event.y < 0 && row1count > 0 -> when {
                    kX >= anchorOffset -> row1count - 1
                    kX < -(row1count - 1 - anchorOffset) -> 0
                    kX < 0 -> row1count - 2 + kX.toInt() - anchorOffset
                    else -> row1count - 1 + kX.toInt() - anchorOffset
                }
                // row 0
                else -> when {
                    kX >= anchorOffset -> row1count + row0count - 1
                    kX < -(row0count - 1 - anchorOffset) -> row1count
                    kX < 0 -> row1count + row0count - 2 + kX.toInt() - anchorOffset
                    else -> row1count + row0count - 1 + kX.toInt() - anchorOffset
                }
            }
            else -> -1
        }
        popupViewExt.invalidate()

        return true
    }

    /**
     * Gets the [KeyData] of the currently active key. May be either the key of the popup preview
     * or one of the keys in extended popup, if shown. Returns null if type parameter [T_KV]
     * is not [KeyView].
     *
     * @param keyView Reference to the keyView currently controlling the popup.
     * @return The [KeyData] object of the currently active key or null.
     */
    fun getActiveKeyData(keyView: T_KV): KeyData? {
        return if (keyView is KeyView) {
            val element = popupViewExt.properties.getElementOrNull()
            if (element != null) {
                keyView.data.popup.getOrNull(element.adjustedIndex) ?: keyView.data
            } else {
                keyView.data
            }
        } else {
            null
        }
    }

    /**
     * Gets the [EmojiKeyData] of the currently active key. May be either the key of the popup
     * preview or one of the keys in extended popup, if shown. Returns null if type parameter
     * [T_KV] is not [EmojiKeyView].
     *
     * @param keyView Reference to the keyView currently controlling the popup.
     * @return The [EmojiKeyData] object of the currently active key or null.
     */
    fun getActiveEmojiKeyData(keyView: T_KV): EmojiKeyData? {
        return if (keyView is EmojiKeyView) {
            val element = popupViewExt.properties.getElementOrNull()
            if (element != null) {
                keyView.data.popup.getOrNull(element.adjustedIndex) ?: keyView.data
            } else {
                keyView.data
            }
        } else {
            null
        }
    }

    /**
     * Hides the key preview popup as well as the extended popup.
     */
    fun hide() {
        popupView.hide()
        popupViewExt.hide()
        popupViewExt.properties.activeElementIndex = -1
    }

    /**
     * Dismisses all currently shown popups. Should be called by the parent keyboard view when it
     * is closing.
     */
    fun dismissAllPopups() {
        popupView.hide()
        popupLayerView?.removeView(popupView)
        popupViewExt.hide()
        popupViewExt.properties.activeElementIndex = -1
        popupLayerView?.removeView(popupViewExt)
    }
}
