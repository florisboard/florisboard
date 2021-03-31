package dev.patrickgold.florisboard.ime.text.gestures

import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Handles the [GestureTypingClassifier] and its interactions with suggestions.
 */
class GlideTypingManager: GlideTypingGesture.Listener, CoroutineScope by MainScope() {

    private var gestureTypingClassifier = StatisticalGestureTypingClassifier()

    override fun onGestureComplete(data: GlideTypingGesture.Detector.PointerData) {
        updateSuggestionsAsync(5, true) {
            gestureTypingClassifier.clear()
        }
    }

    private var lastTime = System.currentTimeMillis()
    override fun onGestureAdd(point: GlideTypingGesture.Detector.Position) {
        this.gestureTypingClassifier.addGesturePoint(point)

        val time = System.currentTimeMillis()
        if (time - lastTime > 100) {
            updateSuggestionsAsync(1, false) {}
            lastTime = time
        }
    }

    fun setLayout(computedLayout: ComputedLayoutData){
        gestureTypingClassifier.setLayout(computedLayout)

    }

    fun setWordData(){
        // FIXME: get this info from dictionary.
        val data = AssetManager.default().loadAssetRaw(AssetRef(AssetSource.Assets, "ime/dict/data.json")).getOrThrow()
        val json = JSONObject(data)
        val map = hashMapOf<String, Int>()
        map.putAll(json.keys().asSequence().map { Pair(it, json.getInt(it)) })
        gestureTypingClassifier.setWordData(map)
    }

    companion object {
        private const val MAX_SUGGESTION_COUNT = 5

        private lateinit var glideTypingManager: GlideTypingManager
        fun getInstance(): GlideTypingManager{
            if (!this::glideTypingManager.isInitialized)
                glideTypingManager = GlideTypingManager()

            return glideTypingManager
        }
    }

    /**
     * Asks gesture classifier for suggestions and then passes that on to the smartbar.
     * Also commits the most confident suggestion if [commit] is set. All happens on an async executor.
     * NB: only fetches [MAX_SUGGESTION_COUNT] suggestions.
     */
    private fun updateSuggestionsAsync(maxSuggestionsToShow: Int, commit: Boolean, callback: () -> Unit) {
        launch(Dispatchers.Default) {
            // To avoid cache misses when maxSuggestions goes from 5 to 1.
            val suggestions = gestureTypingClassifier.getSuggestions(MAX_SUGGESTION_COUNT, true)

            withContext(Dispatchers.Main) {
                if (commit && suggestions.isNotEmpty())
                    FlorisBoard.getInstance().activeEditorInstance.commitText("${suggestions.first()} ")
                TextInputManager.getInstance().smartbarView?.setCandidateSuggestionWords(
                    System.nanoTime(),
                    suggestions.take(maxSuggestionsToShow)
                )
                TextInputManager.getInstance().smartbarView?.updateCandidateSuggestionCapsState()
                callback.invoke()
            }
        }
    }
}
