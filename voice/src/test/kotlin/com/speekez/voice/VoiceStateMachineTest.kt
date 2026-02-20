/*
 * Copyright (C) 2025 SpeekEZ
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

package com.speekez.voice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceStateMachineTest {

    @Test
    fun `initial state is IDLE`() = runTest {
        val stateMachine = VoiceStateMachine(this)
        assertEquals(VoiceState.IDLE, stateMachine.state.value)
        assertNull(stateMachine.errorMessage.value)
    }

    @Test
    fun `startRecording transitions to RECORDING`() = runTest {
        val stateMachine = VoiceStateMachine(this)
        stateMachine.startRecording()
        assertEquals(VoiceState.RECORDING, stateMachine.state.value)
    }

    @Test
    fun `stopRecording transitions from RECORDING to PROCESSING`() = runTest {
        val stateMachine = VoiceStateMachine(this)
        stateMachine.startRecording()
        stateMachine.stopRecording()
        assertEquals(VoiceState.PROCESSING, stateMachine.state.value)
    }

    @Test
    fun `setDone transitions from PROCESSING to DONE and then IDLE after 1500ms`() = runTest {
        val stateMachine = VoiceStateMachine(this)
        stateMachine.startRecording()
        stateMachine.stopRecording()
        stateMachine.setDone()
        assertEquals(VoiceState.DONE, stateMachine.state.value)

        advanceTimeBy(1499)
        runCurrent()
        assertEquals(VoiceState.DONE, stateMachine.state.value)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(VoiceState.IDLE, stateMachine.state.value)
    }

    @Test
    fun `setError transitions from PROCESSING to ERROR and then IDLE after 3000ms`() = runTest {
        val stateMachine = VoiceStateMachine(this)
        stateMachine.startRecording()
        stateMachine.stopRecording()
        stateMachine.setError("Test Error")
        assertEquals(VoiceState.ERROR, stateMachine.state.value)
        assertEquals("Test Error", stateMachine.errorMessage.value)

        advanceTimeBy(2999)
        runCurrent()
        assertEquals(VoiceState.ERROR, stateMachine.state.value)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(VoiceState.IDLE, stateMachine.state.value)
        assertNull(stateMachine.errorMessage.value)
    }

    @Test
    fun `RECORDING auto-transitions to PROCESSING after 60000ms`() = runTest {
        val stateMachine = VoiceStateMachine(this)
        stateMachine.startRecording()
        assertEquals(VoiceState.RECORDING, stateMachine.state.value)

        advanceTimeBy(59999)
        runCurrent()
        assertEquals(VoiceState.RECORDING, stateMachine.state.value)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(VoiceState.PROCESSING, stateMachine.state.value)
    }

    @Test
    fun `reset returns to IDLE from any state`() = runTest {
        val stateMachine = VoiceStateMachine(this)
        stateMachine.startRecording()
        stateMachine.reset()
        assertEquals(VoiceState.IDLE, stateMachine.state.value)

        stateMachine.startRecording()
        stateMachine.stopRecording()
        stateMachine.reset()
        assertEquals(VoiceState.IDLE, stateMachine.state.value)
    }
}
