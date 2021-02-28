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

package dev.patrickgold.florisboard.ime.nlp

class StagedSuggestionList<T : Any, F : Comparable<F>>(
    private val maxSize: Int
) : Collection<WeightedToken<T, F>> {
    private val internalArray: Array<WeightedToken<T, F>?> = Array(maxSize) { null }
    private var internalSize: Int = 0

    override val size: Int
        get() = internalSize

    fun add(token: T, freq: F): Boolean {
        if (internalSize < maxSize) {
            internalArray[internalSize++] = WeightedToken(token, freq)
            internalArray.sortByDescending { it?.freq }
            return true
        } else {
            if (internalArray.last()!!.freq < freq) {
                internalArray[internalArray.lastIndex] = WeightedToken(token, freq)
                internalArray.sortByDescending { it?.freq }
                return true
            }
            return false
        }
    }

    fun canAdd(freq: F): Boolean {
        return internalSize < maxSize || internalArray.last()!!.freq < freq
    }

    fun clear() {
        for (n in internalArray.indices) {
            internalArray[n] = null
        }
        internalSize = 0
    }

    override fun contains(element: WeightedToken<T, F>): Boolean = internalArray.contains(element)

    override fun containsAll(elements: Collection<WeightedToken<T, F>>): Boolean {
        elements.forEach { if (!contains(it)) return false }
        return true
    }

    @Throws(IndexOutOfBoundsException::class)
    operator fun get(index: Int): WeightedToken<T, F> {
        val element = getOrNull(index)
        if (element == null) {
            throw IndexOutOfBoundsException("The specified index $index is not within the bounds of this list!")
        } else {
            return element
        }
    }

    fun getOrNull(index: Int): WeightedToken<T, F>? {
        return internalArray.getOrNull(index)
    }

    override fun isEmpty(): Boolean = internalSize <= 0

    override fun iterator(): Iterator<WeightedToken<T, F>> {
        return StagedIterator(this)
    }

    class StagedIterator<T : Any, F : Comparable<F>> internal constructor (
        private val stagedSuggestionList: StagedSuggestionList<T, F>
    ) : Iterator<WeightedToken<T, F>> {
        var index = 0

        override fun next(): WeightedToken<T, F> = stagedSuggestionList[index++]

        override fun hasNext(): Boolean = stagedSuggestionList.getOrNull(index) != null
    }
}
