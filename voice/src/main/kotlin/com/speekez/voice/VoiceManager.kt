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

class VoiceManager(private val context: Context) {
    interface Provider {
        val voiceManager: VoiceManager
    }

    private var recording: Boolean = false
    private val TAG = "VoiceManager"

    fun startRecording(presetId: Int) {
        recording = true
        Log.i(TAG, "startRecording(presetId=$presetId)")
    }

    fun stopRecording() {
        recording = false
        Log.i(TAG, "stopRecording()")
    }

    fun isRecording(): Boolean = recording

    fun cancelRecording() {
        recording = false
        Log.i(TAG, "cancelRecording()")
    }

    fun onWindowShown() {
        Log.i(TAG, "onWindowShown()")
    }

    fun onWindowHidden() {
        Log.i(TAG, "onWindowHidden()")
        if (recording) {
            cancelRecording()
        }
    }
}
