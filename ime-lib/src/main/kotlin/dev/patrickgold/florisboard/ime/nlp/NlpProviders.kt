/*
 * Copyright (C) 2022 Patrick Goldinger
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

import dev.patrickgold.florisboard.ime.core.ComputedSubtype

/**
 * Base interface for any NLP provider implementation. NLP providers maintain their own internal state and only receive
 * limited events, such as [create], [preload], [destroy] and group specific requests.
 *
 * Providers should NEVER do heavy work in the initialization phase of the object, any first-time setup work should be
 * exclusively done in [preload].
 */
interface NlpProvider {
    /**
     * Is called exactly once before any [preload] or task specific requests, which allows to make one-time setups, set
     * up necessary native bindings, threads, etc.
     */
    fun create()

    /**
     * Is called at least once before a task specific request occurs, to allow for locale-specific preloading of
     * dictionaries and language models.
     *
     * @param subtype Information about the subtype to preload, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     */
    fun preload(subtype: ComputedSubtype)

    /**
     * Is called when the provider is no longer needed and should be destroyed. Any native allocations should be freed
     * up and any asynchronous tasks/threads must be stopped. After this method call finishes, this provider object is
     * considered dead and will be queued to be cleaned up by the GC in the next round.
     */
    fun destroy()
}

/**
 * Interface for an NLP provider specializing in spell check services.
 */
interface SpellingProvider : NlpProvider {
    /**
     * Spell check given [word] in the primary (and optionally secondary if defined) language of given [subtypeId], and
     * return a spelling result. If the given word is spelled correctly, a spelling result with no suggestions should
     * be returned.
     *
     * Spell check requests are considered to be read-only and should at no point be used to train the underlying
     * language model and/or weights in the dictionary.
     *
     * @param subtypeId THe ID of the subtype this request is for, is guaranteed to match one of the subtype IDs which
     *  have been passed to [preload].
     * @param flags The suggestion request flags.
     * @param word The word to spell check, may contain any valid Unicode code point.
     * @param precedingWords List of preceding words, which allows for a more context-based spellcheck. This list can
     *  also be empty, if no surrounding context can be provided.
     * @param followingWords List of following words, which allows for a more context-based spellcheck. This list can
     *  also be empty, if no surrounding context can be provided.
     *
     * @return A spelling result object, which indicates both the validity of this word and if needed suggested
     *  corrections for the misspelled word.
     */
    fun spell(
        subtypeId: Long,
        flags: SuggestionRequestFlags,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
    ): SpellingResult
}

/**
 * Interface for an NLP provider specializing in next/current-word suggestion and autocorrect services.
 */
interface SuggestionProvider : NlpProvider {
    /**
     * Callback from the editor logic that the editor content has changed and that new suggestions should be generated
     * for the new user input. There is no guarantee that candidates returned are actually used, as there may be sudden
     * context changes or clipboard/emoji suggestions overriding the results (if the user has those enabled).
     *
     * @param subtypeId THe ID of the subtype this request is for, is guaranteed to match one of the subtype IDs which
     *  have been passed to [preload].
     * @param flags The suggestion request flags.
     * @param word The current word to use as a base for word prediction, may contain any valid Unicode code point.
     * @param precedingWords List of preceding words, which allows for a more context-based word prediction. This list
     *  can also be empty, if no surrounding context can be provided.
     * @param followingWords List of following words, which allows for a more context-based word prediction. This list
     *  can also be empty, if no surrounding context can be provided.
     *
     * @return A list of candidate suggestions for the current editor content state, complying with the max count
     *  restrictions as best as possible. If the provider cannot at all provide any candidates, an empty list should be
     *  returned, in which case the UI automatically adapts and shows alternative actions.
     */
    fun suggest(
        subtypeId: Long,
        flags: SuggestionRequestFlags,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
    ): List<SuggestionCandidate>

    /**
     * Is called when a suggestion has been accepted, either manually by the user or automatically through auto-commit.
     * This is purely a notification about an event and can safely be ignored if not needed.
     *
     * @param subtypeId THe ID of the subtype this request is for, is guaranteed to match one of the subtype IDs which
     *  have been passed to [preload].
     * @param candidate The exact suggestion candidate which has been accepted.
     */
    fun notifySuggestionAccepted(subtypeId: Long, candidate: SuggestionCandidate)

    /**
     * Is called when a previously automatically accepted suggestion has been reverted by the user with backspace. This
     * is purely a notification about an event and can safely be ignored if not needed.
     *
     * @param subtypeId THe ID of the subtype this request is for, is guaranteed to match one of the subtype IDs which
     *  have been passed to [preload].
     * @param candidate The exact suggestion candidate which has been reverted.
     */
    fun notifySuggestionReverted(subtypeId: Long, candidate: SuggestionCandidate)

    /**
     * Called if the user requests to prevent a certain suggested word from showing again. It is up to the actual
     * implementation to adhere to this user request, this removal is not enforced nor monitored by the NLP manager.
     *
     * @param subtypeId THe ID of the subtype this request is for, is guaranteed to match one of the subtype IDs which
     *  have been passed to [preload].
     * @param candidate The exact suggestion candidate which the user does not want to see again.
     *
     * @return True if the removal request is supported and is accepted, false otherwise.
     */
    fun removeSuggestion(subtypeId: Long, candidate: SuggestionCandidate): Boolean
}

/**
 * Fallback NLP provider which implements all provider variants. Is used in case no other providers can be found.
 */
object FallbackNlpProvider : SpellingProvider, SuggestionProvider {
    override fun create() {
        // Do nothing
    }

    override fun preload(subtype: ComputedSubtype) {
        // Do nothing
    }

    override fun spell(
        subtypeId: Long,
        flags: SuggestionRequestFlags,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>
    ): SpellingResult {
        return SpellingResult.unspecified()
    }

    override fun suggest(
        subtypeId: Long,
        flags: SuggestionRequestFlags,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>
    ): List<SuggestionCandidate> {
        return emptyList()
    }

    override fun notifySuggestionAccepted(subtypeId: Long, candidate: SuggestionCandidate) {
        // Do nothing
    }

    override fun notifySuggestionReverted(subtypeId: Long, candidate: SuggestionCandidate) {
        // Do nothing
    }

    override fun removeSuggestion(subtypeId: Long, candidate: SuggestionCandidate): Boolean {
        return false
    }

    override fun destroy() {
        // Do nothing
    }
}
