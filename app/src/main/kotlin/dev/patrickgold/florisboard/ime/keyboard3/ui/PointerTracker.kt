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
    val focusedKey: TouchKey?,
    val longPress: LongPress?,
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
            focusedKey = downKey.takeIf { it.data.layerId == null },
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
                            downKey.data.output?.let { emit(it) }
                        }
                        interactionController.performFeedback(InteractionKind.KeyRepeat)
                        delay(keyRepeatDelay)
                    }
                }
            } else null,
        )
        require(!trackedPointers.contains(trackedPointer.id))
        trackedPointers[trackedPointer.id] = trackedPointer
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
                val newPeekKey = touchKeyboard.findKey(trackedPointer.peekLayerId, move.position.normalized(size))
                trackedPointers[trackedPointer.id] = trackedPointer.copy(
                    peekKey = newPeekKey,
                    peekLine = PeekLine(trackedPointer.down.position, move.position),
                    peekMustSwitchBack = true,
                    focusedKey = newPeekKey,
                )
            }
        }
    }

    fun onUp(up: PointerInputChange, size: IntSize) {
        val trackedPointer = trackedPointers[up.id] ?: return
        trackedPointer.longPress?.job?.cancel()
        trackedPointer.repeatJob?.cancel()
        up.consume()
        scope.launch {
            imeController.updateState {
                if (trackedPointer.peekLayerId != null) {
                    trackedPointer.peekKey?.data?.output?.let { emit(it) }
                    if (trackedPointer.peekMustSwitchBack) {
                        switchTouchLayer(trackedPointer.downLayerId)
                    }
                } else {
                    trackedPointer.focusedKey?.data?.output?.let { emit(it) }
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
        trackedPointer.longPress?.job?.cancel()
        trackedPointer.repeatJob?.cancel()
        if (trackedPointer.peekLayerId != null) {
            scope.launch {
                imeController.updateState {
                    switchTouchLayer(trackedPointer.downLayerId)
                }
            }
        }
        trackedPointers.remove(id)
    }

    fun cancelAll() {
        trackedPointers.toMap().forEach { (id, _) ->
            onCancel(id)
        }
    }

    private fun Offset.normalized(size: IntSize): Offset {
        return Offset(x / size.width, y / size.height)
    }
}
