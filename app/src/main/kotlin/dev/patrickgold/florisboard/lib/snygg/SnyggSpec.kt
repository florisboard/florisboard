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

import dev.patrickgold.florisboard.lib.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggExplicitInheritValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggValueEncoder

open class SnyggSpec(init: SnyggSpecBuilder.() -> Unit) {
    val elements: Map<String, SnyggPropertySetSpec>

    init {
        val builder = SnyggSpecBuilder()
        init(builder)
        elements = builder.build()
    }

    fun propertySetSpec(element: String): SnyggPropertySetSpec? {
        return elements[element]
    }
}

class SnyggSpecBuilder {
    private val elements: MutableMap<String, SnyggPropertySetSpec> = mutableMapOf()

    fun element(name: String, configure: SnyggPropertySetSpecBuilder.() -> Unit) {
        val builder = SnyggPropertySetSpecBuilder()
        configure(builder)
        elements[name] = builder.build()
    }

    fun build(): Map<String, SnyggPropertySetSpec> {
        return elements.toMap()
    }
}

data class SnyggPropertySetSpec(val supportedProperties: List<SnyggPropertySpec>) {
    fun propertySpec(propertyName: String): SnyggPropertySpec? {
        return supportedProperties.find { it.name == propertyName }
    }
}

class SnyggPropertySetSpecBuilder {
    private var supportedProperties = mutableListOf<SnyggPropertySpec>()

    fun supportedValues(vararg encoders: SnyggValueEncoder) = listOf(
        *encoders, SnyggDefinedVarValue, SnyggExplicitInheritValue,
    )

    fun property(name: String, level: SnyggLevel, supportedValues: List<SnyggValueEncoder>) {
        supportedProperties.add(SnyggPropertySpec(name, level, supportedValues))
    }

    fun build(): SnyggPropertySetSpec {
        return SnyggPropertySetSpec(supportedProperties)
    }
}

data class SnyggPropertySpec(
    val name: String,
    val level: SnyggLevel,
    val encoders: List<SnyggValueEncoder>,
)
