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

package dev.patrickgold.florisboard.ime.keyboard3.touch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.k3lp.lib.text.K3Descriptor
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.lib.text.asK3String
import org.k3lp.model.K3Model
import org.k3lp.model.flick.K3Flick
import org.k3lp.model.key.K3Key
import org.k3lp.model.layer.K3LayerId
import org.k3lp.model.layer.K3TouchLayers
import java.util.*
import kotlin.math.roundToInt

sealed interface TouchModel {
    fun selectKeyboard(deviceWidthMm: Int): TouchKeyboard

    object Empty : TouchModel {
        override fun selectKeyboard(deviceWidthMm: Int): TouchKeyboard {
            return TouchKeyboard.Empty
        }
    }

    class Single(val keyboard: TouchKeyboard) : TouchModel {
        override fun selectKeyboard(deviceWidthMm: Int): TouchKeyboard {
            return keyboard
        }
    }

    class Multiple(val keyboards: List<TouchKeyboard>) : TouchModel {
        override fun selectKeyboard(deviceWidthMm: Int): TouchKeyboard {
            return keyboards.lastOrNull { it.minDeviceWidthMm <= deviceWidthMm }
                ?: keyboards[0]
        }
    }
}

class TouchKeyboard(
    val layers: Map<K3LayerId, TouchLayer>,
    val rowCount: Int,
    val minDeviceWidthMm: Int,
) {
    fun findKey(layerId: K3LayerId, position: Offset): TouchKey? {
        val layer = layers[layerId] ?: layers[K3LayerId.BASE]
        if (layer == null || !NormalizedBounds.contains(position)) {
            return null
        }
        // TODO improve runtime of this
        for (key in layer.keys) {
            if (key.hitbox.contains(position)) {
                return key
            }
        }
        return null
    }

    companion object {
        val NormalizedBounds = Rect(Offset.Zero, Size(1f, 1f))

        val Empty = TouchKeyboard(
            layers = mapOf(
                K3LayerId.BASE to TouchLayer.Empty,
            ),
            rowCount = 4,
            minDeviceWidthMm = 0,
        )
    }
}

class TouchLayer(
    val keys: List<TouchKey>,
) {
    companion object {
        val Empty = TouchLayer(emptyList())
    }
}

class TouchKey(
    val bounds: Rect,
    val hitbox: Rect,
    val label: K3StringOrDescriptor,
    val attrs: K3Key,
    val flick: K3Flick?,
    val isRepeatable: Boolean,
    val isSuitableForSimplePopup: Boolean,
    val isSuitableForExtendedPopup: Boolean,
    val extendedPopupKeys: List<TouchPopupKey>,
) {
    val isSuitableForPopup: Boolean
        get() = isSuitableForSimplePopup || isSuitableForExtendedPopup
}

class TouchPopupKey(
    val label: K3StringOrDescriptor,
    val data: K3Key,
)

context(scope: CoroutineScope)
suspend fun computeTouchModel(
    model: K3Model,
): TouchModel {
    val layersGroups = model.layersByForm.touch
    return when (layersGroups.size) {
        0 -> TouchModel.Empty
        1 -> TouchModel.Single(computeTouchKeyboard(model, layersGroups[0]))
        else -> {
            val keyboards = layersGroups.map { layersGroup ->
                scope.async { computeTouchKeyboard(model, layersGroup) }
            }.awaitAll()
            TouchModel.Multiple(keyboards)
        }
    }
}

private fun computeTouchKeyboard(
    model: K3Model,
    layersGroup: K3TouchLayers,
): TouchKeyboard {
    val layers = layersGroup.layers
    val rowCount = layers.maxOf { (_, layer) -> layer.rows.size }.coerceAtLeast(4)

    val touchLayers = layers.mapValues { (_, layer) ->
        val rows = layer.rows.map {
            it.map { keyId ->
                val key = model.keys.byKeyId[keyId]
                requireNotNull(key) { "unexpected runtime error: model contract broken" }
            }
        }
        val keyHeight = 1f / rows.size
        val touchKeys = mutableListOf<TouchKey>()
        var currentY = 0f
        for (row in rows) {
            val desiredWeightSum = 10f
            val fullWeightSum = row.fold(0f) { acc, key -> acc + key.width.toFloat() }
            val stretchWeightSum = row.fold(0f) { acc, key -> acc + (if (key.stretch) key.width.toFloat() else 0f) }
            val nonStretchSum = fullWeightSum - stretchWeightSum
            val mayGrowKeys = fullWeightSum <= desiredWeightSum
            val mayStretchKeys = mayGrowKeys && stretchWeightSum != 0f
            val desiredKeyWidth = when {
                mayGrowKeys -> 1f / 10f
                else -> desiredWeightSum / fullWeightSum / 10f
            }
            val desiredStretchKeyWidth = when {
                mayGrowKeys && mayStretchKeys -> (desiredWeightSum - nonStretchSum) / desiredWeightSum
                else -> 0f
            }
            var keyWidthSum = 0f
            val keyWidths = mutableListOf<Float>()
            for (key in row) {
                val keyWidthPx = when {
                    mayStretchKeys && key.stretch -> desiredStretchKeyWidth * key.width.toFloat()
                        .roundToInt() / stretchWeightSum
                    else -> desiredKeyWidth * key.width.toFloat()
                }
                keyWidths.add(keyWidthPx)
                keyWidthSum += keyWidthPx
            }
            var currentX = when {
                mayGrowKeys && !mayStretchKeys -> (1f - keyWidthSum) / 2
                else -> 0f
            }
            for ((i, key) in row.withIndex()) {
                val keyWidthPx = keyWidths[i]
                val keyBoundsPx = Rect(
                    offset = Offset(currentX, currentY),
                    size = Size(keyWidthPx, keyHeight),
                )
                val hitbox = when (i) {
                    // if first key -> extend to left edge of keyboard
                    0 -> {
                        Rect(
                            offset = Offset(0f, currentY),
                            size = Size(currentX + keyWidthPx, keyHeight),
                        )
                    }
                    // if last key -> extend to right edge of keyboard
                    row.size - 1 -> {
                        Rect(
                            offset = Offset(currentX, currentY),
                            size = Size(1f - currentX, keyHeight),
                        )
                    }
                    // else same as bounds
                    else -> keyBoundsPx
                }
                val popups = key.longPressKeyIds?.let { longPressKeyIds ->
                    val defaultKeyId = key.longPressDefaultKeyId ?: longPressKeyIds.first()
                    val defaultKeyIndex = longPressKeyIds.indexOf(defaultKeyId)
                    buildList {
                        longPressKeyIds.forEach { keyId ->
                            val key = model.keys.byKeyId[keyId]!!
                            val popupKey = TouchPopupKey(
                                label = computeKeyDisplay(model, key),
                                data = key,
                            )
                            add(popupKey)
                        }
                        if (defaultKeyIndex > 0) {
                            Collections.swap(this, 0, defaultKeyIndex)
                        }
                    }
                }
                val touchKey = TouchKey(
                    bounds = keyBoundsPx,
                    hitbox = hitbox,
                    label = computeKeyDisplay(model, key),
                    attrs = key,
                    flick = key.flickId?.let { model.flicks.byFlickId[it] },
                    isRepeatable = key.output?.isRepeatable() ?: false,
                    isSuitableForSimplePopup = key.isSuitableForSimplePopup() ?: false,
                    isSuitableForExtendedPopup = popups?.isNotEmpty() ?: false,
                    extendedPopupKeys = popups ?: emptyList(),
                )
                touchKeys.add(touchKey)
                currentX += keyWidthPx
            }
            currentY += keyHeight
        }
        TouchLayer(touchKeys.toList())
    }

    return TouchKeyboard(
        layers = touchLayers,
        rowCount = rowCount,
        minDeviceWidthMm = layersGroup.minDeviceWidth,
    )
}

private fun computeKeyDisplay(model: K3Model, key: K3Key): K3StringOrDescriptor {
    val displayByKey = model.displays.byKeyId[key.id]
    if (displayByKey != null) {
        return displayByKey.display
    }
    if (key.output != null) {
        val displayByOut = model.displays.byOutput[key.output]
        if (displayByOut != null) {
            return displayByOut.display
        }
    }
    return key.output ?: key.id.value.asK3String()
}

fun K3StringOrDescriptor.isRepeatable(): Boolean {
    return when (this) {
        is K3String -> false
        is K3Descriptor -> ImeActions.Repeatable.contains(this)
    }
}

private val ASCII_SPACE = " ".asK3String()
fun K3Key.isSuitableForSimplePopup(): Boolean {
    return layerId == null && output is K3String && output != ASCII_SPACE
}
