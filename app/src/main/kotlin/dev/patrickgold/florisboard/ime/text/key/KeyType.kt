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

package dev.patrickgold.florisboard.ime.text.key

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Enum for declaring the type of the key.
 * List of possible key types:
 *  [Wikipedia](https://en.wikipedia.org/wiki/Keyboard_layout#Key_types)
 */
@Serializable(with = KeyTypeSerializer::class)
enum class KeyType {
    CHARACTER,
    ENTER_EDITING,
    FUNCTION,
    LOCK,
    MODIFIER,
    NAVIGATION,
    SYSTEM_GUI,
    NUMERIC,
    PLACEHOLDER,
    UNSPECIFIED;

    override fun toString(): String {
        return super.toString().lowercase()
    }

    companion object {
        fun fromString(string: String): KeyType {
            return valueOf(string.uppercase())
        }
    }
}

class KeyTypeSerializer : KSerializer<KeyType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KeyType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KeyType) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): KeyType {
        return KeyType.fromString(decoder.decodeString())
    }
}
