/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.lib.android

import android.content.res.Resources

/**
 * Helper class for retrieving `com.android.internal.R.*` resources.
 *
 * Usage of this ids should always be done within a try..catch block, as there may be devices which have completely
 * modified system resources or something has changed in a newer Android version.
 */
object AndroidInternalR {
    @Suppress("ClassName")
    object string {
        val ime_action_go by lazy {
            Resources.getSystem().getIdentifier("ime_action_go", "string", "android")
        }
        val ime_action_search by lazy {
            Resources.getSystem().getIdentifier("ime_action_search", "string", "android")
        }
        val ime_action_send by lazy {
            Resources.getSystem().getIdentifier("ime_action_send", "string", "android")
        }
        val ime_action_next by lazy {
            Resources.getSystem().getIdentifier("ime_action_next", "string", "android")
        }
        val ime_action_done by lazy {
            Resources.getSystem().getIdentifier("ime_action_done", "string", "android")
        }
        val ime_action_previous by lazy {
            Resources.getSystem().getIdentifier("ime_action_previous", "string", "android")
        }
        val ime_action_default by lazy {
            Resources.getSystem().getIdentifier("ime_action_default", "string", "android")
        }
    }
}
