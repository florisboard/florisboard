/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard3.ImeLayerIds
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LongPress
import dev.patrickgold.florisboard.ime.keyboard3.interaction.rememberPointerTracker
import dev.patrickgold.florisboard.ime.keyboard3.interaction.trackPointerInput
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchKey
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchLayer
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchModel
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.window.LocalWindowController
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.florisboard.lib.compose.toMm
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.asK3String

@Composable
fun ImeKeyboardBox(
    modifier: Modifier = Modifier,
) {
    val prefs by FlorisPreferenceStore
    val density = LocalDensity.current
    val imeController = LocalImeController.current
    val windowController = LocalWindowController.current

    val imeState by imeController.activeState.collectAsState()
    val model by remember { derivedStateOf { imeState.model } }
    val touchLayerId by remember { derivedStateOf { imeState.touchLayerId } }

    val windowSpec by windowController.activeWindowSpec.collectAsState()
    val keyMarginHPx by remember {
        derivedStateOf { with(density) { windowSpec.keyMarginH.toPx() } }
    }
    val keyMarginVPx by remember {
        derivedStateOf { with(density) { windowSpec.keyMarginV.toPx() } }
    }

    val showNumberRow by prefs.keyboard.numberRow.collectAsState()
    var activeTouchModel by remember {
        mutableStateOf(imeController.touchModelCache.getFor(model, showNumberRow) ?: TouchModel.Empty)
    }
    LaunchedEffect(model, showNumberRow) {
        activeTouchModel = withContext(Dispatchers.Default) {
            imeController.touchModelCache.getOrComputeFor(model, showNumberRow)
        }
    }

    val displayLanguageNamesIn by prefs.localization.displayLanguageNamesIn.collectAsState()
    val languageName = remember(model, displayLanguageNamesIn) {
        // TODO this might not be the right locale, get from new suntypes once impl
        val locale = FlorisLocale.from(model.locales.getOrElse(0) { "en" })
        when (displayLanguageNamesIn) {
            DisplayLanguageNamesIn.SYSTEM_LOCALE -> locale.displayName()
            DisplayLanguageNamesIn.NATIVE_LOCALE -> locale.displayName(locale)
        }.asK3String()
    }

    val devtoolsEnabled by prefs.devtools.enabled.collectAsState()
    val debugShowTouchBoundaries by prefs.devtools.showKeyTouchBoundaries.collectAsState()

    BoxWithConstraints {
        val deviceWidthMm = remember(constraints.maxWidth) {
            with(density) { constraints.maxWidth.toDp().toMm().toInt() }
        }
        val activeTouchKeyboard = remember(activeTouchModel, deviceWidthMm) {
            activeTouchModel.selectKeyboard(deviceWidthMm)
        }
        // TODO this launched effect is a hack, there must be a better way
        LaunchedEffect(activeTouchKeyboard) {
            imeController.updateState {
                state = state.copy(effRowCount = activeTouchKeyboard.rowCount)
            }
        }
        val activeTouchLayer = remember(activeTouchKeyboard, touchLayerId) {
            activeTouchKeyboard.layers[touchLayerId]
                ?: activeTouchKeyboard.layers[ImeLayerIds.Base]
                ?: TouchLayer.Empty
        }

        val pointerTracker = rememberPointerTracker(activeTouchKeyboard)
        val trackedOutputPointer by pointerTracker.trackedOutputPointer.collectAsState()
        val trackedPeekPointer by pointerTracker.trackedPeekPointer.collectAsState()

        val keyboardRowHeightDp = FlorisImeSizing.keyboardRowBaseHeight
        val peekLineWidthPx = with(density) { 8.dp.toPx() }

        Box(
            modifier = modifier
                .layout { measurable, constraints ->
                    val effConstraints = Constraints.fixed(
                        width = constraints.maxWidth,
                        height = with(density) {
                            (keyboardRowHeightDp * activeTouchKeyboard.rowCount).roundToPx()
                        },
                    )
                    val placeable = measurable.measure(effConstraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                .trackPointerInput(pointerTracker, model)
                .drawWithContent {
                    drawContent()
                    trackedPeekPointer?.let { trackedPeekPointer ->
                        val peekLine = trackedPeekPointer.peekLine
                        if (peekLine != null) {
                            drawLine(
                                color = Color.Red, // TODO customizable
                                start = peekLine.start,
                                end = peekLine.end,
                                strokeWidth = peekLineWidthPx,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
        ) {
            for (touchKey in activeTouchLayer.keys) {
                if (touchKey.attrs.gap) {
                    continue
                }
                val touchKey by rememberUpdatedState(touchKey)
                val isPressed by remember {
                    derivedStateOf {
                        trackedOutputPointer?.downKey == touchKey ||
                            trackedPeekPointer?.peekKey == touchKey
                    }
                }
                val longPress by remember {
                    derivedStateOf {
                        trackedOutputPointer?.takeIf { it.downKey == touchKey }?.longPress ?: LongPress.None
                    }
                }
                ImeKeyboardKeyBox(
                    touchKey = touchKey,
                    isPressed = isPressed,
                    longPress = longPress,
                    languageName = languageName,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val effConstraints = Constraints.fixed(
                                width = (constraints.maxWidth * touchKey.bounds.width - 2 * keyMarginHPx).fastRoundToInt(),
                                height = (constraints.maxHeight * touchKey.bounds.height - 2 * keyMarginVPx).fastRoundToInt(),
                            )
                            val placeable = measurable.measure(effConstraints)
                            val offset = IntOffset(
                                x = (constraints.maxWidth * touchKey.bounds.topLeft.x + keyMarginHPx).fastRoundToInt(),
                                y = (constraints.maxHeight * touchKey.bounds.topLeft.y + keyMarginVPx).fastRoundToInt(),
                            )
                            layout(placeable.width, placeable.height) { placeable.place(offset) }
                        },
                )
                if (devtoolsEnabled && debugShowTouchBoundaries) {
                    Box(
                        modifier = Modifier
                            .layoutNormalized(touchKey.hitbox)
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    color = Color.Red,
                                    style = Stroke(width = 1.dp.toPx()),
                                )
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImeKeyboardKeyBox(
    touchKey: TouchKey,
    isPressed: Boolean,
    longPress: LongPress,
    languageName: K3String,
    modifier: Modifier = Modifier,
) {
    val display = if (touchKey.isSuitableForLanguageNameDisplay) {
        languageName
    } else {
        touchKey.label
    }
    val output = touchKey.attrs.output
    val attributes: SnyggQueryAttributes = remember(output) {
        buildMap {
            if (output != null) {
                put(FlorisImeUi.Attr.Output, output.asAttrValue())
            }
        }
    }
    // TODO some keys an be disabled (copy cut etc)
    val selector = if (isPressed) SnyggSelector.PRESSED else SnyggSelector.NONE

    SnyggBox(
        FlorisImeUi.Key.elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier,
    ) {
        Display3(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.Center),
            display = display,
        )
    }
    LongPressBox(longPress, attributes = attributes)
}
