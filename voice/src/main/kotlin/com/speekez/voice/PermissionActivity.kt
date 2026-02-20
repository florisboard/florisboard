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
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

/**
 * A transparent activity used to request microphone permissions for the SpeekEZ keyboard.
 * Since an InputMethodService cannot request runtime permissions directly, this activity
 * acts as a proxy.
 *
 * Note: This activity must be registered in the app's AndroidManifest.xml:
 * <activity android:name="com.speekez.voice.PermissionActivity"
 *           android:theme="@android:style/Theme.Translucent.NoTitleBar"
 *           android:exported="false" />
 */
class PermissionActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        PermissionUtils.saveMicPermissionResult(this, isGranted)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If permission is already granted, just save and finish
        if (PermissionUtils.hasMicPermission(this)) {
            PermissionUtils.saveMicPermissionResult(this, true)
            finish()
            return
        }

        // Check if we should show a rationale
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            showRationaleDialog()
        } else {
            launchPermissionRequest()
        }
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Microphone Permission")
            .setMessage("SpeekEZ needs microphone access to transcribe your voice to text.")
            .setPositiveButton("OK") { _, _ ->
                launchPermissionRequest()
            }
            .setNegativeButton("Cancel") { _, _ ->
                PermissionUtils.saveMicPermissionResult(this, false)
                finish()
            }
            .setOnCancelListener {
                PermissionUtils.saveMicPermissionResult(this, false)
                finish()
            }
            .show()
    }

    private fun launchPermissionRequest() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    companion object {
        /**
         * Creates an intent to start this activity.
         *
         * @param context The context used to create the intent.
         * @return An intent that can be used to start [PermissionActivity].
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, PermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
        }
    }
}
