package dev.patrickgold.florisboard.ime.media.emoji

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.stream.Collectors
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.emoji2.text.EmojiCompat
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.nlp.EmojiSuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.subtypeManager

const val EMOJI_SUGGESTION_INDICATOR = ':'
const val EMOJI_SUGGESTION_MAX_COUNT = 5
private const val EMOJI_SUGGESTION_QUERY_MIN_LENGTH = 3

/**
 * Provides emoji suggestions within a text input context.
 *
 * This class handles the following tasks:
 * - Initializes and maintains a list of supported emojis.
 * - Generates and returns emoji suggestions based on user input and preferences.
 *
 * @param context The application context.
 */
class EmojiSuggestionProvider(private val context: Context) : SuggestionProvider {

    override val providerId = "org.florisboard.nlp.providers.emoji"

    private val prefs by florisPreferenceModel()
    private val editorInstance by context.editorInstance()
    private val subtypeManager by context.subtypeManager()
    private val supportedEmojiSet: Set<Emoji> by lazy { initSupportedEmojiSet() }
    private val lettersRegex = "^:[A-Za-z]*$".toRegex()

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean
    ): List<SuggestionCandidate> {
        val query = validateInputQuery(content.composingText) ?: return emptyList()
        return generateEmojiSuggestions(query, maxCandidateCount)
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        // No-op
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        // No-op
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate) = false

    override suspend fun getListOfWords(subtype: Subtype) = emptyList<String>()

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String) = 0.0

    override suspend fun create() {
        // No-op
    }

    override suspend fun preload(subtype: Subtype) {
        // No-op
    }

    override suspend fun destroy() {
        // No-op
    }

    /**
     * Initializes the list of supported emojis.
     */
    private fun initSupportedEmojiSet() = with(editorInstance.activeInfo) {
        val emojiCompatInstance = FlorisEmojiCompat.getAsFlow(emojiCompatReplaceAll).value
        val systemFontPaint = Paint().apply { typeface = Typeface.DEFAULT }
        val emojiPreferredSkinTone = prefs.media.emojiPreferredSkinTone.get()
        val isEmojiSupported = { emoji: Emoji ->
            val emojiMatch = emojiCompatInstance?.getEmojiMatch(emoji.value, emojiCompatMetadataVersion)
            emojiMatch == EmojiCompat.EMOJI_SUPPORTED || systemFontPaint.hasGlyph(emoji.value)
        }
        parseRawEmojiSpecsFile(context, resolveEmojiAssetPath(context, subtypeManager.activeSubtype)).values.flatten()
            .filter { isEmojiSupported(it.base()) }.map { it.base(emojiPreferredSkinTone) }.toSet()
    }

    /**
     * Validates the user input query for emoji suggestions.
     */
    private fun validateInputQuery(composingText: CharSequence): String? {
        if (!composingText.startsWith(EMOJI_SUGGESTION_INDICATOR)) {
            return null
        }
        if (composingText.length <= EMOJI_SUGGESTION_QUERY_MIN_LENGTH) {
            return null
        }
        if (!lettersRegex.matches(composingText)) {
            return null
        }
        return composingText.substring(1)
    }

    /**
     * Generates emoji suggestions based on user input and preferences. Only returns max [maxCandidateCount] suggestions.
     */
    private suspend fun generateEmojiSuggestions(query: String, maxCandidateCount: Int): List<SuggestionCandidate> {
        return withContext(Dispatchers.Default) {
            supportedEmojiSet.parallelStream()
                .filter { emoji -> emoji.name.contains(query) && emoji.keywords.any { it.contains(query) } }
                .limit(maxCandidateCount.toLong()).map(::EmojiSuggestionCandidate)
                .collect(Collectors.toList())
        }
    }
}
