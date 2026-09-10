/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import androidx.compose.ui.unit.LayoutDirection
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.keyboard3.ImeState
import dev.patrickgold.florisboard.ime.sheet.isAnyBottomSheetVisible
import dev.patrickgold.florisboard.ime.text.key.KeyVariation

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
 *          |          |          | 1111     | Active [KeyVariation]
 *          |          |       11 |          | InputShiftState
 *          |          |     1    |          | Is manual selection mode
 *          |          |    1     |          | Is manual selection mode (start)
 *          |          |   1      |          | Is manual selection mode (end)
 *          |          | 1        |          | Is incognito mode
 *          |        1 |          |          | Is quick actions overflow visible
 *          |       1  |          |          | Is quick actions editor visible
 *          |    1     |          |          | Is composing enabled
 *          |   1      |          |          | Is character half-width enabled
 *          |  1       |          |          | Is Kana Kata enabled
 *          | 1        |          |          | Is Kana small
 *      111 |          |          |          | Ime Ui Mode
 *     1    |          |          |          | Layout Direction (0=LTR, 1=RTL)
 *
 * <Byte 7> | <Byte 6> | <Byte 5> | <Byte 4> | Description
 * ---------|----------|----------|----------|---------------------------------
 *          |          |          |        1 | Subtype selection dialog visible
 *        1 |          |          |          | Devtools: Show drag&drop helpers
 *
 * The resulting structure is only relevant during a runtime lifespan and
 * thus can easily be changed without worrying about destroying some saved state.
 *
 * @property rawValue The internal register used to store the flags and region ints that
 *  this keyboard state represents.
 */
@JvmInline
value class ImeStateFlags(val rawValue: ULong = STATE_ALL_ZERO) {
    companion object {
        const val M_KEY_VARIATION: ULong =                  0x0Fu
        const val O_KEY_VARIATION: Int =                    4
        const val M_INPUT_SHIFT_STATE: ULong =              0x03u
        const val O_INPUT_SHIFT_STATE: Int =                8
        const val M_IME_UI_MODE: ULong =                    0x07u
        const val O_IME_UI_MODE: Int =                      24

        const val F_IS_MANUAL_SELECTION_MODE: ULong =       0x00000800u
        const val F_IS_MANUAL_SELECTION_MODE_START: ULong = 0x00001000u
        const val F_IS_MANUAL_SELECTION_MODE_END: ULong =   0x00002000u
        const val F_IS_INCOGNITO_MODE: ULong =              0x00008000u
        const val F_IS_ACTIONS_OVERFLOW_VISIBLE: ULong =    0x00010000u
        const val F_IS_ACTIONS_EDITOR_VISIBLE: ULong =      0x00020000u
        const val F_IS_COMPOSING_ENABLED: ULong =           0x00100000u

        const val F_IS_CHAR_HALF_WIDTH: ULong =             0x00200000u
        const val F_IS_KANA_KATA: ULong =                   0x00400000u
        const val F_IS_KANA_SMALL: ULong =                  0x00800000u

        const val F_IS_RTL_LAYOUT_DIRECTION: ULong =        0x08000000u

        const val F_IS_SUBTYPE_SELECTION_VISIBLE: ULong =   0x1_0000_0000u

        const val F_DEBUG_SHOW_DRAG_AND_DROP_HELPERS =      0x01_00_00_00_00_00_00_00uL

        const val STATE_ALL_ZERO: ULong =                   0uL
    }

    private fun gettingFlag(f: ULong): Boolean {
        return (rawValue and f) != STATE_ALL_ZERO
    }

    private fun settingFlag(f: ULong, v: Boolean): ImeStateFlags {
        return ImeStateFlags(if (v) { rawValue or f } else { rawValue and f.inv() })
    }

    private fun gettingRegion(m: ULong, o: Int): Int {
        return ((rawValue shr o) and m).toInt()
    }

    private fun settingRegion(m: ULong, o: Int, v: Int): ImeStateFlags {
        return ImeStateFlags((rawValue and (m shl o).inv()) or ((v.toULong() and m) shl o))
    }

    override fun toString(): String {
        return "0x" + rawValue.toString(16).padStart(16, '0')
    }

    val keyVariation: KeyVariation
        get() = KeyVariation.fromInt(gettingRegion(M_KEY_VARIATION, O_KEY_VARIATION))

    fun withKeyVariation(v: KeyVariation) = settingRegion(M_KEY_VARIATION, O_KEY_VARIATION, v.toInt())

    val inputShiftState: InputShiftState
        get() = InputShiftState.fromInt(gettingRegion(M_INPUT_SHIFT_STATE, O_INPUT_SHIFT_STATE))

    fun withInputShiftState(v: InputShiftState) = settingRegion(M_INPUT_SHIFT_STATE, O_INPUT_SHIFT_STATE, v.toInt())

    val imeUiMode: ImeUiMode
        get() = ImeUiMode.fromInt(gettingRegion(M_IME_UI_MODE, O_IME_UI_MODE))

    fun withImeUiMode(v: ImeUiMode) = settingRegion(M_IME_UI_MODE, O_IME_UI_MODE, v.toInt())

    val layoutDirection: LayoutDirection
        get() = if (gettingFlag(F_IS_RTL_LAYOUT_DIRECTION)) LayoutDirection.Rtl else LayoutDirection.Ltr

    fun withLayoutDirection(v: LayoutDirection) = settingFlag(F_IS_RTL_LAYOUT_DIRECTION, v == LayoutDirection.Rtl)

    val isLowercase: Boolean
        get() = inputShiftState == InputShiftState.UNSHIFTED

    val isUppercase: Boolean
        get() = inputShiftState != InputShiftState.UNSHIFTED

    val isManualSelectionMode: Boolean
        get() = gettingFlag(F_IS_MANUAL_SELECTION_MODE)

    fun withManualSelectionMode(v: Boolean) = settingFlag(F_IS_MANUAL_SELECTION_MODE, v)

    val isManualSelectionModeStart: Boolean
        get() = gettingFlag(F_IS_MANUAL_SELECTION_MODE_START)

    fun withManualSelectionModeStart(v: Boolean) = settingFlag(F_IS_MANUAL_SELECTION_MODE_START, v)

    val isManualSelectionModeEnd: Boolean
        get() = gettingFlag(F_IS_MANUAL_SELECTION_MODE_END)

    fun withManualSelectionModeEnd(v: Boolean) = settingFlag(F_IS_MANUAL_SELECTION_MODE_END, v)

    val isIncognitoMode: Boolean
        get() = gettingFlag(F_IS_INCOGNITO_MODE)

    fun withIncognitoMode(v: Boolean) = settingFlag(F_IS_INCOGNITO_MODE, v)

    val isActionsOverflowVisible: Boolean
        get() = gettingFlag(F_IS_ACTIONS_OVERFLOW_VISIBLE)

    fun withActionsOverflowVisible(v: Boolean) = settingFlag(F_IS_ACTIONS_OVERFLOW_VISIBLE, v)

    val isActionsEditorVisible: Boolean
        get() = gettingFlag(F_IS_ACTIONS_EDITOR_VISIBLE)

    fun withActionsEditorVisible(v: Boolean) = settingFlag(F_IS_ACTIONS_EDITOR_VISIBLE, v)

    val isSubtypeSelectionVisible: Boolean
        get() = gettingFlag(F_IS_SUBTYPE_SELECTION_VISIBLE)

    fun withSubtypeSelectionVisible(v: Boolean) = settingFlag(F_IS_SUBTYPE_SELECTION_VISIBLE, v)

    val isComposingEnabled: Boolean
        get() = gettingFlag(F_IS_COMPOSING_ENABLED)

    fun withComposingEnabled(v: Boolean) = settingFlag(F_IS_COMPOSING_ENABLED, v)

    val isKanaKata: Boolean
        get() = gettingFlag(F_IS_KANA_KATA)

    fun withKanaKata(v: Boolean) = settingFlag(F_IS_KANA_KATA, v)

    val isCharHalfWidth: Boolean
        get() = gettingFlag(F_IS_CHAR_HALF_WIDTH)

    fun withCharHalfWidth(v: Boolean) = settingFlag(F_IS_CHAR_HALF_WIDTH, v)

    val isKanaSmall: Boolean
        get() = gettingFlag(F_IS_KANA_SMALL)

    fun withKanaSmall(v: Boolean) = settingFlag(F_IS_KANA_SMALL, v)

    val debugShowDragAndDropHelpers: Boolean
        get() = gettingFlag(F_DEBUG_SHOW_DRAG_AND_DROP_HELPERS)

    fun withDebugShowDragAndDropHelpers(v: Boolean) = settingFlag(F_DEBUG_SHOW_DRAG_AND_DROP_HELPERS, v)
}

fun ImeState.isFullscreenInputRequired(): Boolean {
    return isAnyBottomSheetVisible()
}
