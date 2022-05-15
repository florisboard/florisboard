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
import android.util.Size
import android.view.inputmethod.InlineSuggestion
import android.widget.inline.InlineContentView
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.subtypeManager
import java.util.*

class NlpManager(context: Context) {
    private val prefs by florisPreferenceModel()
    private val clipboardManager by context.clipboardManager()
    private val editorInstance by context.editorInstance()
    private val keyboardManager by context.keyboardManager()
    private val subtypeManager by context.subtypeManager()

    private val _suggestions = MutableLiveData<SuggestionList2?>(null)
    val suggestions: LiveData<SuggestionList2?> get() = _suggestions

    private val _candidates = MutableLiveData<List<Candidate>>(emptyList())
    val candidates: LiveData<List<Candidate>> get() = _candidates

    private val inlineContentViews = Collections.synchronizedMap<InlineSuggestion, InlineContentView>(hashMapOf())
    private val _inlineSuggestions = MutableLiveData<List<InlineSuggestion>>(emptyList())
    val inlineSuggestions: LiveData<List<InlineSuggestion>> get() = _inlineSuggestions

    init {
        clipboardManager.primaryClip.observeForever {
            assembleCandidates()
        }
        suggestions.observeForever {
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
        return getPunctuationRule(subtypeManager.activeSubtype())
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
        if (currentWord.isBlank() && (precedingWords.isEmpty() || precedingWords.all { it.isBlank() })) {
            clearSuggestions()
            return
        }
        val allowPossiblyOffensive = !prefs.suggestion.blockPossiblyOffensive.get()
        val maxSuggestionCount = 16 // TODO: make customizable in prefs
        val suggestions = buildList {
            for (n in 0..6) {
                add("$currentWord$n")
            }
        }
        _suggestions.postValue(SuggestionList2(suggestions, false))
    }

    fun suggestDirectly(suggestions: List<String>) {
        _suggestions.postValue(SuggestionList2(suggestions, false))
    }

    fun clearSuggestions() {
        _suggestions.postValue(null)
    }

    private fun assembleCandidates() {
        val candidates = buildList {
            if (prefs.smartbar.enabled.get() && prefs.suggestion.enabled.get()) {
                if (prefs.suggestion.clipboardContentEnabled.get()) {
                    val now = System.currentTimeMillis()
                    clipboardManager.primaryClip()?.let { item ->
                        if ((now - item.creationTimestampMs) < prefs.suggestion.clipboardContentTimeout.get() * 1000) {
                            add(Candidate.Clip(item))
                        }
                    }
                }
                suggestions.value?.let { suggestionList ->
                    suggestionList.forEachIndexed { n, word ->
                        add(Candidate.Word(word, isAutoInsert = n == 0 && suggestionList.isPrimaryTokenAutoInsert))
                    }
                }
            }
        }
        _candidates.postValue(candidates)
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
        autoExpandCollapseSmartbarActions(candidates.value, inlineSuggestions)
    }

    /**
     * Clears the inline suggestions and triggers the Smartbar update cycle.
     */
    fun clearInlineSuggestions() {
        inlineContentViews.clear()
        _inlineSuggestions.postValue(emptyList())
        autoExpandCollapseSmartbarActions(candidates.value, null)
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

    class SuggestionList2(
        candidates: List<String>,
        val isPrimaryTokenAutoInsert: Boolean,
    ) : List<String> by candidates

    /**
     * Data class describing a computed candidate item.
     */
    sealed class Candidate {
        fun text(): String {
            return when (this) {
                is Clip -> clipboardItem.stringRepresentation()
                is Word -> word
            }
        }

        fun isAutoInsertWord(): Boolean {
            return this is Word && this.isAutoInsert
        }

        /**
         * Computed word candidate, used for suggestions provided by the NLP algorithm.
         *
         * @property word The word this computed candidate item represents. Used in the callback to provide which word
         *  should be filled out.
         * @property isAutoInsert If pressing space bar auto corrects/inserts this word.
         */
        data class Word(val word: String, val isAutoInsert: Boolean) : Candidate()

        /**
         * Computed word candidate, used for clipboard paste suggestions.
         *
         * @property clipboardItem The clipboard item this computed candidate item represents. Used in the callback to
         *  provide which item should be pasted.
         */
        data class Clip(val clipboardItem: ClipboardItem) : Candidate()
    }
}
