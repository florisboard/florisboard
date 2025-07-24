/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

import android.icu.text.BreakIterator
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.editor.EditorRange
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSuggestionType

/**
 * Base interface for any NLP provider implementation. NLP providers maintain their own internal state and only receive
 * limited events, such as [create], [preload], [destroy] and group specific requests.
 *
 * Providers should NEVER do heavy work in the initialization phase of the object, any first-time setup work should be
 * exclusively done in [create].
 *
 * At any point in time there will only be one provider instance per [providerId], even if the instance inherits from
 * multiple categories at once.
 */
sealed interface NlpProvider {
    /**
     * The unique identifier of this NLP provider for referencing and selection purposes. It should adhere to the
     * [Java™ package name standards](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html), with the
     * exception that Java keywords are allowed.
     */
    val providerId: String

    /**
     * Is called exactly once before any [preload] or task specific requests, which allows to make one-time setups, set
     * up necessary native bindings, threads, etc.
     */
    suspend fun create()

    /**
     * Is called at least once before a task specific request occurs, to allow for locale-specific preloading of
     * dictionaries and language models.
     *
     * @param subtype Information about the subtype to preload, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
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
     * @param allowPossiblyOffensive Flag indicating if possibly offensive words are allowed to be suggested. If false,
     *  the suggestion algorithm must treat possibly offensive input as unknown words and ensure before returning that
     *  no potential offensive words for given language are included. This flag is based on explicit choice of the user
     *  in the Settings and should be respected as best as possible.
     * @param isPrivateSession Flag indicating if this suggestion call is done within a private session. If true, it
     *  means that this method should only provide suggestions based on already learned data, but MUST NOT use user
     *  input to train the language model. Private sessions are mostly triggered in browser incognito windows and some
     *  messenger apps, however the user may also have this enabled manually.
     *
     * @return A spelling result object, which indicates both the validity of this word and if needed suggested
     *  corrections for the misspelled word.
     */
    suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
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
     * @param subtype Information about the current subtype, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     * @param content The current content view around the input cursor.
     * @param maxCandidateCount The maximum number of candidates this method should return to seamlessly fit into the
     *  UI. Returning more candidates will result in the overflowing candidates to be dismissed.
     * @param allowPossiblyOffensive Flag indicating if possibly offensive words are allowed to be suggested. If false,
     *  the suggestion algorithm must treat possibly offensive input as unknown words and ensure before returning that
     *  no potential offensive words for given language are included. This flag is based on explicit choice of the user
     *  in the Settings and should be respected as best as possible.
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
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate>

    /**
     * Is called when a suggestion has been accepted, either manually by the user or automatically through auto-commit.
     * This is purely a notification about an event and can safely be ignored if not needed.
     *
     * @param subtype Information about the current subtype, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     * @param candidate The exact suggestion candidate which has been accepted.
     */
    suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate)

    /**
     * Is called when a previously automatically accepted suggestion has been reverted by the user with backspace. This
     * is purely a notification about an event and can safely be ignored if not needed.
     *
     * @param subtype Information about the current subtype, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     * @param candidate The exact suggestion candidate which has been reverted.
     */
    suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate)

    /**
     * Called if the user requests to prevent a certain suggested word from showing again. It is up to the actual
     * implementation to adhere to this user request, this removal is not enforced nor monitored by the NLP manager.
     *
     * @param subtype Information about the current subtype, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     * @param candidate The exact suggestion candidate which the user does not want to see again.
     *
     * @return True if the removal request is supported and is accepted, false otherwise.
     */
    suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean

    /**
     * Interop method allowing the glide typing logic to perform its own magic.
     *
     * @param subtype Information about the current subtype, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     *
     * @return The list of word for the given language(s). If the language is not supported, an empty list should be
     *  returned.
     */
    suspend fun getListOfWords(subtype: Subtype): List<String>

    /**
     * Interop method allowing the glide typing logic to perform its own magic.
     *
     * @param subtype Information about the current subtype, primarily used for getting the primary and secondary
     *  language for correct dictionary selection.
     * @param word The word which frequency is requested.
     *
     * @return The frequency of [word] as a double precision floating value between 0.0 and 1.0. If [word] does not
     *  exist, 0.0 should be returned.
     */
    suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double

    /**
     * When initializing composing text given a new context, the suggestion engine determines the composing range.
     * The default behavior gets the last word according to the current subtype's primaryLocale.
     * @param subtype The current subtype used to determine word or character boundary.
     * @param textBeforeSelection The text whose end we want to compose.
     * @param breakIterators cache of BreakIterator(s) to determine boundary.
     *
     * @return EditorRange indicating composing range.
     */
    suspend fun determineLocalComposing(
        subtype: Subtype,
        textBeforeSelection: CharSequence,
        breakIterators: BreakIteratorGroup,
        localLastCommitPosition: Int,
    ): EditorRange {
        return breakIterators.word(subtype.primaryLocale) {
            it.setText(textBeforeSelection.toString())
            val end = it.last()
            val isWord = it.ruleStatus != BreakIterator.WORD_NONE
            if (isWord) {
                val start = it.previous().let { pos ->
                    // Include Emoji indicator in local composing. This is required so that emoji suggestion indicator'
                    // can be detected in the composing text.
                    (pos - 1).takeIf { updatedPos ->
                        textBeforeSelection.getOrNull(updatedPos) == EmojiSuggestionType.LEADING_COLON.prefix.first()
                    } ?: pos
                }
                EditorRange(start, end)
            } else {
                EditorRange.Unspecified
            }
        }
    }

    val forcesSuggestionOn
        get() = false
}

/**
 * Fallback NLP provider which implements all provider variants. Is used in case no other providers can be found.
 */
object FallbackNlpProvider : SpellingProvider, SuggestionProvider {
    override val providerId = "org.florisboard.nlp.providers.fallback"

    override suspend fun create() {
        // Do nothing
    }

    override suspend fun preload(subtype: Subtype) {
        // Do nothing
    }

    override suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): SpellingResult {
        return SpellingResult.unspecified()
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        return emptyList()
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        // Do nothing
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        // Do nothing
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        return false
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return emptyList()
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return 0.0
    }

    override suspend fun destroy() {
        // Do nothing
    }
}
