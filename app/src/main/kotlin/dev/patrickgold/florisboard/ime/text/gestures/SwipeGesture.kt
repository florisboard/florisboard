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

import android.view.MotionEvent
import android.view.VelocityTracker
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.lib.Pointer
import dev.patrickgold.florisboard.lib.PointerMap
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.util.ViewUtils
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Wrapper class which holds all enums, interfaces and classes for detecting a swipe gesture.
 */
abstract class SwipeGesture {
    /**
     * Class which detects swipes based on given [MotionEvent]s. Only supports single-finger swipes
     * and ignores additional pointers provided, if any.
     *
     * @property listener The listener to report detected swipes to.
     */
    class Detector(private val listener: Listener) {
        private val prefs by florisPreferenceModel()

        var isEnabled: Boolean = true
        private var pointerMap: PointerMap<GesturePointer> = PointerMap { GesturePointer() }
        private val velocityTracker: VelocityTracker = VelocityTracker.obtain()

        fun onTouchEvent(event: MotionEvent) {
            if (!isEnabled) return
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                resetState()
            }
            velocityTracker.addMovement(event)
        }

        fun onTouchDown(event: MotionEvent, pointer: Pointer) {
            if (!isEnabled) return
            pointerMap.add(pointer.id, pointer.index)?.let { gesturePointer ->
                gesturePointer.firstX = ViewUtils.px2dp(event.getX(pointer.index))
                gesturePointer.firstY = ViewUtils.px2dp(event.getY(pointer.index))
                gesturePointer.lastX = gesturePointer.firstX
                gesturePointer.lastY = gesturePointer.firstY
            }
        }

        fun onTouchMove(event: MotionEvent, pointer: Pointer, alwaysTriggerOnMove: Boolean): Boolean {
            if (!isEnabled) return false
            pointerMap.findById(pointer.id)?.let { gesturePointer ->
                gesturePointer.index = pointer.index
                val currentX = ViewUtils.px2dp(event.getX(pointer.index))
                val currentY = ViewUtils.px2dp(event.getY(pointer.index))
                val absDiffX = currentX - gesturePointer.firstX
                val absDiffY = currentY - gesturePointer.firstY
                val relDiffX = currentX - gesturePointer.lastX
                val relDiffY = currentY - gesturePointer.lastY
                val thresholdWidth = prefs.gestures.swipeDistanceThreshold.get().dp.value.toDouble()
                val unitWidth = thresholdWidth / 4.0
                return if (alwaysTriggerOnMove || abs(relDiffX) > (thresholdWidth / 2.0) || abs(relDiffY) > (thresholdWidth / 2.0)) {
                    gesturePointer.lastX = currentX
                    gesturePointer.lastY = currentY
                    val direction = detectDirection(relDiffX.toDouble(), relDiffY.toDouble())
                    val newAbsUnitCountX = (absDiffX / unitWidth).toInt()
                    val newAbsUnitCountY = (absDiffY / unitWidth).toInt()
                    val relUnitCountX = newAbsUnitCountX - gesturePointer.absUnitCountX
                    val relUnitCountY = newAbsUnitCountY - gesturePointer.absUnitCountY
                    gesturePointer.absUnitCountX = newAbsUnitCountX
                    gesturePointer.absUnitCountY = newAbsUnitCountY
                    listener.onSwipe(Event(
                        direction = direction,
                        type = Type.TOUCH_MOVE,
                        pointer.id,
                        gesturePointer.absUnitCountX,
                        gesturePointer.absUnitCountY,
                        relUnitCountX,
                        relUnitCountY,
                    ))
                } else {
                    false
                }
            }
            return false
        }

        fun onTouchUp(event: MotionEvent, pointer: Pointer): Boolean {
            if (!isEnabled) return false
            pointerMap.findById(pointer.id)?.let { gesturePointer ->
                val currentX = ViewUtils.px2dp(event.getX(pointer.index))
                val currentY = ViewUtils.px2dp(event.getY(pointer.index))
                val absDiffX = currentX - gesturePointer.firstX
                val absDiffY = currentY - gesturePointer.firstY
                velocityTracker.computeCurrentVelocity(1000)
                val velocityX = ViewUtils.px2dp(velocityTracker.getXVelocity(pointer.id))
                val velocityY = ViewUtils.px2dp(velocityTracker.getYVelocity(pointer.id))
                flogDebug(LogTopic.GESTURES) { "Velocity: $velocityX $velocityY dp/s" }
                pointerMap.removeById(pointer.id)
                val thresholdSpeed = prefs.gestures.swipeVelocityThreshold.get().toDouble()
                val thresholdWidth = prefs.gestures.swipeDistanceThreshold.get().dp.value.toDouble()
                val unitWidth = thresholdWidth / 4.0
                return if ((abs(absDiffX) > thresholdWidth || abs(absDiffY) > thresholdWidth) && (abs(velocityX) > thresholdSpeed || abs(velocityY) > thresholdSpeed)) {
                    val direction = detectDirection(absDiffX.toDouble(), absDiffY.toDouble())
                    gesturePointer.absUnitCountX = (absDiffX / unitWidth).toInt()
                    gesturePointer.absUnitCountY = (absDiffY / unitWidth).toInt()
                    listener.onSwipe(Event(
                        direction = direction,
                        type = Type.TOUCH_UP,
                        pointer.id,
                        gesturePointer.absUnitCountX,
                        gesturePointer.absUnitCountY,
                        gesturePointer.absUnitCountX,
                        gesturePointer.absUnitCountY,
                    ))
                } else {
                    false
                }
            }
            return false
        }

        @Suppress("UNUSED_PARAMETER")
        fun onTouchCancel(event: MotionEvent, pointer: Pointer) {
            if (!isEnabled) return
            pointerMap.removeById(pointer.id)
        }

        /**
         * Calculates the angle based on the given x any y lengths. The returned angle is in degree
         * and goes clockwise, beginning with 0° at +x, 90° at +y, 180° at -y and 270° at -y.
         *
         * Coordinate system (based on the Android display coordinate system):
         *    -y
         * -x 00 +x
         *    +y
         */
        private fun angle(diffX: Double, diffY: Double): Double {
            return (Math.toDegrees(atan2(diffY, diffX)) + 360) % 360
        }

        /**
         * Detects the direction of a finger swipe by two given events.
         */
        private fun detectDirection(diffX: Double, diffY: Double): Direction {
            val diffAngle = angle(diffX, diffY) / 360.0
            return when {
                diffAngle >= (1/16.0) && diffAngle < (3/16.0) ->        Direction.DOWN_RIGHT
                diffAngle >= (3/16.0) && diffAngle < (5/16.0) ->        Direction.DOWN
                diffAngle >= (5/16.0) && diffAngle < (7/16.0) ->        Direction.DOWN_LEFT
                diffAngle >= (7/16.0) && diffAngle < (9/16.0) ->        Direction.LEFT
                diffAngle >= (9/16.0) && diffAngle < (11/16.0) ->       Direction.UP_LEFT
                diffAngle >= (11/16.0) && diffAngle < (13/16.0) ->      Direction.UP
                diffAngle >= (13/16.0) && diffAngle < (15/16.0) ->      Direction.UP_RIGHT
                else ->                                                 Direction.RIGHT
            }
        }

        /**
         * Resets the state.
         */
        private fun resetState() {
            pointerMap.clear()
            velocityTracker.clear()
        }

        class GesturePointer : Pointer() {
            var firstX: Float = 0.0f
            var firstY: Float = 0.0f
            var lastX: Float = 0.0f
            var lastY: Float = 0.0f
            var absUnitCountX: Int = 0
            var absUnitCountY: Int = 0

            override fun reset() {
                super.reset()
                firstX = 0.0f
                firstY = 0.0f
                lastX = 0.0f
                lastY = 0.0f
                absUnitCountX = 0
                absUnitCountY = 0
            }
        }
    }

    /**
     * An interface which provides an abstract callback function, which will be called for any
     * detected swipe event.
     */
    interface Listener {
        fun onSwipe(event: Event): Boolean
    }

    /**
     * Data class which describes a single gesture event.
     */
    data class Event(
        /** The direction of the swipe. */
        val direction: Direction,
        /** The type of the swipe. */
        val type: Type,
        /** The pointer ID of this event, corresponds to the value reported by the original MotionEvent. */
        val pointerId: Int,
        /** The unit count on the x-axis, measured from the first event (ACTION_DOWN). */
        val absUnitCountX: Int,
        /** The unit count on the y-axis, measured from the first event (ACTION_DOWN). */
        val absUnitCountY: Int,
        /** The unit count on the x-axis, measured from the last event (ACTION_MOVE). */
        val relUnitCountX: Int,
        /** The unit count on the y-axis, measured from the last event (ACTION_MOVE). */
        val relUnitCountY: Int,
    )

    /**
     * ENum which defines the direction of the detected swipe.
     */
    enum class Direction {
        UP_LEFT,
        UP,
        UP_RIGHT,
        RIGHT,
        DOWN_RIGHT,
        DOWN,
        DOWN_LEFT,
        LEFT;
    }

    /**
     * Enum which defines the type of the gesture.
     */
    enum class Type {
        TOUCH_UP,
        TOUCH_MOVE;
    }
}
