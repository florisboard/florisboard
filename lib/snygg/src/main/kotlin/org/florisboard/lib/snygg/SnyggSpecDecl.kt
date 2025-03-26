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

import org.florisboard.lib.snygg.value.SnyggValueEncoder

internal enum class InheritBehavior {
    IMPLICITLY_OR_EXPLICITLY,
    EXPLICITLY_ONLY,
}

open class SnyggSpecDecl internal constructor(init: SnyggSpecDeclBuilder.() -> Unit) {
    internal val properties: Map<String, SnyggSpecPropertyDecl>
    internal val allEncoders: Set<SnyggValueEncoder>

    init {
        val builder = SnyggSpecDeclBuilder()
        init(builder)
        val (builtProperties, builtAllEncoders) = builder.build()
        properties = builtProperties
        allEncoders = builtAllEncoders
    }

    fun encodersOf(property: String): Set<SnyggValueEncoder>? {
        if (property.startsWith("--")) {
            // variable
            return allEncoders
        }
        return properties[property]?.encoders
    }
}

internal class SnyggSpecDeclBuilder {
    private val properties: MutableMap<String, SnyggSpecPropertyDeclEditor> = mutableMapOf()
    private val allPropertyEncoders = mutableSetOf<SnyggValueEncoder>()

    operator fun String.invoke(configure: SnyggSpecPropertyDeclEditor.() -> Unit) {
        val editor = properties.getOrDefault(this, SnyggSpecPropertyDeclEditor())
        configure(editor)
        properties[this] = editor
    }

    fun allProperties(configure: MutableSet<SnyggValueEncoder>.() -> Unit) {
        configure(allPropertyEncoders)
    }

    fun build(): Pair<Map<String, SnyggSpecPropertyDecl>, Set<SnyggValueEncoder>> {
        val propertySpecs = properties.mapValues { (_, editor) ->
            editor.build(allPropertyEncoders)
        }
        val allEncoders = propertySpecs.values.map { it.encoders }.flatten().toSet()
        return propertySpecs to allEncoders
    }
}

internal data class SnyggSpecPropertyDecl(
    val encoders: Set<SnyggValueEncoder>,
    val inheritBehavior: InheritBehavior,
) {
    fun inheritsImplicitly(): Boolean {
        return inheritBehavior == InheritBehavior.IMPLICITLY_OR_EXPLICITLY
    }
}

internal data class SnyggSpecPropertyDeclEditor(
    private val encoders: MutableSet<SnyggValueEncoder> = mutableSetOf(),
    private var inheritBehavior: InheritBehavior = InheritBehavior.EXPLICITLY_ONLY,
) {
    fun add(encoder: SnyggValueEncoder) {
        encoders.add(encoder)
    }

    fun inheritsImplicitly() {
        inheritBehavior = InheritBehavior.IMPLICITLY_OR_EXPLICITLY
    }

    fun build(allPropertyEncoders: Set<SnyggValueEncoder>): SnyggSpecPropertyDecl {
        val encoders = buildSet {
            addAll(allPropertyEncoders)
            addAll(encoders)
        }
        return SnyggSpecPropertyDecl(encoders, inheritBehavior)
    }
}
