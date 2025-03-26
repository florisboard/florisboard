/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package org.florisboard.lib.snygg.value

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

sealed interface SnyggFontValue : SnyggValue

data class SnyggFontStyleValue(val fontStyle: FontStyle) : SnyggFontValue {
    companion object : SnyggEnumLikeValueEncoder<FontStyle>(
        serializationId = "textAlign",
        serializationMapping = mapOf(
            "normal" to FontStyle.Normal,
            "italic" to FontStyle.Italic,
        ),
        default = FontStyle.Normal,
        construct = { SnyggFontStyleValue(it) },
        destruct = { (it as SnyggFontStyleValue).fontStyle },
    )
    override fun encoder() = Companion
}

data class SnyggFontWeightValue(val fontWeight: FontWeight) : SnyggFontValue {
    companion object : SnyggEnumLikeValueEncoder<FontWeight>(
        serializationId = "textAlign",
        serializationMapping = mapOf(
            "thin" to FontWeight(100),
            "extra-light" to FontWeight(200),
            "light" to FontWeight(300),
            "normal" to FontWeight(400),
            "medium" to FontWeight(500),
            "semi-bold" to FontWeight(600),
            "bold" to FontWeight(700),
            "extra-bold" to FontWeight(800),
            "black" to FontWeight(900),
            "100" to FontWeight(100),
            "200" to FontWeight(200),
            "300" to FontWeight(300),
            "400" to FontWeight(400),
            "500" to FontWeight(500),
            "600" to FontWeight(600),
            "700" to FontWeight(700),
            "800" to FontWeight(800),
            "900" to FontWeight(900),
        ),
        default = FontWeight.Normal,
        construct = { SnyggFontWeightValue(it) },
        destruct = { (it as SnyggFontWeightValue).fontWeight },
    )
    override fun encoder() = Companion
}
