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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * States for the voice recording and processing pipeline.
 */
enum class VoiceState {
    IDLE,
    RECORDING,
    PROCESSING,
    DONE,
    ERROR
}

/**
 * VoiceStateMachine manages the state transitions for the voice-to-text pipeline.
 *
 * @property scope The CoroutineScope used for auto-dismissal timers and auto-transitions.
 */
class VoiceStateMachine(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(VoiceState.IDLE)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var autoTransitionJob: Job? = null

    /**
     * Transitions from IDLE to RECORDING.
     * Sets a 60-second timer to auto-transition to PROCESSING.
     */
    fun startRecording() {
        if (_state.value != VoiceState.IDLE) return

        _state.value = VoiceState.RECORDING
        _errorMessage.value = null

        autoTransitionJob?.cancel()
        autoTransitionJob = scope.launch {
            delay(60000)
            if (_state.value == VoiceState.RECORDING) {
                moveToProcessing()
            }
        }
    }

    /**
     * Transitions from RECORDING to PROCESSING.
     */
    fun stopRecording() {
        if (_state.value == VoiceState.RECORDING) {
            moveToProcessing()
        }
    }

    private fun moveToProcessing() {
        autoTransitionJob?.cancel()
        _state.value = VoiceState.PROCESSING
    }

    /**
     * Transitions from PROCESSING to DONE.
     * Sets a 1.5-second timer to auto-dismiss back to IDLE.
     */
    fun setDone() {
        if (_state.value == VoiceState.PROCESSING) {
            _state.value = VoiceState.DONE
            scope.launch {
                delay(1500)
                if (_state.value == VoiceState.DONE) {
                    _state.value = VoiceState.IDLE
                }
            }
        }
    }

    /**
     * Transitions to ERROR state.
     * Sets a 3-second timer to auto-dismiss back to IDLE.
     *
     * @param message The error message to display.
     */
    fun setError(message: String?) {
        if (_state.value == VoiceState.RECORDING || _state.value == VoiceState.PROCESSING) {
            autoTransitionJob?.cancel()
            _errorMessage.value = message
            _state.value = VoiceState.ERROR
            scope.launch {
                delay(3000)
                if (_state.value == VoiceState.ERROR) {
                    _state.value = VoiceState.IDLE
                    _errorMessage.value = null
                }
            }
        }
    }

    /**
     * Resets the state machine to IDLE and clears any errors.
     */
    fun reset() {
        autoTransitionJob?.cancel()
        _state.value = VoiceState.IDLE
        _errorMessage.value = null
    }
}
