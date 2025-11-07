/*
 * Copyright (C) 2022-2025 The OmniBoard Contributors
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

package dev.silo.omniboard.lib.util

import android.icu.text.SimpleDateFormat
import dev.silo.omniboard.lib.OmniLocale
import dev.silo.jetpref.datastore.model.LocalTime
import java.time.Instant
import java.time.format.DateTimeFormatter

object TimeUtils {
    private val ISO_INSTANT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", OmniLocale.ENGLISH.base)

    fun currentUtcTimestamp(): CharSequence {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    }

    val LocalTime.javaLocalTime: java.time.LocalTime
        get() = java.time.LocalTime.of(hour, minute)
}
