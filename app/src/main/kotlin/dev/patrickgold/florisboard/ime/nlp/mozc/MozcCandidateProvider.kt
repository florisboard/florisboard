package dev.patrickgold.florisboard.ime.nlp.mozc

import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.mozc.MozcEngine
import dev.patrickgold.florisboard.ime.nlp.ClipboardSuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.florisboard.lib.kotlin.guardedByLock

class MozcCandidateProvider : SuggestionProvider {
    companion object {
        // Default user ID used for all subtypes, unless otherwise specified.
        // See `ime/core/Subtype.kt` Line 210 and 211 for the default usage
        const val ProviderId = "org.florisboard.nlp.providers.mozc"
    }

    override val providerId = ProviderId

    override suspend fun create() {
        // Do nothing
    }

    override suspend fun preload(subtype: Subtype) {
        // Do nothing
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        val candidateList = MozcEngine.instance.candidates.value
        val candidates = candidateList.subList(0, maxCandidateCount.coerceAtMost(candidateList.size))
        val suggestions = buildList {
            for ((n, candidate) in candidates.withIndex()) {
                add(
                    WordSuggestionCandidate(
                        text = candidate.value,
                        confidence = (1000 - (n))/1000.0000,
                        isEligibleForAutoCommit = false,
                        isEligibleForUserRemoval = false, // mozc cant delete a candidate
                        // We set ourselves as the source provider so we can get notify events for our candidate
                        sourceProvider = this@MozcCandidateProvider,
                    )
                )
            }
        }
        return suggestions
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        // TODO: tell mozc what i chose and tell android to type it, still waiting on upstream to implement k3lp subtypes...
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
