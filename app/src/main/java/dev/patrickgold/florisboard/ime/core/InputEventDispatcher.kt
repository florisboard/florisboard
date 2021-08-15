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

package dev.patrickgold.florisboard.ime.core

import android.os.SystemClock
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.set
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogDebug
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/**
 * The main logic point of processing input events and delegating them to the registered event receivers. Currently,
 * only [InputKeyEvent]s are supported, but in the future this class is thought to be the single point where input
 * events can be dispatched.
 */
class InputEventDispatcher private constructor(
    channelCapacity: Int,
    private val mainDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val repeatableKeyCodes: IntArray
) : InputKeyEventSender {
    private val channel: Channel<InputKeyEvent> = Channel(channelCapacity)
    private val mainScope: CoroutineScope = CoroutineScope(mainDispatcher + SupervisorJob())
    private val defaultScope: CoroutineScope = CoroutineScope(defaultDispatcher + SupervisorJob())
    private val pressedKeys: SparseArray<PressedKeyInfo> = SparseArray()
    var lastKeyEventDown: InputKeyEvent? = null
        private set
    var lastKeyEventUp: InputKeyEvent? = null
        private set

    /**
     * The input key event register. If null, the dispatcher will still process input, but won't dispatch them to an
     * event receiver.
     */
    var keyEventReceiver: InputKeyEventReceiver? = null

    companion object {
        /**
         * The default input event channel capacity to be used in [new].
         */
        private const val DEFAULT_CHANNEL_CAPACITY: Int = 64

        /**
         * Creates a new [InputEventDispatcher] instance from given arguments and returns it.
         *
         * @param channelCapacity The capacity of this input channel, defaults to [DEFAULT_CHANNEL_CAPACITY].
         * @param mainDispatcher The main dispatcher used to switch the context to call the receiver callbacks.
         *  Defaults to [Dispatchers.Main].
         * @param defaultDispatcher The default dispatcher used to switch the context to call the receiver callbacks.
         *  Defaults to [Dispatchers.Default].
         * @param repeatableKeyCodes An int array of all key codes which are repeatable while being pressed down.
         *
         * @return A new [InputEventDispatcher] instance initialized with given arguments.
         */
        fun new(
            channelCapacity: Int = DEFAULT_CHANNEL_CAPACITY,
            mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
            defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
            repeatableKeyCodes: IntArray = intArrayOf()
        ): InputEventDispatcher = InputEventDispatcher(
             channelCapacity, mainDispatcher, defaultDispatcher, repeatableKeyCodes.clone()
        )

        private fun <T> SparseArray<T>.removeAndReturn(key: Int): T? {
            val elem = get(key)
            return if (elem == null) {
                null
            } else {
                remove(key)
                elem
            }
        }
    }

    init {
        defaultScope.launch {
            for (ev in channel) {
                if (!isActive) break
                val startTime = System.nanoTime()
                flogDebug(LogTopic.KEY_EVENTS) { ev.toString() }
                when (ev.action) {
                    InputKeyEvent.Action.DOWN -> {
                        if (pressedKeys.indexOfKey(ev.data.code) >= 0) continue
                        pressedKeys[ev.data.code] = PressedKeyInfo(
                            eventTimeDown = ev.eventTime,
                            repeatKeyPressJob = if (!repeatableKeyCodes.contains(ev.data.code)) { null } else {
                                defaultScope.launch {
                                    delay(600)
                                    while (isActive) {
                                        channel.send(InputKeyEvent.repeat(ev.data))
                                        delay(50)
                                    }
                                }
                            }
                        )
                        withContext(mainDispatcher) {
                            keyEventReceiver?.onInputKeyDown(ev)
                        }
                        if (ev.data.code != KeyCode.INTERNAL_BATCH_EDIT) {
                            lastKeyEventDown = ev
                        }
                    }
                    InputKeyEvent.Action.DOWN_UP -> {
                        pressedKeys.removeAndReturn(ev.data.code)?.repeatKeyPressJob?.cancel()
                        withContext(mainDispatcher) {
                            keyEventReceiver?.onInputKeyDown(ev)
                            keyEventReceiver?.onInputKeyUp(ev)
                        }
                        if (ev.data.code != KeyCode.INTERNAL_BATCH_EDIT) {
                            lastKeyEventDown = ev
                            lastKeyEventUp = ev
                        }
                    }
                    InputKeyEvent.Action.UP -> {
                        pressedKeys.removeAndReturn(ev.data.code)?.repeatKeyPressJob?.cancel()
                        withContext(mainDispatcher) {
                            keyEventReceiver?.onInputKeyUp(ev)
                        }
                        if (ev.data.code != KeyCode.INTERNAL_BATCH_EDIT) {
                            lastKeyEventUp = ev
                        }
                    }
                    InputKeyEvent.Action.REPEAT -> {
                        if (pressedKeys.indexOfKey(ev.data.code) >= 0) {
                            withContext(mainDispatcher) {
                                keyEventReceiver?.onInputKeyRepeat(ev)
                            }
                        }
                    }
                    InputKeyEvent.Action.CANCEL -> {
                        pressedKeys.removeAndReturn(ev.data.code)?.repeatKeyPressJob?.cancel()
                        withContext(mainDispatcher) {
                            keyEventReceiver?.onInputKeyCancel(ev)
                        }
                    }
                }
                flogDebug(LogTopic.KEY_EVENTS) { "Time elapsed: ${(System.nanoTime() - startTime) / 1_000_000}" }
            }
            pressedKeys.forEach { _, value -> value.repeatKeyPressJob?.cancel() }
            pressedKeys.clear()
        }
    }

    override fun send(ev: InputKeyEvent) {
        mainScope.launch {
            channel.send(ev)
        }
    }

    /**
     * Checks if there's currently a key down with given [code].
     *
     * @param code The key code to check for.
     *
     * @return True if the given [code] is currently down, false otherwise.
     */
    fun isPressed(code: Int): Boolean {
        return pressedKeys.indexOfKey(code) >= 0
    }

    /**
     * Closes this dispatcher and cancels the local coroutine scope.
     */
    fun close() {
        keyEventReceiver = null
        mainScope.cancel()
        defaultScope.cancel()
    }

    data class PressedKeyInfo(
        val eventTimeDown: Long,
        val repeatKeyPressJob: Job?
    )
}

/**
 * Data class representing a single input key event.
 *
 * @property eventTime The exact event time when this event occurred, measured in milliseconds since a static point in
 *  the past. The exact point is irrelevant, but while this input dispatcher is active, the point must not change in
 *  order for difference time calculation to succeed.
 * @property action The action of this event.
 * @property data The data of this event.
 * @property count The count how often this event occurred. Is only respected by other methods if the [action] of this
 *  event is [Action.DOWN_UP] or [Action.REPEAT], else always 1 is assumed.
 */
data class InputKeyEvent(
    val eventTime: Long,
    val action: Action,
    val data: KeyData,
    val count: Int
) {
    companion object {
        /**
         * Creates a new input key event with given [keyData] and sets the action to [Action.DOWN].
         *
         * @param keyData The key data of the input key event event to create.
         *
         * @return The created input key event.
         */
        fun down(keyData: KeyData): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.DOWN,
                data = keyData,
                count = 1
            )
        }

        /**
         * Creates a new input key event with given [keyData] and sets the action to [Action.DOWN_UP].
         *
         * @param keyData The key data of the input key event event to create.
         * @param count How often this event occurred. Must be grater or equal to 1, defaults to 1.
         *
         * @return The created input key event.
         */
        fun downUp(keyData: KeyData, count: Int = 1): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.DOWN_UP,
                data = keyData,
                count = count
            )
        }

        /**
         * Creates a new input key event with given [keyData] and sets the action to [Action.UP].
         *
         * @param keyData The key data of the input key event event to create.
         *
         * @return The created input key event.
         */
        fun up(keyData: KeyData): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.UP,
                data = keyData,
                count = 1
            )
        }

        /**
         * Creates a new input key event with given [keyData] and sets the action to [Action.REPEAT].
         *
         * @param keyData The key data of the input key event event to create.
         * @param count How often this event occurred. Must be grater or equal to 1, defaults to 1.
         *
         * @return The created input key event.
         */
        fun repeat(keyData: KeyData, count: Int = 1): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.REPEAT,
                data = keyData,
                count = count
            )
        }

        /**
         * Creates a new input key event with given [keyData] and sets the action to [Action.CANCEL].
         *
         * @param keyData The key data of the input key event event to create.
         *
         * @return The created input key event.
         */
        fun cancel(keyData: KeyData): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.CANCEL,
                data = keyData,
                count = 1
            )
        }
    }

    /**
     * Checks if the [other] input key event is a consecutive event while respecting [maxEventTimeDiff].
     *
     * @param other The other input key event to compare with this one.
     * @param maxEventTimeDiff The maximum event time diff between this event and [other], in milliseconds.
     *
     * @return True if this event is a consecutive event of [other], false otherwise.
     */
    fun isConsecutiveEventOf(other: InputKeyEvent?, maxEventTimeDiff: Long): Boolean {
        return other != null && data.code == other.data.code && eventTime - other.eventTime <= maxEventTimeDiff
    }

    /**
     * Returns a string representation of this input key event.
     */
    override fun toString(): String {
        return "FlorisKeyEvent { eventTime=${eventTime}ms, action=$action, data=$data, count=$count }"
    }

    /**
     * The action of an input key event.
     */
    enum class Action {
        DOWN,
        DOWN_UP,
        UP,
        REPEAT,
        CANCEL,
    }
}

/**
 * Interface which represents an input key event sender.
 */
interface InputKeyEventSender {
    /**
     * Sends given input key event [ev] to the underlying input channel, awaiting to be processed.
     *
     * @param ev The input key event to send.
     */
    fun send(ev: InputKeyEvent)
}

/**
 * Interface which represents an input key event receiver.
 */
interface InputKeyEventReceiver {
    /**
     * Event method which gets called when a key went down.
     *
     * @param ev The associated input key event.
     */
    fun onInputKeyDown(ev: InputKeyEvent)

    /**
     * Event method which gets called when a key went up.
     *
     * @param ev The associated input key event.
     */
    fun onInputKeyUp(ev: InputKeyEvent)

    /**
     * Event method which gets called when a key is called repeatedly while being pressed down.
     *
     * @param ev The associated input key event.
     */
    fun onInputKeyRepeat(ev: InputKeyEvent)

    /**
     * Event method which gets called when a key press is cancelled.
     *
     * @param ev The associated input key event.
     */
    fun onInputKeyCancel(ev: InputKeyEvent)
}
