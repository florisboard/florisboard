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

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.CrashDialogBinding
import dev.patrickgold.florisboard.debug.*
import dev.patrickgold.florisboard.ime.core.Preferences

class CrashDialogActivity : AppCompatActivity() {
    private lateinit var binding: CrashDialogBinding
    private var stacktraces: List<CrashUtility.Stacktrace> = listOf()
    private var errorReport: StringBuilder = StringBuilder()
    private var prefs: Preferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CrashDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // We secure the PrefHelper usage here because the PrefHelper could potentially be the
        // source of the crash, thus making the crash dialog unusable if not wrapped.
        try {
            prefs = Preferences.default()
        } catch (_: Exception) {
        }

        stacktraces = CrashUtility.getUnhandledStacktraces(this)
        errorReport.apply {
            appendLine("#### Environment information")
            appendLine("- FlorisBoard Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("- Device: ${getDeviceName()}")
            appendLine("- Android: ${getAndroidVersion()}")
            appendLine()
            appendLine("#### Features enabled")
            appendLine("```kt")
            val prefs = prefs
            if (prefs != null) {
                try {
                    appendLine("smartbar = ${prefs.smartbar.enabled}")
                    appendLine("suggestions = ${prefs.suggestion.enabled}")
                    appendLine("suggestions_clipboard = ${prefs.suggestion.clipboardContentEnabled}")
                    appendLine("suggestions_next_word = ${prefs.suggestion.usePrevWords}")
                    appendLine("glide = ${prefs.glide.enabled}")
                    appendLine("clipboard_internal = ${prefs.clipboard.enableInternal}")
                    appendLine("clipboard_history = ${prefs.clipboard.enableHistory}")
                } catch (_: Exception) {
                    appendLine("error: Exception was thrown while retrieving preferences, instance not null.")
                }
            } else {
                appendLine("error: Failed to fetch preferences: PrefHelper instance was null!")
            }
            appendLine("```")
            appendLine()
            if (stacktraces.isNotEmpty()) {
                appendLine("#### Attached stacktrace files")
                stacktraces.forEach {
                    generateCollapsibleStacktrace(this, it)
                }
            } else {
                flogWarning(LogTopic.CRASH_UTILITY) {
                    "Stacktrace file list is empty."
                }
            }
        }
        binding.stacktrace.text = errorReport

        binding.reportInstructions.text =
            binding.reportInstructions.text.toString().format(
                resources.getString(R.string.crash_dialog__bug_report_template)
            )

        binding.copyToClipboard.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE)
            val toastMessage: String = if (clipboardManager != null && clipboardManager is ClipboardManager) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(errorReport, errorReport))
                resources.getString(R.string.crash_dialog__copy_to_clipboard_success)
            } else {
                resources.getString(R.string.crash_dialog__copy_to_clipboard_failure)
            }
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        binding.openBugReportForm.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__issue_tracker_url))
            )
            startActivity(browserIntent)
        }

        binding.close.setOnClickListener {
            finish()
        }
    }

    /**
     * Rules for collapsible markdown on GitHub:
     *  https://gist.github.com/pierrejoubert73/902cc94d79424356a8d20be2b382e1ab
     */
    private fun generateCollapsibleStacktrace(sb: StringBuilder, stacktrace: CrashUtility.Stacktrace) {
        sb.apply {
            appendLine("<details>")
            appendLine("<summary>${stacktrace.name}</summary>")
            appendLine()
            appendLine("```")
            appendLine(stacktrace.details)
            appendLine("```")
            appendLine("</details>")
            appendLine()
        }
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase().startsWith(manufacturer.lowercase())) {
            model.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            "${manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} $model"
        }
    }

    private fun getAndroidVersion(): String {
        val fields = Build.VERSION_CODES::class.java.fields
        var codeName: String? = null
        fields.filter { it.getInt(Build.VERSION_CODES::class) == Build.VERSION.SDK_INT }
            .forEach { codeName = it.name }
        return codeName?.let { "${Build.VERSION.RELEASE} (cn=$it sdk=${Build.VERSION.SDK_INT})" } ?: "Unknown"
    }
}
