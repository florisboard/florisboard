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

package dev.patrickgold.florisboard.ime.text.layout

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Defines the type of the layout.
 */
@Serializable(with = LayoutTypeSerializer::class)
enum class LayoutType {
    CHARACTERS,
    CHARACTERS_MOD,
    EXTENSION,
    NUMERIC,
    NUMERIC_ADVANCED,
    NUMERIC_ROW,
    PHONE,
    PHONE2,
    SYMBOLS,
    SYMBOLS_MOD,
    SYMBOLS2,
    SYMBOLS2_MOD;

    override fun toString(): String {
        return super.toString().replace("_", "/").lowercase()
    }

    companion object {
        fun fromString(string: String): LayoutType {
            return valueOf(string.replace("/", "_").uppercase())
        }
    }
}

class LayoutTypeSerializer : KSerializer<LayoutType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LayoutType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LayoutType) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LayoutType {
        return LayoutType.fromString(decoder.decodeString())
    }
}
