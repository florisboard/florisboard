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

package dev.patrickgold.florisboard.ime.popup

import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import dev.patrickgold.florisboard.common.FlorisRect
import dev.patrickgold.florisboard.common.toIntOffset
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKey
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyHintConfiguration
import dev.patrickgold.florisboard.ime.text.keyboard.TextKey
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData

@Composable
fun rememberPopupUiController(boundsProvider: (key: Key) -> FlorisRect): PopupUiController {
    val context = LocalContext.current
    return remember { PopupUiController(context, boundsProvider) }
}

private val ExceptionsForKeyCodes = listOf(
    KeyCode.ENTER,
    KeyCode.LANGUAGE_SWITCH,
    KeyCode.IME_UI_MODE_TEXT,
    KeyCode.IME_UI_MODE_MEDIA,
    KeyCode.IME_UI_MODE_CLIPBOARD,
    KeyCode.KANA_SWITCHER,
    KeyCode.CHAR_WIDTH_SWITCHER,
)

class PopupUiController(
    val context: Context,
    val boundsProvider: (key: Key) -> FlorisRect,
) {
    private var renderPopupInfo by mutableStateOf<RenderPopupInfo?>(null)

    var fontSizeMultiplier: Float = 1.0f
    var keyHintConfiguration: KeyHintConfiguration = KeyHintConfiguration.HINTS_DISABLED

    /** Is true if the preview popup is visible to the user, else false */
    val isShowingPopup: Boolean
        get() = renderPopupInfo != null
    /** Is true if the extended popup is visible to the user, else false */
    val isShowingExtendedPopup: Boolean
        get() = false

    fun isSuitableForPopups(key: Key): Boolean {
        return isSuitableForBasicPopup(key) || isSuitableForExtendedPopup(key)
    }

    private fun isSuitableForBasicPopup(key: Key): Boolean {
        return if (key is TextKey) {
            val c = key.computedData.code
            c > KeyCode.SPACE && c != KeyCode.MULTIPLE_CODE_POINTS && c != KeyCode.CJK_SPACE
        } else {
            true
        }
    }

    private fun isSuitableForExtendedPopup(key: Key): Boolean {
        return if (key is TextKey) {
            val c = key.computedData.code
            c > KeyCode.SPACE && c != KeyCode.MULTIPLE_CODE_POINTS && c != KeyCode.CJK_SPACE || ExceptionsForKeyCodes.contains(c)
        } else {
            true
        }
    }

    /**
     * Shows a preview popup for the passed [key]. Ignores show requests for keys which
     * key code is equal to or less than [KeyCode.SPACE].
     *
     * @param key Reference to the key currently controlling the popup.
     */
    fun show(key: Key) {
        if (!isSuitableForBasicPopup(key)) return

        renderPopupInfo = RenderPopupInfo(
            key = key,
            bounds = boundsProvider(key),
            shouldIndicateExtendedPopups = when (key) {
                is TextKey -> key.computedPopups.getPopupKeys(keyHintConfiguration).isNotEmpty()
                is EmojiKey -> key.computedPopups.getPopupKeys(keyHintConfiguration).isNotEmpty()
                else -> false
            },
        )
    }

    /**
     * Extends the currently showing key preview popup if there are popup keys defined in the
     * key data of the passed [key]. Ignores extend requests for key views which key code
     * is equal to or less than [KeyCode.SPACE]. An exception is made for the codes defined in
     * [ExceptionsForKeyCodes], as they most likely have special keys bound to them.
     *
     * Layout of the extended key popup: (n = key.computedPopups.size)
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
     * @param key Reference to the key currently controlling the popup.
     */
    fun extend(key: Key) {
        //
    }

    /**
     * Updates the current selected key in extended popup according to the passed [event].
     * This function does nothing if the extended popup is not showing and will return false.
     *
     * @param key Reference to the key currently controlling the popup.
     * @param event The [MotionEvent] passed from the keyboard view's onTouch event.
     * @return True if the pointer movement is within the elements bounds, false otherwise.
     */
    fun propagateMotionEvent(key: Key, event: MotionEvent, pointerIndex: Int): Boolean {
        return false
    }

    /**
     * Gets the [TextKeyData] of the currently active key. May be either the key of the popup preview
     * or one of the keys in extended popup, if shown. Returns null if [key] is not a subclass of [TextKey].
     *
     * @param key Reference to the key currently controlling the popup.
     *
     * @return The [TextKeyData] object of the currently active key or null.
     */
    fun getActiveKeyData(key: Key): KeyData? {
        return (key as? TextKey)?.computedData
    }

    fun hide() {
        renderPopupInfo = null
    }

    @Composable
    fun RenderPopups(): Unit = with(LocalDensity.current) {
        renderPopupInfo?.let { popupInfo ->
            PopupBox(
                modifier = Modifier
                    .requiredSize(popupInfo.bounds.size.toDpSize())
                    .absoluteOffset { popupInfo.bounds.topLeft.toIntOffset() },
                key = popupInfo.key,
                fontSizeMultiplier = fontSizeMultiplier,
                shouldIndicateExtendedPopups = popupInfo.shouldIndicateExtendedPopups,
            )
        }
    }

    data class RenderPopupInfo(
        val key: Key,
        val bounds: FlorisRect,
        val shouldIndicateExtendedPopups: Boolean,
    )
}
