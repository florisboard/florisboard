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

package dev.patrickgold.florisboard.res.ext

import dev.patrickgold.florisboard.common.stringBuilder
import dev.patrickgold.florisboard.res.FlorisRef
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.min

@Serializable(with = ExtensionAuthorSerializer::class)
data class ExtensionAuthor(
    val name: String,
    val email: String? = null,
    val url: FlorisRef? = null,
) {
    companion object {
        private val ValidationRegex = """^\s*[\p{L}\d._-][\p{L}\d\s._-]*(<[^<>]+>)?\s*(\([^()]+\))?\s*${'$'}""".toRegex()

        fun from(str: String): ExtensionAuthor? {
            if (str.isBlank() || !ValidationRegex.matches(str)) {
                return null
            }
            val emailStart = str.indexOf('<').let { if (it < 0) str.length else (it + 1) }
            val emailEnd = str.indexOf('>').let { if (it < 0) str.length else it }
            val urlStart = str.indexOf('(').let { if (it < 0) str.length else (it + 1) }
            val urlEnd = str.indexOf(')').let { if (it < 0) str.length else it }
            val nameStart = 0
            val nameEnd = if (emailStart == str.length && urlStart == str.length) {
                str.length
            } else {
                (min(emailStart, urlStart) - 1)
            }
            val name = str.substring(nameStart, nameEnd).trim()
            val email = str.substring(emailStart, emailEnd).trim().takeIf { it.isNotBlank() }
            val url = str.substring(urlStart, urlEnd).trim().takeIf { it.isNotBlank() }
            return ExtensionAuthor(name, email, url?.let { FlorisRef.from(it) })
        }

        fun fromOrTakeRaw(str: String): ExtensionAuthor {
            return from(str) ?: ExtensionAuthor(str)
        }
    }

    override fun toString() = stringBuilder {
        append(name)
        if (email != null && email.isNotBlank()) {
            append(" <$email>")
        }
        if (url != null) {
            append(" ($url)")
        }
    }
}

private class ExtensionAuthorSerializer : KSerializer<ExtensionAuthor> {
    override val descriptor = PrimitiveSerialDescriptor("ExtensionAuthor", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ExtensionAuthor) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ExtensionAuthor {
        return ExtensionAuthor.fromOrTakeRaw(decoder.decodeString())
    }
}
