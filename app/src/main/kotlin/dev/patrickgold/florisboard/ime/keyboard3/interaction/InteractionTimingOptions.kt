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

package dev.patrickgold.florisboard.ime.keyboard3.interaction

import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.lib.text.asK3String
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class InteractionTimingOptions(
    val keyRepeatTimeout: Duration,
    val keyRepeatDelay: Duration,
    val longPressTimeout: Duration,
    val multiTapTimeout: Duration,
    val multiPressTimeout: Duration,
) {
    fun getKeyRepeatTimeout(output: K3StringOrDescriptor? = null): Duration {
        return keyRepeatTimeout
    }

    fun getKeyRepeatDelay(output: K3StringOrDescriptor? = null): Duration {
        val factor = when (output) {
            ImeActions.BackspaceWord,
            ImeActions.DeleteWord,
            ImeActions.Undo,
            ImeActions.Redo -> 5.0
            else -> 1.0
        }
        return keyRepeatDelay * factor
    }

    fun getLongPressTimeout(output: K3StringOrDescriptor? = null): Duration {
        val factor = when (output) {
            ASCII_SPACE -> 2.5
            else -> 1.0
        }
        return longPressTimeout * factor
    }

    companion object {
        val Default = InteractionTimingOptions(
            keyRepeatTimeout = 300.milliseconds,
            keyRepeatDelay = 50.milliseconds,
            longPressTimeout = 300.milliseconds,
            multiTapTimeout = 800.milliseconds,
            multiPressTimeout = 300.milliseconds,
        )

        val ASCII_SPACE = " ".asK3String()
    }
}
