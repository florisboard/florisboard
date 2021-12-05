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

class NlpManager(context: Context) {
    private val prefs by florisPreferenceModel()

    private val _candidates = MutableLiveData<CandidatesList?>(null)
    val candidates: LiveData<CandidatesList?> get() = _candidates

    fun suggest(
        currentWord: String,
        precedingWords: List<String>,
    ) {
        val allowPossiblyOffensive = !prefs.suggestion.blockPossiblyOffensive.get()
        val maxSuggestionCount = 16 // TODO: make customizable in prefs
        val suggestions = buildList {
            for (n in 0..6) {
                add("$currentWord$n")
            }
        }
        _candidates.postValue(CandidatesList(suggestions, false))
    }

    fun clearSuggestions() {
        _candidates.postValue(null)
    }

    class CandidatesList(
        candidates: List<String>,
        val isPrimaryTokenAutoInsert: Boolean,
    ) : List<String> by candidates
}
