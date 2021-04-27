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
import dev.patrickgold.florisboard.debug.*
import dev.patrickgold.florisboard.ime.core.Subtype
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TextKeyboardCache(ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val cache: EnumMap<KeyboardMode, SparseArrayCompat<Deferred<TextKeyboard>>> = EnumMap(KeyboardMode::class.java)
    private val cacheGuard: Mutex = Mutex(locked = false)
    private val scope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())

    init {
        // Initialize all odes with an empty array. As we won't remove the arrays anymore (only clear them), any
        // get operations on `cache` will be automatically assumed to be not null.
        for (mode in KeyboardMode.values()) {
            cache[mode] = SparseArrayCompat()
        }
    }

    fun clear() {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "Clear whole cache" }
        for (mode in KeyboardMode.values()) {
            cache[mode]!!.clear()
        }
    }

    fun clear(mode: KeyboardMode) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "Clear cache for mode '$mode'" }
        cache[mode]!!.clear()
    }

    fun clear(subtype: Subtype) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "Clear cache for subtype '${subtype.toShortString()}'" }
        for (mode in KeyboardMode.values()) {
            cache[mode]!!.remove(subtype.hashCode())
        }
    }

    fun clear(mode: KeyboardMode, subtype: Subtype) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "Clear cache for mode '$mode' and subtype '${subtype.toShortString()}'" }
        cache[mode]!!.remove(subtype.hashCode())
    }

    fun getAsync(mode: KeyboardMode, subtype: Subtype): Deferred<TextKeyboard>? {
        return cache[mode]!![subtype.hashCode()].also {
            flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "Get keyboard '$mode ${subtype.toShortString()}'" }
        }
    }

    fun getOrElseAsync(mode: KeyboardMode, subtype: Subtype, block: suspend () -> TextKeyboard): Deferred<TextKeyboard> {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        val cachedKeyboard = getAsync(mode, subtype)
        return if (cachedKeyboard != null) {
            cachedKeyboard
        } else {
            val keyboard = scope.async { block() }
            set(mode, subtype, keyboard)
            keyboard
        }
    }

    fun set(mode: KeyboardMode, subtype: Subtype, keyboard: Deferred<TextKeyboard>) {
        flogDebug(LogTopic.TEXT_KEYBOARD_VIEW) { "Set keyboard '$mode ${subtype.toShortString()}'" }
        cache[mode]!![subtype.hashCode()] = keyboard
    }
}
