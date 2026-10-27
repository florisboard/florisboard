/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package org.florisboard.lib.snygg.value

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private const val PaddingStart = "paddingStart"
private const val PaddingTop = "paddingTop"
private const val PaddingEnd = "paddingEnd"
private const val PaddingBottom = "paddingBottom"
private const val PaddingHorizontal = "paddingHorizontal"
private const val PaddingVertical = "paddingVertical"
private const val PaddingAll = "paddingAll"

private const val DpUnit = "dp"

data class SnyggPaddingValue(val values: PaddingValues) : SnyggValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            spacedList {
                +float(PaddingStart, unit = DpUnit)
                +float(PaddingTop, unit = DpUnit)
                +float(PaddingEnd, unit = DpUnit)
                +float(PaddingBottom, unit = DpUnit)
            }
        }

        override val alternativeSpecs = listOf(
            SnyggValueSpec {
                spacedList {
                    +float(PaddingHorizontal, unit = DpUnit)
                    +float(PaddingVertical, unit = DpUnit)
                }
            },
            SnyggValueSpec {
                spacedList {
                    +float(PaddingAll, unit = DpUnit)
                }
            },
        )

        override fun defaultValue() = SnyggPaddingValue(PaddingValues(all = 0.dp))

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggPaddingValue)
            val values = v.values
            val map = snyggIdToValueMapOf(
                PaddingStart to values.calculateStartPadding(LayoutDirection.Ltr).value,
                PaddingTop to values.calculateTopPadding().value,
                PaddingEnd to values.calculateEndPadding(LayoutDirection.Ltr).value,
                PaddingBottom to values.calculateBottomPadding().value,
            )
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = snyggIdToValueMapOf()
            runCatching { spec.parse(v, map) }.onSuccess {
                val values = PaddingValues(
                    start = map.getFloat(PaddingStart).dp,
                    top = map.getFloat(PaddingTop).dp,
                    end = map.getFloat(PaddingEnd).dp,
                    bottom = map.getFloat(PaddingBottom).dp,
                )
                return@runCatching SnyggPaddingValue(values)
            }
            runCatching { alternativeSpecs[0].parse(v, map) }.onSuccess {
                val values = PaddingValues(
                    horizontal = map.getFloat(PaddingHorizontal).dp,
                    vertical = map.getFloat(PaddingVertical).dp,
                )
                return@runCatching SnyggPaddingValue(values)
            }
            runCatching { alternativeSpecs[1].parse(v, map) }.onSuccess {
                val values = PaddingValues(
                    all = map.getFloat(PaddingAll).dp,
                )
                return@runCatching SnyggPaddingValue(values)
            }
            error("No matching spec found for \"$v\"")
        }
    }

    override fun encoder() = Companion
}
