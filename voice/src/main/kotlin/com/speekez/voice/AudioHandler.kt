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
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * AudioHandler wraps the Android MediaRecorder for voice capture.
 * Configured for 16kHz mono AAC recording in an M4A container.
 */
class AudioHandler(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    var isRecording = false
        private set

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val TAG = "AudioHandler"

    /**
     * Callback invoked when the recording reaches the 60-second limit.
     */
    var onAutoStop: (() -> Unit)? = null

    /**
     * Initializes and starts recording audio.
     */
    fun start() {
        if (isRecording) return

        try {
            val timestamp = System.currentTimeMillis()
            val file = File(context.cacheDir, "voice_temp_$timestamp.m4a")
            currentFile = file

            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            mediaRecorder = recorder

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioChannels(1)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            startTimer()
            Log.i(TAG, "Started recording: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
        }
    }

    /**
     * Stops the current recording and returns the audio file.
     *
     * @return The recorded [File] or null if not recording.
     */
    fun stop(): File? {
        if (!isRecording) return null

        stopTimer()

        return try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            Log.i(TAG, "Stopped recording")
            currentFile
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
            null
        } finally {
            isRecording = false
        }
    }

    /**
     * Stops the current recording and deletes the temporary file.
     */
    fun cancel() {
        if (!isRecording) return

        stopTimer()

        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder during cancel", e)
        } finally {
            isRecording = false
            currentFile?.delete()
            currentFile = null
            Log.i(TAG, "Cancelled recording and deleted file")
        }
    }

    /**
     * Releases MediaRecorder resources.
     */
    fun cleanup() {
        stopTimer()
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
        } catch (e: Exception) {
            // Ignore stop errors during cleanup
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = scope.launch {
            delay(60000)
            if (isRecording) {
                Log.i(TAG, "Auto-stopping recording after 60s")
                onAutoStop?.invoke()
                stop()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
