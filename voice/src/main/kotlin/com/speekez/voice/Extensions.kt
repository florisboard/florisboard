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

/**
 * Extension function to access the [VoiceManager] from any [Context].
 *
 * @return The [VoiceManager] instance.
 * @throws IllegalStateException If the application context does not implement [VoiceManager.Provider].
 */
fun Context.voiceManager(): Lazy<VoiceManager> {
    val provider = this.applicationContext as? VoiceManager.Provider
    return provider?.voiceManager ?: throw IllegalStateException("Application must implement VoiceManager.Provider")
}
