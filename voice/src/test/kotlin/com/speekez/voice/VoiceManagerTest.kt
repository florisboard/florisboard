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
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VoiceManagerTest {
    private lateinit var context: Context
    private lateinit var voiceManager: VoiceManager

    @BeforeEach
    fun setUp() {
        context = mockk()
        voiceManager = VoiceManager(context)
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun `startRecording sets recording to true`() {
        voiceManager.startRecording(1)
        assertTrue(voiceManager.isRecording())
        verify { Log.i("VoiceManager", "startRecording(presetId=1)") }
    }

    @Test
    fun `stopRecording sets recording to false`() {
        voiceManager.startRecording(1)
        voiceManager.stopRecording()
        assertFalse(voiceManager.isRecording())
        verify { Log.i("VoiceManager", "stopRecording()") }
    }

    @Test
    fun `cancelRecording sets recording to false`() {
        voiceManager.startRecording(1)
        voiceManager.cancelRecording()
        assertFalse(voiceManager.isRecording())
        verify { Log.i("VoiceManager", "cancelRecording()") }
    }

    @Test
    fun `onWindowHidden cancels recording if active`() {
        voiceManager.startRecording(1)
        voiceManager.onWindowHidden()
        assertFalse(voiceManager.isRecording())
        verify { Log.i("VoiceManager", "cancelRecording()") }
    }

    @Test
    fun `onWindowHidden does nothing if not recording`() {
        voiceManager.onWindowHidden()
        assertFalse(voiceManager.isRecording())
        verify(exactly = 0) { Log.i("VoiceManager", "cancelRecording()") }
    }

    @Test
    fun `onWindowShown logs correctly`() {
        voiceManager.onWindowShown()
        verify { Log.i("VoiceManager", "onWindowShown()") }
    }
}
