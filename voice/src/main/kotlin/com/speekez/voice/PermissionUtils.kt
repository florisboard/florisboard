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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utility class for handling microphone permissions.
 */
object PermissionUtils {
    private const val PREFS_NAME = "speekez_permissions"
    private const val KEY_MIC_PERMISSION_GRANTED = "speekez_mic_permission_granted"

    /**
     * Checks if the app has been granted the [Manifest.permission.RECORD_AUDIO] permission.
     *
     * @param context The context.
     * @return True if the permission is granted, false otherwise.
     */
    fun hasMicPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if the microphone permission was previously denied based on stored preferences.
     *
     * @param context The context.
     * @return True if the permission was previously marked as denied, false otherwise.
     */
    fun isMicPermissionPreviouslyDenied(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_MIC_PERMISSION_GRANTED, false)
    }

    /**
     * Saves the result of a microphone permission request to stored preferences.
     *
     * @param context The context.
     * @param granted True if the permission was granted, false otherwise.
     */
    fun saveMicPermissionResult(context: Context, granted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MIC_PERMISSION_GRANTED, granted).apply()
    }
}
