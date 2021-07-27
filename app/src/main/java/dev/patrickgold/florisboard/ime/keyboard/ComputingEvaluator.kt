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

import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.key.*

interface ComputingEvaluator {
    fun evaluateCaps(): Boolean

    fun evaluateCaps(data: KeyData): Boolean

    fun evaluateKanaType(): KanaType = KanaType.HIRA

    fun evaluateKanaSmall(): Boolean = false

    fun evaluateEnabled(data: KeyData): Boolean

    fun evaluateVisible(data: KeyData): Boolean

    fun getActiveSubtype(): Subtype

    fun getKeyVariation(): KeyVariation

    fun getKeyboard(): Keyboard

    fun isSlot(data: KeyData): Boolean

    fun getSlotData(data: KeyData): KeyData?
}

object DefaultComputingEvaluator : ComputingEvaluator {
    override fun evaluateCaps(): Boolean = false

    override fun evaluateCaps(data: KeyData): Boolean = false

    override fun evaluateKanaType(): KanaType = KanaType.HIRA

    override fun evaluateKanaSmall(): Boolean = false

    override fun evaluateEnabled(data: KeyData): Boolean = true

    override fun evaluateVisible(data: KeyData): Boolean = true

    override fun getActiveSubtype(): Subtype = Subtype.DEFAULT

    override fun getKeyVariation(): KeyVariation = KeyVariation.NORMAL

    override fun getKeyboard(): Keyboard = throw NotImplementedError()

    override fun isSlot(data: KeyData): Boolean = false

    override fun getSlotData(data: KeyData): KeyData? = null
}

enum class KanaType {
    HIRA,
    KATA,
    HALF_KATA,
}
