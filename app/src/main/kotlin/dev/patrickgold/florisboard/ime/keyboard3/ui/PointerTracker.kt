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
import androidx.compose.runtime.mutableStateMapOf
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.model.layer.K3LayerId
import kotlin.math.pow

data class TrackedPointer(
    val id: PointerId,
    val down: PointerInputChange,
    val downLayerId: K3LayerId,
    val downKey: TouchKey,
    val peekLayerId: K3LayerId?,
    val peekKey: TouchKey?,
    val peekLine: PeekLine?,
    val peekMustSwitchBack: Boolean,
    val currKey: TouchKey?,
    val repeatJob: Job?,
)

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

    return remember(touchKeyboard) {
        PointerTracker(touchKeyboard, imeController, interactionController, scope, peekDistanceSqMin)
    }
}

class PointerTracker(
    val touchKeyboard: TouchKeyboard,
    val imeController: ImeController,
    val interactionController: InteractionController,
    val scope: CoroutineScope,
    val peekDistanceSqMin: Float,
) {
    val trackedPointers = mutableStateMapOf<PointerId, TrackedPointer>()

    fun onDown(down: PointerInputChange, size: IntSize) {
        val downLayerId = imeController.snapshotState().touchLayerId
        val downKey = touchKeyboard.findKey(downLayerId, down.position.normalized(size)) ?: return
        down.consume()

        val keyRepeatTimeout = interactionController.getKeyRepeatTimeout(downKey.data.output)
        val keyRepeatDelay = interactionController.getKeyRepeatDelay(downKey.data.output)
        val longPressTimeout = interactionController.getLongPressTimeout(downKey.data.output)

        val trackedPointer = TrackedPointer(
            id = down.id,
            down = down,
            downLayerId = downLayerId,
            downKey = downKey,
            peekLayerId = downKey.data.layerId.takeIf { trackedPointers.isEmpty() },
            peekKey = null,
            peekLine = null,
            peekMustSwitchBack = false,
            currKey = downKey,
            repeatJob = if (downKey.isRepeatable) {
                scope.launch {
                    delay(keyRepeatTimeout)
                    while (isActive) {
                        imeController.updateState {
                            downKey.data.output?.let { emit(it) }
                        }
                        interactionController.performFeedback(InteractionKind.KeyRepeat)
                        delay(keyRepeatDelay)
                    }
                }
            } else null,
        )
        val longPress = if (downKey.isSuitableForPopup) {
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
        } else null
        require(!trackedPointers.contains(trackedPointer.id))
        trackedPointers[trackedPointer.id] = trackedPointer
        if (downKey.data.layerId == null) {
            downKey.numPointersFocused.update { it + 1 }
        }
        if (longPress != null) {
            downKey.longPressFlow.update { longPress }
        }
        interactionController.performFeedback(InteractionKind.KeyPress)

        if (trackedPointer.peekLayerId != null) {
            scope.launch {
                imeController.updateState {
                    switchTouchLayer(trackedPointer.peekLayerId)
                }
            }
        }
    }

    fun onMove(move: PointerInputChange, size: IntSize) {
        val trackedPointer = trackedPointers[move.id] ?: return
        move.consume()
        if (trackedPointer.peekLayerId != null) {
            val distanceSq = (move.position - trackedPointer.down.position).getDistanceSquared()
            if (trackedPointer.peekLine != null || distanceSq >= peekDistanceSqMin) {
                val oldPeekKey = trackedPointer.peekKey
                val newPeekKey = touchKeyboard.findKey(trackedPointer.peekLayerId, move.position.normalized(size))
                if (newPeekKey !== oldPeekKey) {
                    oldPeekKey?.numPointersFocused?.update { it - 1 }
                    newPeekKey?.numPointersFocused?.update { it + 1 }
                }
                trackedPointers[trackedPointer.id] = trackedPointer.copy(
                    peekKey = newPeekKey,
                    peekLine = PeekLine(trackedPointer.down.position, move.position),
                    peekMustSwitchBack = true,
                )
            }
        }
    }

    fun onUp(up: PointerInputChange, size: IntSize) {
        val trackedPointer = trackedPointers[up.id] ?: return
        trackedPointer.repeatJob?.cancel()
        trackedPointer.downKey.longPressFlow.update { null }
        if (trackedPointer.downKey.data.layerId == null) {
            trackedPointer.downKey.numPointersFocused.update { it - 1 }
        }
        up.consume()
        scope.launch {
            imeController.updateState {
                if (trackedPointer.peekLayerId != null) {
                    if (trackedPointer.peekMustSwitchBack) {
                        switchTouchLayer(trackedPointer.downLayerId)
                    }
                    if (trackedPointer.peekKey != null) {
                        trackedPointer.peekKey.numPointersFocused.update { it - 1 }
                        trackedPointer.peekKey.data.output?.let { emit(it) }
                    }
                } else {
                    trackedPointer.currKey?.data?.output?.let { emit(it) }
                    for (otherId in trackedPointers.keys.toList()) {
                        val otherTp = trackedPointers[otherId]!!
                        if (otherTp.peekLayerId != null) {
                            trackedPointers[otherId] = otherTp.copy(peekMustSwitchBack = true)
                        }
                    }
                }
            }
        }
        trackedPointers.remove(trackedPointer.id)
    }

    fun onCancel(id: PointerId) {
        val trackedPointer = trackedPointers[id]
        requireNotNull(trackedPointer)
        trackedPointer.downKey.longPressFlow.update { null }
        trackedPointer.repeatJob?.cancel()
        trackedPointer.currKey?.numPointersFocused?.update { it - 1 }
        trackedPointer.peekKey?.numPointersFocused?.update { it - 1 }
        if (trackedPointer.peekLayerId != null) {
            scope.launch {
                imeController.updateState {
                    switchTouchLayer(trackedPointer.downLayerId)
                }
            }
        }
        trackedPointers.remove(id)
    }

    private fun Offset.normalized(size: IntSize): Offset {
        return Offset(x / size.width, y / size.height)
    }
}
