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

import org.florisboard.lib.snygg.value.SnyggValue

private typealias CompiledStyleData =
    Map<String, List<Pair<SnyggRule, SnyggPropertySet>>>

// TODO: WIP, add proper compilation and get/fetch/composable state integration
@JvmInline
value class SnyggTheme internal constructor(
    private val style: CompiledStyleData,
) {
    internal fun get(
        elementName: String,
        attributes: Map<String, Int>,
        selectors: SnyggRule.Selectors,
    ): SnyggPropertySet {
        val styleSets = style[elementName] ?: return SnyggPropertySet()
        val properties = mutableMapOf<String, SnyggValue>()
        TODO()
    }

    companion object {
        internal fun from(stylesheet: SnyggStylesheet): SnyggTheme {
            val style = mutableMapOf<String, MutableList<Pair<SnyggRule, SnyggPropertySet>>>()
            val definedVariables = stylesheet.rules.firstNotNullOfOrNull { (rule, _) ->
                rule.isDefinedVariablesRule()
            } ?: SnyggPropertySet()
            stylesheet.rules.forEach { (rule, propertySet) ->
                val list = style.getOrDefault(rule.elementName, mutableListOf())
                list.add(rule to propertySet)
                style[rule.elementName] = list
            }
            // TODO: we need to compile the vars here!!!!!!!!!!!!!!!!!!
            return SnyggTheme(
                style = style.mapValues { (_, list) ->
                    list.sortedBy { it.first }
                }
            )
        }
    }
}
