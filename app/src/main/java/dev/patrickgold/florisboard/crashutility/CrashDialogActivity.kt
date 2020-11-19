/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.crashutility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.CrashDialogBinding

class CrashDialogActivity : AppCompatActivity() {
    private lateinit var binding: CrashDialogBinding
    private var stacktrace: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CrashDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stacktrace = CrashUtility.getUnhandledStacktrace(this)
        binding.stacktrace.text = stacktrace

        binding.copyToClipboard.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE)
            if (clipboardManager != null && clipboardManager is ClipboardManager) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(stacktrace, stacktrace))
            }
        }

        binding.openBugReportForm.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__issue_tracker_new_issue_url))
            )
            startActivity(browserIntent)
        }

        binding.close.setOnClickListener {
            finish()
        }
    }
}
