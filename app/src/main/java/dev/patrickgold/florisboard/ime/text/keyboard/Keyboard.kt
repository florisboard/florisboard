/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.keyboard

import kotlin.math.abs

abstract class Keyboard {
    abstract fun getKeyForPos(pointerX: Int, pointerY: Int): Key?

    abstract fun keys(): Iterator<Key>
}

class TextKeyboard(
    private val arrangement: Array<Array<TextKey>>,
    val mode: KeyboardMode
) : Keyboard() {
    val rowCount: Int
        get() = arrangement.size

    companion object {
        fun layoutDrawableBounds(key: TextKey) {
            key.visibleDrawableBounds.apply {
                val w = key.visibleBounds.width()
                val h = key.visibleBounds.height()
                val xOffset: Int
                val yOffset: Int
                val factor: Double
                if (w < h) {
                    factor = if (w / h < 0.5) 0.2 else 0.35
                    xOffset = (factor * w).toInt()
                    yOffset = (h - (w - 2 * xOffset)) / 2
                } else {
                    factor = if (h / w < 0.5) 0.2 else 0.35
                    yOffset = (factor * h).toInt()
                    xOffset = (w - (h - 2 * yOffset)) / 2
                }
                left = key.visibleBounds.left + xOffset
                top = key.visibleBounds.top + yOffset
                right = key.visibleBounds.right - xOffset
                bottom = key.visibleBounds.bottom - yOffset
            }
        }

        fun layoutLabelBounds(key: TextKey) {
            key.visibleLabelBounds.apply {
                val w = key.visibleBounds.width()
                val h = key.visibleBounds.height()
                val xOffset: Int
                val yOffset: Int
                val factor: Double
                if (w < h) {
                    factor = if (w / h < 0.5) 0.28 else 0.43
                    xOffset = (factor * w).toInt()
                    yOffset = (h - (w - 2 * xOffset)) / 2
                } else {
                    factor = if (h / w < 0.5) 0.28 else 0.43
                    yOffset = (factor * h).toInt()
                    xOffset = (w - (h - 2 * yOffset)) / 2
                }
                left = key.visibleBounds.left + xOffset
                top = key.visibleBounds.top + yOffset
                right = key.visibleBounds.right - xOffset
                bottom = key.visibleBounds.bottom - yOffset
            }
        }
    }

    override fun getKeyForPos(pointerX: Int, pointerY: Int): TextKey? {
        for (key in keys()) {
            if (key.touchBounds.contains(pointerX, pointerY)) {
                return key
            }
        }
        return null
    }

    fun layout(keyboardView: TextKeyboardView) {
        val desiredTouchBounds = keyboardView.desiredKey.touchBounds
        val desiredVisibleBounds = keyboardView.desiredKey.visibleBounds
        val keyboardWidth = keyboardView.measuredWidth.toDouble()

        for ((r, row) in rows().withIndex()) {
            val posY = desiredTouchBounds.height() * r
            val availableWidth = keyboardWidth / desiredTouchBounds.width()
            var requestedWidth = 0.0
            var shrinkSum = 0.0
            var growSum = 0.0
            for (key in row) {
                requestedWidth += key.flayWidthFactor
                shrinkSum += key.flayShrink
                growSum += key.flayGrow
            }
            if (requestedWidth <= availableWidth) {
                // Requested with is smaller or equal to the available with, so we can grow
                val additionalWidth = availableWidth - requestedWidth
                var posX = 0
                for ((k, key) in row.withIndex()) {
                    val keyWidth = desiredTouchBounds.width() * when (growSum) {
                        0.0 -> when (k) {
                            0, row.size - 1 -> key.flayWidthFactor + additionalWidth / 2.0
                            else -> key.flayWidthFactor
                        }
                        else -> key.flayWidthFactor + additionalWidth * (key.flayGrow / growSum)
                    }
                    key.touchBounds.apply {
                        left = posX
                        top = posY
                        right = (posX + keyWidth).toInt()
                        bottom = posY + desiredTouchBounds.height()
                    }
                    key.visibleBounds.apply {
                        left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left) + when {
                            growSum == 0.0 && k == 0 -> ((additionalWidth / 2.0) * desiredTouchBounds.width()).toInt()
                            else -> 0
                        }
                        top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                        right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right) - when {
                            growSum == 0.0 && k == row.size - 1 -> ((additionalWidth / 2.0) * desiredTouchBounds.width()).toInt()
                            else -> 0
                        }
                        bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                    }
                    layoutDrawableBounds(key)
                    layoutLabelBounds(key)
                    posX += key.touchBounds.width()
                }

            } else {
                // Requested size too big, must shrink.
                val clippingWidth = requestedWidth - availableWidth
                var posX = 0
                for (key in row) {
                    val keyWidth = desiredTouchBounds.width() * if (key.flayShrink == 0.0) {
                        key.flayWidthFactor
                    } else {
                        key.flayWidthFactor - clippingWidth * (key.flayShrink / shrinkSum)
                    }
                    key.touchBounds.apply {
                        left = posX
                        top = posY
                        right = (posX + keyWidth).toInt()
                        bottom = posY + desiredTouchBounds.height()
                    }
                    key.visibleBounds.apply {
                        left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left)
                        top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                        right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right)
                        bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                    }
                    layoutDrawableBounds(key)
                    layoutLabelBounds(key)
                    posX += key.touchBounds.width()
                }
            }
        }
    }

    override fun keys(): Iterator<TextKey> {
        return TextKeyboardIterator(arrangement)
    }

    fun rows(): Iterator<Array<TextKey>> {
        return arrangement.iterator()
    }

    class TextKeyboardIterator internal constructor(
        private val arrangement: Array<Array<TextKey>>
    ) : Iterator<TextKey> {
        private var rowIndex: Int = 0
        private var keyIndex: Int = 0

        override fun hasNext(): Boolean {
            return rowIndex < arrangement.size && keyIndex < arrangement[rowIndex].size
        }

        override fun next(): TextKey {
            val next = arrangement[rowIndex][keyIndex]
            if (keyIndex + 1 == arrangement[rowIndex].size) {
                rowIndex++
                keyIndex = 0
            } else {
                keyIndex++
            }
            return next
        }
    }
}
