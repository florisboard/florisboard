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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class InteractionTimingOptions(
    val keyRepeatTimeout: Duration,
    val keyRepeatDelay: Duration,
    val longPressTimeout: Duration,
    val multiPressTimeout: Duration,
) {
    companion object {
        val Fallback = InteractionTimingOptions(
            keyRepeatTimeout = 300.milliseconds,
            keyRepeatDelay = 50.milliseconds,
            longPressTimeout = 300.milliseconds,
            multiPressTimeout = 300.milliseconds,
        )
    }
}
