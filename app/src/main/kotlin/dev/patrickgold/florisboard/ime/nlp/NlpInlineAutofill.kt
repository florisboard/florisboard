/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.nlp

import android.content.Context
import android.os.Build
import android.util.Size
import android.view.ViewGroup
import android.view.inputmethod.InlineSuggestion
import android.view.inputmethod.InlineSuggestionInfo
import android.widget.inline.InlineContentView
import androidx.annotation.RequiresApi
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.devtools.flogWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class NlpInlineAutofillSuggestion(
    val info: InlineSuggestionInfo,
    val view: InlineContentView?,
)

object NlpInlineAutofill {
    private val currentSequenceId = AtomicInteger(0)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val setterGuard = Mutex()
    private val _suggestions = MutableStateFlow<List<NlpInlineAutofillSuggestion>>(emptyList())
    val suggestions = _suggestions

    @RequiresApi(Build.VERSION_CODES.R)
    fun showInlineSuggestions(context: Context, rawSuggestions: List<InlineSuggestion>): Boolean {
        val sequenceId = generateSequenceId()

        if (rawSuggestions.isEmpty()) {
            clearInlineSuggestions(sequenceId)
            return false
        }

        scope.launch {
            val size = Size(ViewGroup.LayoutParams.WRAP_CONTENT, FlorisImeSizing.Static.smartbarHeightPx)
            val latch = CountDownLatch(rawSuggestions.size)
            val suggestionsArray = Array<NlpInlineAutofillSuggestion?>(rawSuggestions.size) { null }

            flogInfo { "showInlineSuggestions: [${sequenceId}] start inflating suggestions" }
            for ((index, rawSuggestion) in rawSuggestions.withIndex()) {
                rawSuggestion.inflate(context, size, context.mainExecutor) { view ->
                    suggestionsArray[index] = NlpInlineAutofillSuggestion(rawSuggestion.info, view)
                    latch.countDown()
                }
            }

            if (!latch.await(2_000, TimeUnit.MILLISECONDS)) {
                flogWarning { "showInlineSuggestions: [${sequenceId}] timed out while waiting for all " +
                    "suggestions to inflate" }
                return@launch
            }

            val suggestions = suggestionsArray.filterNotNull().sortedByDescending { it.info.isPinned }
            setterGuard.lock()
            flogInfo { "showInlineSuggestions: [${sequenceId}] successfully inflated " +
                "${suggestions.count { it.view != null }} out of ${suggestions.size} suggestions" }
            if (currentSequenceId.get() == sequenceId) {
                flogInfo { "showInlineSuggestions: [${sequenceId}] setting suggestions" }
                _suggestions.value = suggestions
            } else {
                flogWarning { "showInlineSuggestions: [${sequenceId}] seqId != current, skip setting suggestions" }
            }
            setterGuard.unlock()
        }

        return true
    }

    fun clearInlineSuggestions() {
        // Increment sequence id to invalidate eventual pending suggestions
        clearInlineSuggestions(generateSequenceId())
    }

    private fun clearInlineSuggestions(sequenceId: Int) {
        scope.launch {
            setterGuard.lock()
            flogInfo { "clearInlineSuggestions: [${sequenceId}] clearing suggestions" }
            _suggestions.value = emptyList()
            setterGuard.unlock()
        }
    }

    private fun generateSequenceId(): Int {
        return currentSequenceId.incrementAndGet()
    }
}
