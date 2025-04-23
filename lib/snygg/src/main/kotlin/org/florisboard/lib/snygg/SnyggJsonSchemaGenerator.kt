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

package org.florisboard.lib.snygg

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.florisboard.lib.kotlin.io.writeJson
import org.florisboard.lib.kotlin.simpleNameOrEnclosing
import org.florisboard.lib.snygg.value.SnyggKeywordValueSpec
import org.florisboard.lib.snygg.value.SnyggValueEncoder
import org.florisboard.lib.snygg.value.SnyggValueSpec
import java.io.File

object SnyggJsonSchemaGenerator {
    private const val DEFINES_PROPERTY_SET_ID = "snygg-defines-property-set"
    private const val FONT_PROPERTY_SET_ID = "snygg-font-property-set"
    private const val PROPERTY_SET_ID = "snygg-property-set"

    @OptIn(ExperimentalSerializationApi::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val jsonSchemaFilePath = args.getOrElse(0) { error("No path provided to persist json schema") }
        val spec = SnyggSpec

        val jsonSchema = mapOf(
            "\$schema" to "https://json-schema.org/draft-07/schema",
            "\$id" to SnyggStylesheet.SCHEMA_V2,
            "type" to "object",
            "properties" to mapOf(
                "\$schema" to mapOf(
                    "type" to "string",
                    "format" to "uri",
                ),
            ),
            "patternProperties" to mapOf(
                SnyggAnnotationRule.Defines.REGEX.toString() to mapOf(
                    "\$ref" to "#/definitions/$DEFINES_PROPERTY_SET_ID",
                ),
                SnyggAnnotationRule.Font.REGEX.toString() to mapOf(
                    "\$ref" to "#/definitions/$FONT_PROPERTY_SET_ID",
                ),
                SnyggElementRule.REGEX.toString() to mapOf(
                    "\$ref" to "#/definitions/$PROPERTY_SET_ID",
                ),
            ),
            "additionalProperties" to false,
            "definitions" to buildMap {
                spec.allEncoders.forEach { encoder ->
                    put(encoder.id(), encoder.schema())
                }
                put(DEFINES_PROPERTY_SET_ID, convertToSchema(spec.annotationSpecs["defines"]!!))
                put(FONT_PROPERTY_SET_ID, convertToSchema(spec.annotationSpecs["font"]!!))
                put(PROPERTY_SET_ID, convertToSchema(spec.elementsSpec))
            },
        )

        val jsonSchemaObj = convertToJsonObject(jsonSchema)
        println("Writing $jsonSchemaFilePath")
        File(jsonSchemaFilePath).writeJson(jsonSchemaObj, Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        })
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertToJsonObject(map: Map<String, Any?>): JsonObject {
        return JsonObject(
            map.mapValues { (_, value) ->
                when (value) {
                    is Map<*, *> -> Json.encodeToJsonElement(convertToJsonObject(value as Map<String, Any?>))
                    is List<*> if (value.isEmpty() || value[0] is String) -> {
                        Json.encodeToJsonElement(value as List<String>)
                    }
                    is List<*> -> {
                        Json.encodeToJsonElement(convertToJsonObjectList(value))
                    }
                    is String -> Json.encodeToJsonElement(String.serializer(), value)
                    is Boolean -> Json.encodeToJsonElement(Boolean.serializer(), value)
                    else -> error("unknown")
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertToJsonObjectList(list: List<Any?>): List<JsonObject> {
        return list.map { convertToJsonObject(it as Map<String, Any?>) }
    }

    private fun convertToSchema(props: SnyggSpecDecl.SnyggPropertySetSpecDecl): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "patternProperties" to props.patternProperties.mapValues { (_, propertySpec) ->
                oneOfDefinitionRef(
                    *propertySpec.encoders.map { it.id() }.toTypedArray()
                )
            }.mapKeys { (key, _) -> key.toString() },
            "properties" to props.properties.mapValues { (_, propertySpec) ->
                oneOfDefinitionRef(
                    *propertySpec.encoders.map { it.id() }.toTypedArray()
                )
            },
            "additionalProperties" to false,
        )
    }

    private fun oneOf(vararg schemas: Any): Map<String, Any> {
        return mapOf(
            "oneOf" to listOf(*schemas)
        )
    }

    private fun oneOfDefinitionRef(vararg definitionIds: String): Map<String, Any> {
        return oneOf(*definitionIds.map { definitionId ->
            mapOf(
                "\$ref" to "#/definitions/$definitionId",
            )
        }.toTypedArray())
    }

    private fun SnyggValueEncoder.id(): String {
        val className = this::class.simpleNameOrEnclosing() ?: error("could not resolve class name of $this")
        return className.replace("""([a-z])([A-Z])""".toRegex(), "$1-$2").lowercase()
    }

    private fun SnyggValueEncoder.schema(): Map<String, Any> {
        if (alternativeSpecs.isEmpty()) {
            return spec.schema()
        }
        return oneOf(
            spec.schema(),
            *alternativeSpecs.map { it.schema() }.toTypedArray(),
        )
    }

    private fun SnyggValueSpec.schema(): Map<String, Any> {
        if (this is SnyggKeywordValueSpec) {
            return mapOf(
                "type" to "string",
                if (keywords.size == 1) {
                    "const" to keywords[0]
                } else {
                    "enum" to keywords
                },
            )
        }
        return mapOf(
            "type" to "string",
            "pattern" to "^$parsePattern$",
        )
    }
}
