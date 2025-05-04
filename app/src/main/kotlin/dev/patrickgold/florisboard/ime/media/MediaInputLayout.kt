/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import dev.patrickgold.florisboard.ime.input.InputEventDispatcher
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.media.emoji.EmojiData
import dev.patrickgold.florisboard.ime.media.emoji.EmojiPaletteView
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggRow

@SuppressLint("MutableCollectionMutableState")
@Composable
fun MediaInputLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    var emojiLayoutDataMap by remember { mutableStateOf(EmojiData.Fallback) }
    LaunchedEffect(Unit) {
        emojiLayoutDataMap = EmojiData.get(context, "ime/media/emoji/root.txt")
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        SnyggColumn(
            elementName = FlorisImeUi.Media.elementName,
            modifier = modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.imeUiHeight()),
        ) {
            EmojiPaletteView(
                modifier = Modifier.weight(1f),
                fullEmojiMappings = emojiLayoutDataMap,
            )
            SnyggRow(
                elementName = FlorisImeUi.MediaBottomRow.elementName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FlorisImeSizing.keyboardRowBaseHeight * 0.8f),
            ) {
                KeyboardLikeButton(
                    elementName = FlorisImeUi.MediaBottomRowButton.elementName,
                    inputEventDispatcher = keyboardManager.inputEventDispatcher,
                    keyData = TextKeyData.IME_UI_MODE_TEXT,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Text(
                        text = "ABC",
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                KeyboardLikeButton(
                    elementName = FlorisImeUi.MediaBottomRowButton.elementName,
                    inputEventDispatcher = keyboardManager.inputEventDispatcher,
                    keyData = TextKeyData.DELETE,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.Backspace, contentDescription = null)
                }
            }
        }
    }
}

@Composable
internal fun KeyboardLikeButton(
    modifier: Modifier = Modifier,
    inputEventDispatcher: InputEventDispatcher,
    keyData: KeyData,
    elementName: String = FlorisImeUi.MediaEmojiKey.elementName,
    content: @Composable () -> Unit,
) {
    val inputFeedbackController = LocalInputFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val selector = if (isPressed) {
        SnyggSelector.PRESSED
    } else {
        SnyggSelector.NONE
    }

    SnyggBox(
        elementName = elementName,
        attributes = mapOf(FlorisImeUi.Attr.Code to keyData.code),
        selector = selector,
        clickAndSemanticsModifier = modifier
            .indication(interactionSource, ripple())
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false).also {
                        if (it.pressed != it.previousPressed) it.consume()
                    }
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    inputEventDispatcher.sendDown(keyData)
                    inputFeedbackController.keyPress(keyData)
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        interactionSource.tryEmit(PressInteraction.Release(press))
                        inputEventDispatcher.sendUp(keyData)
                    } else {
                        interactionSource.tryEmit(PressInteraction.Cancel(press))
                        inputEventDispatcher.sendCancel(keyData)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
