/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3.interaction

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.fastForEachIndexed
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchPopupKey
import kotlinx.coroutines.Job
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.K3StringOrDescriptor

data class LongPress(
    val anchorBounds: Rect = Rect.Zero,
    val simpleBounds: Rect = Rect.Zero,
    val simpleLabel: K3StringOrDescriptor = K3String.empty(),
    val simpleIndicateExtended: Boolean = false,
    val extendedBounds: Rect = Rect.Zero,
    val extendedKeys: List<TouchPopupKey> = emptyList(),
    val extendedFocusedIndex: Int = 0,
    val extendedJob: Job? = null,
) {
    fun shouldShowSimplePopup(): Boolean {
        return !simpleBounds.isEmpty
    }

    fun shouldShowExtendedPopup(): Boolean {
        return !extendedBounds.isEmpty
    }

    fun getNearestKeyIndex(position: Offset): Int {
        extendedKeys.fastForEachIndexed { index, extendedKey ->
            if (extendedKey.bounds.contains(position)) {
                return index
            }
        }
        return extendedFocusedIndex
    }

    companion object {
        val None = LongPress()
    }
}

fun layoutExtendedLongPressKeys(
    extendedKeys: List<TouchPopupKey>,
    anchorBounds: Rect,
): List<TouchPopupKey> {
    val n  = extendedKeys.size
    val nLeftMax = (anchorBounds.left / anchorBounds.width).toInt()
    val nRightMax = ((1f - anchorBounds.right) / anchorBounds.width).toInt()
    var nLeftUsed0 = 0
    var nRightUsed0 = 0
    var nLeftUsed1 = 0
    var nRightUsed1 = 0
    var nTopUsed1 = 0
    val isMultiLine = n > 5
    return extendedKeys.mapIndexed { index, popupKey ->
        if (index == 0) {
            // default popup key, always directly at the anchor
            popupKey.withNewBounds(anchorBounds)
        } else {
            // prioritizes distance to anchor (according to available space)
            // aka row 0 before row 1 && right before left
            val (xFactor, yFactor) = when {
                isMultiLine && nRightUsed0 == 0 && nRightMax != 0 -> {
                    ++nRightUsed0 to 0
                }
                isMultiLine && nLeftUsed0 == 0 && nLeftMax != 0 -> {
                    -(++nLeftUsed0) to 0
                }
                isMultiLine && nTopUsed1 == 0 -> {
                    0 to -(++nTopUsed1)
                }
                isMultiLine && nRightUsed1 < nRightUsed0 && nRightUsed1 < nRightMax -> {
                    ++nRightUsed1 to -1
                }
                isMultiLine && nLeftUsed1 < nLeftUsed0 && nLeftUsed1 < nLeftMax -> {
                    -(++nLeftUsed1) to -1
                }
                nRightUsed0 <= nLeftUsed0 && nRightUsed0 < nRightMax || nLeftUsed0 >= nLeftMax -> {
                    ++nRightUsed0 to 0
                }
                else -> {
                    -(++nLeftUsed0) to 0
                }
            }
            popupKey.withNewBounds(
                anchorBounds.translate(
                    translateX = xFactor * anchorBounds.width,
                    translateY = yFactor * anchorBounds.height,
                )
            )
        }
    }
}
