/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.lib.util

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import dev.patrickgold.florisboard.lib.FlorisLocale
import org.florisboard.lib.android.AndroidVersion
import java.time.Instant
import java.time.format.DateTimeFormatter

object TimeUtils {
    private val ISO_INSTANT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", FlorisLocale.ENGLISH.base)

    fun currentUtcTimestamp(): CharSequence {
        return if (AndroidVersion.ATLEAST_API26_O) {
            DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        } else {
            ISO_INSTANT.format(Calendar.getInstance(TimeZone.GMT_ZONE, FlorisLocale.ENGLISH.base))
        }
    }
}
