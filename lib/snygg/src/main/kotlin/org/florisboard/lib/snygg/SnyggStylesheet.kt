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
    fun edit() = SnyggStylesheetEditor(this.schema, this.rules)

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
            var schema: String? = null
            val ruleMap = mutableMapOf<SnyggRule, SnyggPropertySet>()
            for ((key, jsonElement) in jsonObject.entries) {
                if (key == "\$schema") {
                    schema = config.json.decodeFromJsonElement<String>(jsonElement)
                } else {
                    val rule = SnyggRule.fromOrNull(key)
                    if (rule == null) {
                        if (config.ignoreInvalidRules) {
                            continue
                        }
                        throw SerializationException("Invalid rule '$key'")
                    }
                    val propertySet = SnyggPropertySet.fromJsonElement(rule, config, jsonElement)
                    ruleMap[rule] = propertySet
                }
            }
            if (schema == null) {
                if (config.ignoreMissingSchema) {
                    // assume schema
                    schema = SCHEMA_V2
                } else {
                    throw SerializationException("no schema provided :(")
                }
            }
            SnyggStylesheet(schema, ruleMap)
        }
    }
}

data class SnyggJsonConfiguration private constructor(
    internal val ignoreMissingSchema: Boolean = false,
    internal val ignoreInvalidRules: Boolean = false,
    internal val ignoreUnknownProperties: Boolean = false,
    internal val ignoreUnknownValues: Boolean = false,
    internal val json: Json,
) {
    companion object {
        val DEFAULT = of()

        @OptIn(ExperimentalSerializationApi::class)
        fun of(
            ignoreMissingSchema: Boolean = false,
            ignoreInvalidRules: Boolean = false,
            ignoreUnknownProperties: Boolean = false,
            ignoreUnknownValues: Boolean = false,
            prettyPrint: Boolean = false,
            prettyPrintIndent: String = "  ",
        ): SnyggJsonConfiguration {
            return SnyggJsonConfiguration(
                ignoreMissingSchema,
                ignoreInvalidRules,
                ignoreUnknownProperties,
                ignoreUnknownValues,
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
