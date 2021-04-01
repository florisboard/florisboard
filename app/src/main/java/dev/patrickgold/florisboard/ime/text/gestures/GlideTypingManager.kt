package dev.patrickgold.florisboard.ime.text.gestures

import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Handles the [GlideTypingClassifier]. Basically responsible for linking [GlideTypingGesture.Detector]
 * with [GlideTypingClassifier].
 */
class GlideTypingManager : GlideTypingGesture.Listener, CoroutineScope by MainScope() {

    private var glideTypingClassifier = StatisticalGlideTypingClassifier()

    override fun onGestureComplete(data: GlideTypingGesture.Detector.PointerData) {
        updateSuggestionsAsync(5, true) {
            glideTypingClassifier.clear()
        }
    }

    private var lastTime = System.currentTimeMillis()
    override fun onGestureAdd(point: GlideTypingGesture.Detector.Position) {
        this.glideTypingClassifier.addGesturePoint(point)

        val time = System.currentTimeMillis()
        if (time - lastTime > 100) {
            updateSuggestionsAsync(1, false) {}
            lastTime = time
        }
    }

    /**
     * Change the layout of the internal gesture classifier
     */
    fun setLayout(computedLayout: ComputedLayoutData) {
        glideTypingClassifier.setLayout(computedLayout)
    }

    /**
     * Set the word data for the internal gesture classifier
     */
    fun setWordData() {
        launch (Dispatchers.Default) {
            // FIXME: get this info from dictionary.
            val data =
                AssetManager.default().loadAssetRaw(AssetRef(AssetSource.Assets, "ime/dict/data.json")).getOrThrow()
            val json = JSONObject(data)
            val map = hashMapOf<String, Int>()
            map.putAll(json.keys().asSequence().map { Pair(it, json.getInt(it)) })
            glideTypingClassifier.setWordData(map)
        }
    }

    companion object {
        private const val MAX_SUGGESTION_COUNT = 8

        private lateinit var glideTypingManager: GlideTypingManager
        fun getInstance(): GlideTypingManager {
            if (!this::glideTypingManager.isInitialized)
                glideTypingManager = GlideTypingManager()

            return glideTypingManager
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
            val suggestions = glideTypingClassifier.getSuggestions(MAX_SUGGESTION_COUNT, true)

            withContext(Dispatchers.Main) {
                val textInputManager = TextInputManager.getInstance()
                textInputManager.glideSuggestionsActive = true
                textInputManager.smartbarView?.setCandidateSuggestionWords(
                    System.nanoTime(),
                    suggestions.take(maxSuggestionsToShow)
                )
                textInputManager.smartbarView?.updateCandidateSuggestionCapsState()
                if (commit && suggestions.isNotEmpty()) {
                    FlorisBoard.getInstance().activeEditorInstance.commitGesture(suggestions.first())
                }
                callback.invoke(true)
            }
        }
    }
}
