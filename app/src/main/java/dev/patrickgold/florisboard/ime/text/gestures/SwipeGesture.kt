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

import android.content.Context
import android.view.MotionEvent
import dev.patrickgold.florisboard.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan

/**
 * Wrapper class which holds all enums, interfaces and classes for detecting a swipe gesture.
 */
abstract class SwipeGesture {
    companion object {
        /**
         * Returns a numeric value for a given [DistanceThreshold], based on the values defined in
         * the resources dimens.xml file.
         */
        fun numericValue(context: Context, of: DistanceThreshold): Double {
            return when (of) {
                DistanceThreshold.VERY_SHORT -> context.resources.getDimension(R.dimen.gesture_distance_threshold_very_short)
                DistanceThreshold.SHORT -> context.resources.getDimension(R.dimen.gesture_distance_threshold_short)
                DistanceThreshold.NORMAL -> context.resources.getDimension(R.dimen.gesture_distance_threshold_normal)
                DistanceThreshold.LONG -> context.resources.getDimension(R.dimen.gesture_distance_threshold_long)
                DistanceThreshold.VERY_LONG -> context.resources.getDimension(R.dimen.gesture_distance_threshold_very_long)
            }.toDouble()
        }

        /**
         * Returns a numeric value for a given [VelocityThreshold], based on the values defined in
         * the resources dimens.xml file.
         */
        fun numericValue(context: Context, of: VelocityThreshold): Double {
            return when (of) {
                VelocityThreshold.VERY_SLOW -> context.resources.getInteger(R.integer.gesture_velocity_threshold_very_slow)
                VelocityThreshold.SLOW -> context.resources.getInteger(R.integer.gesture_velocity_threshold_slow)
                VelocityThreshold.NORMAL -> context.resources.getInteger(R.integer.gesture_velocity_threshold_normal)
                VelocityThreshold.FAST -> context.resources.getInteger(R.integer.gesture_velocity_threshold_fast)
                VelocityThreshold.VERY_FAST -> context.resources.getInteger(R.integer.gesture_velocity_threshold_very_fast)
            }.toDouble()
        }
    }

    /**
     * Class which detects swipes based on given [MotionEvent]s. Only supports single-finger swipes
     * and ignores additional pointers provided, if any.
     *
     * @property listener The listener to report detected swipes to.
     */
    class Detector(private val context: Context, private val listener: Listener) {
        private var firstMotionEvent: MotionEvent? = null
        private var lastMotionEvent: MotionEvent? = null
        private var absUnitCountX: Int = 0
        private var absUnitCountY: Int = 0
        private var thresholdWidth: Double = numericValue(context, DistanceThreshold.NORMAL)
        private var unitWidth: Double = thresholdWidth / 4.0

        var distanceThreshold: DistanceThreshold = DistanceThreshold.NORMAL
            set(value) {
                field = value
                thresholdWidth = numericValue(context, value)
                unitWidth = thresholdWidth / 4.0
            }
        var velocityThreshold: VelocityThreshold = VelocityThreshold.NORMAL

        /**
         * Method which evaluates if a given [event] is a gesture.
         *
         * @param event The MotionEvent which should be checked for a gesture.
         * @param alwaysTriggerOnMove Set to true if the moving detection algorithm should always
         *  trigger, regardless of the distance from the previous event. Defaults to false.
         * @return True if the given [event] is a gesture, false otherwise.
         */
        fun onTouchEvent(event: MotionEvent, alwaysTriggerOnMove: Boolean = false): Boolean {
            try {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        resetState()
                        firstMotionEvent = MotionEvent.obtainNoHistory(event)
                        lastMotionEvent = firstMotionEvent
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val firstEvent = firstMotionEvent ?: return false
                        val absDiffX = event.x - firstEvent.x
                        val absDiffY = event.y - firstEvent.y
                        val lastEvent = lastMotionEvent ?: return false
                        val relDiffX = event.x - lastEvent.x
                        val relDiffY = event.y - lastEvent.y
                        return if (alwaysTriggerOnMove || abs(relDiffX) > (thresholdWidth / 2.0) || abs(relDiffY) > (thresholdWidth / 2.0)) {
                            lastMotionEvent = MotionEvent.obtainNoHistory(event)
                            val direction = detectDirection(relDiffX.toDouble(), relDiffY.toDouble())
                            val newAbsUnitCountX = (absDiffX / unitWidth).toInt()
                            val newAbsUnitCountY = (absDiffY / unitWidth).toInt()
                            val relUnitCountX = newAbsUnitCountX - absUnitCountX
                            val relUnitCountY = newAbsUnitCountY - absUnitCountY
                            absUnitCountX = newAbsUnitCountX
                            absUnitCountY = newAbsUnitCountY
                            listener.onSwipe(Event(
                                direction = direction,
                                type = Type.TOUCH_MOVE,
                                absUnitCountX,
                                absUnitCountY,
                                relUnitCountX,
                                relUnitCountY
                            ))
                        } else {
                            false
                        }
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP -> {
                        val firstEvent = firstMotionEvent ?: return false
                        val absDiffX = event.x - firstEvent.x
                        val absDiffY = event.y - firstEvent.y
                        /*val velocityThresholdNV = numericValue(velocityThreshold)
                        val velocity =
                            ((convertPixelsToDp(
                                sqrt(diffX.pow(2) + diffY.pow(2)),
                                context
                            ) / event.downTime) * 10.0f.pow(8)).toInt()*/
                        // return if ((abs(diffX) > distanceThresholdNV || abs(diffY) > distanceThresholdNV) && velocity >= velocityThresholdNV) {
                        val ret = if ((abs(absDiffX) > thresholdWidth || abs(absDiffY) > thresholdWidth)) {
                            val direction = detectDirection(absDiffX.toDouble(), absDiffY.toDouble())
                            absUnitCountX = (absDiffX / unitWidth).toInt()
                            absUnitCountY = (absDiffY / unitWidth).toInt()
                            listener.onSwipe(Event(
                                direction = direction,
                                type = Type.TOUCH_UP,
                                absUnitCountX,
                                absUnitCountY,
                                absUnitCountX,
                                absUnitCountY
                            ))
                        } else {
                            false
                        }
                        resetState()
                        return ret
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        resetState()
                    }
                    else -> return false
                }
                return false
            } catch(e: Exception) {
                return false
            }
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
            val tmpAngle = abs(360 * atan(diffY / diffX) / (2 * PI))
            return if (diffX < 0 && diffY >= 0) {
                180.0f - tmpAngle
            } else if (diffX < 0 && diffY < 0) {
                180.0f + tmpAngle
            } else if (diffX >= 0 && diffY < 0) {
                360.0f - tmpAngle
            } else {
                tmpAngle
            }
        }

        /**
         * Detects the direction of a finger swipe by two given events.
         */
        private fun detectDirection(diffX: Double, diffY: Double): Direction {
            val diffAngle = angle(diffX, diffY) / 360
            return when {
                diffAngle >= (1/16.0f) && diffAngle < (3/16.0f) ->      Direction.DOWN_RIGHT
                diffAngle >= (3/16.0f) && diffAngle < (5/16.0f) ->      Direction.DOWN
                diffAngle >= (5/16.0f) && diffAngle < (7/16.0f) ->      Direction.DOWN_LEFT
                diffAngle >= (7/16.0f) && diffAngle < (9/16.0f) ->      Direction.LEFT
                diffAngle >= (9/16.0f) && diffAngle < (11/16.0f) ->     Direction.UP_LEFT
                diffAngle >= (11/16.0f) && diffAngle < (13/16.0f) ->    Direction.UP
                diffAngle >= (13/16.0f) && diffAngle < (15/16.0f) ->    Direction.UP_RIGHT
                else ->                                                 Direction.RIGHT
            }
        }

        /**
         * Resets the state.
         */
        private fun resetState() {
            firstMotionEvent = null
            lastMotionEvent = null
            absUnitCountX = 0
            absUnitCountY = 0
        }
    }

    /**
     * An interface which provides an abstract callback function, which will be called for any
     * detected swipe event.
     */
    fun interface Listener {
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
        /** The unit count on the x-axis, measured from the first event (ACTION_DOWN). */
        val absUnitCountX: Int,
        /** The unit count on the y-axis, measured from the first event (ACTION_DOWN). */
        val absUnitCountY: Int,
        /** The unit count on the x-axis, measured from the last event (ACTION_MOVE). */
        val relUnitCountX: Int,
        /** The unit count on the y-axis, measured from the last event (ACTION_MOVE). */
        val relUnitCountY: Int
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
        LEFT,
    }

    /**
     * Enum which defines the type of the gesture.
     */
    enum class Type {
        TOUCH_UP,
        TOUCH_MOVE;
    }
}
