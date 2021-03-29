package dev.patrickgold.florisboard.ime.text.gestures

import android.util.SparseArray
import androidx.core.util.set
import dev.patrickgold.florisboard.ime.text.gestures.GestureTypingClassifier.Gesture
import dev.patrickgold.florisboard.ime.text.key.FlorisKeyData
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData
import timber.log.Timber
import java.text.Normalizer
import java.util.*
import kotlin.math.*

class StatisticalGestureTypingClassifier : GestureTypingClassifier {

    private var isInitialized: Boolean = false
    private val gesture = Gesture()
    private var keysByCharacter: SparseArray<FlorisKeyData> = SparseArray()
    private var words: Array<String> = arrayOf()
    private var wordFrequencies: Array<Int> = arrayOf()
    private var keys: ArrayList<FlorisKeyData> = arrayListOf()
    private lateinit var pruner: Pruner

    companion object {
        private const val PRUNING_LENGTH_THRESHOLD = 8.42
        private const val SAMPLING_POINTS: Int = 300

        /**
         * Standard deviation of the distribution of distances between the shapes of two gestures
         * representing the same word. It's expressed for normalized gestures and is therefore
         * independent of the keyboard or key size.
         */
        private const val SHAPE_STD = 22.08f

        /**
         * Standard deviation of the distribution of distances between the locations of two gestures
         * representing the same word. It's expressed as a factor of key radius as it's applied to
         * un-normalized gestures and is therefore dependent on the size of the keys/keyboard.
         */
        private const val LOCATION_STD = 0.5109f
    }

    override fun addGesturePoint(position: GlideTypingGesture.Detector.Position) {
        gesture.addPoint(position.x, position.y)
    }

    override fun isInitialized(): Boolean {
        return isInitialized
    }

    override fun initialize(computedLayoutData: ComputedLayoutData, words: Array<String>, wordFrequencies: Array<Int>) {
        computedLayoutData.arrangement.forEach { row ->
            row.forEach {
                keysByCharacter[it.code] = it
                this.keys.add(it)
            }
        }

        this.words = words
        this.wordFrequencies = wordFrequencies
        this.pruner = Pruner(PRUNING_LENGTH_THRESHOLD, words, keysByCharacter)


        isInitialized = true
    }

    override fun initGestureFromPointerData(pointerData: GlideTypingGesture.Detector.PointerData) {
        for (position in pointerData.positions) {
            addGesturePoint(position)
        }
    }

    override fun getSuggestions(maxSuggestionCount: Int, gestureCompleted: Boolean): List<String> {
        val candidates = arrayListOf<String>()
        val candidateWeights = arrayListOf<Int>()

        // 'h' just because it's in the middle of the keyboard and has a typical size unlike some
        // special keys.
        val key = keysByCharacter.get('h'.toInt())
        val radius: Int = min(key.height, key.width)
        val normalizedUserGesture: Gesture = gesture.normalizeByBoxSide()
        Timber.d("Words: ${words.asList()} ${this.keys.map { "${it.label} ${it.x} ${it.y}" }}")
        var remainingWords = pruner.pruneByExtremities(gesture, this.keys)
        Timber.d("Remaining words: $remainingWords")
        remainingWords     = pruner.pruneByLength(gesture, remainingWords, keysByCharacter)

        Timber.d("Remaining words: $remainingWords")


        for (i in remainingWords.indices) {
            val word = remainingWords[i]
            val wordGesture: Gesture = Gesture.generateIdealGesture(word, keysByCharacter)
            val normalizedGesture: Gesture = wordGesture.normalizeByBoxSide()
            val shapeDistance = calcShapeDistance(normalizedGesture, normalizedUserGesture)
            val locationDistance = calcLocationDistance(wordGesture, gesture)
            val shapeProbability = calcGaussianProbability(shapeDistance, 0.0f, SHAPE_STD)
            val locationProbability = calcGaussianProbability(locationDistance, 0.0f, LOCATION_STD * radius)
            val frequency = wordFrequencies[words.indexOf(word)]
            val confidenceFloat = 1.0 / (shapeProbability * locationProbability * frequency)
            val confidence = ((confidenceFloat) * 255).toInt()
            var candidateDistanceSortedIndex = 0
            while (candidateDistanceSortedIndex < candidateWeights.size
                && candidateWeights[candidateDistanceSortedIndex] <= confidence) {
                candidateDistanceSortedIndex++
            }
            if (candidateDistanceSortedIndex < maxSuggestionCount) {
                candidateWeights.add(candidateDistanceSortedIndex, confidence)
                candidates.add(candidateDistanceSortedIndex, word)
                if (candidateWeights.size > maxSuggestionCount) {
                    candidateWeights.removeAt(maxSuggestionCount)
                    candidates.removeAt(maxSuggestionCount)
                }
            }
        }
        return candidates
    }

    override fun clear() {
        this.gesture.clear()
    }

    private fun calcLocationDistance(gesture1: Gesture, gesture2: Gesture): Float {
        var totalDistance = 0.0f
        for (i in 0 until SAMPLING_POINTS) {
            val x1 = gesture1.sampleX(i.toFloat() / SAMPLING_POINTS)
            val x2 = gesture2.sampleX(i.toFloat() / SAMPLING_POINTS)
            val y1 = gesture1.sampleY(i.toFloat() / SAMPLING_POINTS)
            val y2 = gesture2.sampleY(i.toFloat() / SAMPLING_POINTS)
            val distance = abs(x1 - x2) + abs(y1 - y2)
            totalDistance += distance
        }
        return totalDistance / SAMPLING_POINTS / 2

    }

    private fun calcGaussianProbability(value: Float, mean: Float, standardDeviation: Float): Float {
        val factor = 1.0 / (standardDeviation * sqrt(2 * PI))
        val exponent = ((value - mean) / standardDeviation).toDouble().pow(2.0)
        val probability = factor * exp(-1.0 / 2 * exponent)
        return probability.toFloat()
    }

    private fun calcShapeDistance(gesture1: Gesture, gesture2: Gesture): Float {
        var distance: Float
        var totalDistance = 0.0f
        for (i in 0 until SAMPLING_POINTS) {
            val x1 = gesture1.sampleX(i.toFloat() / SAMPLING_POINTS)
            val x2 = gesture2.sampleX(i.toFloat() / SAMPLING_POINTS)
            val y1 = gesture1.sampleY(i.toFloat() / SAMPLING_POINTS)
            val y2 = gesture2.sampleY(i.toFloat() / SAMPLING_POINTS)
            distance = Gesture.distance(x1, y1, x2, y2)
            totalDistance += distance
        }
        return totalDistance
    }

    class Pruner(
        /**
         * The length difference between a user gesture and a word gesture above which a word will
         * be pruned.
         */
        private val lengthThreshold: Double, words: Array<String>, keysByCharacter: SparseArray<FlorisKeyData>) {

        /** A tree that provides fast access to words based on their first and last letter.  */
        private val wordTree = HashMap<Pair<FlorisKeyData, FlorisKeyData>, ArrayList<String>>()

        /**
         * Finds the words whose start and end letter are closest to the start and end points of the
         * user gesture.
         *
         * @param userGesture The current user gesture.
         * @param keys The keys on the keyboard.
         * @return A list of likely words.
         */
        fun pruneByExtremities(
            userGesture: Gesture, keys: Iterable<FlorisKeyData>): ArrayList<String> {
            val remainingWords = ArrayList<String>()
            val startX = userGesture.getFirstX()
            val startY = userGesture.getFirstY()
            val endX = userGesture.getLastX()
            val endY = userGesture.getLastY()
            val startKeys = findNClosestKeys(startX, startY, 2, keys)
            val endKeys = findNClosestKeys(endX, endY, 2, keys)
            for (startKey in startKeys) {
                for (endKey in endKeys) {
                    Timber.d("$startKey $endKey")
                    val keyPair: Pair<FlorisKeyData, FlorisKeyData> = Pair(startKey, endKey)
                    val wordsForKeys = wordTree[keyPair]
                    if (wordsForKeys != null) {
                        remainingWords.addAll(wordsForKeys)
                    }
                }
            }
            return remainingWords
        }

        /**
         * Finds the words whose ideal gesture length is within a certain threshold of the user
         * gesture's length.
         *
         * @param userGesture The current user gesture.
         * @param words A list of words to consider.
         * @return A list of words that remained after pruning the input list by length.
         */
        fun pruneByLength(
            userGesture: Gesture,
            words: ArrayList<String>,
            keysByCharacter: SparseArray<FlorisKeyData>): ArrayList<String> {
            val remainingWords = ArrayList<String>()

            // 'h' just because it's in the middle of the keyboard and has a typical size unlike
            // some
            // special keys.
            val key = keysByCharacter['h'.toInt()]
            val radius = min(key.height, key.width)
            val userLength = userGesture.getLength()
            for (word in words) {
                val idealGesture = Gesture.generateIdealGesture(word, keysByCharacter)
                val wordIdealLength = idealGesture.getLength()
                if (abs(userLength - wordIdealLength) < lengthThreshold * radius) {
                    remainingWords.add(word)
                }
            }
            return remainingWords
        }

        companion object {
            private fun getFirstKeyLastKey(
                word: String, keysByCharacter: SparseArray<FlorisKeyData>): Pair<FlorisKeyData, FlorisKeyData> {
                val firstLetter = word[0]
                val lastLetter = word[word.length - 1]
                var baseCharacter: Char = Normalizer.normalize(firstLetter.toString(), Normalizer.Form.NFD)[0]
                val firstKey: FlorisKeyData = keysByCharacter[baseCharacter.toInt()]
                baseCharacter = Normalizer.normalize(lastLetter.toString(), Normalizer.Form.NFD)[0]
                val lastKey: FlorisKeyData = keysByCharacter[baseCharacter.toInt()]
                return Pair(firstKey, lastKey)
            }

            /**
             * Finds a chosen number of keys closest to a given point on the keyboard.
             *
             * @param x X coordinate of the point.
             * @param y Y coordinate of the point.
             * @param n The number of keys to return.
             * @param keys The keys of the keyboard.
             * @return A list of the n closest keys.
             */
            private fun findNClosestKeys(
                x: Float, y: Float, n: Int, keys: Iterable<FlorisKeyData>): Iterable<FlorisKeyData> {
                val keyDistances = HashMap<FlorisKeyData, Float>()
                for (key in keys) {
                    val distance = Gesture.distance(key.x, key.y, x, y)
                    keyDistances[key] = distance
                }

                return keyDistances.entries.sortedWith{ c1, c2 -> c1.value.compareTo(c2.value) }.take(n).map { it.key }
            }
        }

        init {
            for (word in words) {
                val keyPair: Pair<FlorisKeyData, FlorisKeyData> = getFirstKeyLastKey(word, keysByCharacter)
                var wordsForPair = wordTree[keyPair]
                if (wordsForPair == null) {
                    wordsForPair = java.util.ArrayList()
                    wordTree[keyPair] = wordsForPair
                }
                wordsForPair.add(word)
            }
        }
    }


}
