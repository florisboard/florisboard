/*
 * Copyright (C) 2021 Patrick Goldinger
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
import android.util.LruCache
import android.util.Size
import android.view.inputmethod.InlineSuggestion
import android.widget.inline.InlineContentView
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.kotlin.collectLatestIn
import dev.patrickgold.florisboard.lib.util.NetworkUtils
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class NlpManager(context: Context) {
    private val prefs by florisPreferenceModel()
    private val clipboardManager by context.clipboardManager()
    private val editorInstance by context.editorInstance()
    private val keyboardManager by context.keyboardManager()
    private val subtypeManager by context.subtypeManager()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _activeSuggestionsFlow = MutableStateFlow(listOf<SuggestionCandidate>())
    val activeSuggestionsFlow = _activeSuggestionsFlow.asStateFlow()
    inline var activeSuggestions
        get() = activeSuggestionsFlow.value
        private set(v) { _activeSuggestionsFlow.value = v }

    private val _activeCandidatesFlow = MutableStateFlow(listOf<SuggestionCandidate>())
    val activeCandidatesFlow = _activeCandidatesFlow.asStateFlow()
    inline var activeCandidates
        get() = activeCandidatesFlow.value
        private set(v) { _activeCandidatesFlow.value = v }

    private val inlineContentViews = Collections.synchronizedMap<InlineSuggestion, InlineContentView>(hashMapOf())
    private val _inlineSuggestions = MutableLiveData<List<InlineSuggestion>>(emptyList())
    val inlineSuggestions: LiveData<List<InlineSuggestion>> get() = _inlineSuggestions

    val debugOverlaySuggestionsInfos = LruCache<Long, Pair<String, SpellingResult>>(10)
    var debugOverlayVersion = MutableLiveData(0)
    private val debugOverlayVersionSource = AtomicInteger(0)

    init {
        clipboardManager.primaryClipFlow.collectLatestIn(scope) {
            assembleCandidates()
        }
        activeSuggestionsFlow.collectLatestIn(scope) {
            assembleCandidates()
        }
        prefs.suggestion.clipboardContentEnabled.observeForever {
            assembleCandidates()
        }
    }

    /**
     * Gets the punctuation rule from the currently active subtype and returns it. Falls back to a default one if the
     * subtype does not exist or defines an invalid punctuation rule.
     *
     * @return The punctuation rule or a fallback.
     */
    fun getActivePunctuationRule(): PunctuationRule {
        return getPunctuationRule(subtypeManager.activeSubtype)
    }

    /**
     * Gets the punctuation rule from the given subtype and returns it. Falls back to a default one if the subtype does
     * not exist or defines an invalid punctuation rule.
     *
     * @return The punctuation rule or a fallback.
     */
    fun getPunctuationRule(subtype: Subtype): PunctuationRule {
        return keyboardManager.resources.punctuationRules.value
            ?.get(subtype.punctuationRule) ?: PunctuationRule.Fallback
    }

    fun suggest(
        currentWord: String,
        precedingWords: List<String>,
    ) {
        val word = if (currentWord.isBlank() && (precedingWords.isEmpty() || precedingWords.all { it.isBlank() })) {
            "next"
        } else {
            currentWord
        }
        val allowPossiblyOffensive = !prefs.suggestion.blockPossiblyOffensive.get()
        val maxSuggestionCount = 16 // TODO: make customizable in prefs
        val suggestions = buildList {
            for (n in 0..6) {
                add(WordSuggestionCandidate("$word$n"))
            }
        }
        activeSuggestions = suggestions
    }

    fun suggestDirectly(suggestions: List<SuggestionCandidate>) {
        activeSuggestions = suggestions
    }

    fun clearSuggestions() {
        activeSuggestions = emptyList()
    }

    private fun assembleCandidates() {
        val candidates = buildList {
            if (prefs.smartbar.enabled.get() && prefs.suggestion.enabled.get()) {
                if (prefs.suggestion.clipboardContentEnabled.get()) {
                    val now = System.currentTimeMillis()
                    clipboardManager.primaryClip?.let { item ->
                        if ((now - item.creationTimestampMs) < prefs.suggestion.clipboardContentTimeout.get() * 1000) {
                            add(ClipboardSuggestionCandidate(item))
                            if (item.type == ItemType.TEXT) {
                                val text = item.stringRepresentation()
                                NetworkUtils.getUrls(text).forEach { match ->
                                    if (match.value != text) {
                                        add(ClipboardSuggestionCandidate(item.copy(text = match.value)))
                                    }
                                }
                                NetworkUtils.getEmailAddresses(text).forEach { match ->
                                    if (match.value != text) {
                                        add(ClipboardSuggestionCandidate(item.copy(text = match.value)))
                                    }
                                }
                            }
                        }
                    }
                }
                activeSuggestions.let { suggestions ->
                    suggestions.forEachIndexed { n, candidate ->
                        add(WordSuggestionCandidate(
                            text = candidate.text,
                            secondaryText = if (n % 2 == 1) "secondary" else null,
                            confidence = 0.5,
                        ))
                    }
                }
            }
        }
        activeCandidates = candidates
        autoExpandCollapseSmartbarActions(candidates, inlineSuggestions.value)
    }

    /**
     * Inflates the given inline suggestions. Once all provided views are ready, the suggestions
     * strip is updated and the Smartbar update cycle is triggered.
     *
     * @param inlineSuggestions A collection of inline suggestions to be inflated and shown.
     */
    fun showInlineSuggestions(inlineSuggestions: List<InlineSuggestion>) {
        inlineContentViews.clear()
        _inlineSuggestions.postValue(inlineSuggestions)
        autoExpandCollapseSmartbarActions(activeCandidates, inlineSuggestions)
    }

    /**
     * Clears the inline suggestions and triggers the Smartbar update cycle.
     */
    fun clearInlineSuggestions() {
        inlineContentViews.clear()
        _inlineSuggestions.postValue(emptyList())
        autoExpandCollapseSmartbarActions(activeCandidates, null)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun inflateOrGet(context: Context, size: Size, inlineSuggestion: InlineSuggestion, callback: (InlineContentView) -> Unit) {
        val view = inlineContentViews[inlineSuggestion]
        if (view != null) {
            callback(view)
        } else {
            try {
                inlineSuggestion.inflate(context, size, context.mainExecutor) { inflatedView ->
                    if (inflatedView != null) {
                        inlineContentViews[inlineSuggestion] = inflatedView
                        callback(inflatedView)
                    }
                }
            } catch (e: Exception) {
                flogError { e.toString() }
            }
        }
    }

    private fun autoExpandCollapseSmartbarActions(list1: List<*>?, list2: List<*>?) {
        if (prefs.smartbar.enabled.get() && prefs.smartbar.primaryActionsAutoExpandCollapse.get()) {
            val isSelection = editorInstance.activeContent.selection.isSelectionMode
            val isExpanded = list1.isNullOrEmpty() && list2.isNullOrEmpty() || isSelection
            prefs.smartbar.primaryActionsExpandWithAnimation.set(false)
            prefs.smartbar.primaryActionsExpanded.set(isExpanded)
        }
    }

    fun addToDebugOverlay(word: String, info: SpellingResult) {
        val version = debugOverlayVersionSource.incrementAndGet()
        debugOverlaySuggestionsInfos.put(System.currentTimeMillis(), word to info)
        debugOverlayVersion.postValue(version)
    }

    fun clearDebugOverlay() {
        val version = debugOverlayVersionSource.incrementAndGet()
        debugOverlaySuggestionsInfos.evictAll()
        debugOverlayVersion.postValue(version)
    }
}
