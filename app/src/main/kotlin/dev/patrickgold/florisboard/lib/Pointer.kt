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

package dev.patrickgold.florisboard.lib

import androidx.annotation.RestrictTo

/**
 * A simple helper object managing touch pointer objects. This class is designed to hold
 * at max [capacity] at once. It tries to reduce the need to recreate objects and to resize
 * arrays by creating a fixed-size list and by reusing pointers. This map supports iterating
 * over all active pointers.
 *
 * @property capacity The capacity of this map, determining the maximum number of pointers this
 *  map can hold at once. This value must be greater than or equal to one. Should a smaller capacity
 *  be passed, automatically the minimum capacity `1` is assumed.
 * @param init The initializer for each pointer. Note that [Pointer.reset] is called before
 *  storing the new object, to ensure that this pointer is not initialized with some pointer data.
 */
class PointerMap<P : Pointer>(val capacity: Int = 4, init: (Int) -> P) : Iterable<P> {
    /**
     * The internal list of pointers, is not intended for public access.
     */
    private val pointers: List<P> = List(capacity.coerceAtLeast(1)) { i ->
        init(i).also { pointer -> pointer.reset() }
    }

    /**
     * Adds a new pointer with given [id] and [index] and returns it. If this map is already at max
     * capacity, null is returned and the pointer could not be added.
     *
     * @param id The id of the pointer to add.
     * @param index The index of the pointer to add.
     *
     * @return The newly added pointer or null if the map is already full.
     */
    fun add(id: Int, index: Int): P? {
        for (pointer in pointers) {
            if (pointer.isNotUsed) {
                pointer.id = id
                pointer.index = index
                return pointer
            }
        }
        return null
    }

    /**
     * Clears this map and resets all pointers.
     */
    fun clear() {
        for (pointer in pointers) {
            pointer.reset()
        }
    }

    /**
     * Finds a pointer by given [id].
     *
     * @param id The id of the pointer which should be found.
     *
     * @return The pointer with given [id] or null.
     */
    fun findById(id: Int): P? {
        for (pointer in pointers) {
            if (pointer.id == id) {
                return pointer
            }
        }
        return null
    }

    /**
     * Gets a pointer from the internal array based on the internal array index. This method
     * is intended to be used only by the [PointerIterator].
     *
     * @param index
     *
     * @return The pointer for given index or null, excluding unused pointers.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    fun get(index: Int): P? {
        val pointer = pointers.getOrNull(index)
        if (pointer != null && pointer.isUsed) {
            return pointer
        }
        return null
    }

    override fun iterator(): Iterator<P> {
        return PointerIterator(this)
    }

    /**
     * Removes a pointer with given [id] and returns a boolean result.
     *
     * @param id The id of the pointer to remove. If the id is not existent, noting happens.
     *
     * @return True if a pointer was removed, false otherwise.
     */
    fun removeById(id: Int): Boolean {
        for (pointer in pointers) {
            if (pointer.id == id) {
                pointer.reset()
                return true
            }
        }
        return false
    }

    /**
     * Returns the size of this map (only counting active pointers). This value is anywhere
     * between 0 and [capacity].
     */
    val size: Int
        get() = pointers.count { it.isUsed }
}

class PointerIterator<P : Pointer>(private val pointerMap: PointerMap<P>) : Iterator<P> {
    private var index: Int = 0

    override fun hasNext(): Boolean {
        do {
            if (pointerMap.get(index) != null) {
                return true
            }
        } while (++index < pointerMap.capacity)
        return false
    }

    override fun next(): P {
        return pointerMap.get(index++)!!
    }
}

/**
 * Abstract touch pointer definition.
 */
abstract class Pointer {
    companion object {
        const val UNUSED_P: Int = -1
    }

    /**
     * The id of this pointer, corresponds to the motion event this pointer originated.
     */
    var id: Int = UNUSED_P

    /**
     * The index of this pointer, corresponds to the motion event this pointer originated.
     */
    var index: Int = UNUSED_P

    /**
     * True if this pointer is used and active, false otherwise.
     */
    val isUsed: Boolean
        get() = id >= 0

    /**
     * False if this pointer is used and active, true otherwise.
     */
    val isNotUsed: Boolean
        get() = !isUsed

    /**
     * Resets this pointer to be used again.
     */
    open fun reset() {
        id = UNUSED_P
        index = UNUSED_P
    }
}
