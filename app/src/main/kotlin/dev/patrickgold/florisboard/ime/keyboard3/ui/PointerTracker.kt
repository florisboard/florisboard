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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard3.ImeController
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.InteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.InteractionKind
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchKey
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchKeyboard
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchPopupKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.model.layer.K3LayerId
import kotlin.math.pow

sealed interface TrackedPointer {
    val id: PointerId
    val down: PointerInputChange
    val downKey: TouchKey

    data class Peek(
        override val id: PointerId,
        override val down: PointerInputChange,
        override val downKey: TouchKey,
        val downLayerId: K3LayerId,
        val peekLayerId: K3LayerId,
        val peekKey: TouchKey?,
        val peekLine: PeekLine?,
        val peekMustSwitchBack: Boolean,
    ) : TrackedPointer

    data class Output(
        override val id: PointerId,
        override val down: PointerInputChange,
        override val downKey: TouchKey,
        val longPress: LongPress?,
        val repeatJob: Job?,
    ) : TrackedPointer
}

data class LongPress(
    val job: Job?,
    val simpleBounds: Rect,
    val simpleLabel: K3StringOrDescriptor,
    val simpleIndicateExtended: Boolean,
    val extendedBounds: Rect,
    val extendedKeys: List<TouchPopupKey>,
    val extendedFocusedIndex: Int,
) {
    fun shouldShowSimplePopup(): Boolean {
        return !simpleBounds.isEmpty
    }

    fun shouldShowExtendedPopup(): Boolean {
        return !extendedBounds.isEmpty
    }
}

data class PeekLine(
    val start: Offset,
    val end: Offset,
)

@Composable
fun rememberPointerTracker(
    touchKeyboard: TouchKeyboard,
): PointerTracker {
    val prefs by FlorisPreferenceStore
    val density = LocalDensity.current
    val imeController = LocalImeController.current
    val interactionController = LocalInteractionController.current
    val scope = rememberCoroutineScope()

    // TODO make configurable
    val peekDistanceSqMin = with(density) { 30.dp.toPx().pow(2) }

    val pointerTracker = remember(touchKeyboard) {
        PointerTracker(touchKeyboard, imeController, interactionController, scope, peekDistanceSqMin)
    }

    DisposableEffect(pointerTracker) {
        onDispose {
            pointerTracker.cancelAll()
        }
    }

    return pointerTracker
}

// TODO better sync and async processing
class PointerTracker(
    val touchKeyboard: TouchKeyboard,
    val imeController: ImeController,
    val interactionController: InteractionController,
    val scope: CoroutineScope,
    val peekDistanceSqMin: Float,
) {
    val trackedPeekPointer = MutableStateFlow<TrackedPointer.Peek?>(null)
    val trackedOutputPointer = MutableStateFlow<TrackedPointer.Output?>(null)

    fun onDown(down: PointerInputChange, size: IntSize) {
        val downLayerId = imeController.snapshotState().touchLayerId
        val downKey = touchKeyboard.findKey(downLayerId, down.position.normalized(size)) ?: return

        val keyRepeatTimeout = interactionController.getKeyRepeatTimeout(downKey.attrs.output)
        val keyRepeatDelay = interactionController.getKeyRepeatDelay(downKey.attrs.output)
        val longPressTimeout = interactionController.getLongPressTimeout(downKey.attrs.output)

        val peekLayerId = downKey.attrs.layerId
        if (peekLayerId != null) {
            // this is a peek key action
            if (trackedPeekPointer.value != null) {
                return
            }
            trackedPeekPointer.value = TrackedPointer.Peek(
                id = down.id,
                down = down,
                downKey = downKey,
                downLayerId = downLayerId,
                peekLayerId = peekLayerId,
                peekKey = null,
                peekLine = null,
                peekMustSwitchBack = false,
            )
            scope.launch {
                imeController.updateState {
                    switchTouchLayer(peekLayerId)
                }
            }
            interactionController.performFeedback(InteractionKind.KeyPress)
        } else {
            trackedOutputPointer.value?.let { onUp(down.copy(id = it.id), size) }
            trackedOutputPointer.value = TrackedPointer.Output(
                id = down.id,
                down = down,
                downKey = downKey,
                longPress = if (downKey.isSuitableForPopup) {
                    LongPress(
                        job = if (downKey.isSuitableForExtendedPopup) {
                            scope.launch {
                                delay(longPressTimeout)
                                // TODO
                            }
                        } else null,
                        simpleBounds = if (downKey.isSuitableForSimplePopup) {
                            downKey.bounds.let { bounds ->
                                val popupWidth = 0.1f
                                val popupHeight = bounds.height * 2f
                                val popupX = bounds.bottomCenter.x - popupWidth / 2f
                                val popupY = bounds.bottom - popupHeight
                                Rect(
                                    offset = Offset(popupX, popupY),
                                    size = Size(popupWidth, popupHeight),
                                )
                            }
                        } else Rect.Zero,
                        simpleLabel = downKey.label,
                        simpleIndicateExtended = downKey.isSuitableForExtendedPopup,
                        extendedBounds = Rect.Zero,
                        extendedKeys = downKey.extendedPopupKeys,
                        extendedFocusedIndex = 0,
                    )
                } else null,
                repeatJob = if (downKey.isRepeatable) {
                    scope.launch {
                        delay(keyRepeatTimeout)
                        while (isActive) {
                            imeController.updateState {
                                downKey.attrs.output?.let { emit(it) }
                            }
                            interactionController.performFeedback(InteractionKind.KeyRepeat)
                            delay(keyRepeatDelay)
                        }
                    }
                } else null,
            )
            interactionController.performFeedback(InteractionKind.KeyPress)
        }
    }

    fun onMove(move: PointerInputChange, size: IntSize) {
        trackedPeekPointer.value?.takeIf { it.id == move.id }?.let { trackedPointer ->
            val distanceSq = (move.position - trackedPointer.down.position).getDistanceSquared()
            if (trackedPointer.peekLine != null || distanceSq >= peekDistanceSqMin) {
                val newPeekKey = touchKeyboard.findKey(trackedPointer.peekLayerId, move.position.normalized(size))
                trackedPeekPointer.value = trackedPointer.copy(
                    peekKey = newPeekKey,
                    peekLine = PeekLine(trackedPointer.down.position, move.position),
                    peekMustSwitchBack = true,
                )
            }
        }
        trackedOutputPointer.value?.takeIf { it.id == move.id }?.let { trackedPointer ->
            // TODO
        }
    }

    fun onUp(up: PointerInputChange, size: IntSize) {
        trackedPeekPointer.value?.takeIf { it.id == up.id }?.let { trackedPointer ->
            scope.launch {
                imeController.updateState {
                    trackedPointer.peekKey?.attrs?.output?.let { emit(it) }
                    if (trackedPointer.peekMustSwitchBack) {
                        switchTouchLayer(trackedPointer.downLayerId)
                    }
                }
            }
            trackedPeekPointer.value = null
        }
        trackedOutputPointer.value?.takeIf { it.id == up.id }?.let { trackedPointer ->
            trackedPointer.longPress?.job?.cancel()
            trackedPointer.repeatJob?.cancel()
            scope.launch {
                imeController.updateState {
                    trackedPointer.downKey.attrs.output?.let { emit(it) }
                    trackedPeekPointer.update { it?.copy(peekMustSwitchBack = true) }
                }
            }
            trackedOutputPointer.value = null
        }
    }

    fun onCancel(id: PointerId) {
        trackedPeekPointer.value?.takeIf { it.id == id }?.let { trackedPointer ->
            scope.launch {
                imeController.updateState {
                    switchTouchLayer(trackedPointer.downLayerId)
                }
            }
            trackedPeekPointer.value = null
        }
        trackedOutputPointer.value?.takeIf { it.id == id }?.let { trackedPointer ->
            trackedPointer.longPress?.job?.cancel()
            trackedPointer.repeatJob?.cancel()
            trackedOutputPointer.value = null
        }
    }

    fun cancelAll() {
        trackedPeekPointer.value?.let { onCancel(it.id) }
        trackedOutputPointer.value?.let { onCancel(it.id) }
    }

    private fun Offset.normalized(size: IntSize): Offset {
        return Offset(x / size.width, y / size.height)
    }
}
