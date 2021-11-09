/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.keyboard

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = LayoutTypeSerializer::class)
enum class LayoutType(val id: String) {
    CHARACTERS("characters"),
    CHARACTERS_MOD("characters-mod"),
    EXTENSION("extension"),
    NUMERIC("numeric"),
    NUMERIC_ADVANCED("numeric-advanced"),
    NUMERIC_ROW("numeric-row"),
    PHONE("phone"),
    PHONE2("phone2"),
    SYMBOLS("symbols"),
    SYMBOLS_MOD("symbols-mod"),
    SYMBOLS2("symbols2"),
    SYMBOLS2_MOD("symbols2-mod");
}

private class LayoutTypeSerializer : KSerializer<LayoutType> {
    override val descriptor = PrimitiveSerialDescriptor("LayoutType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LayoutType) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): LayoutType {
        return LayoutType.values().find { it.id == decoder.decodeString() }!!
    }
}
