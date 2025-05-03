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

class SnyggStylesheetEditor(
    private val schema: String,
    initRules: Map<SnyggRule, SnyggPropertySet>? = null,
    comparator: Comparator<SnyggRule>? = null,
) {
    val rules = sortedMapOf<SnyggRule, SnyggPropertySetEditor>(comparator ?: DefaultRuleComparator)

    init {
        if (initRules != null) {
            rules.putAll(
                initRules.mapValues { (_, propertySet) -> propertySet.edit() }
            )
        }
    }

    fun defines(propertySetBlock: SnyggSinglePropertySetEditor.() -> Unit) {
        val propertySetEditor = SnyggSinglePropertySetEditor()
        propertySetBlock(propertySetEditor)
        val rule = SnyggAnnotationRule.Defines
        rules[rule] = propertySetEditor
    }

    fun font(fontName: String, propertySetBlock: SnyggMultiplePropertySetsEditor.() -> Unit) {
        val propertySetEditor = SnyggMultiplePropertySetsEditor()
        propertySetBlock(propertySetEditor)
        val rule = SnyggAnnotationRule.Font(fontName)
        rules[rule] = propertySetEditor
    }

    operator fun String.invoke(
        vararg attributes: Pair<String, List<Any>>,
        selector: SnyggSelector = SnyggSelector.NONE,
        propertySetBlock: SnyggSinglePropertySetEditor.() -> Unit,
    ) {
        val propertySetEditor = SnyggSinglePropertySetEditor()
        propertySetBlock(propertySetEditor)
        val rule = SnyggElementRule(
            elementName = this,
            attributes = SnyggAttributes.of(*attributes),
            selector = selector,
        )
        rules[rule] = propertySetEditor
    }

    fun build(): SnyggStylesheet {
        val rulesMap = rules.mapValues { (_, propertySetEditor) -> propertySetEditor.build() }
        return SnyggStylesheet(schema, rulesMap)
    }

    private object DefaultRuleComparator : Comparator<SnyggRule> {
        override fun compare(a: SnyggRule, b: SnyggRule): Int {
            return a.compareTo(b)
        }
    }
}
