/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

data class SuggestionCandidates(val candidates: List<SuggestionCandidate>, val selectedIndex: Int? = null) {
    inline fun ifEmpty(defaultValue: () -> SuggestionCandidates): SuggestionCandidates {
        contract {
            callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
        }
        return if (isEmpty()) defaultValue() else this
    }

    fun isNotEmpty() = !isEmpty()

    fun isEmpty() = candidates.isEmpty()
    fun firstOrNull(predicate: (SuggestionCandidate) -> Boolean): SuggestionCandidate? = candidates.firstOrNull(predicate)

    companion object {
        val EMPTY = SuggestionCandidates(emptyList(), null)
    }
}

fun SuggestionCandidates?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || this.isEmpty()
}

fun MutableList<SuggestionCandidate>.addAll(elements: SuggestionCandidates) {
    addAll(elements.candidates)
}
