package dev.patrickgold.florisboard.ime.text.gestures

import android.util.SparseArray
import androidx.collection.LruCache
import androidx.core.util.set
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.key.FlorisKeyData
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData
import java.text.Normalizer
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.*

/**
 * Classifies gestures by comparing them with an "ideal gesture".
 *
 * Check out Étienne Desticourt's excellent write up at https://github.com/AnySoftKeyboard/AnySoftKeyboard/pull/1870
 */
class StatisticalGlideTypingClassifier : GlideTypingClassifier {

    private val gesture = Gesture()
    private var keysByCharacter: SparseArray<FlorisKeyData> = SparseArray()
    private var words: Set<String> = setOf()
    private var wordFrequencies: Map<String, Int> = hashMapOf()
    private var keys: ArrayList<FlorisKeyData> = arrayListOf()
    private lateinit var pruner: Pruner
    private var wordDataSet = false
    private var layoutSet = false
    val ready: Boolean
        get() = !(wordDataSet || layoutSet) && this.keys.isNotEmpty()

    companion object {
        /**
         * Describes the allowed length variance in a gesture. If a gesture is too long or too short, it is immediately
         * discarded to save cycles.
         */
        private const val PRUNING_LENGTH_THRESHOLD = 8.42

        /**
         * describes the number of points to sample a gesture at, i.e the resolution.
         */
        private const val SAMPLING_POINTS: Int = 200

        /**
         * The minimum distance between points to be added to a gesture.
         */
        private const val MIN_DIST_TO_ADD = 1000

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

        /**
         * This is a very small cache that caches suggestions, so that they aren't recalculated e.g when releasing
         * a pointer when the suggestions were already calculated. Avoids a lot of micro pauses.
         */
        private const val SUGGESTION_CACHE_SIZE = 5

        /**
         * For multiple subtypes, the pruner is cached.
         */
        private const val PRUNER_CACHE_SIZE = 5
    }

    override fun addGesturePoint(position: GlideTypingGesture.Detector.Position) {
        if (!gesture.isEmpty) {
            val dx = gesture.getLastX() - position.x
            val dy = gesture.getLastY() - position.y

            if (dx * dx + dy * dy > MIN_DIST_TO_ADD) {
                gesture.addPoint(position.x, position.y)
            }
        } else {
            gesture.addPoint(position.x, position.y)
        }
    }

    override fun setLayout(computedLayoutData: ComputedLayoutData) {
        keysByCharacter.clear()
        keys.clear()
        computedLayoutData.arrangement.forEach { row ->
            row.forEach {
                keysByCharacter[it.code] = it
                this.keys.add(it)
            }
        }

        layoutSet = true

        if (wordDataSet && layoutSet){
            initializePruner()
        }
    }

    override fun setWordData(words: HashMap<String, Int>) {
        this.words = words.keys
        this.wordFrequencies = words

        this.wordDataSet = true

        if (wordDataSet && layoutSet){
            initializePruner()
        }
    }

    private val prunerCache = LruCache<Subtype, Pruner>(PRUNER_CACHE_SIZE)

    /**
     * Exists because Pruner requires both word data and layout are initialized,
     * however we don't know what order they're initialized in.
     */
    private fun initializePruner(){
        val currentSubtype = FlorisBoard.getInstance().activeSubtype
        val cached = prunerCache.get(currentSubtype)
        if (cached == null) {
            this.pruner = Pruner(PRUNING_LENGTH_THRESHOLD, this.words, keysByCharacter)
            prunerCache.put(currentSubtype, this.pruner)
        }else {
            this.pruner = cached
        }
        // Reset so that same mechanism can be used in case subtype changes.
        this.layoutSet = false
        this.wordDataSet = false
    }

    override fun initGestureFromPointerData(pointerData: GlideTypingGesture.Detector.PointerData) {
        for (position in pointerData.positions) {
            addGesturePoint(position)
        }
    }


    private val lruSuggestionCache = LruCache<Pair<Gesture, Int>, List<String>>(SUGGESTION_CACHE_SIZE)
    override fun getSuggestions(maxSuggestionCount: Int, gestureCompleted: Boolean): List<String> {

        return when (val cached = lruSuggestionCache.get(Pair(this.gesture, maxSuggestionCount))) {
            null -> {
                val suggestions = unCachedGetSuggestions(maxSuggestionCount)
                lruSuggestionCache.put(Pair(this.gesture.clone(), maxSuggestionCount), suggestions)

                suggestions
            }
            else -> {
                cached
            }
        }
    }

    private fun unCachedGetSuggestions(maxSuggestionCount: Int): List<String> {
        val candidates = arrayListOf<String>()
        val candidateWeights = arrayListOf<Float>()
        val key = keys.firstOrNull() ?: return listOf()
        val radius: Int = min(key.height, key.width)
        var remainingWords = pruner.pruneByExtremities(gesture, this.keys)
        val userGesture = gesture.resample(SAMPLING_POINTS)
        val normalizedUserGesture: Gesture = userGesture.normalizeByBoxSide()
        remainingWords = pruner.pruneByLength(gesture, remainingWords, keysByCharacter, keys)

        for (i in remainingWords.indices) {
            val word = remainingWords[i]
            val wordGesture: Gesture = Gesture.generateIdealGesture(word, keysByCharacter).resample(SAMPLING_POINTS)
            val normalizedGesture: Gesture = wordGesture.normalizeByBoxSide()
            val shapeDistance = calcShapeDistance(normalizedGesture, normalizedUserGesture)
            val locationDistance = calcLocationDistance(wordGesture, userGesture)
            val shapeProbability = calcGaussianProbability(shapeDistance, 0.0f, SHAPE_STD)
            val locationProbability = calcGaussianProbability(locationDistance, 0.0f, LOCATION_STD * radius)
            val frequency = wordFrequencies[word]!!
            val confidence = 1.0f / (shapeProbability * locationProbability * frequency)

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
            val x1 = gesture1.getX(i)
            val x2 = gesture2.getX(i)
            val y1 = gesture1.getY(i)
            val y2 = gesture2.getY(i)
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
            val x1 = gesture1.getX(i)
            val x2 = gesture2.getX(i)
            val y1 = gesture1.getY(i)
            val y2 = gesture2.getY(i)
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
        private val lengthThreshold: Double, words: Set<String>, keysByCharacter: SparseArray<FlorisKeyData>) {

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
                    val keyPair = Pair(startKey, endKey)
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
            keysByCharacter: SparseArray<FlorisKeyData>,
            keys: List<FlorisKeyData>): ArrayList<String> {
            val remainingWords = ArrayList<String>()

            val key = keys.firstOrNull() ?: return arrayListOf()
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
                word: String, keysByCharacter: SparseArray<FlorisKeyData>): Pair<FlorisKeyData, FlorisKeyData>? {
                val firstLetter = word[0]
                val lastLetter = word[word.length - 1]
                val firstBaseChar = Normalizer.normalize(firstLetter.toString(), Normalizer.Form.NFD)[0]
                val lastBaseChar = Normalizer.normalize(lastLetter.toString(), Normalizer.Form.NFD)[0]
                return when {
                    keysByCharacter.indexOfKey(firstBaseChar.toInt()) < 0 || keysByCharacter.indexOfKey(lastBaseChar.toInt()) < 0 -> {
                        null
                    }
                    else -> {
                        val firstKey: FlorisKeyData = keysByCharacter[firstBaseChar.toInt()]
                        val lastKey: FlorisKeyData = keysByCharacter[lastBaseChar.toInt()]
                        Pair(firstKey, lastKey)
                    }
                }
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

                return keyDistances.entries.sortedWith { c1, c2 -> c1.value.compareTo(c2.value) }.take(n).map { it.key }
            }
        }

        init {
            for (word in words) {
                val keyPair = getFirstKeyLastKey(word, keysByCharacter)
                keyPair?.let {
                    wordTree.getOrPut(keyPair, { arrayListOf() }).add(word)
                }
            }
        }
    }

    class Gesture(private val xs: FloatArray, private val ys: FloatArray, private var size: Int) {
        val isEmpty: Boolean
            get() = size == 0

        constructor() : this(FloatArray(MAX_SIZE), FloatArray(MAX_SIZE), 0)

        companion object {
            private const val MAX_SIZE = 1000
            private const val MAX_IDEAL_CACHE_SIZE = 10000
            private val lruIdealCache = LruCache<String, Gesture>(MAX_IDEAL_CACHE_SIZE)

            fun generateIdealGesture(word: String, keysByCharacter: SparseArray<FlorisKeyData>): Gesture {
                return when (val cached = lruIdealCache.get(word)) {
                    null -> {
                        val ideal = unCachedGenerateIdealGesture(word, keysByCharacter)
                        lruIdealCache.put(word, ideal)
                        ideal
                    }
                    else -> {
                        cached
                    }
                }
            }

            private fun unCachedGenerateIdealGesture(
                word: String, keysByCharacter: SparseArray<FlorisKeyData>): Gesture {
                val idealGesture = Gesture()
                var previousLetter = '\u0000'

                // Add points for each key
                for (c in word) {
                    val lc = Character.toLowerCase(c)
                    var key = keysByCharacter[lc.toInt()]
                    if (key == null) {
                        // Try finding the base character instead, e.g., the "e" key instead of "é"
                        val baseCharacter: Char = Normalizer.normalize(lc.toString(), Normalizer.Form.NFD)[0]
                        key = keysByCharacter[baseCharacter.toInt()]
                        if (key == null) {
                            continue
                        }
                    }

                    // We adda little loop on  the key for duplicate letters
                    // so that we can differentiate words like pool and poll, lull and lul, etc...
                    if (previousLetter == lc) {
                        // bottom right
                        idealGesture.addPoint(
                            key.x + key.width / 4.0f, key.y + key.height / 4.0f)
                        // top right
                        idealGesture.addPoint(
                            key.x + key.width / 4.0f, key.y - key.height / 4.0f)
                        // top left
                        idealGesture.addPoint(
                            key.x - key.width / 4.0f, key.y - key.height / 4.0f)
                        // bottom left
                        idealGesture.addPoint(
                            key.x - key.width / 4.0f, key.y + key.height / 4.0f)
                    } else {
                        idealGesture.addPoint(key.x, key.y)
                    }
                    previousLetter = lc
                }
                return idealGesture
            }

            fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
                return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
            }

        }

        fun addPoint(x: Float, y: Float) {
            if (size >= MAX_SIZE){
                return
            }
            xs[size] = x
            ys[size] = y
            size += 1
        }


        /**
         * Resamples the gesture into a new gesture with the chosen number of points by oversampling
         * it.
         *
         * @param numPoints The number of points that the new gesture will have. Must be superior to
         * the number of points in the current gesture.
         * @return An oversampled copy of the gesture.
         */
        fun resample(numPoints: Int): Gesture {
            val interpointDistance = (getLength() / numPoints)
            val resampledGesture = Gesture()
            resampledGesture.addPoint(xs[0], ys[0])
            var lastX = xs[0]
            var lastY = ys[0]
            var newX: Float
            var newY: Float
            var cumulativeError = 0.0f

            // otherwise nothing happens if size is only 1:
            if (this.size == 1) {
                for (i in 0 until SAMPLING_POINTS) {
                    resampledGesture.addPoint(xs[0], ys[0])
                }
            }


            for (i in 0 until size - 1) {
                // We calculate the unit vector from the two points we're between in the actual
                // gesture
                var dx = xs[i + 1] - xs[i]
                var dy = ys[i + 1] - ys[i]
                val norm = sqrt(dx.pow(2.0f) + dy.pow(2.0f))
                dx /= norm
                dy /= norm

                // The number of evenly sampled points that fit between the two actual points
                var numNewPoints = norm / interpointDistance

                // The number of point that'd fit between the two actual points is often not round,
                // which means we'll get an increasingly large error as we resample the gesture
                // and round down that number. To compensate for this we keep track of the error
                // and add additional points when it gets too large.
                cumulativeError += numNewPoints - numNewPoints.toInt()
                if (cumulativeError > 1) {
                    numNewPoints = (numNewPoints.toInt() + cumulativeError.toInt()).toFloat()
                    cumulativeError %= 1
                }
                for (j in 0 until numNewPoints.toInt()) {
                    newX = lastX + dx * interpointDistance
                    newY = lastY + dy * interpointDistance
                    lastX = newX
                    lastY = newY
                    resampledGesture.addPoint(newX, newY)
                }
            }
            return resampledGesture
        }

        fun normalizeByBoxSide(): Gesture {

            val normalizedGesture = Gesture()

            var maxX = -1.0f
            var maxY = -1.0f
            var minX = 10000.0f
            var minY = 10000.0f

            for (i in 0 until size) {
                maxX = max(xs[i], maxX)
                maxY = max(ys[i], maxY)
                minX = min(xs[i], minX)
                minY = min(ys[i], minY)
            }

            val width = maxX - minX
            val height = maxY - minY
            val longestSide = max(width, height)

            val centroidX = (width / 2 + minX) / longestSide
            val centroidY = (height / 2 + minY) / longestSide

            for (i in 0 until size) {
                val x = xs[i] / longestSide - centroidX
                val y = ys[i] / longestSide - centroidY
                normalizedGesture.addPoint(x, y)
            }

            return normalizedGesture
        }

        fun getFirstX(): Float = xs[0]
        fun getFirstY(): Float = ys[0]
        fun getLastX(): Float = xs[size-1]
        fun getLastY(): Float = ys[size-1]

        fun getLength(): Float {
            var length = 0f
            for (i in 1 until size) {
                val previousX = xs[i - 1]
                val previousY = ys[i - 1]
                val currentX = xs[i]
                val currentY = ys[i]
                length += distance(previousX, previousY, currentX, currentY)
            }

            return length
        }

        fun clear() {
            this.size = 0
        }

        fun getX(i: Int): Float = xs[i]
        fun getY(i: Int): Float = ys[i]

        fun clone(): Gesture {
            return Gesture(xs.clone(), ys.clone(), size)
        }


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Gesture

            if (this.size != other.size) return false

            for (i in 0 until size){
                if (xs[i] != other.xs[i] || ys[i] != other.ys[i]) return false
            }

            return true
        }

        override fun hashCode(): Int {
            var result = xs.contentHashCode()
            result = 31 * result + ys.contentHashCode()
            result = 31 * result + size
            return result
        }
    }


}
