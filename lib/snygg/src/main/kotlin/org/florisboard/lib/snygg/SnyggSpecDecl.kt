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

open class SnyggSpecDecl internal constructor(init: SnyggSpecDeclBuilder.() -> Unit) {
    private val properties: Map<String, Set<SnyggValueEncoder>>
    private val allEncoders: Set<SnyggValueEncoder>

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
        return properties[property]
    }
}

internal class SnyggSpecDeclBuilder {
    private val properties: MutableMap<String, MutableSet<SnyggValueEncoder>> = mutableMapOf()
    private val allPropertyEncoders = mutableSetOf<SnyggValueEncoder>()

    operator fun String.invoke(configure: MutableSet<SnyggValueEncoder>.() -> Unit) {
        val encoders = properties.getOrDefault(this, mutableSetOf())
        configure(encoders)
        properties[this] = encoders
    }

    fun allProperties(configure: MutableSet<SnyggValueEncoder>.() -> Unit) {
        configure(allPropertyEncoders)
    }

    fun build(): Pair<Map<String, Set<SnyggValueEncoder>>, Set<SnyggValueEncoder>> {
        val finalProperties = properties.mapValues { (_, encoders) ->
            buildSet {
                addAll(allPropertyEncoders)
                addAll(encoders)
            }
        }
        val allEncoders = properties.values.flatten().toSet()
        return finalProperties to allEncoders
    }
}
