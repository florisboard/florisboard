package dev.patrickgold.florisboard.ime.text.gestures

import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.keyboard.TextKey
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.res.FlorisRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Handles the [GlideTypingClassifier]. Basically responsible for linking [GlideTypingGesture.Detector]
 * with [GlideTypingClassifier].
 */
class GlideTypingManager : GlideTypingGesture.Listener, CoroutineScope by MainScope() {
    private var glideTypingClassifier = StatisticalGlideTypingClassifier()
    private val prefs get() = Preferences.default()

    companion object {
        private const val MAX_SUGGESTION_COUNT = 8

        private lateinit var glideTypingManager: GlideTypingManager
        fun getInstance(): GlideTypingManager {
            if (!this::glideTypingManager.isInitialized) {
                glideTypingManager = GlideTypingManager()
            }
            return glideTypingManager
        }
    }

    override fun onGlideComplete(data: GlideTypingGesture.Detector.PointerData) {
        updateSuggestionsAsync(MAX_SUGGESTION_COUNT, true) {
            glideTypingClassifier.clear()
        }
    }

    override fun onGlideCancelled() {
        glideTypingClassifier.clear()
    }

    private var lastTime = System.currentTimeMillis()
    override fun onGlideAddPoint(point: GlideTypingGesture.Detector.Position) {
        val normalized = GlideTypingGesture.Detector.Position(point.x, point.y)

        this.glideTypingClassifier.addGesturePoint(normalized)

        val time = System.currentTimeMillis()
        if (prefs.glide.showPreview && time - lastTime > prefs.glide.previewRefreshDelay) {
            updateSuggestionsAsync(1, false) {}
            lastTime = time
        }
    }

    /**
     * Change the layout of the internal gesture classifier
     */
    fun setLayout(keys: List<TextKey>) {
        glideTypingClassifier.setLayout(keys, FlorisBoard.getInstance().activeSubtype)
    }

    private val wordDataCache = hashMapOf<String, Int>()

    /**
     * Set the word data for the internal gesture classifier
     */
    fun setWordData(subtype: Subtype) {
        launch(Dispatchers.Default) {
            if (wordDataCache.isEmpty()) {
                // FIXME: get this info from dictionary.
                val data =
                    AssetManager.default().loadTextAsset(FlorisRef.assets("ime/dict/data.json"))
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
            // To avoid cache misses when maxSuggestions goes from 5 to 1.
            val time = System.nanoTime()
            val suggestions = glideTypingClassifier.getSuggestions(MAX_SUGGESTION_COUNT, true)

            withContext(Dispatchers.Main) {
                val textInputManager = TextInputManager.getInstance()
                textInputManager.isGlidePostEffect = true
                textInputManager.smartbarView?.setCandidateSuggestionWords(
                    time,
                    // FIXME
                    /*suggestions.subList(
                        1.coerceAtMost(min(commit.compareTo(false), suggestions.size)),
                        maxSuggestionsToShow.coerceAtMost(suggestions.size)
                    ).map { textInputManager.fixCase(it) }*/
                    null
                )
                textInputManager.smartbarView?.updateCandidateSuggestionCapsState()
                if (commit && suggestions.isNotEmpty()) {
                    textInputManager.handleGesture(suggestions.first())
                }
                callback.invoke(true)
            }
        }
    }
}
