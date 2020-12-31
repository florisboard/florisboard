/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.popup

import dev.patrickgold.florisboard.ime.text.key.KeyHintMode

/**
 * A popup set for a single key. This set describes, if the key has a [hint] character,
 * a [main] character and other [relevant] popups.
 *
 * Note, that a hint character should **never** be set in a json extended popup file, rather it
 * should only be dynamically set by the LayoutManager.
 *
 * The order in which these defined popups will be shown depends on the current [KeyHintMode],
 * al well as the calculations made by the KeyPopupManager.
 *
 * The popup set can be accessed like an array with the addition that negative indexes defined
 * within this companion object are allowed (as long as the corresponding [hint] or [main]
 * character is *not* null).
 */
class PopupSet<T> (
    var hint: T? = null,
    var main: T? = null,
    var relevant: List<T> = listOf()
) : Collection<T> {
    companion object {
        const val HINT_INDEX: Int = -2
        const val MAIN_INDEX: Int = -1
    }

    override val size: Int
        get() = if (hint != null) { 1 } else { 0 } + if (main != null) { 1 } else { 0 } +
                relevant.size

    fun size(keyHintMode: KeyHintMode): Int {
        return if (keyHintMode == KeyHintMode.DISABLED && hint != null) {
            size - 1
        } else {
            size
        }
    }

    operator fun get(index: Int): T {
        val item = getOrNull(index)
        if (item == null) {
            throw IndexOutOfBoundsException(
                "Specified index $index is not an valid entry in this PopupSet!"
            )
        } else {
            return item
        }
    }

    fun getOrNull(index: Int): T? {
        if (index >= relevant.size || index < HINT_INDEX) {
            return null
        }
        return when (index) {
            HINT_INDEX -> hint
            MAIN_INDEX -> main
            else -> relevant.getOrNull(index)
        }
    }

    override fun contains(element: T): Boolean {
        return hint == element || main == element || relevant.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for (element in elements) {
            if (!contains(element)) {
                return false
            }
        }
        return true
    }

    override fun iterator(): Iterator<T> {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    fun merge(other: PopupSet<T>) {
        val tempRelevant = relevant.toMutableList()
        tempRelevant.addAll(other.relevant)
        other.hint?.let {
            if (hint == null) {
                hint = it
            } else {
                tempRelevant.add(it)
            }
        }
        other.main?.let {
            if (main == null) {
                main = it
            } else {
                tempRelevant.add(it)
            }
        }
        relevant = tempRelevant.toList()
    }
}
