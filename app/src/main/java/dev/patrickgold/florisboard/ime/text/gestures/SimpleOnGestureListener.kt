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

import android.view.GestureDetector
import android.view.MotionEvent

/**
 * Basically the same as [GestureDetector.SimpleOnGestureListener], but as an interface, so it can
 * be used in classes which already inherit from another superclass. All events will return false
 * by default or do nothing if they should not return anything.
 */
interface SimpleOnGestureListener : GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener, GestureDetector.OnContextClickListener {

    override fun onContextClick(event: MotionEvent?): Boolean {
        return false
    }

    override fun onDoubleTap(event: MotionEvent?): Boolean {
        return false
    }

    override fun onDoubleTapEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onDown(event: MotionEvent?): Boolean {
        return false
    }

    override fun onFling(
        event1: MotionEvent?,
        event2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(event: MotionEvent?) {
        // Stub
    }

    override fun onScroll(
        event1: MotionEvent?,
        event2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onShowPress(event: MotionEvent?) {
        // Stub
    }

    override fun onSingleTapConfirmed(event: MotionEvent?): Boolean {
        return false
    }

    override fun onSingleTapUp(event: MotionEvent?): Boolean {
        return false
    }
}
