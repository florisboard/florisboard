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
import kotlinx.coroutines.Deferred
import java.util.*
import kotlin.collections.HashMap

class TextKeyboardCache {
    private var cache: EnumMap<KeyboardMode, HashMap<Int, Deferred<TextKeyboard>>> = EnumMap(KeyboardMode::class.java)

    init {
        // Initialize all odes with an empty HashMap. As we won't remove the HashMaps anymore (only clear them), any
        // get operations on `cache` will be automatically assumed to be not null.
        for (mode in KeyboardMode.values()) {
            cache[mode] = hashMapOf()
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
        cache[mode]!!.remove(subtype.hashCode())?.cancel()
    }

    fun get(mode: KeyboardMode, subtype: Subtype): Deferred<TextKeyboard>? {
        return cache[mode]!![subtype.hashCode()]
    }

    fun getOrElse(mode: KeyboardMode, subtype: Subtype, block: () -> Deferred<TextKeyboard>): Deferred<TextKeyboard> {
        val cachedKeyboard = get(mode, subtype)
        return if (cachedKeyboard != null) {
            cachedKeyboard
        } else {
            val keyboard = block()
            cache[mode]!![subtype.hashCode()] = keyboard
            keyboard
        }
    }

    fun set(mode: KeyboardMode, subtype: Subtype, keyboard: Deferred<TextKeyboard>) {
        cache[mode]!![subtype.hashCode()] = keyboard
    }
}
