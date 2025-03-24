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

import androidx.compose.material3.ColorScheme
import org.florisboard.lib.color.getColor
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDynamicDarkColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicLightColorValue
import org.florisboard.lib.snygg.value.SnyggImplicitInheritValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue

/**
 * Pre-compiled style data for a SnyggTheme.
 *
 * The map must fulfill the following contract:
 * 1. The map key represents the element name.
 * 2. All rules mapped to a key are guaranteed to have the same element
 *    name. This is not checked again during style querying.
 * 3. All rules per key are sorted in ascending order based on the comparison
 *    logic defined by SnyggRule.
 * 4. All SnyggPropertySet values are pre-computed and must not contain
 *    variable references.
 * 5. All annotation elements have been pre-processed and stripped from the data.
 */
private typealias CompiledStyleData =
    Map<String, List<Pair<SnyggRule, SnyggPropertySet>>>

typealias SnyggQueryAttributes = Map<String, Int>

typealias SnyggQuerySelectors = SnyggRule.Selectors

fun selectorsOf(
    pressed: Boolean = false,
    focus: Boolean = false,
    disabled: Boolean = false,
) = SnyggQuerySelectors(pressed, focus, disabled)

/**
 * Represents the runtime style data, which is used for styling UI elements.
 */
@JvmInline
value class SnyggTheme internal constructor(
    private val style: CompiledStyleData,
) {
    internal fun queryStatic(
        elementName: String,
        attributes: SnyggQueryAttributes,
        selectors: SnyggQuerySelectors,
        dynamicLightColorScheme: ColorScheme,
        dynamicDarkColorScheme: ColorScheme,
    ): SnyggPropertySet {
        val styleSets = style[elementName] ?: return SnyggPropertySet()
        val editor = SnyggPropertySetEditor()
        for ((rule, propertySet) in styleSets) {
            if (rule.isMatchForQuery(attributes, selectors)) {
                editor.applyAllNonImplicit(propertySet)
            }
        }
        for ((key, value) in editor.properties) {
            if (value is SnyggDynamicLightColorValue) {
                editor.properties[key] = SnyggStaticColorValue(
                    color = dynamicLightColorScheme.getColor(value.colorName)
                )
            } else if (value is SnyggDynamicDarkColorValue) {
                editor.properties[key] = SnyggStaticColorValue(
                    color = dynamicDarkColorScheme.getColor(value.colorName)
                )
            }
        }
        return editor.build()
    }

    companion object {
        internal fun compileFrom(stylesheet: SnyggStylesheet): SnyggTheme {
            val elements = mutableMapOf<String, MutableList<Pair<SnyggRule, SnyggPropertySet>>>()
            var variablesSet: SnyggPropertySet? = null
            stylesheet.rules.forEach { (rule, propertySet) ->
                if (rule.isDefinedVariablesRule()) {
                    variablesSet = propertySet
                } else {
                    val list = elements.getOrDefault(rule.elementName, mutableListOf())
                    list.add(rule to propertySet)
                    elements[rule.elementName] = list
                }
            }
            val variables = variablesSet?.properties ?: emptyMap()
            val style = elements.mapValues { (_, rulesets) ->
                rulesets.sortBy { it.first }
                return@mapValues rulesets.map { (rule, propertySet) ->
                    val editor = propertySet.edit()
                    for ((property, value) in editor.properties) {
                        if (value is SnyggDefinedVarValue) {
                            editor.properties[property] =
                                variables[value.key] ?: SnyggImplicitInheritValue
                        }
                    }
                    return@map rule to editor.build()
                }
            }
            return SnyggTheme(style)
        }
    }
}

private fun SnyggRule.isMatchForQuery(
    queryAttributes: SnyggQueryAttributes,
    querySelectors: SnyggQuerySelectors,
): Boolean {
    return selectors.isMatchForQuery(querySelectors) &&
        attributes.isMatchForQuery(queryAttributes)
}

private fun SnyggRule.Attributes.isMatchForQuery(query: SnyggQueryAttributes): Boolean {
    for ((attrKey, attrValues) in this) {
        val queryValue = query[attrKey] ?: return false
        if (!attrValues.contains(queryValue)) {
            return false
        }
    }
    return true
}

private fun SnyggRule.Selectors.isMatchForQuery(query: SnyggQuerySelectors): Boolean {
    if (pressed && !query.pressed) {
        return false
    }
    if (focus && !query.focus) {
        return false
    }
    if (disabled && !query.disabled) {
        return false
    }
    return true
}
