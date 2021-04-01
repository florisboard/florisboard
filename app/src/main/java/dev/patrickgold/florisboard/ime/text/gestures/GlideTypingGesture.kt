package dev.patrickgold.florisboard.ime.text.gestures

import android.content.Context
import android.view.MotionEvent
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.text.key.KeyCode
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
    class Detector(context: Context) {
        private var pointerDataMap: MutableMap<Int, PointerData> = mutableMapOf()
        var velocityThreshold: VelocityThreshold = VelocityThreshold.NORMAL
        private val keySize = context.resources.getDimensionPixelSize(R.dimen.key_width).toDouble()
        private val listeners: ArrayList<Listener> = arrayListOf()

        companion object {
            private const val MAX_DETECT_TIME = 500
            private const val VELOCITY_THRESHOLD = 0.65
            private val SWIPE_GESTURE_KEYS = arrayOf(KeyCode.DELETE, KeyCode.SHIFT, KeyCode.SPACE)
        }

        /**
         * Method which evaluates if a given [event] is a gesture.
         * @return whether or not the event was interpreted as part of a gesture.
         */
        fun onTouchEvent(event: MotionEvent, initialKeyCodes: MutableMap<Int, Int>): Boolean {
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
                    val pointerIndex = event.actionIndex
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
                        if (dist > keySize && (dist / time) > VELOCITY_THRESHOLD && (initialKeyCodes[pointerId] !in SWIPE_GESTURE_KEYS)) {
                            pointerData.isActuallyGesture = true
                            // Let listener know all those points need to be added.
                            pointerData.positions.take(pointerData.positions.size - 1).forEach { point ->
                                listeners.forEach {
                                    it.onGestureAdd(point)
                                }
                            }
                        } else if (time > MAX_DETECT_TIME) {
                            pointerData.isActuallyGesture = false
                        }

                    }

                    if (pointerData?.isActuallyGesture == true)
                        pointerData.positions.last().let { point -> listeners.forEach { it.onGestureAdd(point) } }

                    return pointerData?.isActuallyGesture ?: false
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    pointerDataMap.remove(pointerId)?.let { pointerData ->
                        if (pointerData.isActuallyGesture == true) {
                            listeners.forEach { listener -> listener.onGestureComplete(pointerData) }
                        } else {
                            resetState()
                        }
                    }
                    return false
                }
                MotionEvent.ACTION_CANCEL -> {
                    resetState()
                }
                else -> return false
            }
            return false
        }

        fun registerListener(listener: Listener) {
            this.listeners.add(listener)
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
        /**
         * Called when a gesture is complete.
         */
        fun onGestureComplete(data: Detector.PointerData)

        /**
         * Called when a point is added to a gesture.
         * Will not be called before a series of events is detected as a gesture.
         */
        fun onGestureAdd(point: Detector.Position)
    }

}
