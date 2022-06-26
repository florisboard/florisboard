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

import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent

/**
 * Base interface for any NLP provider implementation. NLP providers maintain their own internal state and only receive
 * limited events, such as [create], [preload], [destroy] and group specific requests. Providers should NEVER do heavy
 * work in the initialization phase of the object, any first-time setup work should be exclusively done in [create].
 */
sealed interface NlpProvider {
    /**
     * Is called exactly once before any [preload] or task specific requests, which allows to make one-time setups, set
     * up necessary native bindings, threads, etc.
     */
    suspend fun create()

    /**
     * Is called at least once before a task specific request occurs, to allow for locale-specific preloading of
     * dictionaries and language models.
     */
    suspend fun preload(subtype: Subtype)

    /**
     * Is called when the provider is no longer needed and should be destroyed. Any native allocations should be freed
     * up and any asynchronous tasks/threads must be stopped. After this method call finishes, this provider object is
     * considered dead and will be queued to be cleaned up by the GC in the next round.
     */
    suspend fun destroy()
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
     * @param subtype Information about the current subtype, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     * @param content The current content view around the input cursor.
     * @param maxCandidateCount The maximum number of candidates this method should return to seamlessly fit into the
     *  UI. Returning more candidates will result in the overflowing candidates to be dismissed.
     * @param isPrivateSession Flag indicating if this suggestion call is done within a private session. If true, it
     *  means that this method should only provide suggestions based on already learned data, but MUST NOT use user
     *  input to train the language model. Private sessions are mostly triggered in browser incognito windows and some
     *  messenger apps, however the user may also have this enabled manually.
     *
     * @return A list of candidate suggestions for the current editor content state, complying with the max count
     *  restrictions as best as possible. If the provider cannot at all provide any candidates, an empty list should be
     *  returned, in which case the UI automatically adapts and shows alternative actions.
     */
    suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate>
}

/**
 * Interface for an NLP provider specializing in spell check services.
 */
interface SpellingProvider : NlpProvider {
    /**
     * Spell check given [word] in the primary (and optionally secondary if defined) language of given [subtype], and
     * return a spelling result. If the given word is spelled correctly, a spelling result with no suggestions should
     * be returned.
     *
     * Spell check requests are considered to be read-only and should at no point be used to train the underlying
     * language model and/or weights in the dictionary.
     *
     * @param subtype Information about the current subtype, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     * @param word The word to spell check, may contain any valid Unicode code point.
     * @param precedingWords List of preceding words, which allows for a more context-based spellcheck. This list can
     *  also be empty, if no surrounding context can be provided.
     * @param followingWords List of following words, which allows for a more context-based spellcheck. This list can
     *  also be empty, if no surrounding context can be provided.
     * @param maxSuggestionCount The maximum number of suggestions this method should return to seamlessly fit into the
     *  UI. Returning more suggestions will result in the overflowing suggestions to be dismissed.
     *
     * @return A spelling result object, which indicates both the validity of this word and if needed suggested
     *  corrections for the misspelled word.
     */
    suspend fun spell(
        subtype: Subtype,
        word: CharSequence,
        precedingWords: List<CharSequence>,
        followingWords: List<CharSequence>,
        maxSuggestionCount: Int,
    ): SpellingResult
}
