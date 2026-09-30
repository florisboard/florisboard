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
import dev.patrickgold.florisboard.ime.keyboard3.ImeLayerIds
import dev.patrickgold.florisboard.ime.keyboard3.hint.LongPressKeyHintPlacement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.k3lp.lib.meta.source.SourceFileRef
import org.k3lp.lib.text.K3Descriptor
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.lib.text.asK3String
import org.k3lp.model.K3Model
import org.k3lp.model.flick.K3Flick
import org.k3lp.model.key.K3Key
import org.k3lp.model.key.K3KeyId
import org.k3lp.model.layer.K3LayerId
import org.k3lp.model.layer.K3TouchLayers
import kotlin.contracts.contract
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
        val layer = if (layerId == ImeLayerIds.Caps) {
            layers[layerId] ?: layers[ImeLayerIds.Shift] ?: layers[ImeLayerIds.Base]
        } else {
            layers[layerId] ?: layers[ImeLayerIds.Base]
        }
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
                ImeLayerIds.Base to TouchLayer.Empty,
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
    val display: K3StringOrDescriptor,
    val attrs: K3Key,
    val flick: K3Flick?,
    val isShiftKey: Boolean,
    val isRepeatable: Boolean,
    val isSuitableForSpaceBarDisplayOverride: Boolean,
    val isSuitableForSimplePopup: Boolean,
    val isSuitableForExtendedPopup: Boolean,
    val extendedPopupKeys: List<TouchPopupKey>,
    val longPressKeyHint: K3StringOrDescriptor?,
    val longPressKeyHintPlacement: LongPressKeyHintPlacement,
    val shouldOverrideDisplayWithMultiTapKeys: Boolean,
    val shouldHighlightPendingMultiTapKey: Boolean,
    val multiTapKeys: List<TouchMultiTapKey>,
) {
    val isSuitableForPopup: Boolean
        get() = isSuitableForSimplePopup || isSuitableForExtendedPopup

    companion object {
        val FnKeyId = K3KeyId("fn-key")
    }
}

class TouchPopupKey(
    val bounds: Rect,
    val display: K3StringOrDescriptor,
    val data: K3Key,
) {
    fun withNewBounds(bounds: Rect): TouchPopupKey {
        return TouchPopupKey(bounds, display, data)
    }
}

class TouchMultiTapKey(
    val display: K3StringOrDescriptor,
    val data: K3Key,
)

context(scope: CoroutineScope)
suspend fun computeTouchModel(
    model: K3Model,
    options: TouchModelOptions,
): TouchModel {
    val layersGroups = model.layersByForm.touch
    return when (layersGroups.size) {
        0 -> TouchModel.Empty
        1 -> TouchModel.Single(computeTouchKeyboard(model, layersGroups[0], options))
        else -> {
            val keyboards = layersGroups.map { layersGroup ->
                scope.async { computeTouchKeyboard(model, layersGroup, options) }
            }.awaitAll()
            TouchModel.Multiple(keyboards)
        }
    }
}

private inline fun List<K3KeyId>.withKeysResolved(
    model: K3Model,
    predicate: (K3Key) -> Boolean = { true },
): List<K3Key> {
    contract {
        callsInPlace(predicate)
    }
    return mapNotNull { keyId ->
        val key = model.keys.byKeyId[keyId]
        key?.takeIf(predicate)
    }
}

private data object TouchModelComputeRef : SourceFileRef {
    override fun toString(): String {
        return "TouchModelComputeRef"
    }
}
private fun FnKeyAction.asK3Key(): K3Key {
    return K3Key(
        id = K3KeyId(hashCode().toString()),
        output = output,
        width = width,
        origin = TouchModelComputeRef,
    )
}

private fun computeTouchKeyboard(
    model: K3Model,
    layersGroup: K3TouchLayers,
    options: TouchModelOptions,
): TouchKeyboard {
    val layers = layersGroup.layers
    val keyPredicate = { key: K3Key -> options.fnKeyEnabled || key.id != TouchKey.FnKeyId }

    val numberRow = if (options.showNumberRow) {
        val numberRowLayer = layers[ImeLayerIds.Numrow]
        if (numberRowLayer != null && numberRowLayer.rows.size == 1) {
            numberRowLayer.rows[0].withKeysResolved(model, keyPredicate)
        } else null
    } else null

    val rowCount = layers.maxOf { (_, layer) -> layer.rows.size }.coerceAtLeast(4) +
        if (numberRow != null) 1 else 0

    val touchLayers = buildMap {
        for ((_, layer) in layers) {
            if (layer.id == ImeLayerIds.Numrow) {
                // the numrow layer is not intended as a standalone layer => do not waste compute time on it here
                put(layer.id, TouchLayer.Empty)
                continue
            }
            val rows = buildList {
                if (numberRow != null && (layer.id == ImeLayerIds.Base || layer.id == ImeLayerIds.Shift)) {
                    add(numberRow)
                }
                layer.rows.forEach { row ->
                    add(row.withKeysResolved(model, keyPredicate))
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
                    val isFnKey = key.id == TouchKey.FnKeyId
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
                    val popups = when {
                        isFnKey -> options.fnKeyArrangement.longPressActions.map { action ->
                            val key = action.asK3Key()
                            TouchPopupKey(
                                bounds = Rect.Zero,
                                display = computeKeyDisplay(model, key),
                                data = key,
                            )
                        }
                        else -> key.longPressKeyIds?.let { longPressKeyIds ->
                            val defaultKeyId = key.longPressDefaultKeyId ?: longPressKeyIds.first()
                            val defaultKeyIndex = longPressKeyIds.indexOf(defaultKeyId)
                            buildList {
                                longPressKeyIds.forEach { keyId ->
                                    val key = model.keys.byKeyId[keyId]!!
                                    val popupKey = TouchPopupKey(
                                        bounds = Rect.Zero,
                                        display = computeKeyDisplay(model, key),
                                        data = key,
                                    )
                                    add(popupKey)
                                }
                                if (defaultKeyIndex > 0) {
                                    val defaultPopupKey = removeAt(defaultKeyIndex)
                                    add(0, defaultPopupKey)
                                }
                            }
                        } ?: emptyList()
                    }
                    val multiTapKeys = when {
                        isFnKey -> emptyList() // no support for multi-tap on fn key
                        else -> key.multiTapKeyIds?.let { multiTapKeyIds ->
                            buildList {
                                add(
                                    TouchMultiTapKey(
                                        display = computeKeyDisplay(model, key),
                                        data = key,
                                    )
                                )
                                for (multiTapKeyId in multiTapKeyIds) {
                                    val multiTapKey = model.keys.byKeyId[multiTapKeyId] ?: continue
                                    add(
                                        TouchMultiTapKey(
                                            display = computeKeyDisplay(model, multiTapKey),
                                            data = multiTapKey,
                                        )
                                    )
                                }
                            }
                        } ?: emptyList()
                    }
                    val flicks = when {
                        isFnKey -> null // TODO
                        else -> key.flickId?.let { model.flicks.byFlickId[it] }
                    }
                    val attrs = if (isFnKey) options.fnKeyArrangement.simpleAction.asK3Key() else key
                    val display = computeKeyDisplay(model, attrs)
                    val touchKey = TouchKey(
                        bounds = keyBoundsPx,
                        hitbox = hitbox,
                        display = display,
                        attrs = attrs,
                        flick = flicks,
                        isShiftKey = key.isPureLayerSwitchKey() && when (layer.id) {
                            ImeLayerIds.Base -> key.layerId == ImeLayerIds.Shift || key.layerId == ImeLayerIds.Caps
                            ImeLayerIds.Shift -> key.layerId == ImeLayerIds.Base || key.layerId == ImeLayerIds.Caps
                            ImeLayerIds.Caps -> key.layerId == ImeLayerIds.Base || key.layerId == ImeLayerIds.Shift
                            else -> false
                        },
                        isRepeatable = attrs.output?.isRepeatable() ?: false,
                        isSuitableForSpaceBarDisplayOverride = attrs.isSuitableForSpaceBarDisplayOverride(display),
                        isSuitableForSimplePopup = attrs.isSuitableForSimplePopup(),
                        isSuitableForExtendedPopup = popups.isNotEmpty(),
                        extendedPopupKeys = popups,
                        longPressKeyHint = if (options.longPressKeyHintEnabled) popups.firstOrNull()?.display else null,
                        longPressKeyHintPlacement = options.longPressKeyHintPlacement,
                        shouldOverrideDisplayWithMultiTapKeys =
                            model.displays.byKeyId[key.id] == null && multiTapKeys.isNotEmpty(),
                        shouldHighlightPendingMultiTapKey = options.multiTapHighlightEnabled,
                        multiTapKeys = multiTapKeys,
                    )
                    touchKeys.add(touchKey)
                    currentX += keyWidthPx
                }
                currentY += keyHeight
            }
            val touchLayer = TouchLayer(touchKeys.toList())
            put(layer.id, touchLayer)
        }
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

fun computeKeyDisplay(model: K3Model, output: K3StringOrDescriptor): K3StringOrDescriptor {
    return model.displays.byOutput[output]?.display ?: output
}

fun K3StringOrDescriptor.isRepeatable(): Boolean {
    return when (this) {
        is K3String -> false
        is K3Descriptor -> ImeActions.Repeatable.contains(this)
    }
}

private val ASCII_SPACE = " ".asK3String()

fun K3Key.isPureLayerSwitchKey(): Boolean {
    return !gap && layerId != null && output == null && longPressKeyIds.isNullOrEmpty() &&
        multiTapKeyIds.isNullOrEmpty() && flickId == null
}

fun K3Key.isSuitableForSpaceBarDisplayOverride(display: K3StringOrDescriptor): Boolean {
    return layerId == null && output is K3String && output == ASCII_SPACE && output == display
}
fun K3Key.isSuitableForSimplePopup(): Boolean {
    return layerId == null && output is K3String && output != ASCII_SPACE
}
