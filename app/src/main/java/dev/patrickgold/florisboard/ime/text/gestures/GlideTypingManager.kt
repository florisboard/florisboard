package dev.patrickgold.florisboard.ime.text.gestures

import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.key.KeyView
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Handles the [GlideTypingClassifier]. Basically responsible for linking [GlideTypingGesture.Detector]
 * with [GlideTypingClassifier].
 */
class GlideTypingManager : GlideTypingGesture.Listener, CoroutineScope by MainScope() {

    private var glideTypingClassifier = StatisticalGlideTypingClassifier()
    private val initialDimensions: HashMap<Subtype, Dimensions> = hashMapOf()
    private var currentDimensions: Dimensions = Dimensions(0f, 0f)
    private lateinit var prefHelper: PrefHelper

    companion object {
        private const val MAX_SUGGESTION_COUNT = 8

        private lateinit var glideTypingManager: GlideTypingManager
        fun getInstance(): GlideTypingManager {
            if (!this::glideTypingManager.isInitialized) {
                glideTypingManager = GlideTypingManager()
                glideTypingManager.prefHelper = PrefHelper.getDefaultInstance(FlorisBoard.getInstance().context)
            }
            return glideTypingManager
        }
    }

    override fun onGestureComplete(data: GlideTypingGesture.Detector.PointerData) {
        updateSuggestionsAsync(MAX_SUGGESTION_COUNT, true) {
            glideTypingClassifier.clear()
        }
    }

    private var lastTime = System.currentTimeMillis()
    override fun onGestureAdd(point: GlideTypingGesture.Detector.Position) {
        val normalized = GlideTypingGesture.Detector.Position(normalizeX(point.x), normalizeY(point.y))

        this.glideTypingClassifier.addGesturePoint(normalized)

        val time = System.currentTimeMillis()
        if (prefHelper.glide.showPreview && time - lastTime > prefHelper.glide.previewRefreshDelay) {
            updateSuggestionsAsync(1, false) {}
            lastTime = time
        }
    }

    /**
     * Change the layout of the internal gesture classifier
     */
    fun setLayout(keys: Sequence<KeyView>, dimensions: Dimensions) {
        glideTypingClassifier.setLayout(keys, FlorisBoard.getInstance().activeSubtype)
        initialDimensions.getOrPut(FlorisBoard.getInstance().activeSubtype, {
            dimensions
        })
    }

    /**
     * Set the word data for the internal gesture classifier
     */
    fun setWordData(subtype: Subtype) {
        launch(Dispatchers.Default) {
            // FIXME: get this info from dictionary.
            val data =
                AssetManager.default().loadAssetRaw(AssetRef(AssetSource.Assets, "ime/dict/data.json")).getOrThrow()
            val json = JSONObject(data)
            val map = hashMapOf<String, Int>()
            map.putAll(json.keys().asSequence().map { Pair(it, json.getInt(it)) })
            glideTypingClassifier.setWordData(map, subtype)
        }
    }

    fun updateDimensions(dimensions: Dimensions) {
        this.currentDimensions = dimensions
    }

    /**
     * To avoid constantly having to regenerate Pruners every time we switch between landscape and portrait or enable/
     * disable one handed mode, we just normalize the x, y coordinates to the same range as the original which were
     * active when the Pruner was created.
     */
    private fun normalizeX(x: Float): Float {
        val initial = initialDimensions[FlorisBoard.getInstance().activeSubtype] ?: return x

        return scaleRange(
            x,
            0f,
            currentDimensions.width,
            0f,
            initial.width
        )
    }

    /**
     * To avoid constantly having to regenerate Pruners every time we switch between landscape and portrait or enable/
     * disable one handed mode, we just normalize the x, y coordinates to the same range as the original which were
     * active when the Pruner was created.
     */
    private fun normalizeY(y: Float): Float {
        val initial = initialDimensions[FlorisBoard.getInstance().activeSubtype] ?: return y

        return scaleRange(
            y,
            0f,
            currentDimensions.height,
            0f,
            initial.height
        )
    }

    private fun scaleRange(x: Float, oldMin: Float, oldMax: Float, newMin: Float, newMax: Float): Float {
        return (((x - oldMin) * (newMax - newMin)) / (oldMax - oldMin)) + newMin
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
                textInputManager.hackyGlideSuggestionSkip = false
                textInputManager.smartbarView?.setCandidateSuggestionWords(
                    System.nanoTime(),
                    suggestions.take(maxSuggestionsToShow).map { textInputManager.fixCase(it) }
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

data class Dimensions(
    val width: Float,
    val height: Float
)
