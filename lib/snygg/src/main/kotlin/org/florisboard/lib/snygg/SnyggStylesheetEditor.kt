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
) {
    private val rules = sortedMapOf<SnyggRule, SnyggPropertySetEditor>()

    init {
        if (initRules != null) {
            rules.putAll(
                initRules.mapValues { (_, propertySet) -> propertySet.edit() }
            )
        }
    }

    fun annotation(name: String, propertySetBlock: SnyggPropertySetEditor.() -> Unit) {
        val propertySetEditor = SnyggPropertySetEditor()
        propertySetBlock(propertySetEditor)
        val rule = SnyggRule(
            elementName = "@$name",
        )
        rules[rule] = propertySetEditor
    }

    fun defines(propertySetBlock: SnyggPropertySetEditor.() -> Unit) {
        annotation("defines", propertySetBlock)
    }

    operator fun String.invoke(
        vararg attributes: Pair<String, List<Int>>,
        pressed: Boolean = false,
        focus: Boolean = false,
        disabled: Boolean = false,
        propertySetBlock: SnyggPropertySetEditor.() -> Unit,
    ) {
        val propertySetEditor = SnyggPropertySetEditor()
        propertySetBlock(propertySetEditor)
        val rule = SnyggRule(
            elementName = this,
            attributes = SnyggRule.Attributes.of(*attributes),
            selectors = SnyggRule.Selectors(pressed, focus, disabled),
        )
        rules[rule] = propertySetEditor
    }

    fun build(): SnyggStylesheet {
        val rulesMap = rules.mapValues { (_, propertySetEditor) -> propertySetEditor.build() }
        return SnyggStylesheet(schema, rulesMap)
    }
}
