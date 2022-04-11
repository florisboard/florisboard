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

package dev.patrickgold.florisboard.lib.crashutility

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.AppPrefs
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogWarning
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class SafePreferenceInstanceWrapper : ReadOnlyProperty<Any?, AppPrefs?> {
    val cachedPreferenceModel = try {
        florisPreferenceModel()
    } catch (_: Throwable) {
        null
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): AppPrefs? {
        return cachedPreferenceModel?.getValue(thisRef, property)
    }
}

class CrashDialogActivity : ComponentActivity() {
    private var stacktraces: List<CrashUtility.Stacktrace> = listOf()
    private var errorReport: StringBuilder = StringBuilder()
    private val prefs by SafePreferenceInstanceWrapper()

    private val stacktrace by lazy { findViewById<TextView>(R.id.stacktrace) }
    private val reportInstructions by lazy { findViewById<TextView>(R.id.report_instructions) }
    private val copyToClipboard by lazy { findViewById<Button>(R.id.copy_to_clipboard) }
    private val openBugReportForm by lazy { findViewById<Button>(R.id.open_bug_report_form) }
    private val close by lazy { findViewById<Button>(R.id.close) }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = layoutInflater.inflate(R.layout.crash_dialog, null)
        setContentView(layout)

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
                    appendLine("smartbar = ${prefs.smartbar.enabled.get()}")
                    appendLine("suggestions = ${prefs.suggestion.enabled.get()}")
                    appendLine("suggestions_clipboard = ${prefs.suggestion.clipboardContentEnabled.get()}")
                    appendLine("suggestions_next_word = ${prefs.suggestion.usePrevWords.get()}")
                    appendLine("glide = ${prefs.glide.enabled.get()}")
                    appendLine("clipboard_internal = ${prefs.clipboard.useInternalClipboard.get()}")
                    appendLine("clipboard_history = ${prefs.clipboard.historyEnabled.get()}")
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
        stacktrace.text = errorReport

        reportInstructions.text =
            reportInstructions.text.toString().format(
                resources.getString(R.string.crash_dialog__bug_report_template)
            )

        copyToClipboard.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE)
            val toastMessage: String = if (clipboardManager != null && clipboardManager is ClipboardManager) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(errorReport, errorReport))
                resources.getString(R.string.crash_dialog__copy_to_clipboard_success)
            } else {
                resources.getString(R.string.crash_dialog__copy_to_clipboard_failure)
            }
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        openBugReportForm.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__issue_tracker_url))
            )
            startActivity(browserIntent)
        }

        close.setOnClickListener {
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
