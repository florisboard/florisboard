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

@file:OptIn(ExperimentalContracts::class)

package dev.patrickgold.florisboard.ime.text.keyboard

import androidx.collection.SparseArrayCompat
import androidx.collection.set
import dev.patrickgold.florisboard.ime.core.Subtype
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TextKeyboardCache {
    private val cache: EnumMap<KeyboardMode, SparseArrayCompat<TextKeyboard>> = EnumMap(KeyboardMode::class.java)

    init {
        // Initialize all odes with an empty array. As we won't remove the arrays anymore (only clear them), any
        // get operations on `cache` will be automatically assumed to be not null.
        for (mode in KeyboardMode.values()) {
            cache[mode] = SparseArrayCompat()
        }
    }

    fun clear() {
        for (mode in KeyboardMode.values()) {
            clear(mode)
        }
    }

    fun clear(mode: KeyboardMode) {
        cache[mode]!!.clear()
    }

    fun clear(subtype: Subtype) {
        for (mode in KeyboardMode.values()) {
            clear(mode, subtype)
        }
    }

    fun clear(mode: KeyboardMode, subtype: Subtype) {
        cache[mode]!![subtype.hashCode()]
        cache[mode]!!.remove(subtype.hashCode())
    }

    fun get(mode: KeyboardMode, subtype: Subtype): TextKeyboard? {
        return cache[mode]!![subtype.hashCode()]
    }

    inline fun getOrElse(mode: KeyboardMode, subtype: Subtype, block: () -> TextKeyboard): TextKeyboard {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        val cachedKeyboard = get(mode, subtype)
        return if (cachedKeyboard != null) {
            cachedKeyboard
        } else {
            val keyboard = block()
            set(mode, subtype, keyboard)
            keyboard
        }
    }

    fun set(mode: KeyboardMode, subtype: Subtype, keyboard: TextKeyboard) {
        cache[mode]!![subtype.hashCode()] = keyboard
    }
}
