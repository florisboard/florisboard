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

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

sealed interface SnyggTextValue : SnyggValue

data class SnyggTextAlignValue(val textAlign: TextAlign) : SnyggTextValue {
    companion object : SnyggEnumLikeValueEncoder<TextAlign>(
        serializationId = "textAlign",
        serializationMapping = mapOf(
            "left" to TextAlign.Left,
            "right" to TextAlign.Right,
            "center" to TextAlign.Center,
            "justify" to TextAlign.Justify,
            "start" to TextAlign.Start,
            "end" to TextAlign.End,
        ),
        default = TextAlign.Left,
        construct = { SnyggTextAlignValue(it) },
        destruct = { (it as SnyggTextAlignValue).textAlign },
    )
    override fun encoder() = Companion
}

data class SnyggTextDecorationLineValue(val textDecoration: TextDecoration) : SnyggTextValue {
    companion object : SnyggEnumLikeValueEncoder<TextDecoration>(
        serializationId = "textAlign",
        serializationMapping = mapOf(
            "none" to TextDecoration.None,
            "underline" to TextDecoration.Underline,
            "line-through" to TextDecoration.LineThrough,
        ),
        default = TextDecoration.None,
        construct = { SnyggTextDecorationLineValue(it) },
        destruct = { (it as SnyggTextDecorationLineValue).textDecoration },
    )
    override fun encoder() = Companion
}

data class SnyggTextOverflowValue(val textOverflow: TextOverflow) : SnyggTextValue {
    companion object : SnyggEnumLikeValueEncoder<TextOverflow>(
        serializationId = "textAlign",
        serializationMapping = mapOf(
            "clip" to TextOverflow.Clip,
            "ellipsis" to TextOverflow.Ellipsis,
            "visible" to TextOverflow.Visible,
        ),
        default = TextOverflow.Clip,
        construct = { SnyggTextOverflowValue(it) },
        destruct = { (it as SnyggTextOverflowValue).textOverflow },
    )
    override fun encoder() = Companion
}
