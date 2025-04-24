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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.florisboard.lib.color.getColor
import org.florisboard.lib.snygg.value.SnyggAssetResolver
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDynamicDarkColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicLightColorValue
import org.florisboard.lib.snygg.value.SnyggDefaultAssetResolver
import org.florisboard.lib.snygg.value.SnyggUndefinedValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggUriValue
import java.io.File

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
private typealias CompiledStyleData = Map<String, List<Pair<SnyggElementRule, SnyggPropertySet>>>

typealias SnyggQueryAttributes = Map<String, Any>

private typealias CompiledFontFamilyData = Map<String, FontFamily>

/**
 * Represents the runtime style data, which is used for styling UI elements.
 */
data class SnyggTheme internal constructor(
    private val style: CompiledStyleData,
    private val fontFamilies: CompiledFontFamilyData,
) {
    internal fun query(
        elementName: String,
        attributes: SnyggQueryAttributes,
        selector: SnyggSelector,
        parentStyle: SnyggPropertySet,
        dynamicLightColorScheme: ColorScheme,
        dynamicDarkColorScheme: ColorScheme,
    ): SnyggPropertySet {
        val editor = SnyggPropertySetEditor()
        val styleSets = style[elementName]
        if (styleSets == null) {
            editor.inheritImplicitly(parentStyle)
            return editor.build()
        }
        for ((rule, propertySet) in styleSets) {
            if (rule.isMatchForQuery(attributes, selector)) {
                editor.applyAll(propertySet, parentStyle)
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

    internal fun getFontFamily(fontName: String): FontFamily? {
        return fontFamilies[fontName]
    }

    companion object {
        internal fun compileFrom(
            stylesheet: SnyggStylesheet,
            assetResolver: SnyggAssetResolver = SnyggDefaultAssetResolver,
        ): SnyggTheme {
            val elements = mutableMapOf<String, MutableList<Pair<SnyggElementRule, SnyggPropertySet>>>()
            var variablesSet: SnyggPropertySet? = null
            val fonts = mutableMapOf<String, MutableList<Font>>()
            stylesheet.rules.forEach { (rule, propertySet) ->
                when (rule) {
                    is SnyggAnnotationRule.Defines -> {
                        variablesSet = propertySet
                    }
                    is SnyggAnnotationRule.Font -> {
                        val src = propertySet.src
                        if (src !is SnyggUriValue) return@forEach
                        val fontPath = assetResolver.resolveAbsolutPath(src.uri).getOrNull()
                        if (fontPath == null) return@forEach
                        fonts.getOrPut(rule.fontName) { mutableListOf() }.apply {
                            add(
                                Font(
                                    file = File(fontPath),
                                    weight = FontWeight.Normal,
                                    style = FontStyle.Normal,
                                )
                            )
                        }
                    }
                    is SnyggElementRule -> {
                        val list = elements.getOrDefault(rule.name, mutableListOf())
                        list.add(rule to propertySet)
                        elements[rule.name] = list
                    }
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
                                variables[value.key] ?: SnyggUndefinedValue
                        }
                    }
                    return@map rule to editor.build()
                }
            }
            val fontFamilies = fonts.mapValues { (_, fonts) ->
                FontFamily(fonts)
            }
            return SnyggTheme(style, fontFamilies)
        }
    }
}

private fun SnyggElementRule.isMatchForQuery(
    queryAttributes: SnyggQueryAttributes,
    querySelector: SnyggSelector,
): Boolean {
    return selector.isMatchForQuery(querySelector) &&
        attributes.isMatchForQuery(queryAttributes)
}

private fun SnyggAttributes.isMatchForQuery(query: SnyggQueryAttributes): Boolean {
    for ((attrKey, attrValues) in this) {
        val queryValue = query[attrKey]?.toString() ?: return false
        if (!attrValues.contains(queryValue)) {
            return false
        }
    }
    return true
}

private fun SnyggSelector.isMatchForQuery(query: SnyggSelector): Boolean {
    return this == SnyggSelector.NONE || this == query
}
