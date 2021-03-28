package dev.patrickgold.florisboard.ime.text.gestures

import android.content.Context
import android.view.MotionEvent
import timber.log.Timber

/**
 * Wrapper class which holds all enums, interfaces and classes for detecting a swipe gesture.
 */
class Gesture {

    /**
     * Class which detects swipes based on given [MotionEvent]s. Only supports single-finger swipes
     * and ignores additional pointers provided, if any.
     *
     * @property listener The listener to report detected swipes to.
     */
    class Detector(private val context: Context, private val listener: Listener) {
        private var pointerDataMap: MutableMap<Int, PointerData> = mutableMapOf()
        var velocityThreshold: VelocityThreshold = VelocityThreshold.NORMAL

        /**
         * Method which evaluates if a given [event] is a gesture.
         *
         */
        fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        resetState()
                    }
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    pointerDataMap[pointerId] = PointerData(
                        mutableListOf(
                            Position(event.getX(pointerIndex), event.getY(pointerIndex))
                        )
                    )
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    for (pointerIndex in 0 until event.pointerCount) {
                        val pointerId = event.getPointerId(pointerIndex)
                        pointerDataMap[pointerId]?.positions?.add(
                            Position(event.getX(pointerIndex), event.getY(pointerIndex))
                        )
                        pointerDataMap[pointerId]?.positions?.let { listener.onGestureAdd(it) }
                        return pointerDataMap[pointerId]?.positions?.size ?: 0 > 5
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    Timber.d("Gesture detected: ${pointerDataMap[pointerId]?.positions?.size}")
                    return pointerDataMap.remove(pointerId)?.let {
                        listener.onGestureComplete(Event(it, pointerId))
                    } ?: false
                }
                MotionEvent.ACTION_CANCEL -> {
                    resetState()
                }
                else -> return false
            }
            return false
        }

        private fun resetState() {
            pointerDataMap.clear()
        }

        data class PointerData(
            val positions: MutableList<Position>
        )

        data class Position(
            val x: Float,
            val y: Float
        )

    }

    interface Listener {
        fun onGestureComplete(event: Event): Boolean
        fun onGestureAdd(gesture: MutableList<Gesture.Detector.Position>)
    }

    /**
     * Data class which describes a single gesture event.
     */
    data class Event(
        val data: Detector.PointerData,
        /** The pointer ID of this event, corresponds to the value reported by the original MotionEvent. */
        val pointerId: Int,
    )

}
