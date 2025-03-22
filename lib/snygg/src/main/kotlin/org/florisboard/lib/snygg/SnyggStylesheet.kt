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
    fun edit(): SnyggStylesheetEditor {
        val ruleMap = rules.mapValues { (_, propertySet) -> propertySet.edit() }
        return SnyggStylesheetEditor(ruleMap)
    }

    companion object {
        private const val SCHEMA_V2 = "https://schemas.florisboard.org/snygg/v2/stylesheet"

        fun v2(stylesheetBlock: SnyggStylesheetEditor.() -> Unit): SnyggStylesheet {
            val builder = SnyggStylesheetEditor()
            stylesheetBlock(builder)
            return builder.build(SCHEMA_V2)
        }
    }

    object Serializer : KSerializer<SnyggStylesheet> {
        override val descriptor: SerialDescriptor
            get() = JsonObject.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SnyggStylesheet) {
            val schemaElem = Json.encodeToJsonElement(value.schema)
            val ruleElems = value.rules.map { (rule, propertySet) ->
                rule.toString() to Json.encodeToJsonElement(propertySet)
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
                    val propertySet = Json.decodeFromJsonElement<SnyggPropertySet>(elem)
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

/*
@Serializable(with = SnyggStylesheetSerializer::class)
class SnyggStylesheetOLD(
    val rules: Map<SnyggRule, SnyggPropertySet>,
    val isFullyQualified: Boolean = false,
) {
    companion object {
        private const val Unspecified = Int.MIN_VALUE
        private val FallbackPropertySet = SnyggPropertySet(mapOf())

        fun getPropertySets(
            rules: Map<SnyggRule, SnyggPropertySet>,
            referenceRule: SnyggRule,
        ): List<SnyggPropertySet> {
            return rules.keys.filter { rule ->
                rule.element == referenceRule.element
                    && (rule.codes.isEmpty() || referenceRule.codes.isEmpty() || rule.codes.any { it in referenceRule.codes })
                    && (rule.groups.isEmpty() || referenceRule.groups.isEmpty() || rule.groups.any { it in referenceRule.groups })
                    && (rule.shiftStates.isEmpty() || referenceRule.shiftStates.isEmpty() || rule.shiftStates.any { it in referenceRule.shiftStates })
                    && ((referenceRule.pressedSelector == rule.pressedSelector) || !rule.pressedSelector)
                    && ((referenceRule.focusSelector == rule.focusSelector) || !rule.focusSelector)
                    && ((referenceRule.disabledSelector == rule.disabledSelector) || !rule.disabledSelector)
            }.sortedDescending().map { rules[it]!! }
        }

        //TODO: Do not hardcode and adapt to new syntax
        fun getPropertySet(
            rules: Map<SnyggRule, SnyggPropertySet>,
            element: String,
            code: Int = Unspecified,
            group: Int = Unspecified,
            mode: Int = Unspecified,
            isPressed: Boolean = false,
            isFocus: Boolean = false,
            isDisabled: Boolean = false,
        ): SnyggPropertySet {
            val possibleRules = rules.keys.filter { rule ->
                rule.element == element
                    && (code == Unspecified || rule.codes.isEmpty() || rule.codes.contains(code))
                    && (group == Unspecified || rule.groups.isEmpty() || rule.groups.contains(group))
                    && (mode == Unspecified || rule.shiftStates.isEmpty() || rule.shiftStates.contains(mode))
                    && (isPressed == rule.pressedSelector || !rule.pressedSelector)
                    && (isFocus == rule.focusSelector || !rule.focusSelector)
                    && (isDisabled == rule.disabledSelector || !rule.disabledSelector)
            }.sorted()
            return when {
                possibleRules.isEmpty() -> FallbackPropertySet
                possibleRules.size == 1 -> rules[possibleRules.first()]!!
                else -> {
                    val mergedPropertySets = mutableMapOf<String, SnyggValue>()
                    for (rule in possibleRules) {
                        val propertySet = rules[rule]!!
                        mergedPropertySets.putAll(propertySet.properties)
                    }
                    SnyggPropertySet(mergedPropertySets)
                }
            }
        }
    }

    @Composable
    fun get(
        element: String,
        code: Int = Unspecified,
        group: Int = Unspecified,
        mode: Int = Unspecified,
        isPressed: Boolean = false,
        isFocus: Boolean = false,
        isDisabled: Boolean = false,
    ) = remember(this, element, code, group, mode, isPressed, isFocus, isDisabled) {
        getPropertySet(rules, element, code, group, mode, isPressed, isFocus, isDisabled)
    }

    fun getStatic(
        element: String,
        code: Int = Unspecified,
        group: Int = Unspecified,
        mode: Int = Unspecified,
        isPressed: Boolean = false,
        isFocus: Boolean = false,
        isDisabled: Boolean = false,
    ) = getPropertySet(rules, element, code, group, mode, isPressed, isFocus, isDisabled)

    operator fun plus(other: SnyggStylesheet): SnyggStylesheet {
        val mergedRules = mutableMapOf<SnyggRule, SnyggPropertySet>()
        mergedRules.putAll(other.rules)
        for ((rule, propertySet) in rules) {
            if (mergedRules.containsKey(rule)) {
                val otherPropertySet = mergedRules[rule]!!
                val mergedProperties = buildMap {
                    putAll(propertySet.properties)
                    putAll(otherPropertySet.properties)
                }
                mergedRules[rule] = SnyggPropertySet(mergedProperties)
            } else {
                mergedRules[rule] = propertySet
            }
        }
        return SnyggStylesheet(mergedRules)
    }

    // TODO: divide in smaller, testable sections
    fun compileToFullyQualified(stylesheetSpec: SnyggSpec): SnyggStylesheet {
        val newRules = mutableMapOf<SnyggRule, SnyggPropertySet>()
        var definedVariables: SnyggPropertySet? = null
        for (rule in rules.keys.sorted()) {
            if (rule.isAnnotation) {
                if (rule.element == "defines") {
                    definedVariables = rules[rule]
                }
                continue
            }
            val editor = rules[rule]!!.edit()
            for ((propertyName, propertyValue) in editor.properties) {
                if (propertyValue is SnyggDefinedVarValue) {
                    editor.properties[propertyName] =
                        definedVariables?.properties?.get(propertyValue.key) ?: SnyggImplicitInheritValue
                }
            }
            newRules[rule] = editor.build()
        }
        val newRulesWithPressed = mutableMapOf<SnyggRule, SnyggPropertySet>()
        for ((rule, propSet) in newRules) {
            newRulesWithPressed[rule] = propSet
            if (!rule.pressedSelector) {
                val propertySetSpec = stylesheetSpec.propertySetSpec(rule.element) ?: continue
                val pressedRule = rule.copy(pressedSelector = true)
                if (!newRules.containsKey(pressedRule)) {
                    val editor = SnyggPropertySetEditor()
                    val possiblePropertySets = getPropertySets(rules, pressedRule) + getPropertySets(newRules, pressedRule)
                    propertySetSpec.supportedProperties.forEach { supportedProperty ->
                        if (!editor.properties.containsKey(supportedProperty.name)) {
                            val value = possiblePropertySets.firstNotNullOfOrNull {
                                it.properties[supportedProperty.name]
                            } ?: SnyggImplicitInheritValue
                            editor.properties[supportedProperty.name] = if (value is SnyggDefinedVarValue) {
                                definedVariables?.properties?.get(value.key) ?: SnyggImplicitInheritValue
                            } else {
                                value
                            }
                        }
                    }
                    newRulesWithPressed[pressedRule] = editor.build()
                }
            }
        }
        return SnyggStylesheet(newRulesWithPressed, isFullyQualified = true)
    }
}
*/
