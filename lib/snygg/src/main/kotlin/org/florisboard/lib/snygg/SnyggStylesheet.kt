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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

data class SnyggStylesheet internal constructor(
    val schema: String,
    val rules: Map<SnyggRule, SnyggPropertySet>,
) {
    fun edit(comparator: Comparator<SnyggRule>? = null): SnyggStylesheetEditor {
        return SnyggStylesheetEditor(this.schema, this.rules, comparator)
    }

    fun toJson(
        config: SnyggJsonConfiguration = SnyggJsonConfiguration.DEFAULT,
    ): Result<String> = runCatching {
        val schemaElem = config.json.encodeToJsonElement(schema)
        val ruleElems = rules.map { (rule, propertySet) ->
            rule.toString() to propertySet.toJsonElement(rule, config)
        }.toMap()
        val jsonObject = JsonObject(
            mapOf(
                "\$schema" to schemaElem
            ) + ruleElems
        )
        config.json.encodeToString(jsonObject)
    }

    companion object {
        const val SCHEMA_V2 = "https://schemas.florisboard.org/snygg/v2/stylesheet"

        private val SCHEMA_PATTERN = """^https://schemas.florisboard.org/snygg/v[0-9]+/stylesheet$""".toRegex()

        fun v2(stylesheetBlock: SnyggStylesheetEditor.() -> Unit): SnyggStylesheet {
            val builder = SnyggStylesheetEditor(SCHEMA_V2)
            stylesheetBlock(builder)
            return builder.build()
        }

        fun fromJson(
            json: String,
            config: SnyggJsonConfiguration = SnyggJsonConfiguration.DEFAULT,
        ): Result<SnyggStylesheet> = runCatching {
            val jsonObject = config.json.decodeFromString<JsonObject>(json)
            var schema: String? = jsonObject["\$schema"]?.let { config.json.decodeFromJsonElement(it) }
            if (schema == null) {
                if (config.ignoreMissingSchema) {
                    // assume schema
                    schema = SCHEMA_V2
                } else {
                    throw SnyggMissingSchemaException()
                }
            } else {
                val schemaMatch = SCHEMA_PATTERN.matchEntire(schema)
                if (schemaMatch == null) {
                    if (config.ignoreInvalidSchema) {
                        // assume schema
                        schema = SCHEMA_V2
                    } else {
                        throw SnyggInvalidSchemaException(schema)
                    }
                }
                if (schema != SCHEMA_V2 && !config.ignoreUnsupportedSchema) {
                    throw SnyggUnsupportedSchemaException(schema)
                }
            }
            val ruleMap = mutableMapOf<SnyggRule, SnyggPropertySet>()
            for ((key, jsonElement) in jsonObject.entries) {
                if (key == "\$schema") {
                    continue
                } else {
                    val rule = SnyggRule.fromOrNull(key)
                    if (rule == null) {
                        if (config.ignoreInvalidRules) {
                            continue
                        }
                        throw SnyggInvalidRuleException(key)
                    }
                    val propertySet = SnyggPropertySet.fromJsonElement(rule, config, jsonElement)
                    ruleMap[rule] = propertySet
                }
            }
            SnyggStylesheet(schema, ruleMap)
        }
    }
}

data class SnyggJsonConfiguration private constructor(
    internal val ignoreMissingSchema: Boolean = false,
    internal val ignoreInvalidSchema: Boolean = false,
    internal val ignoreUnsupportedSchema: Boolean = false,
    internal val ignoreInvalidRules: Boolean = false,
    internal val ignoreInvalidProperties: Boolean = false,
    internal val ignoreInvalidValues: Boolean = false,
    internal val json: Json,
) {
    companion object {
        val DEFAULT = of()

        @OptIn(ExperimentalSerializationApi::class)
        fun of(
            ignoreMissingSchema: Boolean = false,
            ignoreInvalidSchema: Boolean = false,
            ignoreUnsupportedSchema: Boolean = false,
            ignoreInvalidRules: Boolean = false,
            ignoreInvalidProperties: Boolean = false,
            ignoreInvalidValues: Boolean = false,
            prettyPrint: Boolean = false,
            prettyPrintIndent: String = "  ",
        ): SnyggJsonConfiguration {
            return SnyggJsonConfiguration(
                ignoreMissingSchema,
                ignoreInvalidSchema,
                ignoreUnsupportedSchema,
                ignoreInvalidRules,
                ignoreInvalidProperties,
                ignoreInvalidValues,
                json = Json {
                    if (prettyPrint) {
                        this.prettyPrint = true
                        this.prettyPrintIndent = prettyPrintIndent
                    }
                },
            )
        }
    }
}

class SnyggMissingSchemaException : SerializationException(
    message = "Stylesheet is missing schema reference",
)

class SnyggInvalidSchemaException(schema: String) : SerializationException(
    message = "Stylesheet references invalid schema '$schema'",
)

class SnyggUnsupportedSchemaException(schema: String) : SerializationException(
    message = "Stylesheet references unsupported schema '$schema'",
)

class SnyggMissingRequiredPropertyException(rule: SnyggRule, property: String) : SerializationException(
    message = "Rule '$rule' is missing required property '$property'",
)

class SnyggInvalidRuleException(ruleStr: String) : SerializationException(
    message = "Invalid rule '$ruleStr'",
)

class SnyggInvalidPropertyException(rule: SnyggRule, property: String) : SerializationException(
    message = "Rule '$rule' contains unknown or invalid property '$property'",
)

class SnyggInvalidValueException(rule: SnyggRule, property: String, valueStr: String) : SerializationException(
    message = "Rule '$rule' property '$property' contains unknown or invalid value '$valueStr'",
)
