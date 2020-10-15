/*
 * Copyright (C) 2020 Patrick Goldinger
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

import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper

class SwipeHandler(private val prefs: PrefHelper) {
    private val florisboard: FlorisBoard? = FlorisBoard.getInstanceOrNull()

    /**
     * Handles a given [Swipe] and executes the [SwipeAction] defined in the prefs. Also checks if
     * glide typing is enabled and ignores [Swipe.UP], [Swipe.DOWN], [Swipe.LEFT] and [Swipe.RIGHT]
     * to prevent gesture collisions in this case.
     */
    fun handle(swipe: Swipe) {
        var swipeAction: SwipeAction = SwipeAction.NO_ACTION
        if (!prefs.glide.enabled) {
            swipeAction = when (swipe) {
                Swipe.UP -> prefs.gestures.swipeUp
                Swipe.DOWN -> prefs.gestures.swipeDown
                Swipe.LEFT -> prefs.gestures.swipeLeft
                Swipe.RIGHT -> prefs.gestures.swipeRight
                else -> swipeAction
            }
        }
        swipeAction = when (swipe) {
            Swipe.SPACE_BAR_LEFT -> prefs.gestures.spaceBarSwipeLeft
            Swipe.SPACE_BAR_RIGHT -> prefs.gestures.spaceBarSwipeRight
            Swipe.DELETE_KEY_LEFT -> prefs.gestures.deleteKeySwipeLeft
            else -> swipeAction
        }
        florisboard?.executeSwipeAction(swipeAction)
    }
}
