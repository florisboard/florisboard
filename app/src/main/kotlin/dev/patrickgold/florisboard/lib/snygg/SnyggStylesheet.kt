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

package dev.patrickgold.florisboard.lib.snygg

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.patrickgold.florisboard.ime.theme.FlorisImeUiSpec
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggVarValueEncoders
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

val SnyggStylesheetJsonConfig = Json

@Serializable(with = SnyggStylesheetSerializer::class)
class SnyggStylesheet(
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

    fun edit(): SnyggStylesheetEditor {
        val ruleMap = rules.mapValues { (_, propertySet) -> propertySet.edit() }
        return SnyggStylesheetEditor(ruleMap)
    }
}

fun SnyggStylesheet(stylesheetBlock: SnyggStylesheetEditor.() -> Unit): SnyggStylesheet {
    val builder = SnyggStylesheetEditor()
    stylesheetBlock(builder)
    return builder.build()
}

class SnyggStylesheetEditor(initRules: Map<SnyggRule, SnyggPropertySetEditor>? = null){
    val rules = sortedMapOf<SnyggRule, SnyggPropertySetEditor>()

    init {
        if (initRules != null) {
            rules.putAll(initRules)
        }
    }

    fun annotation(name: String, propertySetBlock: SnyggPropertySetEditor.() -> Unit) {
        val propertySetEditor = SnyggPropertySetEditor()
        propertySetBlock(propertySetEditor)
        val rule = SnyggRule(
            isAnnotation = true,
            element = name,
        )
        rules[rule] = propertySetEditor
    }

    fun defines(propertySetBlock: SnyggPropertySetEditor.() -> Unit) {
        annotation("defines", propertySetBlock)
    }

    operator fun String.invoke(
        codes: List<Int> = listOf(),
        groups: List<Int> = listOf(),
        modes: List<Int> = listOf(),
        pressedSelector: Boolean = false,
        focusSelector: Boolean = false,
        disabledSelector: Boolean = false,
        propertySetBlock: SnyggPropertySetEditor.() -> Unit,
    ) {
        val propertySetEditor = SnyggPropertySetEditor()
        propertySetBlock(propertySetEditor)
        val rule = SnyggRule(
            isAnnotation = false,
            element = this,
            codes.toMutableList(),
            groups.toMutableList(),
            modes.toMutableList(),
            pressedSelector,
            focusSelector,
            disabledSelector,
        )
        rules[rule] = propertySetEditor
    }

    fun build(isFullyQualified: Boolean = false): SnyggStylesheet {
        val rulesMap = rules.mapValues { (_, propertySetEditor) -> propertySetEditor.build() }
        return SnyggStylesheet(rulesMap, isFullyQualified)
    }
}

class SnyggStylesheetSerializer : KSerializer<SnyggStylesheet> {
    private val propertyMapSerializer = MapSerializer(String.serializer(), String.serializer())
    private val ruleMapSerializer = MapSerializer(SnyggRule.serializer(), propertyMapSerializer)

    override val descriptor = ruleMapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: SnyggStylesheet) {
        val rawRuleMap = value.rules.mapValues { (_, propertySet) ->
            propertySet.properties.mapValues { (_, snyggValue) ->
                snyggValue.encoder().serialize(snyggValue).getOrThrow()
            }
        }
        ruleMapSerializer.serialize(encoder, rawRuleMap)
    }

    override fun deserialize(decoder: Decoder): SnyggStylesheet {
        val rawRuleMap = ruleMapSerializer.deserialize(decoder)
        val ruleMap = mutableMapOf<SnyggRule, SnyggPropertySet>()
        for ((rule, rawProperties) in rawRuleMap) {
            // FIXME: hardcoding which spec to use, the selection should happen dynamically
            val stylesheetSpec = FlorisImeUiSpec
            if (rule.isDefinedVariablesRule()) {
                val parsedProperties = rawProperties.mapValues { (_, rawValue) ->
                    SnyggVarValueEncoders.firstNotNullOfOrNull { it.deserialize(rawValue).getOrNull() }
                        ?: SnyggImplicitInheritValue
                }
                ruleMap[rule] = SnyggPropertySet(parsedProperties)
                continue
            }
            val propertySetSpec = stylesheetSpec.propertySetSpec(rule.element) ?: continue
            val properties = rawProperties.mapValues { (name, value) ->
                val propertySpec = propertySetSpec.propertySpec(name)
                if (propertySpec != null) {
                    for (encoder in propertySpec.encoders) {
                        encoder.deserialize(value).onSuccess { snyggValue ->
                            return@mapValues snyggValue
                        }
                    }
                }
                return@mapValues SnyggImplicitInheritValue
            }
            ruleMap[rule] = SnyggPropertySet(properties)
        }
        return SnyggStylesheet(ruleMap)
    }
}
