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
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The main logic point of processing input events and delegating them to the registered event receivers. Currently,
 * only [InputKeyEvent]s are supported, but in the future this class is thought to be the single point where input
 * events can be dispatched.
 */
class InputEventDispatcher private constructor(
    parentScope: CoroutineScope,
    channelCapacity: Int,
    private val mainDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val requiredDownUpKeyCodes: IntArray
) : InputKeyEventSender {
    private val channel: Channel<InputKeyEvent> = Channel(channelCapacity)
    private val scope: CoroutineScope = CoroutineScope(parentScope.coroutineContext)
    private val jobs: HashMap<Int, Job> = hashMapOf()
    private var lastKeyEvent: InputKeyEvent? = null

    /**
     * The input key event register. If null, the dispatcher will still process input, but won't dispatch them to an
     * event receiver.
     */
    var keyEventReceiver: InputKeyEventReceiver? = null

    companion object {
        /**
         * The default input event channel capacity to be used in [new].
         */
        private const val DEFAULT_CHANNEL_CAPACITY: Int = 32

        /**
         * Creates a new [InputEventDispatcher] instance from given arguments and returns it.
         *
         * @param parentScope The parent coroutine scope which this dispatcher will attach its own scope to.
         * @param channelCapacity The capacity of this input channel, defaults to [DEFAULT_CHANNEL_CAPACITY].
         * @param mainDispatcher The main dispatcher used to switch the context to call the receiver callbacks.
         *  Defaults to [Dispatchers.Main].
         * @param defaultDispatcher The default dispatcher used to switch the context to call the receiver callbacks.
         *  Defaults to [Dispatchers.Default].
         * @param requiredDownUpKeyCodes An int array of all key codes which require separate down/up events. This does
         *  nothing in this dispatcher, but is used by [requireSeparateDownUp] to return a boolean result. Defaults to
         *  an empty array.
         *
         * @return A new [InputEventDispatcher] instance initialized with given arguments.
         */
        fun new(
            parentScope: CoroutineScope,
            channelCapacity: Int = DEFAULT_CHANNEL_CAPACITY,
            mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
            defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
            requiredDownUpKeyCodes: IntArray = intArrayOf()
        ): InputEventDispatcher = InputEventDispatcher(
            parentScope, channelCapacity, mainDispatcher, defaultDispatcher, requiredDownUpKeyCodes.clone()
        )
    }

    init {
        scope.launch(defaultDispatcher) {
            for (ev in channel) {
                if (!isActive) break
                val startTime = System.nanoTime()
                if (BuildConfig.DEBUG) {
                    Timber.d(ev.toString())
                }
                when (ev.action) {
                    InputKeyEvent.Action.DOWN -> {
                        if (jobs.containsKey(ev.data.code)) continue
                        jobs[ev.data.code] = scope.launch(defaultDispatcher) {
                            delay(600)
                            while (isActive) {
                                if (ev.data.code != KeyCode.SHIFT) {
                                    channel.send(InputKeyEvent.repeat(ev.data))
                                }
                                delay(50)
                            }
                        }
                        withContext(mainDispatcher) {
                            keyEventReceiver?.onInputKeyDown(ev)
                        }
                    }
                    InputKeyEvent.Action.DOWN_UP -> {
                        jobs.remove(ev.data.code)?.cancel()
                        withContext(mainDispatcher) {
                            keyEventReceiver?.onInputKeyDown(ev)
                            keyEventReceiver?.onInputKeyUp(ev)
                        }
                        lastKeyEvent = ev
                    }
                    InputKeyEvent.Action.UP -> {
                        jobs.remove(ev.data.code)?.cancel()
                        withContext(mainDispatcher) {
                            keyEventReceiver?.onInputKeyUp(ev)
                        }
                        lastKeyEvent = ev
                    }
                    InputKeyEvent.Action.REPEAT -> {
                        if (jobs.containsKey(ev.data.code)) {
                            withContext(mainDispatcher) {
                                keyEventReceiver?.onInputKeyRepeat(ev)
                            }
                        }
                    }
                    InputKeyEvent.Action.CANCEL -> {
                        jobs.remove(ev.data.code)?.cancel()
                        withContext(mainDispatcher) {
                            keyEventReceiver?.onInputKeyCancel(ev)
                        }
                    }
                }
                if (BuildConfig.DEBUG) {
                    Timber.d("Time elapsed: ${(System.nanoTime() - startTime) / 1_000_000}")
                }
            }
            val jobIterator = jobs.iterator()
            while (jobIterator.hasNext()) {
                jobIterator.next().value.cancel()
                jobIterator.remove()
            }
        }
    }

    override fun send(ev: InputKeyEvent) {
        scope.launch(mainDispatcher) {
            if (ev.action == InputKeyEvent.Action.UP) {
                jobs.remove(ev.data.code)?.cancel()
            }
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
        return jobs.containsKey(code)
    }

    /**
     * Checks if a given event [ev] is a consecutive event of the last event.
     *
     * @param ev The event to check for.
     * @param maxEventTimeDiff The maximum event time diff between [ev] and the last event, in milliseconds.
     */
    fun isConsecutiveOfLastEvent(ev: InputKeyEvent, maxEventTimeDiff: Long): Boolean {
        return ev.isConsecutiveEventOf(lastKeyEvent, maxEventTimeDiff)
    }

    /**
     * Checks if given [code] requires a separate down/up.
     *
     * @param code The key code to check for.
     *
     * @return True if the given [code] requires a separate down/up, false otherwise.
     */
    fun requireSeparateDownUp(code: Int): Boolean {
        return requiredDownUpKeyCodes.contains(code)
    }

    /**
     * Closes this dispatcher and cancels the local coroutine scope.
     */
    fun close() {
        keyEventReceiver = null
        scope.cancel()
    }
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
 *  event is [Action.DOWN_UP], else always 1 is assumed.
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
         *
         * @return The created input key event.
         */
        fun repeat(keyData: KeyData): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.REPEAT,
                data = keyData,
                count = 1
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
