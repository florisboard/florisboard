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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * VoiceManager orchestrates the voice recording and processing pipeline.
 * It integrates [AudioHandler] for audio capture and [VoiceStateMachine] for state management.
 */
class VoiceManager(private val context: Context) {
    interface Provider {
        val voiceManager: VoiceManager
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateMachine = VoiceStateMachine(scope)
    private val audioHandler = AudioHandler(context).apply {
        onAutoStop = {
            Log.i("VoiceManager", "AudioHandler auto-stop triggered")
            stateMachine.stopRecording()
            handleProcessing()
        }
    }

    private val TAG = "VoiceManager"

    /**
     * Observable state of the voice pipeline.
     */
    val state: StateFlow<VoiceState> = stateMachine.state

    /**
     * Observable error message when the state is [VoiceState.ERROR].
     */
    val errorMessage: StateFlow<String?> = stateMachine.errorMessage

    init {
        scope.launch {
            stateMachine.state.collect { currentState ->
                if (currentState == VoiceState.PROCESSING) {
                    handleProcessing()
                }
            }
        }
    }

    private fun handleProcessing() {
        if (!audioHandler.isRecording) return

        val audioFile = audioHandler.stop()
        if (audioFile != null) {
            processAudio(audioFile)
        } else {
            // Only set error if we are currently in PROCESSING and failed to get a file
            if (stateMachine.state.value == VoiceState.PROCESSING) {
                stateMachine.setError("Failed to capture audio")
            }
        }
    }

    /**
     * Starts the recording process for a given preset.
     *
     * @param presetId The ID of the preset being used.
     */
    fun startRecording(presetId: Int) {
        Log.i(TAG, "startRecording(presetId=$presetId)")
        stateMachine.startRecording()
        audioHandler.start()
    }

    /**
     * Stops the recording process and moves to processing.
     */
    fun stopRecording() {
        Log.i(TAG, "stopRecording()")
        stateMachine.stopRecording()
    }

    private fun processAudio(file: File) {
        scope.launch {
            try {
                // Mock processing/transcription delay
                delay(500)
                stateMachine.setDone()
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio processing", e)
                stateMachine.setError(e.message ?: "Unknown error during processing")
            } finally {
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    /**
     * Returns true if currently recording.
     */
    fun isRecording(): Boolean = stateMachine.state.value == VoiceState.RECORDING

    /**
     * Cancels the current recording and resets the state.
     */
    fun cancelRecording() {
        Log.i(TAG, "cancelRecording()")
        audioHandler.cancel()
        stateMachine.reset()
    }

    /**
     * Called when the keyboard window is shown.
     */
    fun onWindowShown() {
        Log.i(TAG, "onWindowShown()")
    }

    /**
     * Called when the keyboard window is hidden.
     * Cancels any active recording.
     */
    fun onWindowHidden() {
        Log.i(TAG, "onWindowHidden()")
        if (isRecording()) {
            cancelRecording()
        }
    }
}
