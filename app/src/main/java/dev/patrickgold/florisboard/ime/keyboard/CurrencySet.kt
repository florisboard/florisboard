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

package dev.patrickgold.florisboard.ime.keyboard

import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
class CurrencySet(
    val id: String,
    val label: String,
    private val slots: List<TextKeyData>
) {
    companion object {
        fun isCurrencySlot(keyCode: Int): Boolean {
            return when (keyCode) {
                KeyCode.CURRENCY_SLOT_1,
                KeyCode.CURRENCY_SLOT_2,
                KeyCode.CURRENCY_SLOT_3,
                KeyCode.CURRENCY_SLOT_4,
                KeyCode.CURRENCY_SLOT_5,
                KeyCode.CURRENCY_SLOT_6 -> true
                else -> false
            }
        }

        fun default(): CurrencySet = CurrencySet(
            id = "default",
            label = "Default",
            slots = listOf(
                TextKeyData(code = 36, label = "$"),
                TextKeyData(code = 162, label = "¢"),
                TextKeyData(code = 8364, label = "€"),
                TextKeyData(code = 163, label = "£"),
                TextKeyData(code = 165, label = "¥"),
                TextKeyData(code = 8369, label = "₱")
            )
        )
    }

    fun getSlot(keyCode: Int): TextKeyData? {
        val slot = abs(keyCode) - abs(KeyCode.CURRENCY_SLOT_1)
        return slots.getOrNull(slot)
    }

    override fun toString(): String {
        return "${CurrencySet::class.simpleName} { id=$id, label\"$label\", slots=$slots }"
    }
}
