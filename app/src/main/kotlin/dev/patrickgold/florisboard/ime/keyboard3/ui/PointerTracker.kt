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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.model.K3Model
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

class PointerTracker(
    val touchKeyboard: TouchKeyboard,
    val imeController: ImeController,
    val interactionController: InteractionController,
    val peekDistanceSqMin: Float,
) {
    private val mutationGuard = Mutex()

    val trackedPeekPointer: StateFlow<TrackedPointer.Peek?>
        field = MutableStateFlow(null)

    val trackedOutputPointer: StateFlow<TrackedPointer.Output?>
        field = MutableStateFlow(null)

    suspend fun mutateLocked(action: suspend PointerTracker.() -> Unit) {
        mutationGuard.withLock { action() }
    }

    suspend fun handleDown(down: PointerInputChange, size: IntSize, scope: CoroutineScope) {
        trackedPeekPointer.value?.let { trackedPointer ->
            if (trackedPointer.peekKey != null) {
                handleUp(down.copy(id = trackedPointer.id), size)
            }
        }
        trackedOutputPointer.value?.let { trackedPointer ->
            handleUp(down.copy(id = trackedPointer.id), size)
        }

        val downLayerId = imeController.snapshotState().touchLayerId
        val downKey = touchKeyboard.findKey(downLayerId, down.position.normalized(size)) ?: return

        val keyRepeatTimeout = interactionController.getKeyRepeatTimeout(downKey.attrs.output)
        val keyRepeatDelay = interactionController.getKeyRepeatDelay(downKey.attrs.output)
        val longPressTimeout = interactionController.getLongPressTimeout(downKey.attrs.output)

        val peekLayerId = downKey.attrs.layerId
        if (peekLayerId != null) {
            // this is a peek key action
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
            imeController.updateState {
                switchTouchLayer(peekLayerId)
            }
            interactionController.performFeedback(InteractionKind.KeyPress)
        } else {
            trackedOutputPointer.value = TrackedPointer.Output(
                id = down.id,
                down = down,
                downKey = downKey,
                longPress = if (downKey.isSuitableForPopup) {
                    LongPress(
                        job = if (downKey.isSuitableForExtendedPopup) {
                            scope.launch {
                                delay(longPressTimeout)
                                mutateLocked {
                                    trackedOutputPointer.update { trackedPointer ->
                                        trackedPointer?.copy(
                                            longPress = trackedPointer.longPress?.copy(
                                                simpleIndicateExtended = false,
                                            )
                                        )
                                    }
                                }
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

    suspend fun handleMove(move: PointerInputChange, size: IntSize) {
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

    suspend fun handleUp(up: PointerInputChange, size: IntSize) {
        trackedPeekPointer.value?.takeIf { it.id == up.id }?.let { trackedPointer ->
            imeController.updateState {
                trackedPointer.peekKey?.attrs?.output?.let { emit(it) }
                if (trackedPointer.peekMustSwitchBack) {
                    switchTouchLayer(trackedPointer.downLayerId)
                }
            }
            trackedPeekPointer.value = null
        }
        trackedOutputPointer.value?.takeIf { it.id == up.id }?.let { trackedPointer ->
            trackedPointer.longPress?.job?.cancel()
            trackedPointer.repeatJob?.cancel()
            imeController.updateState {
                trackedPointer.downKey.attrs.output?.let { emit(it) }
            }
            trackedPeekPointer.update { it?.copy(peekMustSwitchBack = true) }
            trackedOutputPointer.value = null
        }
    }

    suspend fun cancelNonPresent(changes: List<PointerInputChange>) {
        trackedPeekPointer.value?.id?.let { id ->
            if (changes.fastAll { it.id != id }) {
                cancelPeek()
            }
        }
        trackedOutputPointer.value?.id?.let { id ->
            if (changes.fastAll { it.id != id }) {
                cancelOutput()
            }
        }
    }

    suspend fun cancelAll() {
        cancelPeek()
        cancelOutput()
    }

    private suspend fun cancelPeek() {
        val trackedPointer = trackedPeekPointer.getAndUpdate { null } ?: return
        imeController.updateState {
            switchTouchLayer(trackedPointer.downLayerId)
        }
    }

    private suspend fun cancelOutput() {
        val trackedPointer = trackedOutputPointer.getAndUpdate { null } ?: return
        trackedPointer.longPress?.job?.cancel()
        trackedPointer.repeatJob?.cancel()
    }

    private fun Offset.normalized(size: IntSize): Offset {
        return Offset(x / size.width, y / size.height)
    }
}

@Composable
fun rememberPointerTracker(
    touchKeyboard: TouchKeyboard,
): PointerTracker {
    val prefs by FlorisPreferenceStore
    val density = LocalDensity.current
    val imeController = LocalImeController.current
    val interactionController = LocalInteractionController.current

    // TODO make configurable
    val peekDistanceSqMin = with(density) { 30.dp.toPx().pow(2) }

    val pointerTracker = remember(touchKeyboard) {
        PointerTracker(touchKeyboard, imeController, interactionController, peekDistanceSqMin)
    }

    return pointerTracker
}

fun Modifier.trackPointerInput(
    pointerTracker: PointerTracker,
    model: K3Model,
) = this.pointerInput(pointerTracker, model) {
    try {
        coroutineScope {
            val scope = this
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val sizeAtEvent = size
                    scope.launch {
                        pointerTracker.mutateLocked {
                            cancelNonPresent(event.changes)
                        }
                    }
                    event.changes.fastForEach { change ->
                        if (change.changedToDown()) {
                            scope.launch {
                                pointerTracker.mutateLocked {
                                    handleDown(change, sizeAtEvent, scope)
                                }
                            }
                        } else if (change.changedToUp()) {
                            scope.launch {
                                pointerTracker.mutateLocked {
                                    handleUp(change, sizeAtEvent)
                                }
                            }
                        } else if (!change.isConsumed) {
                            scope.launch {
                                pointerTracker.mutateLocked {
                                    handleMove(change, sizeAtEvent)
                                }
                            }
                        }
                        change.consume()
                    }
                }
            }
        }
    } finally {
        withContext(NonCancellable) {
            pointerTracker.mutateLocked {
                cancelAll()
            }
        }
    }
}
