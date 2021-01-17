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

package dev.patrickgold.florisboard.util

import java.util.*

object TimeUtil {
    fun decode(v: Int): Time {
        return Time(
            hour = (v shr 8) and 0xFF,
            minute = v and 0xFF
        )
    }

    fun encode(v: Time): Int {
        return encode(v.hour, v.minute)
    }

    fun encode(h: Int, m: Int): Int {
        return ((h shl 8) and 0xFF00) + (m and 0xFF)
    }

    fun asString(v: Time): String {
        return String.format("%02d:%02d", v.hour, v.minute)
    }

    fun currentLocalTime(): Time {
        val rightNow = Calendar.getInstance()
        return Time(
            hour = rightNow[Calendar.HOUR_OF_DAY],
            minute = rightNow[Calendar.MINUTE]
        )
    }

    fun isNightTime(sunrise: Time, sunset: Time, current: Time): Boolean {
        return isNightTime(encode(sunrise), encode(sunset), encode(current))
    }

    fun isNightTime(sunrise: Int, sunset: Int, current: Int): Boolean {
        return if (sunrise <= sunset) {
            current !in sunrise..sunset
        } else {
            current in sunset..sunrise
        }
    }

    data class Time(val hour: Int, val minute: Int)
}
