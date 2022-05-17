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

package dev.patrickgold.florisboard.ime.text.key

object KeyCode {
    object Spec {
        const val CHARACTERS_MIN = 1
        const val CHARACTERS_MAX = 65535
        val CHARACTERS = CHARACTERS_MIN..CHARACTERS_MAX

        const val INTERNAL_MIN = -9999
        const val INTERNAL_MAX = -1
        val INTERNAL = INTERNAL_MIN..INTERNAL_MAX
    }

    const val UNSPECIFIED =                    0

    const val PHONE_WAIT =                    59 // ;
    const val PHONE_PAUSE =                   44 // ,

    const val SPACE =                         32
    const val ESCAPE =                        27
    const val ENTER =                         10
    const val TAB =                            9

    const val CTRL =                          -1
    const val CTRL_LOCK =                     -2
    const val ALT =                           -3
    const val ALT_LOCK =                      -4
    const val FN =                            -5
    const val FN_LOCK =                       -6
    const val DELETE =                        -7
    const val DELETE_WORD =                   -8
    const val FORWARD_DELETE =                -9
    const val FORWARD_DELETE_WORD =          -10
    const val SHIFT =                        -11
    const val CAPS_LOCK =                    -13

    const val ARROW_LEFT =                   -21
    const val ARROW_RIGHT =                  -22
    const val ARROW_UP =                     -23
    const val ARROW_DOWN =                   -24
    const val MOVE_START_OF_PAGE =           -25
    const val MOVE_END_OF_PAGE =             -26
    const val MOVE_START_OF_LINE =           -27
    const val MOVE_END_OF_LINE =             -28

    const val CLIPBOARD_COPY =               -31
    const val CLIPBOARD_CUT =                -32
    const val CLIPBOARD_PASTE =              -33
    const val CLIPBOARD_SELECT =             -34
    const val CLIPBOARD_SELECT_ALL =         -35
    const val CLIPBOARD_CLEAR_HISTORY =      -36
    const val CLIPBOARD_CLEAR_FULL_HISTORY = -37
    const val CLIPBOARD_CLEAR_PRIMARY_CLIP = -38

    const val COMPACT_LAYOUT_TO_LEFT =      -111
    const val COMPACT_LAYOUT_TO_RIGHT =     -112
    const val SPLIT_LAYOUT =                -113
    const val MERGE_LAYOUT =                -114

    const val UNDO =                        -131
    const val REDO =                        -132

    const val VIEW_CHARACTERS =             -201
    const val VIEW_SYMBOLS =                -202
    const val VIEW_SYMBOLS2 =               -203
    const val VIEW_NUMERIC =                -204
    const val VIEW_NUMERIC_ADVANCED =       -205
    const val VIEW_PHONE =                  -206
    const val VIEW_PHONE2 =                 -207

    const val IME_UI_MODE_TEXT =            -211
    const val IME_UI_MODE_MEDIA =           -212
    const val IME_UI_MODE_CLIPBOARD =       -213

    const val SYSTEM_INPUT_METHOD_PICKER =  -221
    const val SYSTEM_PREV_INPUT_METHOD =    -222
    const val SYSTEM_NEXT_INPUT_METHOD =    -223
    const val IME_SUBTYPE_PICKER =          -224
    const val IME_PREV_SUBTYPE =            -225
    const val IME_NEXT_SUBTYPE =            -226
    const val LANGUAGE_SWITCH =             -227
    const val TOGGLE_SMARTBAR_VISIBILITY =  -228

    const val IME_SHOW_UI =                 -231
    const val IME_HIDE_UI =                 -232
    const val VOICE_INPUT =                 -233

    const val URI_COMPONENT_TLD =           -255

    const val SETTINGS =                    -301

    const val CURRENCY_SLOT_1 =             -801
    const val CURRENCY_SLOT_2 =             -802
    const val CURRENCY_SLOT_3 =             -803
    const val CURRENCY_SLOT_4 =             -804
    const val CURRENCY_SLOT_5 =             -805
    const val CURRENCY_SLOT_6 =             -806

    const val MULTIPLE_CODE_POINTS =        -902

    const val CHAR_WIDTH_SWITCHER =        -9701
    const val CHAR_WIDTH_FULL =            -9702
    const val CHAR_WIDTH_HALF =            -9703

    const val KANA_SMALL =                 12307
    const val KANA_SWITCHER =              -9710
    const val KANA_HIRA =                  -9711
    const val KANA_KATA =                  -9712
    const val KANA_HALF_KATA =             -9713

    const val KESHIDA =                     1600
    const val HALF_SPACE =                  8204

    const val CJK_SPACE =                  12288
}
