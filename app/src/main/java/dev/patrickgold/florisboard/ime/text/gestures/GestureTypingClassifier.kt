package dev.patrickgold.florisboard.ime.text.gestures

import android.util.SparseArray
import dev.patrickgold.florisboard.ime.text.key.FlorisKeyData
import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData
import timber.log.Timber
import java.text.Normalizer
import kotlin.math.*

/**
 * Subclass this to be able to handle gesture typing. Takes in raw pointer data, and
 * spits out what it thinks the gesture is.
 */
interface GestureTypingClassifier {
    /**
     * Called to notify gesture classifier that it can add a new point to the gesture.
     * @param position The position to add
     */
    fun addGesturePoint(position: GlideTypingGesture.Detector.Position)


    fun isInitialized(): Boolean

    fun initialize(computedLayoutData: ComputedLayoutData, words: Array<String>, wordFrequencies: Array<Int>)

    /**
     * Process a completed gesture and find its location.
     */
    fun initGestureFromPointerData(pointerData: GlideTypingGesture.Detector.PointerData)

    /**
     * Generate suggestions to show to the user.
     *
     * @param maxSuggestionCount The maximum number of suggestions that are accepted.
     * @param gestureCompleted Whether the gesture is finished. (e.g to use a different algorithm for in progress words)
     */
    fun getSuggestions(maxSuggestionCount: Int, gestureCompleted: Boolean) : List<String>

    fun clear()

    class Gesture {
        private val xs = arrayListOf<Float>()
        private val ys = arrayListOf<Float>()

        companion object {
            private const val MIN_DIST_TO_ADD = 0
            private val cachedIdeal = hashMapOf<String, Gesture>()

            fun generateIdealGesture(word: String, keysByCharacter: SparseArray<FlorisKeyData>): Gesture {
                return cachedIdeal.getOrPut(word, { unCachedGenerateIdealGesture(word, keysByCharacter) })
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
                            Timber.w("Key $lc not found on keyboard!")
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
                        Timber.d("Adding point at $lc ${key.x} ${key.y}")
                        idealGesture.addPoint(key.x, key.y)
                    }
                    previousLetter = lc
                }
                return idealGesture
            }

            fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
                return sqrt((x1-x2).pow(2) + (y1-y2).pow(2))
            }
        }
        fun addPoint(x: Float, y: Float) {
            if (xs.size == 0) {
                xs.add(x)
                ys.add(y)
                return
            }

            val dx = xs.last() - x
            val dy = ys.last() - y

            val size = xs.size

            if (dx*dx + dy*dy > MIN_DIST_TO_ADD){
                xs.add(x)
                ys.add(y)
            }
            /*else if (abs(angleBetweenPoints(gesturePointsX[size - 2], gesturePointsY[size - 2], gesturePointsX[size - 1], gesturePointsY[size - 1], x, y )) > PI/8){
                gesturePointsX.add(x)
                gesturePointsY.add(y)
            }*/
        }

        private fun angleBetweenPoints(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
            return atan2(y3 - y2, x3 - x2) - atan2(y1 - y2, x1 - x2)
        }

        fun normalizeByBoxSide(): Gesture {

            val normalizedGesture: Gesture = Gesture()

            var maxX = -1.0f
            var maxY = -1.0f
            var minX = 10000.0f
            var minY = 10000.0f

            for (i in 0 until xs.size) {
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

            for (i in 0 until xs.size) {
                val x = xs[i] / longestSide - centroidX
                val y = ys[i] / longestSide - centroidY
                normalizedGesture.addPoint(x, y)
            }

            return normalizedGesture
        }

        fun getFirstX(): Float = xs[0]
        fun getFirstY(): Float = ys[0]
        fun getLastX(): Float = xs.last()
        fun getLastY(): Float = ys.last()

        fun getLength(): Float {
            var length = 0f
            for (i in 1 until xs.size) {
                val previousX = xs[i - 1]
                val previousY = ys[i - 1]
                val currentX = xs[i]
                val currentY = ys[i]
                length += distance(previousX, previousY, currentX, currentY)
            }

            return length
        }

        /**
         * Sample x coordinate at a point on the gesture
         * @param pt Ranges from 0 (start of gesture) to 1 (end of gesture)
         */
        fun sampleX(pt: Float): Float {
            val index = pt * (xs.size-1)
            val prev = xs[floor(index).toInt()]
            val next = xs[ceil(index).toInt()]

            val dist = index % 1
            // linear interpolate
            return prev * (1-dist)  + next * dist
        }


        /**
         * Sample y coordinate at a point on the gesture
         * @param pt Ranges from 0 (start of gesture) to 1 (end of gesture)
         */
        fun sampleY(pt: Float): Float {
            val index = pt * (ys.size - 1)
            val prev = ys[floor(index).toInt()]
            val next = ys[ceil(index).toInt()]

            val dist = index % 1
            // linear interpolate
            return prev * (1-dist)  + next * dist
        }

        fun clear() {
            this.xs.clear()
            this.ys.clear()
        }
    }
}
