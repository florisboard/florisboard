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
class AudioHandlerTest {
    private lateinit var context: Context
    private lateinit var audioHandler: AudioHandler
    private lateinit var cacheDir: File
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk()
        cacheDir = File("build/tmp/test_cache")
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

        audioHandler = AudioHandler(context)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        cacheDir.deleteRecursively()
    }

    @Test
    fun `start and stop cycle returns a file`() {
        audioHandler.start()
        val file = audioHandler.stop()
        
        assertNotNull(file)
        assertTrue(file!!.name.startsWith("voice_temp_"))
        assertTrue(file.name.endsWith(".m4a"))
        
        verify {
            anyConstructed<MediaRecorder>().prepare()
            anyConstructed<MediaRecorder>().start()
            anyConstructed<MediaRecorder>().stop()
        }
    }

    @Test
    fun `cancel deletes the file`() {
        val pathSlot = slot<String>()
        every { anyConstructed<MediaRecorder>().setOutputFile(capture(pathSlot)) } just Runs
        
        audioHandler.start()
        
        assertTrue(pathSlot.isCaptured)
        val file = File(pathSlot.captured)
        file.createNewFile()
        assertTrue(file.exists())
        
        audioHandler.cancel()
        
        assertFalse(file.exists())
        verify {
            anyConstructed<MediaRecorder>().stop()
            anyConstructed<MediaRecorder>().reset()
        }
    }

    @Test
    fun `cleanup releases resources`() {
        audioHandler.start()
        audioHandler.cleanup()
        
        verify {
            anyConstructed<MediaRecorder>().release()
        }
    }

    @Test
    fun `auto stop after 60 seconds`() = runTest(testDispatcher) {
        var autoStopped = false
        audioHandler.onAutoStop = { autoStopped = true }
        
        audioHandler.start()
        
        // Advance time by 61 seconds
        advanceTimeBy(61000)
        
        assertTrue(autoStopped)
        verify {
            anyConstructed<MediaRecorder>().stop()
        }
    }
}
