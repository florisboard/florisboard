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

@file:Suppress("MemberVisibilityCanBePrivate")

package dev.patrickgold.florisboard.ime.keyboard

import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.LiveData
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.lib.devtools.flogError
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.Delegates

/**
 * This class is a helper managing the state of the text input logic which
 * affects the keyboard view in rendering and layouting the keys.
 *
 * The state class can hold flags or small unsigned integers, all added up
 * at max 64-bit though.
 *
 * The structure of this 8-byte state register is as follows: (Lower 4 bytes are pretty experimental rn)
 *
 * <Byte 3> | <Byte 2> | <Byte 1> | <Byte 0> | Description
 * ---------|----------|----------|----------|---------------------------------
 *          |          |          |     1111 | Active [KeyboardMode]
 *          |          |          | 1111     | Active [KeyVariation]
 *          |          |       11 |          | InputShiftState
 *          |          |      1   |          | Is selection active (length > 0)
 *          |          |     1    |          | Is manual selection mode
 *          |          |    1     |          | Is manual selection mode (start)
 *          |          |   1      |          | Is manual selection mode (end)
 *          |          | 1        |          | Is private mode
 *          |        1 |          |          | Is Smartbar quick actions visible
 *          |       1  |          |          | Is Smartbar showing inline suggestions
 *          |      1   |          |          | Is composing enabled
 *          |   1      |          |          | Is character half-width enabled
 *          |  1       |          |          | Is Kana Kata enabled
 *          | 1        |          |          | Is Kana small
 *      111 |          |          |          | Ime Ui Mode
 *     1    |          |          |          | Layout Direction (0=LTR, 1=RTL)
 *
 * The resulting structure is only relevant during a runtime lifespan and
 * thus can easily be changed without worrying about destroying some saved state.
 *
 * @property rawValue The internal register used to store the flags and region ints that
 *  this keyboard state represents.
 */
class KeyboardState private constructor(initValue: ULong) : LiveData<KeyboardState>() {
    companion object {
        const val M_KEYBOARD_MODE: ULong =                  0x0Fu
        const val O_KEYBOARD_MODE: Int =                    0
        const val M_KEY_VARIATION: ULong =                  0x0Fu
        const val O_KEY_VARIATION: Int =                    4
        const val M_INPUT_SHIFT_STATE: ULong =              0x03u
        const val O_INPUT_SHIFT_STATE: Int =                8
        const val M_IME_UI_MODE: ULong =                    0x07u
        const val O_IME_UI_MODE: Int =                      24

        const val F_IS_SELECTION_MODE: ULong =              0x00000400u
        const val F_IS_MANUAL_SELECTION_MODE: ULong =       0x00000800u
        const val F_IS_MANUAL_SELECTION_MODE_START: ULong = 0x00001000u
        const val F_IS_MANUAL_SELECTION_MODE_END: ULong =   0x00002000u
        const val F_IS_PRIVATE_MODE: ULong =                0x00008000u
        const val F_IS_QUICK_ACTIONS_VISIBLE: ULong =       0x00010000u
        const val F_IS_SHOWING_INLINE_SUGGESTIONS: ULong =  0x00020000u
        const val F_IS_COMPOSING_ENABLED: ULong =           0x00040000u

        const val F_IS_CHAR_HALF_WIDTH: ULong =             0x00200000u
        const val F_IS_KANA_KATA: ULong =                   0x00400000u
        const val F_IS_KANA_SMALL: ULong =                  0x00800000u

        const val F_IS_RTL_LAYOUT_DIRECTION: ULong =        0x08000000u

        const val STATE_ALL_ZERO: ULong =                   0uL

        const val INTEREST_ALL: ULong =                     ULong.MAX_VALUE
        const val INTEREST_NONE: ULong =                    0uL

        const val BATCH_ZERO: Int =                         0

        fun new(value: ULong = STATE_ALL_ZERO) = KeyboardState(value)
    }

    private var rawValue by Delegates.observable(initValue) { _, old, new -> if (old != new) dispatchState() }
    private val batchEditCount = AtomicInteger(BATCH_ZERO)

    init {
        dispatchState()
    }

    override fun setValue(value: KeyboardState?) {
        flogError { "Do not use setValue() directly" }
    }

    override fun postValue(value: KeyboardState?) {
        flogError { "Do not use postValue() directly" }
    }

    /**
     * Dispatches the new state to all observers if [batchEditCount] is [BATCH_ZERO] (= no active batch edits).
     */
    private fun dispatchState() {
        if (batchEditCount.get() == BATCH_ZERO) {
            try {
                super.setValue(this)
            } catch (e: Exception) {
                super.postValue(this)
            }
        }
    }

    /**
     * Begins a batch edit. Any modifications done during an active batch edit will not be dispatched to observers
     * until [endBatchEdit] is called. At any time given there can be multiple active batch edits at once. This
     * method is thread-safe and can be called from any thread.
     */
    fun beginBatchEdit() {
        batchEditCount.incrementAndGet()
    }

    /**
     * Ends a batch edit. Will dispatch the current state if there are no more other batch edits active. This method is
     * thread-safe and can be called from any thread.
     */
    fun endBatchEdit() {
        batchEditCount.decrementAndGet()
        dispatchState()
    }

    /**
     * Performs a batch edit by executing the modifier [block]. Any exception that [block] throws will be caught and
     * re-thrown after correctly ending the batch edit.
     */
    inline fun batchEdit(block: (KeyboardState) -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        beginBatchEdit()
        try {
            block(this)
        } catch (e: Throwable) {
            throw e
        } finally {
            endBatchEdit()
        }
    }

    /**
     * Resets this state register.
     *
     * @param newValue Optional, used to initialize the register value after the reset.
     *  Defaults to [STATE_ALL_ZERO].
     */
    fun reset(newValue: ULong = STATE_ALL_ZERO) {
        rawValue = newValue
    }

    /**
     * Resets this state register.
     *
     * @param newState A reference to a state which register value should be copied after
     *  the reset.
     */
    fun reset(newState: KeyboardState) {
        rawValue = newState.rawValue
    }

    fun snapshot(): KeyboardState {
        return new(rawValue)
    }

    internal fun getFlag(f: ULong): Boolean {
        return (rawValue and f) != STATE_ALL_ZERO
    }

    internal fun setFlag(f: ULong, v: Boolean) {
        rawValue = if (v) { rawValue or f } else { rawValue and f.inv() }
    }

    internal fun getRegion(m: ULong, o: Int): Int {
        return ((rawValue shr o) and m).toInt()
    }

    internal fun setRegion(m: ULong, o: Int, v: Int) {
        rawValue = (rawValue and (m shl o).inv()) or ((v.toULong() and m) shl o)
    }

    override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyboardState

        if (rawValue != other.rawValue) return false

        return true
    }

    override fun toString(): String {
        return "0x" + rawValue.toString(16).padStart(16, '0')
    }

    var keyVariation: KeyVariation
        get() = KeyVariation.fromInt(getRegion(M_KEY_VARIATION, O_KEY_VARIATION))
        set(v) { setRegion(M_KEY_VARIATION, O_KEY_VARIATION, v.toInt()) }

    var keyboardMode: KeyboardMode
        get() = KeyboardMode.fromInt(getRegion(M_KEYBOARD_MODE, O_KEYBOARD_MODE))
        set(v) { setRegion(M_KEYBOARD_MODE, O_KEYBOARD_MODE, v.toInt()) }

    var inputShiftState: InputShiftState
        get() = InputShiftState.fromInt(getRegion(M_INPUT_SHIFT_STATE, O_INPUT_SHIFT_STATE))
        set(v) { setRegion(M_INPUT_SHIFT_STATE, O_INPUT_SHIFT_STATE, v.toInt()) }

    var imeUiMode: ImeUiMode
        get() = ImeUiMode.fromInt(getRegion(M_IME_UI_MODE, O_IME_UI_MODE))
        set(v) { setRegion(M_IME_UI_MODE, O_IME_UI_MODE, v.toInt()) }

    var layoutDirection: LayoutDirection
        get() = if (getFlag(F_IS_RTL_LAYOUT_DIRECTION)) LayoutDirection.Rtl else LayoutDirection.Ltr
        set(v) { setFlag(F_IS_RTL_LAYOUT_DIRECTION, v == LayoutDirection.Rtl) }

    val isLowercase: Boolean
        get() = inputShiftState == InputShiftState.UNSHIFTED

    val isUppercase: Boolean
        get() = inputShiftState != InputShiftState.UNSHIFTED

    var isSelectionMode: Boolean
        get() = getFlag(F_IS_SELECTION_MODE)
        set(v) { setFlag(F_IS_SELECTION_MODE, v) }

    var isManualSelectionMode: Boolean
        get() = getFlag(F_IS_MANUAL_SELECTION_MODE)
        set(v) { setFlag(F_IS_MANUAL_SELECTION_MODE, v) }

    var isManualSelectionModeStart: Boolean
        get() = getFlag(F_IS_MANUAL_SELECTION_MODE_START)
        set(v) { setFlag(F_IS_MANUAL_SELECTION_MODE_START, v) }

    var isManualSelectionModeEnd: Boolean
        get() = getFlag(F_IS_MANUAL_SELECTION_MODE_END)
        set(v) { setFlag(F_IS_MANUAL_SELECTION_MODE_END, v) }

    var isCursorMode: Boolean
        get() = !isSelectionMode
        set(v) { isSelectionMode = !v }

    var isPrivateMode: Boolean
        get() = getFlag(F_IS_PRIVATE_MODE)
        set(v) { setFlag(F_IS_PRIVATE_MODE, v) }

    var isQuickActionsVisible: Boolean
        get() = getFlag(F_IS_QUICK_ACTIONS_VISIBLE)
        set(v) { setFlag(F_IS_QUICK_ACTIONS_VISIBLE, v) }

    var isShowingInlineSuggestions: Boolean
        get() = getFlag(F_IS_SHOWING_INLINE_SUGGESTIONS)
        set(v) { setFlag(F_IS_SHOWING_INLINE_SUGGESTIONS, v) }

    var isComposingEnabled: Boolean
        get() = getFlag(F_IS_COMPOSING_ENABLED)
        set(v) { setFlag(F_IS_COMPOSING_ENABLED, v) }

    var isKanaKata: Boolean
        get() = getFlag(F_IS_KANA_KATA)
        set(v) { setFlag(F_IS_KANA_KATA, v) }

    var isCharHalfWidth: Boolean
        get() = getFlag(F_IS_CHAR_HALF_WIDTH)
        set(v) { setFlag(F_IS_CHAR_HALF_WIDTH, v) }

    var isKanaSmall: Boolean
        get() = getFlag(F_IS_KANA_SMALL)
        set(v) { setFlag(F_IS_KANA_SMALL, v) }
}
