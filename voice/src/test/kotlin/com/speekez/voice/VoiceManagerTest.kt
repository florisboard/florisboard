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

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceManagerTest {
    private lateinit var context: Context
    private lateinit var voiceManager: VoiceManager
    private lateinit var cacheDir: File
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk()
        cacheDir = File("build/tmp/voice_manager_test_cache")
        cacheDir.mkdirs()
        every { context.cacheDir } returns cacheDir

        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockkConstructor(MediaRecorder::class)
        every { anyConstructed<MediaRecorder>().setAudioSource(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setOutputFormat(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setAudioEncoder(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setAudioSamplingRate(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setAudioChannels(any()) } just Runs
        every { anyConstructed<MediaRecorder>().setOutputFile(any<String>()) } just Runs
        every { anyConstructed<MediaRecorder>().prepare() } just Runs
        every { anyConstructed<MediaRecorder>().start() } just Runs
        every { anyConstructed<MediaRecorder>().stop() } just Runs
        every { anyConstructed<MediaRecorder>().reset() } just Runs
        every { anyConstructed<MediaRecorder>().release() } just Runs

        voiceManager = VoiceManager(context)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        cacheDir.deleteRecursively()
    }

    @Test
    fun `startRecording transitions state to RECORDING`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        assertEquals(VoiceState.RECORDING, voiceManager.state.value)
        assertTrue(voiceManager.isRecording())
    }

    @Test
    fun `stopRecording transitions state to PROCESSING then DONE`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        
        // We need to make sure the file exists so stop() returns it
        val pathSlot = slot<String>()
        verify { anyConstructed<MediaRecorder>().setOutputFile(capture(pathSlot)) }
        val file = File(pathSlot.captured)
        file.createNewFile()

        voiceManager.stopRecording()
        runCurrent()
        assertEquals(VoiceState.PROCESSING, voiceManager.state.value)
        
        // Advance time to complete mock processing (500ms in VoiceManager)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(VoiceState.DONE, voiceManager.state.value)
        
        // Advance time to auto-dismiss (1500ms in VoiceStateMachine)
        advanceTimeBy(1501)
        runCurrent()
        assertEquals(VoiceState.IDLE, voiceManager.state.value)
    }

    @Test
    fun `cancelRecording resets state to IDLE`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        voiceManager.cancelRecording()
        runCurrent()
        assertEquals(VoiceState.IDLE, voiceManager.state.value)
        assertFalse(voiceManager.isRecording())
    }

    @Test
    fun `auto-stop after 60s transitions to PROCESSING`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        
        // We need to make sure the file exists
        val pathSlot = slot<String>()
        verify { anyConstructed<MediaRecorder>().setOutputFile(capture(pathSlot)) }
        File(pathSlot.captured).createNewFile()

        // Advance time by 60s
        advanceTimeBy(60001)
        runCurrent()
        
        assertEquals(VoiceState.PROCESSING, voiceManager.state.value)
    }

    @Test
    fun `onWindowHidden cancels recording`() = runTest {
        voiceManager.startRecording(1)
        runCurrent()
        voiceManager.onWindowHidden()
        runCurrent()
        assertEquals(VoiceState.IDLE, voiceManager.state.value)
    }
}
