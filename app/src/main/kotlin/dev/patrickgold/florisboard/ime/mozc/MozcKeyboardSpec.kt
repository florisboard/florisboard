/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.mozc

import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.CompositionMode
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Request.CrossingEdgeBehavior
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Request.SpaceOnAlphanumeric
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Request.SpecialRomanjiTable

enum class MozcKeyboardSpec(
    val specName: MozcKeyboardSpecificationName,
    val isHardwareKeyboard: Boolean,
    val compositionMode: CompositionMode,
    val specialRomanjiTable: SpecialRomanjiTable,
    val spaceOnAlphanumeric: SpaceOnAlphanumeric,
    val isKanaModifierInsensitiveConversion: Boolean,
    val crossingEdgeBehavior: CrossingEdgeBehavior
) {
    // 12 keys.
    TWELVE_KEY_TOGGLE_KANA(
        MozcKeyboardSpecificationName("TWELVE_KEY_TOGGLE_KANA", 0, 2, 0),
        false,
        CompositionMode.HIRAGANA,
        SpecialRomanjiTable.TWELVE_KEYS_TO_HIRAGANA,
        SpaceOnAlphanumeric.SPACE_OR_CONVERT_KEEPING_COMPOSITION,
        true,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    TWELVE_KEY_TOGGLE_ALPHABET(
        MozcKeyboardSpecificationName("TWELVE_KEY_TOGGLE_ALPHABET", 0, 2, 0),
        false,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.TWELVE_KEYS_TO_HALFWIDTHASCII,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    TWELVE_KEY_TOGGLE_QWERTY_ALPHABET(
        MozcKeyboardSpecificationName("TWELVE_KEY_TOGGLE_QWERTY_ALPHABET", 0, 5, 0),
        false,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.QWERTY_MOBILE_TO_HALFWIDTHASCII,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.COMMIT_WITHOUT_CONSUMING
    ),

    // Flick mode.
    TWELVE_KEY_FLICK_KANA(
        MozcKeyboardSpecificationName("TWELVE_KEY_FLICK_KANA", 0, 2, 0),
        false,
        CompositionMode.HIRAGANA,
        SpecialRomanjiTable.FLICK_TO_HIRAGANA,
        SpaceOnAlphanumeric.SPACE_OR_CONVERT_KEEPING_COMPOSITION,
        true,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    TWELVE_KEY_FLICK_ALPHABET(
        MozcKeyboardSpecificationName("TWELVE_KEY_FLICK_ALPHABET", 0, 2, 0),
        false,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.FLICK_TO_HALFWIDTHASCII,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.COMMIT_WITHOUT_CONSUMING
    ),

    TWELVE_KEY_TOGGLE_FLICK_KANA(
        MozcKeyboardSpecificationName("TWELVE_KEY_TOGGLE_FLICK_KANA", 0, 2, 0),
        false,
        CompositionMode.HIRAGANA,
        SpecialRomanjiTable.TOGGLE_FLICK_TO_HIRAGANA,
        SpaceOnAlphanumeric.SPACE_OR_CONVERT_KEEPING_COMPOSITION,
        true,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    TWELVE_KEY_TOGGLE_FLICK_ALPHABET(
        MozcKeyboardSpecificationName("TWELVE_KEY_TOGGLE_FLICK_ALPHABET", 0, 2, 0),
        false,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.TOGGLE_FLICK_TO_HALFWIDTHASCII,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    // QWERTY keyboard.
    QWERTY_KANA(
        MozcKeyboardSpecificationName("QWERTY_KANA", 0, 4, 0),
        false,
        CompositionMode.HIRAGANA,
        SpecialRomanjiTable.QWERTY_MOBILE_TO_HIRAGANA,
        SpaceOnAlphanumeric.SPACE_OR_CONVERT_KEEPING_COMPOSITION,
        false,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    QWERTY_ALPHABET(
        MozcKeyboardSpecificationName("QWERTY_ALPHABET", 0, 5, 0),
        false,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.QWERTY_MOBILE_TO_HALFWIDTHASCII,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.COMMIT_WITHOUT_CONSUMING
    ),

    QWERTY_ALPHABET_NUMBER(
        MozcKeyboardSpecificationName("QWERTY_ALPHABET_NUMBER", 0, 3, 0),
        false,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.QWERTY_MOBILE_TO_HALFWIDTHASCII,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.COMMIT_WITHOUT_CONSUMING
    ),

    // Godan keyboard.
    GODAN_KANA(
        MozcKeyboardSpecificationName("GODAN_KANA", 0, 2, 0),
        false,
        CompositionMode.HIRAGANA,
        SpecialRomanjiTable.GODAN_TO_HIRAGANA,
        SpaceOnAlphanumeric.SPACE_OR_CONVERT_KEEPING_COMPOSITION,
        true,
        CrossingEdgeBehavior.COMMIT_WITHOUT_CONSUMING
    ),

    NUMBER(
        MozcKeyboardSpecificationName("NUMBER", 0, 1, 0),
        false,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.QWERTY_MOBILE_TO_HALFWIDTHASCII,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    // Number keyboard on symbol input view.
    SYMBOL_NUMBER(
        MozcKeyboardSpecificationName("TWELVE_KEY_SYMBOL_NUMBER", 0, 1, 0),
        false,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.QWERTY_MOBILE_TO_HALFWIDTHASCII,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    // HARDWARE QWERTY keyboard.
    HARDWARE_QWERTY_KANA(
        MozcKeyboardSpecificationName("HARDWARE_QWERTY_KANA", 0, 1, 0),
        true,
        CompositionMode.HIRAGANA,
        SpecialRomanjiTable.DEFAULT_TABLE,
        SpaceOnAlphanumeric.SPACE_OR_CONVERT_KEEPING_COMPOSITION,
        false,
        CrossingEdgeBehavior.DO_NOTHING
    ),

    HARDWARE_QWERTY_ALPHABET(
        MozcKeyboardSpecificationName("HARDWARE_QWERTY_ALPHABET", 0, 1, 0),
        true,
        CompositionMode.HALF_ASCII,
        SpecialRomanjiTable.DEFAULT_TABLE,
        SpaceOnAlphanumeric.COMMIT,
        false,
        CrossingEdgeBehavior.DO_NOTHING
    )
}

