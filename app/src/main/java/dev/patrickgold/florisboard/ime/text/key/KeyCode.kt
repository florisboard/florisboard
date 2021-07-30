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

package dev.patrickgold.florisboard.ime.text.key

object KeyCode {
    const val SPACE =                         32
    const val ENTER =                         10
    const val TAB =                            9
    const val ESCAPE =                        27

    const val DELETE =                        -5
    const val DELETE_WORD =                   -7
    const val FORWARD_DELETE =                -8

    const val QUICK_TEXT =                   -10
    const val QUICK_TEXT_POPUP =            -102
    const val DOMAIN =                        -9

    const val SHIFT =                         -1
    const val ALT =                           -6
    const val CTRL =                         -11
    const val SHIFT_LOCK =                   -14
    const val CTRL_LOCK =                    -15

    const val ARROW_LEFT =                   -20
    const val ARROW_RIGHT =                  -21
    const val ARROW_UP =                     -22
    const val ARROW_DOWN =                   -23
    const val MOVE_START_OF_PAGE =           -24
    const val MOVE_END_OF_PAGE =             -25
    const val MOVE_START_OF_LINE =           -26
    const val MOVE_END_OF_LINE =             -27

    const val SETTINGS =                    -100
    const val CANCEL =                        -3
    const val CLEAR_INPUT =                  -13
    const val VOICE_INPUT =                   -4

    const val UNSPECIFIED =                    0

    const val SPLIT_LAYOUT =                -110
    const val MERGE_LAYOUT =                -111
    const val COMPACT_LAYOUT_TO_LEFT =      -112
    const val COMPACT_LAYOUT_TO_RIGHT =     -113

    const val UTILITY_KEYBOARD =            -120

    const val CLIPBOARD_COPY =              -130
    const val CLIPBOARD_CUT =               -131
    const val CLIPBOARD_PASTE =             -132
    const val CLIPBOARD_PASTE_POPUP =       -133
    const val CLIPBOARD_SELECT =            -134
    const val CLIPBOARD_SELECT_ALL =        -135
    const val CLEAR_CLIPBOARD_HISTORY =     -136

    const val UNDO =                        -137
    const val REDO =                        -138

    const val PHONE_PAUSE =                   44
    const val PHONE_WAIT =                    59

    const val VIEW_CHARACTERS =             -201
    const val VIEW_SYMBOLS =                -202
    const val VIEW_SYMBOLS2 =               -203
    const val VIEW_NUMERIC =                -204
    const val VIEW_NUMERIC_ADVANCED =       -205
    const val VIEW_PHONE =                  -206
    const val VIEW_PHONE2 =                 -207

    const val LANGUAGE_SWITCH =             -210
    const val SHOW_INPUT_METHOD_PICKER =    -211
    const val SWITCH_TO_TEXT_CONTEXT =      -212
    const val SWITCH_TO_MEDIA_CONTEXT =     -213
    const val SWITCH_TO_CLIPBOARD_CONTEXT = -214
    const val TOGGLE_ONE_HANDED_MODE_LEFT = -215
    const val TOGGLE_ONE_HANDED_MODE_RIGHT =-216
    const val URI_COMPONENT_TLD =           -255

    const val CURRENCY_SLOT_1 =             -801
    const val CURRENCY_SLOT_2 =             -802
    const val CURRENCY_SLOT_3 =             -803
    const val CURRENCY_SLOT_4 =             -804
    const val CURRENCY_SLOT_5 =             -805
    const val CURRENCY_SLOT_6 =             -806

    const val INTERNAL_BATCH_EDIT =         -901
    const val MULTIPLE_CODE_POINTS =        -902

    const val KESHIDA =                     1600
    const val HALF_SPACE =                  8204

    const val CJK_SPACE =                  12288

    const val KANA_SMALL =                 -9700
    const val KANA_SWITCHER =              -9710
    const val KANA_HIRA =                  -9711
    const val KANA_KATA =                  -9712
    const val KANA_HALF =                  -9713
}
