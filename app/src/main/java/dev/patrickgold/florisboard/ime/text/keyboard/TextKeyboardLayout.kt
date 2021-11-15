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

package dev.patrickgold.florisboard.ime.text.keyboard

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.common.isOrientationPortrait
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.common.observeAsTransformingState
import dev.patrickgold.florisboard.common.toIntOffset
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.ImeOptions
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.keyboard.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyHintConfiguration
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.snygg.ui.SnyggSurface
import dev.patrickgold.florisboard.snygg.ui.solidColor
import dev.patrickgold.florisboard.snygg.ui.spSize
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState

// TODO clean up code style
@Composable
fun TextKeyboardLayout(
    modifier: Modifier = Modifier,
) = with(LocalDensity.current) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val prefs by florisPreferenceModel()
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val activeState by keyboardManager.activeState.observeAsNonNullState()
    val activeKeyboard by keyboardManager.activeKeyboard.observeAsNonNullState()
    val inputFeedbackController = LocalInputFeedbackController.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.keyboardRowBaseHeight * activeKeyboard.rowCount),
    ) {
        if (constraints.isZero) {
            return@BoxWithConstraints
        }
        val keyMarginH by prefs.keyboard.keySpacingHorizontal.observeAsTransformingState { it.dp.toPx() }
        val keyMarginV by prefs.keyboard.keySpacingVertical.observeAsTransformingState { it.dp.toPx() }
        val keyHintConfiguration = prefs.keyboard.keyHintConfiguration()
        val keyboardWidth = constraints.maxWidth.toFloat()
        val keyboardHeight = constraints.maxHeight.toFloat()
        val desiredKey = remember { TextKey(data = TextKeyData.UNSPECIFIED) }
        desiredKey.touchBounds.apply {
            width = keyboardWidth / 10.0f
            height = keyboardHeight / activeKeyboard.rowCount.coerceAtLeast(1).toFloat()
        }
        desiredKey.visibleBounds.applyFrom(desiredKey.touchBounds).deflateBy(keyMarginH, keyMarginV)
        TextKeyboard.layoutDrawableBounds(desiredKey, 1.0f)
        TextKeyboard.layoutLabelBounds(desiredKey)
        for (key in activeKeyboard.keys()) {
            key.compute(keyboardManager.computingEvaluator)
            computeLabelsAndDrawables(key, activeKeyboard, activeState, keyHintConfiguration, subtypeManager)
        }
        activeKeyboard.layout(keyboardWidth, keyboardHeight, desiredKey)

        val oneHandedMode by prefs.keyboard.oneHandedMode.observeAsState()
        val oneHandedModeFactor by prefs.keyboard.oneHandedModeScaleFactor.observeAsTransformingState { it / 100.0f }
        val fontSizeMultiplierBase by if (configuration.isOrientationPortrait()) {
            prefs.keyboard.fontSizeMultiplierPortrait
        } else {
            prefs.keyboard.fontSizeMultiplierLandscape
        }.observeAsTransformingState { it / 100.0f }
        val fontSizeMultiplier = fontSizeMultiplierBase * if (oneHandedMode != OneHandedMode.OFF) {
            oneHandedModeFactor
        } else {
            1.0f
        }
        for (key in activeKeyboard.keys()) {
            TextKeyButton(key, fontSizeMultiplier)
        }
    }
}

@Composable
private fun TextKeyButton(key: TextKey, fontSizeMultiplier: Float) = with(LocalDensity.current) {
    val keyStyle = FlorisImeTheme.style.get(
        element = FlorisImeUi.Key,
        code = key.computedData.code,
    )
    val fontSize = keyStyle.fontSize.spSize() * fontSizeMultiplier * when (key.computedData.code) {
        KeyCode.VIEW_CHARACTERS,
        KeyCode.VIEW_SYMBOLS,
        KeyCode.VIEW_SYMBOLS2 -> 0.80f
        KeyCode.VIEW_NUMERIC,
        KeyCode.VIEW_NUMERIC_ADVANCED -> 0.55f
        else -> 1.0f
    }
    SnyggSurface(
        modifier = Modifier
            .requiredSize(key.visibleBounds.size.toDpSize())
            .absoluteOffset { key.visibleBounds.topLeft.toIntOffset() },
        background = keyStyle.background,
        shape = keyStyle.shape,
    ) {
        key.label?.let { label ->
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center),
                text = label,
                color = keyStyle.foreground.solidColor(),
                fontSize = fontSize,
                maxLines = 1,
                softWrap = false,
                overflow = when (key.computedData.code) {
                    KeyCode.SPACE -> TextOverflow.Ellipsis
                    else -> TextOverflow.Visible
                },
            )
        }
        key.foregroundDrawableId?.let { drawableId ->
            Icon(
                modifier = Modifier
                    .requiredSize(fontSize.toDp() * 1.1f)
                    .align(Alignment.Center),
                painter = painterResource(drawableId),
                contentDescription = null,
                tint = keyStyle.foreground.solidColor(),
            )
        }
    }
}

/**
 * Computes the labels and drawables needed to draw the key.
 */
@Composable
private fun computeLabelsAndDrawables(
    key: TextKey,
    activeKeyboard: TextKeyboard,
    activeState: KeyboardState,
    keyHintConfiguration: KeyHintConfiguration,
    subtypeManager: SubtypeManager,
): TextKey {
    // Reset attributes first to avoid invalid states if not updated
    key.label = null
    key.hintedLabel = null
    key.foregroundDrawableId = null

    val data = key.computedData
    if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE && data.code != KeyCode.CJK_SPACE
        && data.code != KeyCode.HALF_SPACE && data.code != KeyCode.KESHIDA || data.type == KeyType.NUMERIC
    ) {
        key.label = data.asString(isForDisplay = true)
        key.computedPopups.getPopupKeys(keyHintConfiguration).hint?.asString(isForDisplay = true).let {
            key.hintedLabel = it
        }
    } else {
        when (data.code) {
            KeyCode.ARROW_LEFT -> {
                key.foregroundDrawableId = R.drawable.ic_keyboard_arrow_left
            }
            KeyCode.ARROW_RIGHT -> {
                key.foregroundDrawableId = R.drawable.ic_keyboard_arrow_right
            }
            KeyCode.CLIPBOARD_COPY -> {
                key.foregroundDrawableId = R.drawable.ic_content_copy
            }
            KeyCode.CLIPBOARD_CUT -> {
                key.foregroundDrawableId = R.drawable.ic_content_cut
            }
            KeyCode.CLIPBOARD_PASTE -> {
                key.foregroundDrawableId = R.drawable.ic_content_paste
            }
            KeyCode.CLIPBOARD_SELECT_ALL -> {
                key.foregroundDrawableId = R.drawable.ic_select_all
            }
            KeyCode.DELETE -> {
                key.foregroundDrawableId = R.drawable.ic_backspace
            }
            KeyCode.ENTER -> {
                val imeOptions = activeState.imeOptions
                key.foregroundDrawableId = when (imeOptions.enterAction) {
                    ImeOptions.EnterAction.DONE -> R.drawable.ic_done
                    ImeOptions.EnterAction.GO -> R.drawable.ic_arrow_right_alt
                    ImeOptions.EnterAction.NEXT -> R.drawable.ic_arrow_right_alt
                    ImeOptions.EnterAction.NONE -> R.drawable.ic_keyboard_return
                    ImeOptions.EnterAction.PREVIOUS -> R.drawable.ic_arrow_right_alt
                    ImeOptions.EnterAction.SEARCH -> R.drawable.ic_search
                    ImeOptions.EnterAction.SEND -> R.drawable.ic_send
                    ImeOptions.EnterAction.UNSPECIFIED -> R.drawable.ic_keyboard_return
                }
                if (imeOptions.flagNoEnterAction) {
                    key.foregroundDrawableId = R.drawable.ic_keyboard_return
                }
            }
            KeyCode.LANGUAGE_SWITCH -> {
                key.foregroundDrawableId = R.drawable.ic_language
            }
            KeyCode.PHONE_PAUSE -> key.label = stringRes(R.string.key__phone_pause)
            KeyCode.PHONE_WAIT -> key.label = stringRes(R.string.key__phone_wait)
            KeyCode.SHIFT -> {
                key.foregroundDrawableId = when (activeState.caps) {
                    true -> R.drawable.ic_keyboard_capslock
                    else -> R.drawable.ic_keyboard_arrow_up
                }
            }
            KeyCode.SPACE, KeyCode.CJK_SPACE -> {
                when (activeKeyboard.mode) {
                    KeyboardMode.NUMERIC,
                    KeyboardMode.NUMERIC_ADVANCED,
                    KeyboardMode.PHONE,
                    KeyboardMode.PHONE2 -> {
                        key.foregroundDrawableId = R.drawable.ic_space_bar
                    }
                    KeyboardMode.CHARACTERS -> {
                        key.label = subtypeManager.activeSubtype().primaryLocale.let { it.displayName() }
                    }
                    else -> {
                    }
                }
            }
            KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                key.foregroundDrawableId = R.drawable.ic_sentiment_satisfied
            }
            KeyCode.SWITCH_TO_CLIPBOARD_CONTEXT -> {
                key.foregroundDrawableId = R.drawable.ic_assignment
            }
            KeyCode.KANA_SWITCHER -> {
                key.foregroundDrawableId = if (activeState.isKanaKata) {
                    R.drawable.ic_keyboard_kana_switcher_kata
                } else {
                    R.drawable.ic_keyboard_kana_switcher_hira
                }
            }
            KeyCode.CHAR_WIDTH_SWITCHER -> {
                key.foregroundDrawableId = if (activeState.isCharHalfWidth) {
                    R.drawable.ic_keyboard_char_width_switcher_full
                } else {
                    R.drawable.ic_keyboard_char_width_switcher_half
                }
            }
            KeyCode.CHAR_WIDTH_FULL -> {
                key.foregroundDrawableId = R.drawable.ic_keyboard_char_width_switcher_full
            }
            KeyCode.CHAR_WIDTH_HALF -> {
                key.foregroundDrawableId = R.drawable.ic_keyboard_char_width_switcher_half
            }
            KeyCode.SWITCH_TO_TEXT_CONTEXT,
            KeyCode.VIEW_CHARACTERS -> {
                key.label = stringRes(R.string.key__view_characters)
            }
            KeyCode.VIEW_NUMERIC,
            KeyCode.VIEW_NUMERIC_ADVANCED -> {
                key.label = stringRes(R.string.key__view_numeric)
            }
            KeyCode.VIEW_PHONE -> {
                key.label = stringRes(R.string.key__view_phone)
            }
            KeyCode.VIEW_PHONE2 -> {
                key.label = stringRes(R.string.key__view_phone2)
            }
            KeyCode.VIEW_SYMBOLS -> {
                key.label = stringRes(R.string.key__view_symbols)
            }
            KeyCode.VIEW_SYMBOLS2 -> {
                key.label = stringRes(R.string.key__view_symbols2)
            }
            KeyCode.HALF_SPACE -> {
                key.label = stringRes(R.string.key__view_half_space)
            }
            KeyCode.KESHIDA -> {
                key.label = stringRes(R.string.key__view_keshida)
            }
        }
    }
    return key
}
