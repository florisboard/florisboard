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

package dev.patrickgold.florisboard.ime.input

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

enum class InputFeedbackActivationMode  {
    RESPECT_SYSTEM_SETTINGS,
    IGNORE_SYSTEM_SETTINGS;

    companion object {
        @Composable
        fun audioListEntries() = listPrefEntries {
            entry(
                key = RESPECT_SYSTEM_SETTINGS,
                label = stringRes(R.string.enum__input_feedback_activation_mode__audio_respect_system_settings),
            )
            entry(
                key = IGNORE_SYSTEM_SETTINGS,
                label = stringRes(R.string.enum__input_feedback_activation_mode__audio_ignore_system_settings),
            )
        }

        @Composable
        fun hapticListEntries() = listPrefEntries {
            entry(
                key = RESPECT_SYSTEM_SETTINGS,
                label = stringRes(R.string.enum__input_feedback_activation_mode__haptic_respect_system_settings),
            )
            entry(
                key = IGNORE_SYSTEM_SETTINGS,
                label = stringRes(R.string.enum__input_feedback_activation_mode__haptic_ignore_system_settings),
            )
        }
    }
}
