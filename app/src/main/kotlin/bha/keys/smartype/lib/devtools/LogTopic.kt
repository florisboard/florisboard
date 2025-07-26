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

package bha.keys.smartype.lib.devtools

/**
 * This object holds all custom log topics for the [Flog] utility.
 *
 * _Contributors:_ if you add a new feature which is relatively large, you can
 * add a new topic here, just make sure it is a 2^n value and does not
 * exceed the maximum value of [FlogTopic].
 */
@Suppress("MemberVisibilityCanBePrivate", "Unused")
object LogTopic {
    const val NONE: bha.keys.smartype.lib.devtools.FlogTopic =
        bha.keys.smartype.lib.devtools.Flog.TOPIC_NONE
    const val OTHER: bha.keys.smartype.lib.devtools.FlogTopic =
        bha.keys.smartype.lib.devtools.Flog.TOPIC_OTHER
    const val ALL: bha.keys.smartype.lib.devtools.FlogTopic =
        bha.keys.smartype.lib.devtools.Flog.TOPIC_ALL

    const val IMS_EVENTS: bha.keys.smartype.lib.devtools.FlogTopic =           1u
    const val KEY_EVENTS: bha.keys.smartype.lib.devtools.FlogTopic =           2u

    const val SUBTYPE_MANAGER: bha.keys.smartype.lib.devtools.FlogTopic =      4u
    const val LAYOUT_MANAGER: bha.keys.smartype.lib.devtools.FlogTopic =       8u
    const val TEXT_KEYBOARD_VIEW: bha.keys.smartype.lib.devtools.FlogTopic =   16u
    const val GESTURES: bha.keys.smartype.lib.devtools.FlogTopic =             32u
    const val SMARTBAR: bha.keys.smartype.lib.devtools.FlogTopic =             64u
    const val THEME_MANAGER: bha.keys.smartype.lib.devtools.FlogTopic =        128u

    const val GLIDE: bha.keys.smartype.lib.devtools.FlogTopic =                512u
    const val CLIPBOARD: bha.keys.smartype.lib.devtools.FlogTopic =            1024u
    const val CRASH_UTILITY: bha.keys.smartype.lib.devtools.FlogTopic =        2048u

    const val SPELL_EVENTS: bha.keys.smartype.lib.devtools.FlogTopic =         4096u
    const val EDITOR_INSTANCE: bha.keys.smartype.lib.devtools.FlogTopic =      0x00_00_20_00u

    const val FILE_IO: bha.keys.smartype.lib.devtools.FlogTopic =              0x00_01_00_00u
    const val EXT_MANAGER: bha.keys.smartype.lib.devtools.FlogTopic =          0x00_02_00_00u
    const val EXT_INDEXING: bha.keys.smartype.lib.devtools.FlogTopic =         0x00_04_00_00u
}
