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

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

object LocaleUtils {
    private val DELIMITER = """[_-]""".toRegex()

    fun stringToLocale(string: String): Locale {
        return when {
            string.contains(DELIMITER) -> {
                val lc = string.split(DELIMITER)
                Locale(lc[0], lc[1])
            }
            else -> {
                Locale(string)
            }
        }
    }

    class JsonAdapter() {
        @FromJson
        fun fromJson(raw: String): Locale {
            return stringToLocale(raw)
        }
        @ToJson
        fun toJson(raw: Locale): String {
            return raw.toString()
        }
    }
}
