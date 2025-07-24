/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.text.gestures

import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.keyboard.TextKey

/**
 * Inherit this to be able to handle gesture typing. Takes in raw pointer data, and
 * spits out what it thinks the gesture is.
 */
interface GlideTypingClassifier {
    /**
     * Called to notify gesture classifier that it can add a new point to the gesture.
     *
     * @param position The position to add
     */
    fun addGesturePoint(position: GlideTypingGesture.Detector.Position)

    /**
     * Change the layout of the gesture classifier.
     */
    fun setLayout(keyViews: List<TextKey>, subtype: Subtype)

    /**
     * Change the word data of the gesture classifier.
     */
    fun setWordData(subtype: Subtype)

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
    fun getSuggestions(maxSuggestionCount: Int, gestureCompleted: Boolean): List<CharSequence>

    fun clear()
}
