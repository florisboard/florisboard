/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package org.florisboard.lib.snygg

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable(with = SnyggStylesheet.Serializer::class)
data class SnyggStylesheet internal constructor(
    val schema: String,
    val rules: Map<SnyggRule, SnyggPropertySet>,
) {
    fun edit() = SnyggStylesheetEditor(this.schema, this.rules)

    companion object {
        internal const val SCHEMA_V2 = "https://schemas.florisboard.org/snygg/v2/stylesheet"

        fun v2(stylesheetBlock: SnyggStylesheetEditor.() -> Unit): SnyggStylesheet {
            val builder = SnyggStylesheetEditor(SCHEMA_V2)
            stylesheetBlock(builder)
            return builder.build()
        }
    }

    object Serializer : KSerializer<SnyggStylesheet> {
        override val descriptor: SerialDescriptor
            get() = JsonObject.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SnyggStylesheet) {
            val schemaElem = Json.encodeToJsonElement(value.schema)
            val ruleElems = value.rules.map { (rule, propertySet) ->
                rule.toString() to Json.encodeToJsonElement(propertySet.toJsonObject())
            }.toMap()
            val jsonObject = JsonObject(
                mapOf(
                    "\$schema" to schemaElem
                ) + ruleElems
            )
            encoder.encodeSerializableValue(JsonObject.serializer(), jsonObject)
        }

        override fun deserialize(decoder: Decoder): SnyggStylesheet {
            val jsonObject = decoder.decodeSerializableValue(JsonObject.serializer())
            var schema: String? = null
            val ruleMap = mutableMapOf<SnyggRule, SnyggPropertySet>()
            for ((key, elem) in jsonObject.entries) {
                if (key == "\$schema") {
                    schema = Json.decodeFromJsonElement<String>(elem)
                } else {
                    val rule = SnyggRule.fromOrNull(key) ?: throw SerializationException("Invalid rule")
                    val propertySet = SnyggPropertySet.from(rule, Json.decodeFromJsonElement<JsonObject>(elem))
                    ruleMap[rule] = propertySet
                }
            }
            if (schema == null) {
                throw SerializationException("no schema :(")
            }
            return SnyggStylesheet(schema, ruleMap)
        }
    }
}
