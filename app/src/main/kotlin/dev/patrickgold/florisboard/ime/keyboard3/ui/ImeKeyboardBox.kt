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
import androidx.compose.runtime.State
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
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastRoundToInt
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchKey
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchLayer
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchModel
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.window.LocalWindowController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.florisboard.lib.compose.toMm
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.k3lp.model.layer.K3LayerId

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

    var activeTouchModel by remember {
        mutableStateOf(imeController.touchModelCache.getFor(model) ?: TouchModel.Empty)
    }
    LaunchedEffect(model) {
        activeTouchModel = withContext(Dispatchers.Default) {
            imeController.touchModelCache.getOrComputeFor(model)
        }
    }

    BoxWithConstraints {
        val deviceWidthMm = remember(constraints.maxWidth) {
            with(density) { constraints.maxWidth.toDp().toMm().toInt() }
        }
        val activeTouchKeyboard = remember(activeTouchModel, deviceWidthMm) {
            activeTouchModel.selectKeyboard(deviceWidthMm)
        }
        val activeTouchLayer = remember(activeTouchKeyboard, touchLayerId) {
            activeTouchKeyboard.layers[touchLayerId]
                ?: activeTouchKeyboard.layers[K3LayerId.BASE]
                ?: TouchLayer.Empty
        }
        val pointerTracker = rememberPointerTracker(activeTouchKeyboard)

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
                .pointerInput(model) {
                    val currentContext = currentCoroutineContext()
                    awaitPointerEventScope {
                        while (currentContext.isActive) {
                            // TODO this pointer logic is VERY KEEN on sending up, even survives
                            //  mouse leave&re-enter in the emulator => OOB checks
                            val event = awaitPointerEvent()
                            // TODO evaluate this cancellation logic
                            pointerTracker.trackedPointers.toMap().forEach { (id, _) ->
                                val change = event.changes.fastFirstOrNull { it.id == id }
                                if (change == null) {
                                    // we can safely call remove() in onCancel(), as we iterate over the
                                    // immutable snapshot of the map returned by onMap()
                                    pointerTracker.onCancel(id)
                                }
                            }
                            event.changes.fastForEach { change ->
                                if (change.changedToDown()) {
                                    pointerTracker.onDown(change, size)
                                } else if (change.changedToUp()) {
                                    pointerTracker.onUp(change, size)
                                } else if (!change.isConsumed) {
                                    pointerTracker.onMove(change, size)
                                }
                            }
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    for ((_, trackedPointer) in pointerTracker.trackedPointers) {
                        val peekLine = trackedPointer.peekLine
                        if (trackedPointer.peekLine != null) {
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
                if (touchKey.data.gap) {
                    continue
                }
                ImeKeyboardKeyBox(
                    touchKey = touchKey,
                    pointerTracker = pointerTracker,
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
            }
        }
    }
}

@Composable
private fun ImeKeyboardKeyBox(
    touchKey: TouchKey,
    pointerTracker: PointerTracker,
    modifier: Modifier = Modifier,
) {
    val touchKeyState = rememberUpdatedState(touchKey)
    val trackedPointer by remember {
        derivedStateOf {
            pointerTracker.trackedPointers.values.firstOrNull { trackedPointer ->
                trackedPointer.focusedKey == touchKeyState.value
            }
        }
    }

    val label = touchKey.label // TODO for space replace label by active subtype language
    val output = touchKey.data.output
    val attributes: SnyggQueryAttributes = remember(output) {
        buildMap {
            if (output != null) {
                put(FlorisImeUi.Attr.Output, output.asAttrValue())
            }
        }
    }
    val selector by remember {
        derivedStateOf {
            when {
                trackedPointer != null -> SnyggSelector.PRESSED
                else -> SnyggSelector.NONE
            }
        }
    }

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
            display = label,
        )
    }
    trackedPointer?.longPress?.let { longPress ->
        LongPressBox(longPress, attributes = attributes)
    }
}
