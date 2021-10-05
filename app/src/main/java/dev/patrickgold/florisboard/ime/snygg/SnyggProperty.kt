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

package dev.patrickgold.florisboard.ime.snygg

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

sealed interface SnyggProperty

@Serializable(with = SnyggAppearancePropertySerializer::class)
sealed class SnyggAppearanceProperty : SnyggProperty {
    companion object {
        private val SolidColorRegex = """^#([a-fA-F0-9]{2})?[a-fA-F0-9]{6}${'$'}""".toRegex()

        fun from(raw: String): SnyggAppearanceProperty {
            val str = raw.trim()
            return when {
                SolidColorRegex.matches(str) -> {
                    val colorInt = android.graphics.Color.parseColor(str)
                    SolidColor(Color(colorInt))
                }
                else -> SolidColor(Color.White)
            }
        }
    }

    data class SolidColor(
        val color: Color,
    ) : SnyggAppearanceProperty()

    data class LinearGradient(
        val dummy: Int,
    ) : SnyggAppearanceProperty()

    data class RadialGradient(
        val dummy: Int,
    ) : SnyggAppearanceProperty()

    data class ImageRef(
        val relPath: String,
    ) : SnyggAppearanceProperty()

    data class Inherit(
        val rawValue: String = "inherit",
    ) : SnyggAppearanceProperty()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = SnyggAppearanceProperty::class)
class SnyggAppearancePropertySerializer : KSerializer<SnyggAppearanceProperty> {
    override val descriptor = PrimitiveSerialDescriptor("SnyggAppearanceProperty", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SnyggAppearanceProperty) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): SnyggAppearanceProperty {
        return SnyggAppearanceProperty.from(decoder.decodeString())
    }
}
