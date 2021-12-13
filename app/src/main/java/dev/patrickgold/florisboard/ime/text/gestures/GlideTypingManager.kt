package dev.patrickgold.florisboard.ime.text.gestures

import android.content.Context
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.nlp.SuggestionList
import dev.patrickgold.florisboard.ime.text.keyboard.TextKey
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.min

/**
 * Handles the [GlideTypingClassifier]. Basically responsible for linking [GlideTypingGesture.Detector]
 * with [GlideTypingClassifier].
 */
class GlideTypingManager(context: Context) : GlideTypingGesture.Listener, CoroutineScope by MainScope() {
    companion object {
        private const val MAX_SUGGESTION_COUNT = 8
    }

    private val prefs by florisPreferenceModel()
    private val assetManager by context.assetManager()
    private val keyboardManager by context.keyboardManager()
    private val nlpManager by context.nlpManager()
    private val subtypeManager by context.subtypeManager()

    private var glideTypingClassifier = StatisticalGlideTypingClassifier()
    private var lastTime = System.currentTimeMillis()
    private val wordDataCache = hashMapOf<String, Int>()

    override fun onGlideComplete(data: GlideTypingGesture.Detector.PointerData) {
        updateSuggestionsAsync(MAX_SUGGESTION_COUNT, true) {
            glideTypingClassifier.clear()
        }
    }

    override fun onGlideCancelled() {
        glideTypingClassifier.clear()
    }

    override fun onGlideAddPoint(point: GlideTypingGesture.Detector.Position) {
        val normalized = GlideTypingGesture.Detector.Position(point.x, point.y)

        this.glideTypingClassifier.addGesturePoint(normalized)

        val time = System.currentTimeMillis()
        if (prefs.glide.showPreview.get() && time - lastTime > prefs.glide.previewRefreshDelay.get()) {
            updateSuggestionsAsync(1, false) {}
            lastTime = time
        }
    }

    /**
     * Change the layout of the internal gesture classifier
     */
    fun setLayout(keys: List<TextKey>) {
        if (keys.isNotEmpty()) {
            glideTypingClassifier.setLayout(keys, subtypeManager.activeSubtype())
        }
    }

    /**
     * Set the word data for the internal gesture classifier
     */
    fun setWordData(subtype: Subtype) {
        launch(Dispatchers.Default) {
            if (wordDataCache.isEmpty()) {
                // FIXME: get this info from dictionary.
                val data = assetManager.loadTextAsset(FlorisRef.assets("ime/dict/data.json"))
                        .getOrThrow()
                val json = JSONObject(data)
                wordDataCache.putAll(json.keys().asSequence().map { Pair(it, json.getInt(it)) })
            }
            glideTypingClassifier.setWordData(wordDataCache, subtype)
        }
    }

    /**
     * Asks gesture classifier for suggestions and then passes that on to the smartbar.
     * Also commits the most confident suggestion if [commit] is set. All happens on an async executor.
     * NB: only fetches [MAX_SUGGESTION_COUNT] suggestions.
     *
     * @param callback Called when this function completes. Takes a boolean, which indicates if suggestions
     * were successfully set.
     */
    private fun updateSuggestionsAsync(maxSuggestionsToShow: Int, commit: Boolean, callback: (Boolean) -> Unit) {
        if (!glideTypingClassifier.ready) {
            callback.invoke(false)
            return
        }

        launch(Dispatchers.Default) {
            val suggestions = glideTypingClassifier.getSuggestions(MAX_SUGGESTION_COUNT, true)

            withContext(Dispatchers.Main) {
                val suggestionList = SuggestionList.new(1)
                suggestions.subList(
                    1.coerceAtMost(min(commit.compareTo(false), suggestions.size)),
                    maxSuggestionsToShow.coerceAtMost(suggestions.size)
                ).map { keyboardManager.fixCase(it) }.forEach {
                    suggestionList.add(it, 255)
                }
                nlpManager.suggestDirectly(suggestionList.toList())
                suggestionList.dispose()
                if (commit && suggestions.isNotEmpty()) {
                    keyboardManager.commitGesture(suggestions.first())
                }
                callback.invoke(true)
            }
        }
    }
}
