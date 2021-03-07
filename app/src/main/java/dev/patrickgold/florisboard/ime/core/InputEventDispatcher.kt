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

class InputEventDispatcher private constructor(
    parentScope: CoroutineScope,
    channelCapacity: Int,
    private val mainDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val requiredDownUpKeyCodes: List<Int>
) : InputKeyEventSender {
    private val channel: Channel<InputKeyEvent> = Channel(channelCapacity)
    private val scope: CoroutineScope = CoroutineScope(parentScope.coroutineContext)
    private val jobs: MutableMap<Int, Job> = mutableMapOf()
    private var lastKeyEvent: InputKeyEvent? = null

    var keyEventReceiver: InputKeyEventReceiver? = null

    companion object {
        private const val DEFAULT_CHANNEL_CAPACITY: Int = 32

        fun new(
            parentScope: CoroutineScope,
            channelCapacity: Int = DEFAULT_CHANNEL_CAPACITY,
            mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
            defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
            requiredDownUpKeyCodes: List<Int>
        ): InputEventDispatcher {
            return InputEventDispatcher(
                parentScope,
                channelCapacity,
                mainDispatcher,
                defaultDispatcher,
                requiredDownUpKeyCodes.toList()
            )
        }
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
                                delay(25)
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
                        if (jobs.containsKey(ev.data.code)) {
                            jobs.remove(ev.data.code)?.cancel()
                            withContext(mainDispatcher) {
                                keyEventReceiver?.onInputKeyUp(ev)
                            }
                            lastKeyEvent = ev
                        }
                    }
                    InputKeyEvent.Action.REPEAT -> {
                        if (jobs.containsKey(ev.data.code)) {
                            withContext(mainDispatcher) {
                                keyEventReceiver?.onInputKeyRepeat(ev)
                            }
                        }
                    }
                    InputKeyEvent.Action.CANCEL -> {
                        if (jobs.containsKey(ev.data.code)) {
                            withContext(mainDispatcher) {
                                keyEventReceiver?.onInputKeyCancel(ev)
                            }
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
        scope.launch(defaultDispatcher) {
            channel.send(ev)
        }
    }

    fun isPressed(code: Int): Boolean {
        return jobs.containsKey(code)
    }

    fun isConsecutiveOfLastEvent(ev: InputKeyEvent, maxEventTimeDiff: Long): Boolean {
        return ev.isConsecutiveEventOf(lastKeyEvent, maxEventTimeDiff)
    }

    fun requireSeparateDownUp(code: Int): Boolean {
        return requiredDownUpKeyCodes.contains(code)
    }

    fun close() {
        keyEventReceiver = null
        scope.cancel()
    }
}

data class InputKeyEvent(
    val eventTime: Long,
    val action: Action,
    val data: KeyData,
    val count: Int
) {
    companion object {
        fun down(keyData: KeyData): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.DOWN,
                data = keyData,
                count = 1
            )
        }

        fun downUp(keyData: KeyData, count: Int = 1): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.DOWN_UP,
                data = keyData,
                count = count
            )
        }

        fun up(keyData: KeyData): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.UP,
                data = keyData,
                count = 1
            )
        }

        fun repeat(keyData: KeyData): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.REPEAT,
                data = keyData,
                count = 1
            )
        }

        fun cancel(keyData: KeyData): InputKeyEvent {
            return InputKeyEvent(
                eventTime = SystemClock.uptimeMillis(),
                action = Action.CANCEL,
                data = keyData,
                count = 1
            )
        }
    }

    fun isConsecutiveEventOf(other: InputKeyEvent?, maxEventTimeDiff: Long): Boolean {
        return other != null && data.code == other.data.code && eventTime - other.eventTime <= maxEventTimeDiff
    }

    override fun toString(): String {
        return "FlorisKeyEvent { eventTime=${eventTime}ms, action=$action, data=$data, repeatCount=$count }"
    }

    enum class Action {
        DOWN,
        DOWN_UP,
        UP,
        REPEAT,
        CANCEL,
    }
}

interface InputKeyEventSender {
    fun send(ev: InputKeyEvent)
}

interface InputKeyEventReceiver {
    fun onInputKeyDown(ev: InputKeyEvent)

    fun onInputKeyUp(ev: InputKeyEvent)

    fun onInputKeyRepeat(ev: InputKeyEvent)

    fun onInputKeyCancel(ev: InputKeyEvent)
}
