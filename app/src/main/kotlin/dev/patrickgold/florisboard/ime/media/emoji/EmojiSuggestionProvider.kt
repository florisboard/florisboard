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
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.subtypeManager
import io.github.reactivecircus.cache4k.Cache

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
    private lateinit var supportedEmojiSet: Set<Emoji>
    private val lettersRegex = "^:[A-Za-z]*$".toRegex()

    private val cachedEmojiMappings = Cache.Builder().build<FlorisLocale, Map<EmojiSkinTone, List<Emoji>>>()

    override suspend fun create() {
        //supportedEmojiSet = initSupportedEmojiSet()
    }

    override suspend fun preload(subtype: Subtype) {
        subtype.locales().forEach { locale ->
            cachedEmojiMappings.get(locale) {
                val map = mutableMapOf<EmojiSkinTone, MutableList<Emoji>>()
                EmojiLayoutData.get(context, locale).values.flatten().forEach { emojiSet ->
                    emojiSet.emojis.forEach { emoji ->
                        val list = map.getOrPut(emoji.skinTone) { mutableListOf() }
                        list.add(emoji)
                    }
                }
                map.map { it.key to it.value.toList() }.toMap()
            }
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean
    ): List<SuggestionCandidate> {
        val preferredSkinTone = prefs.media.emojiPreferredSkinTone.get()
        val query = validateInputQuery(content.composingText) ?: return emptyList()
        val emojis = cachedEmojiMappings.get(subtype.primaryLocale)?.get(preferredSkinTone) ?: emptyList()
        val candidates = withContext(Dispatchers.Default) {
            emojis.parallelStream()
                .filter { emoji -> emoji.name.contains(query) && emoji.keywords.any { it.contains(query) } }
                .limit(maxCandidateCount.toLong())
                .map { EmojiSuggestionCandidate(it) }
                .collect(Collectors.toList())
        }
        return candidates
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

    override suspend fun destroy() {
        cachedEmojiMappings.invalidateAll()
    }

    /**
     * Initializes the list of supported emojis.
     */
    // TODO: what do do about this??
    /*private fun initSupportedEmojiSet() = with(editorInstance.activeInfo) {
        val emojiCompatInstance = FlorisEmojiCompat.getAsFlow(false).value
        val systemFontPaint = Paint().apply { typeface = Typeface.DEFAULT }
        val emojiPreferredSkinTone = prefs.media.emojiPreferredSkinTone.get()
        val isEmojiSupported = { emoji: Emoji ->
            val emojiMatch = emojiCompatInstance?.getEmojiMatch(emoji.value, emojiCompatMetadataVersion)
            emojiMatch == EmojiCompat.EMOJI_SUPPORTED || systemFontPaint.hasGlyph(emoji.value)
        }
        getOrLoadEmojiDataMap(context, resolveEmojiAssetPath(context, subtypeManager.activeSubtype.primaryLocale)).values.flatten()
            .filter { isEmojiSupported(it.base()) }.map { it.base(emojiPreferredSkinTone) }.toSet()
    }*/

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
}
