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

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.common.isOrientationPortrait
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.common.observeAsTransformingState
import dev.patrickgold.florisboard.common.toIntOffset
import dev.patrickgold.florisboard.ime.core.InputEventDispatcher
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.snygg.ui.SnyggSurface
import dev.patrickgold.florisboard.snygg.ui.solidColor
import dev.patrickgold.florisboard.snygg.ui.spSize
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState

// TODO clean up code style
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextKeyboardLayout(
    modifier: Modifier = Modifier,
    isPreview: Boolean,
) = with(LocalDensity.current) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val prefs by florisPreferenceModel()
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val activeState by keyboardManager.activeState.observeAsNonNullState()
    val activeKeyboard by keyboardManager.activeKeyboard.observeAsNonNullState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.keyboardRowBaseHeight * activeKeyboard.rowCount)
        //.pointerInput(Unit) {
        //    forEachGesture {
        //        awaitPointerEventScope {
        //            flogDebug { "Await first pointer" }
        //            val down = awaitFirstDown(requireUnconsumed = false)
        //            flogDebug { down.toString() }
        //            do {
        //                flogDebug { "Await pointer event" }
        //                val event = awaitPointerEvent()
        //                val cancelled = event.changes.any { it.positionChangeConsumed() }
        //                if (!cancelled) {
        //                    for (pointer in event.changes) {
        //                        if (pointer.positionChanged())
        //                        flogDebug { pointer.toString() }
        //                    }
        //                }
        //            } while (!cancelled)
        //        }
        //    }
        //}
    ) {
        if (constraints.isZero) {
            return@BoxWithConstraints
        }
        val keyMarginH by prefs.keyboard.keySpacingHorizontal.observeAsTransformingState { it.dp.toPx() }
        val keyMarginV by prefs.keyboard.keySpacingVertical.observeAsTransformingState { it.dp.toPx() }
        val desiredKey = remember { TextKey(data = TextKeyData.UNSPECIFIED) }
        LaunchedEffect(activeKeyboard, constraints, activeState) {
            val keyboardWidth = constraints.maxWidth.toFloat()
            val keyboardHeight = constraints.maxHeight.toFloat()
            desiredKey.touchBounds.apply {
                width = keyboardWidth / 10.0f
                height = keyboardHeight / activeKeyboard.rowCount.coerceAtLeast(1).toFloat()
            }
            desiredKey.visibleBounds.applyFrom(desiredKey.touchBounds).deflateBy(keyMarginH, keyMarginV)
            for (key in activeKeyboard.keys()) {
                key.compute(keyboardManager.computingEvaluator)
                key.computeLabelsAndDrawables(context, keyboardManager.computingEvaluator)
            }
            activeKeyboard.layout(keyboardWidth, keyboardHeight, desiredKey)
        }

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
            TextKeyButton(key, keyboardManager.inputEventDispatcher, fontSizeMultiplier)
        }
    }
}

@Composable
private fun TextKeyButton(
    key: TextKey,
    inputEventDispatcher: InputEventDispatcher,
    fontSizeMultiplier: Float,
) = with(LocalDensity.current) {
    val inputFeedbackController = LocalInputFeedbackController.current
    val keyStyle = FlorisImeTheme.style.get(
        element = FlorisImeUi.Key,
        code = key.computedData.code,
        isPressed = key.isPressed,
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
            .absoluteOffset { key.visibleBounds.topLeft.toIntOffset() }
            .pointerInput(key.computedData) {
                detectTapGestures(
                    onPress = {
                        key.isPressed = true
                        inputEventDispatcher.send(InputKeyEvent.down(key.computedData))
                        inputFeedbackController.keyPress(key.computedData)
                        if (tryAwaitRelease()) {
                            inputEventDispatcher.send(InputKeyEvent.up(key.computedData))
                        } else {
                            inputEventDispatcher.send(InputKeyEvent.cancel(key.computedData))
                        }
                        key.isPressed = false
                    },
                )
            },
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
