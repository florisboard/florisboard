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
import java.lang.Exception
import kotlin.math.*

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
    class Detector(private val context: Context, private val listener: Listener) {
        private val eventList: MutableList<MotionEvent> = mutableListOf()
        private var indexFirst: Int = 0
        private var indexLastMoveRecognized: Int = 0

        var distanceThreshold: DistanceThreshold = DistanceThreshold.NORMAL
        var velocityThreshold: VelocityThreshold = VelocityThreshold.NORMAL

        fun onTouchEvent(event: MotionEvent): Boolean {
            try {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        clearEventList()
                        eventList.add(MotionEvent.obtainNoHistory(event))
                    }
                    MotionEvent.ACTION_MOVE -> {
                        eventList.add(MotionEvent.obtainNoHistory(event))
                        val lastEvent = eventList[indexLastMoveRecognized]
                        val diffX = event.x - lastEvent.x
                        val diffY = event.y - lastEvent.y
                        val distanceThresholdNV = numericValue(distanceThreshold) / 4.0f
                        return if (abs(diffX) > distanceThresholdNV || abs(diffY) > distanceThresholdNV) {
                            indexLastMoveRecognized = eventList.size - 1
                            val direction = detectDirection(diffX.toDouble(), diffY.toDouble())
                            listener.onSwipe(direction, Type.TOUCH_MOVE)
                        } else {
                            false
                        }
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP -> {
                        val firstEvent = eventList[indexFirst]
                        val diffX = event.x - firstEvent.x
                        val diffY = event.y - firstEvent.y
                        val distanceThresholdNV = numericValue(distanceThreshold)
                        /*val velocityThresholdNV = numericValue(velocityThreshold)
                        val velocity =
                            ((convertPixelsToDp(
                                sqrt(diffX.pow(2) + diffY.pow(2)),
                                context
                            ) / event.downTime) * 10.0f.pow(8)).toInt()*/
                        clearEventList()
                        // return if ((abs(diffX) > distanceThresholdNV || abs(diffY) > distanceThresholdNV) && velocity >= velocityThresholdNV) {
                        return if ((abs(diffX) > distanceThresholdNV || abs(diffY) > distanceThresholdNV)) {
                            val direction = detectDirection(diffX.toDouble(), diffY.toDouble())
                            listener.onSwipe(direction, Type.TOUCH_UP)
                        } else {
                            false
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        clearEventList()
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
         * Cleans up and clears the event list.
         */
        private fun clearEventList() {
            for (event in eventList) {
                event.recycle()
            }
            eventList.clear()
            indexFirst = 0
            indexLastMoveRecognized = 0
        }

        /**
         * Returns a numeric value for a given [DistanceThreshold], based on the values defined in
         * the resources dimens.xml file.
         */
        private fun numericValue(of: DistanceThreshold): Double {
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
        private fun numericValue(of: VelocityThreshold): Double {
            return when (of) {
                VelocityThreshold.VERY_SLOW -> context.resources.getInteger(R.integer.gesture_velocity_threshold_very_slow)
                VelocityThreshold.SLOW -> context.resources.getInteger(R.integer.gesture_velocity_threshold_slow)
                VelocityThreshold.NORMAL -> context.resources.getInteger(R.integer.gesture_velocity_threshold_normal)
                VelocityThreshold.FAST -> context.resources.getInteger(R.integer.gesture_velocity_threshold_fast)
                VelocityThreshold.VERY_FAST -> context.resources.getInteger(R.integer.gesture_velocity_threshold_very_fast)
            }.toDouble()
        }
    }

    interface Listener {
        fun onSwipe(direction: Direction, type: Type): Boolean
    }

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

    enum class Type {
        TOUCH_UP,
        TOUCH_MOVE;
    }
}
