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

object LayoutTypeId {
    const val CHARACTERS =                  "characters"
    const val CHARACTERS_MOD =              "charactersMod"
    const val EXTENSION =                   "extension"
    const val NUMERIC =                     "numeric"
    const val NUMERIC_ADVANCED =            "numericAdvanced"
    const val NUMERIC_ROW =                 "numericRow"
    const val PHONE =                       "phone"
    const val PHONE2 =                      "phone2"
    const val SYMBOLS =                     "symbols"
    const val SYMBOLS_MOD =                 "symbolsMod"
    const val SYMBOLS2 =                    "symbols2"
    const val SYMBOLS2_MOD =                "symbols2Mod"
}

@Serializable(with = LayoutTypeSerializer::class)
enum class LayoutType(val id: String) {
    CHARACTERS(LayoutTypeId.CHARACTERS),
    CHARACTERS_MOD(LayoutTypeId.CHARACTERS_MOD),
    EXTENSION(LayoutTypeId.EXTENSION),
    NUMERIC(LayoutTypeId.NUMERIC),
    NUMERIC_ADVANCED(LayoutTypeId.NUMERIC_ADVANCED),
    NUMERIC_ROW(LayoutTypeId.NUMERIC_ROW),
    PHONE(LayoutTypeId.PHONE),
    PHONE2(LayoutTypeId.PHONE2),
    SYMBOLS(LayoutTypeId.SYMBOLS),
    SYMBOLS_MOD(LayoutTypeId.SYMBOLS_MOD),
    SYMBOLS2(LayoutTypeId.SYMBOLS2),
    SYMBOLS2_MOD(LayoutTypeId.SYMBOLS2_MOD);
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
