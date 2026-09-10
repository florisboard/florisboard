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

package dev.patrickgold.florisboard.ime.keyboard3.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard3.ImeController
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchKey
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchKeyboard
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import org.k3lp.model.K3Model
import org.k3lp.model.layer.K3LayerId
import kotlin.math.pow
import kotlin.time.Duration

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
        val longPress: LongPress,
        val repeatJob: Job?,
    ) : TrackedPointer
}

data class PeekLine(
    val start: Offset,
    val end: Offset,
)

class PointerTracker(
    val touchKeyboard: TouchKeyboard,
    val imeController: ImeController,
    val interactionController: InteractionController,
    val showSimplePopups: Boolean,
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

    context(scope: CoroutineScope)
    suspend fun handleDown(down: PointerInputChange, size: IntSize) {
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
                    val anchorBounds = downKey.bounds.let { bounds ->
                        // TODO this is only tested for PHONE_PORTRAIT, test+fix other form factors!
                        val w = 0.1f
                        val h = bounds.height * 0.9f
                        val x = bounds.bottomCenter.x - w / 2f
                        val y = bounds.bottom - h * 2.3f
                        Rect(
                            offset = Offset(x, y),
                            size = Size(w, h),
                        )
                    }
                    LongPress(
                        anchorBounds = anchorBounds,
                        simpleBounds = if (showSimplePopups && downKey.isSuitableForSimplePopup) {
                            anchorBounds.copy(
                                bottom = anchorBounds.bottom + anchorBounds.height * 1.2f,
                            )
                        } else Rect.Zero,
                        simpleLabel = downKey.label,
                        simpleIndicateExtended = downKey.isSuitableForExtendedPopup,
                        extendedBounds = Rect.Zero,
                        extendedKeys = downKey.extendedPopupKeys,
                        extendedFocusedIndex = 0,
                        extendedJob = if (downKey.isSuitableForExtendedPopup) {
                            launchExtendedLongPressJob(longPressTimeout)
                        } else null,
                    )
                } else LongPress.None,
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
            val longPress = trackedPointer.longPress
            if (longPress.shouldShowExtendedPopup()) {
                val position = move.position.normalized(size).let { position ->
                    Offset(
                        x = (position.x)
                            .fastCoerceIn(longPress.extendedBounds.left, longPress.extendedBounds.right),
                        y = (position.y - longPress.anchorBounds.height)
                            .fastCoerceIn(longPress.extendedBounds.top, longPress.extendedBounds.bottom - 0.01f),
                    )
                }
                val newFocusedIndex = longPress.getNearestKeyIndex(position)
                if (newFocusedIndex != longPress.extendedFocusedIndex) {
                    trackedOutputPointer.value = trackedPointer.copy(
                        longPress = longPress.copy(extendedFocusedIndex = newFocusedIndex),
                    )
                }
            }
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
            trackedPointer.longPress.extendedJob?.cancel()
            trackedPointer.repeatJob?.cancel()
            imeController.updateState {
                val output = if (trackedPointer.longPress.shouldShowExtendedPopup()) {
                    trackedPointer.longPress.extendedKeys.getOrNull(trackedPointer.longPress.extendedFocusedIndex)
                        ?.data?.output
                } else {
                    trackedPointer.downKey.attrs.output
                }
                output?.let { emit(it) }
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
        trackedPointer.longPress.extendedJob?.cancel()
        trackedPointer.repeatJob?.cancel()
    }

    private fun Offset.normalized(size: IntSize): Offset {
        return Offset(x / size.width, y / size.height)
    }

    context(scope: CoroutineScope)
    private fun launchExtendedLongPressJob(
        longPressTimeout: Duration,
    ): Job = scope.launch(Dispatchers.Default) {
        delay(longPressTimeout)
        mutateLocked {
            val trackedPointer = trackedOutputPointer.value ?: return@mutateLocked
            val longPress = trackedPointer.longPress
            val anchorBounds = longPress.anchorBounds
            val extendedKeys = layoutExtendedLongPressKeys(longPress.extendedKeys, anchorBounds)
            val extendedBounds = anchorBounds.copy(
                left = extendedKeys.fastFold(anchorBounds.left) { acc, key -> minOf(acc, key.bounds.left) },
                top = extendedKeys.fastFold(anchorBounds.top) { acc, key -> minOf(acc, key.bounds.top) },
                right = extendedKeys.fastFold(anchorBounds.right) { acc, key -> maxOf(acc, key.bounds.right) },
                bottom = extendedKeys.fastFold(anchorBounds.bottom) { acc, key -> maxOf(acc, key.bounds.bottom) },
            )
            trackedOutputPointer.value = trackedPointer.copy(
                longPress = trackedPointer.longPress.copy(
                    simpleIndicateExtended = false,
                    extendedBounds = extendedBounds,
                    extendedKeys = extendedKeys,
                ),
            )
        }
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

    val showSimplePopups by prefs.keyboard.popupEnabled.collectAsState()
    // TODO make configurable
    val peekDistanceSqMin = with(density) { 30.dp.toPx().pow(2) }

    val pointerTracker = remember(touchKeyboard, showSimplePopups) {
        PointerTracker(touchKeyboard, imeController, interactionController, showSimplePopups, peekDistanceSqMin)
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
                                    handleDown(change, sizeAtEvent)
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
