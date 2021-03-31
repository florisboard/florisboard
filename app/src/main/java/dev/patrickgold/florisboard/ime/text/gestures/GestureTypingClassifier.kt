package dev.patrickgold.florisboard.ime.text.gestures

import dev.patrickgold.florisboard.ime.text.layout.ComputedLayoutData

/**
 * Inherit this to be able to handle gesture typing. Takes in raw pointer data, and
 * spits out what it thinks the gesture is.
 */
interface GestureTypingClassifier {
    /**
     * Called to notify gesture classifier that it can add a new point to the gesture.
     * @param position The position to add
     */
    fun addGesturePoint(position: GlideTypingGesture.Detector.Position)

    /**
     * Change the layout of the gesture classifier.
     */
    fun setLayout(computedLayoutData: ComputedLayoutData)

    /**
     * Change the word data of the gesture classifier.
     */
    fun setWordData(words: HashMap<String, Int>)

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

}
