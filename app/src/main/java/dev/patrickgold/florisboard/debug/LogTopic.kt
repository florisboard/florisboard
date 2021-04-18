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

@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.patrickgold.florisboard.debug

/**
 * This object holds all custom log topics for the [Flog] utility.
 *
 * _Contributors:_ if you add a new feature which is relatively large, you can
 * add a new topic here, just make sure it is a 2^n value and does not
 * exceed the maximum value of [FlogTopic].
 */
@Suppress("MemberVisibilityCanBePrivate", "Unused")
object LogTopic {
    const val NONE: FlogTopic =                 Flog.TOPIC_NONE
    const val OTHER: FlogTopic =                Flog.TOPIC_OTHER
    const val ALL: FlogTopic =                  Flog.TOPIC_ALL

    const val IMS_EVENTS: FlogTopic =           1u
    const val KEY_EVENTS: FlogTopic =           2u
    const val GLIDE: FlogTopic =                512u
    const val CLIPBOARD: FlogTopic =            1024u
    const val CRASH_UTILITY: FlogTopic =        2048u
}
