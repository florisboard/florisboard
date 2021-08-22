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

import android.graphics.Rect
import dev.patrickgold.florisboard.ime.keyboard.Keyboard
import dev.patrickgold.florisboard.ime.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.popup.PopupMapping
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import kotlin.math.abs
import kotlin.math.roundToInt

class TextKeyboard(
    val arrangement: Array<Array<TextKey>>,
    val mode: KeyboardMode,
    val extendedPopupMapping: PopupMapping?,
    val extendedPopupMappingDefault: PopupMapping?
) : Keyboard() {
    val rowCount: Int
        get() = arrangement.size

    val keyCount: Int
        get() = arrangement.sumOf { it.size }

    companion object {
        fun layoutDrawableBounds(key: TextKey, factor: Double) {
            layoutForegroundBounds(key, key.visibleDrawableBounds, 0.21 * (1.0 / factor), isLabel = false)
        }

        fun layoutLabelBounds(key: TextKey) {
            layoutForegroundBounds(key, key.visibleLabelBounds, 0.28, isLabel = true)
        }

        private fun layoutForegroundBounds(key: TextKey, bounds: Rect, factor: Double, isLabel: Boolean) {
            bounds.apply {
                val w = key.visibleBounds.width().toDouble()
                val h = key.visibleBounds.height().toDouble()
                val xOffset: Double
                val yOffset: Double
                if (w < h) {
                    xOffset = factor * w
                    yOffset = if ((key.computedData.code == KeyCode.SPACE || key.computedData.code == KeyCode.CJK_SPACE) && isLabel) {
                        xOffset
                    } else {
                        (h - (w - 2.0 * xOffset)) / 2.0
                    }
                } else {
                    yOffset = factor * h
                    xOffset = if ((key.computedData.code == KeyCode.SPACE || key.computedData.code == KeyCode.CJK_SPACE) && isLabel) {
                        yOffset
                    } else {
                        (w - (h - 2.0 * yOffset)) / 2.0
                    }
                }
                left = key.visibleBounds.left + xOffset.toInt()
                top = key.visibleBounds.top + yOffset.toInt()
                right = key.visibleBounds.right - xOffset.toInt()
                bottom = key.visibleBounds.bottom - yOffset.toInt()
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

    override fun layout(keyboardView: KeyboardView) {
        if (arrangement.isEmpty() || keyboardView !is TextKeyboardView) return

        val desiredTouchBounds = keyboardView.desiredKey.touchBounds
        val desiredVisibleBounds = keyboardView.desiredKey.visibleBounds
        if (desiredTouchBounds.isEmpty || desiredVisibleBounds.isEmpty) return
        val keyboardWidth = keyboardView.measuredWidth.toDouble()
        val keyboardHeight = keyboardView.measuredHeight.toDouble()
        if (keyboardWidth.isNaN() || keyboardHeight.isNaN()) return
        val rowMarginH = abs(desiredTouchBounds.width() - desiredVisibleBounds.width())
        val rowMarginV = (keyboardHeight - desiredTouchBounds.height() * rowCount.toDouble()) / (rowCount - 1).coerceAtLeast(1).toDouble()

        for ((r, row) in rows().withIndex()) {
            val posY = (desiredTouchBounds.height() + rowMarginV) * r
            val availableWidth = (keyboardWidth - rowMarginH) / desiredTouchBounds.width()
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
                var posX = rowMarginH / 2.0
                for ((k, key) in row.withIndex()) {
                    val keyWidth = desiredTouchBounds.width() * when (growSum) {
                        0.0 -> when (k) {
                            0, row.size - 1 -> key.flayWidthFactor + additionalWidth / 2.0
                            else -> key.flayWidthFactor
                        }
                        else -> key.flayWidthFactor + additionalWidth * (key.flayGrow / growSum)
                    }
                    key.touchBounds.apply {
                        left = posX.roundToInt()
                        top = posY.roundToInt()
                        right = (posX + keyWidth).roundToInt()
                        bottom = (posY + desiredTouchBounds.height()).roundToInt()
                    }
                    key.visibleBounds.apply {
                        left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left) + when {
                            growSum == 0.0 && k == 0 -> ((additionalWidth / 2.0) * desiredTouchBounds.width()).roundToInt()
                            else -> 0
                        }
                        top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                        right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right) - when {
                            growSum == 0.0 && k == row.size - 1 -> ((additionalWidth / 2.0) * desiredTouchBounds.width()).roundToInt()
                            else -> 0
                        }
                        bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                    }
                    layoutDrawableBounds(key, keyboardView.fontSizeMultiplier)
                    layoutLabelBounds(key)
                    posX += keyWidth
                    // After-adjust touch bounds for the row margin
                    key.touchBounds.apply {
                        if (k == 0) {
                            left = 0
                        } else if (k == row.size - 1) {
                            right = keyboardWidth.roundToInt()
                        }
                    }
                }
            } else {
                // Requested size too big, must shrink.
                val clippingWidth = requestedWidth - availableWidth
                var posX = rowMarginH / 2.0
                for ((k, key) in row.withIndex()) {
                    val keyWidth = desiredTouchBounds.width() * if (key.flayShrink == 0.0) {
                        key.flayWidthFactor
                    } else {
                        key.flayWidthFactor - clippingWidth * (key.flayShrink / shrinkSum)
                    }
                    key.touchBounds.apply {
                        left = posX.roundToInt()
                        top = posY.roundToInt()
                        right = (posX + keyWidth).roundToInt()
                        bottom = (posY + desiredTouchBounds.height()).roundToInt()
                    }
                    key.visibleBounds.apply {
                        left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left)
                        top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                        right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right)
                        bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                    }
                    layoutDrawableBounds(key, keyboardView.fontSizeMultiplier)
                    layoutLabelBounds(key)
                    posX += keyWidth
                    // After-adjust touch bounds for the row margin
                    key.touchBounds.apply {
                        if (k == 0) {
                            left = 0
                        } else if (k == row.size - 1) {
                            right = keyboardWidth.roundToInt()
                        }
                    }
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
