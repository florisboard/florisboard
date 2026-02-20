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
import com.speekez.api.ApiRouterManager
import com.speekez.core.NetworkUtils
import com.speekez.data.dailyStatsDao
import com.speekez.data.entity.DailyStats
import com.speekez.data.entity.Preset
import com.speekez.data.entity.RefinementLevel
import com.speekez.data.entity.Transcription
import com.speekez.data.presetDao
import com.speekez.data.transcriptionDao
import com.speekez.security.EncryptedPreferencesManager
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VoiceManager orchestrates the voice recording and processing pipeline.
 * It integrates [AudioHandler] for audio capture and [VoiceStateMachine] for state management.
 */
class VoiceManager(private val context: Context) {
    interface Provider {
        val voiceManager: Lazy<VoiceManager>
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateMachine = VoiceStateMachine(scope)

    private val prefs = EncryptedPreferencesManager(context)
    private val apiRouterManager = ApiRouterManager(context, prefs)
    private val presetDao = context.presetDao()
    private val transcriptionDao = context.transcriptionDao()
    private val dailyStatsDao = context.dailyStatsDao()

    var inputConnectionProvider: (() -> InputConnection?)? = null
    var onTranscriptionComplete: ((String) -> Unit)? = null

    private var activePreset: Preset? = null
    private var recordingStartTime: Long = 0

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
        scope.launch {
            try {
                val preset = presetDao.getPresetById(presetId.toLong())
                if (preset == null) {
                    stateMachine.setError("Preset not found")
                    return@launch
                }
                activePreset = preset

                if (!PermissionUtils.hasMicPermission(context)) {
                    stateMachine.setError("Microphone permission denied")
                    return@launch
                }

                if (!NetworkUtils.isOnline(context)) {
                    stateMachine.setError("No internet connection")
                    return@launch
                }

                val sttClient = apiRouterManager.getSttClient()
                if (sttClient == null) {
                    stateMachine.setError("API key not configured")
                    return@launch
                }

                stateMachine.startRecording()
                audioHandler.start()
                recordingStartTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                stateMachine.setError(e.message ?: "Failed to start recording")
            }
        }
    }

    /**
     * Stops the recording process and moves to processing.
     */
    fun stopRecording() {
        Log.i(TAG, "stopRecording()")
        stateMachine.stopRecording()
    }

    private fun processAudio(file: File) {
        val preset = activePreset ?: return
        val durationMs = System.currentTimeMillis() - recordingStartTime

        scope.launch(Dispatchers.IO) {
            try {
                val sttClient = apiRouterManager.getSttClient()
                    ?: throw IllegalStateException("STT client not available")
                
                val sttModel = apiRouterManager.getSttModel(preset.modelTier)
                
                val rawText = sttClient.transcribe(file, sttModel, preset.inputLanguages)
                
                var finalResult = rawText
                if (preset.refinementLevel != RefinementLevel.NONE) {
                    val refinementClient = apiRouterManager.getRefinementClient()
                        ?: throw IllegalStateException("Refinement client not available")
                    val refinementModel = apiRouterManager.getRefinementModel(preset.modelTier)
                    finalResult = refinementClient.refine(rawText, refinementModel, preset.systemPrompt)
                }

                withContext(Dispatchers.Main) {
                    val ic = inputConnectionProvider?.invoke()
                    ic?.commitText(finalResult, 1)
                    
                    if (prefs.isCopyToClipboardEnabled()) {
                        onTranscriptionComplete?.invoke(finalResult)
                    }
                }

                val wordCount = finalResult.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                val wpm = if (durationMs > 0) (wordCount.toFloat() / (durationMs / 60000f)) else 0f

                val transcription = Transcription(
                    presetId = preset.id,
                    rawText = rawText,
                    refinedText = finalResult,
                    audioDurationMs = durationMs,
                    wordCount = wordCount,
                    wpm = wpm,
                    createdAt = System.currentTimeMillis()
                )
                transcriptionDao.insert(transcription)

                updateDailyStats(wordCount, durationMs)
                presetDao.incrementUsageCount(preset.id)

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

    private suspend fun updateDailyStats(wordCount: Int, durationMs: Long) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val existingStats = dailyStatsDao.getByDate(date)

        // Typing speed assumed 40 WPM for time saved calculation
        val typingWpm = 40f
        val expectedTypingTimeSeconds = (wordCount / typingWpm) * 60f
        val actualRecordingTimeSeconds = durationMs / 1000f
        val timeSavedSeconds = (expectedTypingTimeSeconds - actualRecordingTimeSeconds).toLong().coerceAtLeast(0L)

        if (existingStats != null) {
            val newWordCount = existingStats.wordCount + wordCount
            val newRecordingCount = existingStats.recordingCount + 1
            
            // Recalculate average WPM
            // To do this accurately, we'd need total duration.
            // Total Duration = Total Word Count / Avg WPM
            val oldTotalDurationMin = if (existingStats.avgWpm > 0) existingStats.wordCount / existingStats.avgWpm else 0f
            val newTotalDurationMin = oldTotalDurationMin + (durationMs / 60000f)
            val newAvgWpm = if (newTotalDurationMin > 0) newWordCount / newTotalDurationMin else 0f

            val updatedStats = existingStats.copy(
                recordingCount = newRecordingCount,
                wordCount = newWordCount,
                avgWpm = newAvgWpm,
                timeSavedSeconds = existingStats.timeSavedSeconds + timeSavedSeconds
            )
            dailyStatsDao.insertOrUpdate(updatedStats)
        } else {
            val avgWpm = if (durationMs > 0) (wordCount.toFloat() / (durationMs / 60000f)) else 0f
            val newStats = DailyStats(
                date = date,
                recordingCount = 1,
                wordCount = wordCount,
                avgWpm = avgWpm,
                timeSavedSeconds = timeSavedSeconds
            )
            dailyStatsDao.insertOrUpdate(newStats)
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
