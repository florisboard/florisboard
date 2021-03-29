package dev.patrickgold.florisboard.ime.text.gestures

import android.content.Context
import android.view.MotionEvent
import dev.patrickgold.florisboard.R
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Wrapper class which holds all enums, interfaces and classes for detecting a swipe gesture.
 */
class GlideTypingGesture {

    /**
     * Class which detects swipes based on given [MotionEvent]s. Only supports single-finger swipes
     * and ignores additional pointers provided, if any.
     *
     * @property listener The listener to report detected swipes to.
     */
    class Detector(context: Context, private val listener: Listener) {
        private var pointerDataMap: MutableMap<Int, PointerData> = mutableMapOf()
        var velocityThreshold: VelocityThreshold = VelocityThreshold.NORMAL
        private val keySize = context.resources.getDimensionPixelSize(R.dimen.key_width).toDouble()


        companion object {
            private const val MAX_DETECT_TIME = 500
            private const val VELOCITY_THRESHOLD = 0.65
        }

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
                        ),
                        System.currentTimeMillis()
                    )
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    for (pointerIndex in 0 until event.pointerCount) {
                        val pointerId = event.getPointerId(pointerIndex)
                        val pointerData = pointerDataMap[pointerId]
                        val pos = Position(event.getX(pointerIndex), event.getY(pointerIndex))
                        pointerData?.positions?.add(
                            pos
                        )

                        if (pointerData != null && pointerData.isActuallyGesture == null) {
                            // evaluate whether is actually a gesture
                            val dist = pointerData.positions[0].dist(pos)
                            val time = (System.currentTimeMillis() - pointerData.startTime) + 1
                            // avoiding square roots because they're very slow
                            if (dist > keySize && (dist / time) > VELOCITY_THRESHOLD) {
                                pointerData.isActuallyGesture = true
                            } else if (time > MAX_DETECT_TIME) {
                                pointerData.isActuallyGesture = false
                            }

                        }

                        pointerData?.positions?.let { listener.onGestureAdd(it) }
                        return pointerData?.isActuallyGesture ?: false
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    return pointerDataMap.remove(pointerId)?.let {
                        if (it.isActuallyGesture == true)
                            listener.onGestureComplete(Event(it, pointerId))
                        true
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
            val positions: MutableList<Position>,
            val startTime: Long,
            var isActuallyGesture: Boolean? = null
        )

        data class Position(
            val x: Float,
            val y: Float
        ) {
            fun dist(p2: Position): Float {
                return sqrt((p2.x - x).pow(2) + (p2.y - y).pow(2))
            }
        }

    }

    interface Listener {
        fun onGestureComplete(event: Event): Boolean
        fun onGestureAdd(gesture: MutableList<Detector.Position>)
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
