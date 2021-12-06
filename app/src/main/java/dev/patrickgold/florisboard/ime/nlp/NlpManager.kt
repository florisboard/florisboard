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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem

class NlpManager(context: Context) {
    private val prefs by florisPreferenceModel()
    private val clipboardManager by context.clipboardManager()

    private val _suggestions = MutableLiveData<SuggestionList2?>(null)
    val suggestions: LiveData<SuggestionList2?> get() = _suggestions

    private val _candidates = MutableLiveData<List<Candidate>>(emptyList())
    val candidates: LiveData<List<Candidate>> get() = _candidates

    init {
        clipboardManager.primaryClip.observeForever {
            assembleCandidates()
        }
        suggestions.observeForever {
            assembleCandidates()
        }
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

    fun clearSuggestions() {
        _suggestions.postValue(null)
    }

    private fun assembleCandidates() {
        val candidates = buildList {
            clipboardManager.primaryClip()?.let { add(Candidate.Clip(it)) }
            suggestions.value?.let { suggestionList ->
                suggestionList.forEachIndexed { n, word ->
                    add(Candidate.Word(word, isAutoInsert = n == 0 && suggestionList.isPrimaryTokenAutoInsert))
                }
            }
        }
        _candidates.postValue(candidates)
        if (prefs.smartbar.enabled.get() && prefs.smartbar.actionRowAutoExpandCollapse.get()) {
            prefs.smartbar.actionRowExpandWithAnimation.set(false)
            prefs.smartbar.actionRowExpanded.set(candidates.isEmpty())
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
