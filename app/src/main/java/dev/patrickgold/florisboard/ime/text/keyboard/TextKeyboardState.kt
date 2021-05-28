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

import dev.patrickgold.florisboard.ime.core.ImeOptions
import dev.patrickgold.florisboard.ime.text.key.KeyVariation

/**
 * This class is a helper managing the state of the text input logic which
 * affects the keyboard view in rendering and layouting the keys (only things
 * which change very often, no preference settings which are synced only when
 * the window is shown).
 *
 * The state class can hold flags or small unsigned integers, all added up
 * at max 32-bit though.
 *
 * The structure of this 32-bit/4-byte state is as follows:
 *  Byte 0: Caps
 *   0   | Flag   | Caps
 *   1   | Flag   | Caps lock
 *   2-4 | Region | Key variation
 *   5-7 | Unused
 *  Byte 1: ImeOptions
 *   0-3 | Region | Enter action
 *   4   | Flag   | No enter action
 *   5-7 | Unused
 *  Byte 2: Unused
 *   0-7 | Unused
 *  Byte 3: Unused
 *   0-7 | Unused
 *
 * The resulting structure is only relevant during a runtime lifespan and
 * thus can easily be changed without worrying about destroying some logic.
 */
class TextKeyboardState(var value: UInt) {
    companion object {
        const val FLAG_CAPS: UInt =                     0x00000001u
        const val FLAG_CAPS_LOCK: UInt =                0x00000002u
        const val FLAG_NO_ENTER_ACTION: UInt =          0x00001000u

        const val MASK_KEY_VARIATION: UInt =            0x07u
        const val OFFSET_KEY_VARIATION: Int =           2
        const val MASK_ENTER_ACTION: UInt =             0x0Fu
        const val OFFSET_ENTER_ACTION: Int =            8

        const val INTEREST_ALL: UInt =                  UInt.MAX_VALUE

        fun new() = TextKeyboardState(0u)
    }

    var matrixOfInterest: UInt = INTEREST_ALL

    fun update(newValue: UInt) {
        value = newValue and matrixOfInterest
    }

    private fun getFlag(f: UInt): Boolean {
        return (value and f) != 0u
    }

    private fun setFlag(f: UInt, v: Boolean) {
        value = if (v) { value or f } else { value and f.inv() }
    }

    private fun getRegion(m: UInt, o: Int): Int {
        return ((value shr o) and m).toInt()
    }

    private fun setRegion(m: UInt, o: Int, v: Int) {
        value = (value and (m shl o).inv()) or ((v.toUInt() and m) shl o)
    }

    override operator fun equals(other: Any?): Boolean {
        if (other is TextKeyboardState) {
            return (other.value and matrixOfInterest) == value
        }
        return false
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + matrixOfInterest.hashCode()
        return result
    }

    var caps: Boolean
        get() = getFlag(FLAG_CAPS)
        set(s) { setFlag(FLAG_CAPS, s) }

    var capsLock: Boolean
        get() = getFlag(FLAG_CAPS_LOCK)
        set(s) { setFlag(FLAG_CAPS_LOCK, s) }

    var noEnterAction: Boolean
        get() = getFlag(FLAG_NO_ENTER_ACTION)
        set(s) { setFlag(FLAG_NO_ENTER_ACTION, s) }

    var enterAction: ImeOptions.Action
        get() = ImeOptions.Action.fromInt(getRegion(MASK_ENTER_ACTION, OFFSET_ENTER_ACTION))
        set(v) { setRegion(MASK_ENTER_ACTION, OFFSET_ENTER_ACTION, v.value) }

    var keyVariation: KeyVariation
        get() = KeyVariation.fromInt(getRegion(MASK_KEY_VARIATION, OFFSET_KEY_VARIATION))
        set(v) { setRegion(MASK_KEY_VARIATION, OFFSET_KEY_VARIATION, v.value) }
}
