/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.input

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.collection.SparseArrayCompat
import androidx.collection.isNotEmpty
import androidx.collection.set
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.lib.android.removeAndReturn
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class InputEventDispatcher private constructor(private val repeatableKeyCodes: IntArray) {
    companion object {
        private val DoubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()
        private val KeyRepeatDelay = ViewConfiguration.getKeyRepeatDelay().toLong()

        fun new(repeatableKeyCodes: IntArray = intArrayOf()) = InputEventDispatcher(repeatableKeyCodes.clone())
    }

    private val prefs by florisPreferenceModel()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val pressedKeys = guardedByLock { SparseArrayCompat<PressedKeyInfo>() }
    private var lastKeyEventDown: EventData = EventData(0L, TextKeyData.UNSPECIFIED)
    private var lastKeyEventUp: EventData = EventData(0L, TextKeyData.UNSPECIFIED)

    /**
     * The input key event register. If null, the dispatcher will still process input, but won't dispatch them to an
     * event receiver.
     */
    var keyEventReceiver: InputKeyEventReceiver? = null

    private fun determineLongPressDelay(data: KeyData): Long {
        val delayMillis = prefs.keyboard.longPressDelay.get().toLong()
        val factor = when (data.code) {
            KeyCode.SPACE, KeyCode.CJK_SPACE, KeyCode.SHIFT -> 2.5f
            KeyCode.LANGUAGE_SWITCH -> 2.0f
            else -> 1.0f
        }
        return (delayMillis * factor).toLong()
    }

    private fun determineRepeatDelay(data: KeyData): Long {
        val factor = when (data.code) {
            KeyCode.DELETE_WORD, KeyCode.FORWARD_DELETE_WORD -> 5.0f
            else -> 1.0f
        }
        return (KeyRepeatDelay * factor).toLong()
    }

    private fun determineRepeatData(data: KeyData): KeyData {
        return when (data.code) {
            KeyCode.DELETE -> when (prefs.gestures.deleteKeyLongPress.get()) {
                SwipeAction.DELETE_WORD -> TextKeyData.DELETE_WORD
                else -> TextKeyData.DELETE
            }
            KeyCode.FORWARD_DELETE -> when (prefs.gestures.deleteKeyLongPress.get()) {
                SwipeAction.DELETE_WORD -> TextKeyData.FORWARD_DELETE_WORD
                else -> TextKeyData.FORWARD_DELETE
            }
            else -> data
        }
    }

    fun sendDown(
        data: KeyData,
        onLongPress: () -> Boolean = { false },
        onRepeat: () -> Boolean = { true },
    ) = runBlocking {
        flogDebug { data.toString() }
        val eventTime = SystemClock.uptimeMillis()
        val result = pressedKeys.withLock { pressedKeys ->
            if (pressedKeys.containsKey(data.code)) return@withLock null
            val pressedKeyInfo = PressedKeyInfo(eventTime).also { pressedKeyInfo ->
                pressedKeyInfo.job = scope.launch {
                    val longPressDelay = determineLongPressDelay(data)
                    delay(longPressDelay)
                    val longPressResult = withContext(Dispatchers.Main) { onLongPress() }
                    if (longPressResult) {
                        pressedKeyInfo.blockUp = true
                    } else if (repeatableKeyCodes.contains(data.code)) {
                        val repeatData = determineRepeatData(data)
                        val repeatDelay = determineRepeatDelay(repeatData)
                        while (isActive) {
                            val onRepeatResult = withContext(Dispatchers.Main) { onRepeat() }
                            if (onRepeatResult) {
                                keyEventReceiver?.onInputKeyRepeat(repeatData)
                                pressedKeyInfo.blockUp = true
                            }
                            delay(repeatDelay)
                        }
                    }
                }
            }
            pressedKeys[data.code] = pressedKeyInfo
            return@withLock pressedKeyInfo
        }
        if (result != null) {
            keyEventReceiver?.onInputKeyDown(data)
            lastKeyEventDown = EventData(eventTime, data)
        }
        result
    }

    fun sendUp(data: KeyData) = runBlocking {
        flogDebug { data.toString() }
        val (result, isBlocked) = pressedKeys.withLock { pressedKeys ->
            if (pressedKeys.containsKey(data.code)) {
                val pressedKeyInfo = pressedKeys.removeAndReturn(data.code)?.also { it.cancelJobs() }
                return@withLock true to (pressedKeyInfo?.blockUp == true)
            }
            return@withLock false to false
        }
        if (result) {
            if (!isBlocked) {
                keyEventReceiver?.onInputKeyUp(data)
                lastKeyEventUp = EventData(SystemClock.uptimeMillis(), data)
            } else {
                keyEventReceiver?.onInputKeyCancel(data)
            }
        }
    }

    fun sendDownUp(data: KeyData) = runBlocking {
        flogDebug { data.toString() }
        pressedKeys.withLock { pressedKeys ->
            pressedKeys.removeAndReturn(data.code)?.also { it.cancelJobs() }
        }
        val eventData = EventData(SystemClock.uptimeMillis(), data)
        keyEventReceiver?.onInputKeyDown(data)
        lastKeyEventDown = eventData
        keyEventReceiver?.onInputKeyUp(data)
        lastKeyEventUp = eventData
    }

    fun sendCancel(data: KeyData) = runBlocking {
        flogDebug { data.toString() }
        val result = pressedKeys.withLock { pressedKeys ->
            if (pressedKeys.containsKey(data.code)) {
                pressedKeys.removeAndReturn(data.code)?.also { it.cancelJobs() }
                return@withLock true
            }
            return@withLock false
        }
        if (result) {
            keyEventReceiver?.onInputKeyCancel(data)
        }
    }

    /**
     * Checks if there's currently a key down with given [code].
     *
     * @param code The key code to check for.
     *
     * @return True if the given [code] is currently down, false otherwise.
     */
    fun isPressed(code: Int): Boolean = runBlocking {
        pressedKeys.withLock { it.containsKey(code) }
    }

    fun isAnyPressed(): Boolean = runBlocking {
        pressedKeys.withLock { it.isNotEmpty() }
    }

    fun isConsecutiveDown(data: KeyData): Boolean {
        val event = lastKeyEventDown
        return event.data.code == data.code && (SystemClock.uptimeMillis() - event.time) < DoubleTapTimeout
    }

    fun isConsecutiveUp(data: KeyData): Boolean {
        val event = lastKeyEventUp
        return event.data.code == data.code && (SystemClock.uptimeMillis() - event.time) < DoubleTapTimeout
    }

    fun isUninterruptedEventSequence(data: KeyData): Boolean {
        return lastKeyEventDown.data.code == data.code
    }

    /**
     * Closes this dispatcher and cancels the local coroutine scope.
     */
    fun close() {
        keyEventReceiver = null
        scope.cancel()
    }

    data class PressedKeyInfo(
        val eventTimeDown: Long,
        var job: Job? = null,
        var blockUp: Boolean = false,
    ) {
        fun cancelJobs() {
            job?.cancel()
        }
    }

    data class EventData(
        val time: Long,
        val data: KeyData,
    )
}

/**
 * Interface which represents an input key event receiver.
 */
interface InputKeyEventReceiver {
    /**
     * Event method which gets called when a key went down.
     *
     * @param data The associated input key data.
     */
    fun onInputKeyDown(data: KeyData)

    /**
     * Event method which gets called when a key went up.
     *
     * @param data The associated input key data.
     */
    fun onInputKeyUp(data: KeyData)

    /**
     * Event method which gets called when a key is called repeatedly while being pressed down.
     *
     * @param data The associated input key data.
     */
    fun onInputKeyRepeat(data: KeyData)

    /**
     * Event method which gets called when a key press is cancelled.
     *
     * @param data The associated input key data.
     */
    fun onInputKeyCancel(data: KeyData)
}
