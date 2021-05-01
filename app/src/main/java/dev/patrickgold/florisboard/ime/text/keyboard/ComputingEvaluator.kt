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

import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.key.*

interface TextComputingEvaluator {
    fun evaluateCaps(): Boolean

    fun evaluateCaps(data: TextKeyData): Boolean

    fun evaluateEnabled(data: TextKeyData): Boolean

    fun evaluateVisible(data: TextKeyData): Boolean

    fun getActiveSubtype(): Subtype

    fun getKeyVariation(): KeyVariation

    fun getKeyboard(): TextKeyboard

    fun isSlot(data: TextKeyData): Boolean

    fun getSlotData(data: TextKeyData): TextKeyData?
}

object DefaultTextComputingEvaluator : TextComputingEvaluator {
    override fun evaluateCaps(): Boolean = false

    override fun evaluateCaps(data: TextKeyData): Boolean = false

    override fun evaluateEnabled(data: TextKeyData): Boolean = true

    override fun evaluateVisible(data: TextKeyData): Boolean = true

    override fun getActiveSubtype(): Subtype = Subtype.DEFAULT

    override fun getKeyVariation(): KeyVariation = KeyVariation.NORMAL

    override fun getKeyboard(): TextKeyboard = throw NotImplementedError()

    override fun isSlot(data: TextKeyData): Boolean = false

    override fun getSlotData(data: TextKeyData): TextKeyData? = null
}
