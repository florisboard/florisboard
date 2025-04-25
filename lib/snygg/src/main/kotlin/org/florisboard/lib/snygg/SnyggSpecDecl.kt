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

enum class InheritBehavior {
    IMPLICITLY_OR_EXPLICITLY,
    EXPLICITLY_ONLY,
}

open class SnyggSpecDecl internal constructor(configure: SnyggSpecDeclBuilder.() -> Unit) {
    internal val annotationSpecs: Map<RuleDecl, SnyggPropertySetSpecDecl>
    internal val elementsSpec: SnyggPropertySetSpecDecl
    internal val meta: JsonSchemaMeta
    private val _allEncoders = mutableSetOf<SnyggValueEncoder>()
    internal val allEncoders: Set<SnyggValueEncoder>
        get() = _allEncoders

    init {
        val builder = SnyggSpecDeclBuilder()
        builder.configure()
        val (annotationSpecsB, elementsSpecB, metaB) = builder.build()
        annotationSpecs = annotationSpecsB
        elementsSpec = elementsSpecB
        meta = metaB
    }

    fun propertySetSpecOf(rule: SnyggRule): SnyggPropertySetSpecDecl? {
        val propertySetSpec = when (rule) {
            is SnyggAnnotationRule -> annotationSpecs[rule.meta()]
            is SnyggElementRule -> elementsSpec
        } ?: return null
        return propertySetSpec
    }

    fun propertiesOf(rule: SnyggRule): Set<String> {
        val propertySetSpec = propertySetSpecOf(rule)
        return propertySetSpec?.properties?.keys.orEmpty()
    }

    fun encodersOf(rule: SnyggRule, property: String): Set<SnyggValueEncoder>? {
        val propertySetSpec = propertySetSpecOf(rule) ?: return null
        val encoders = propertySetSpec.properties[property]?.encoders
        if (encoders != null) {
            return encoders
        }
        for ((regex, patternPropertySetSpec) in propertySetSpec.patternProperties) {
            if (regex.matchEntire(property) != null) {
                return patternPropertySetSpec.encoders
            }
        }
        return null
    }

    interface RuleDecl {
        val name: String
        val pattern: Regex
    }

    internal inner class SnyggSpecDeclBuilder {
        private val annotationSpecs = mutableMapOf<RuleDecl, SnyggPropertySetSpecDeclBuilder>()
        private val elementsSpec = SnyggPropertySetSpecDeclBuilder()
        val meta = JsonSchemaMetaBuilder()

        fun annotation(meta: RuleDecl, configure: SnyggPropertySetSpecDeclBuilder.() -> Unit) {
            val annotationSpec = annotationSpecs.getOrDefault(meta, SnyggPropertySetSpecDeclBuilder())
            annotationSpec.configure()
            annotationSpecs[meta] = annotationSpec
        }

        fun elements(configure: SnyggPropertySetSpecDeclBuilder.() -> Unit) {
            elementsSpec.configure()
        }

        fun build(): Triple<Map<RuleDecl, SnyggPropertySetSpecDecl>, SnyggPropertySetSpecDecl, JsonSchemaMeta> {
            val annotationSpecs = annotationSpecs.mapValues { (_, builder) ->
                builder.build()
            }
            val elementsSpec = elementsSpec.build()
            return Triple(annotationSpecs, elementsSpec, meta.build())
        }
    }

    data class SnyggPropertySetSpecDecl(
        val patternProperties: Map<Regex, SnyggPropertySpecDecl>,
        val properties: Map<String, SnyggPropertySpecDecl>,
        val meta: JsonSchemaMeta,
    )

    internal inner class SnyggPropertySetSpecDeclBuilder {
        private val patternProperties = mutableMapOf<Regex, SnyggPropertySpecDeclBuilder>()
        private val properties = mutableMapOf<String, SnyggPropertySpecDeclBuilder>()
        val meta = JsonSchemaMetaBuilder()
        private val implicitEncoders = mutableSetOf<SnyggValueEncoder>()

        fun pattern(regex: Regex, configure: SnyggPropertySpecDeclBuilder.() -> Unit) {
            val builder = patternProperties.getOrDefault(regex, SnyggPropertySpecDeclBuilder())
            builder.configure()
            patternProperties[regex] = builder
        }

        operator fun String.invoke(configure: SnyggPropertySpecDeclBuilder.() -> Unit) {
            val builder = properties.getOrDefault(this, SnyggPropertySpecDeclBuilder())
            builder.configure()
            properties[this] = builder
        }

        fun implicit(configure: MutableSet<SnyggValueEncoder>.() -> Unit) {
            implicitEncoders.configure()
        }

        fun build(): SnyggPropertySetSpecDecl {
            _allEncoders.addAll(implicitEncoders)
            val patternPropertySpecs = patternProperties.mapValues { (_, builder) ->
                builder.build(implicitEncoders)
            }
            val propertySpecs = properties.mapValues { (_, builder) ->
                builder.build(implicitEncoders)
            }
            return SnyggPropertySetSpecDecl(
                patternProperties = patternPropertySpecs,
                properties = propertySpecs,
                meta = meta.build(),
            )
        }
    }

    data class SnyggPropertySpecDecl(
        val encoders: Set<SnyggValueEncoder>,
        val inheritBehavior: InheritBehavior,
        val meta: JsonSchemaMeta,
    ) {
        fun inheritsImplicitly(): Boolean {
            return inheritBehavior == InheritBehavior.IMPLICITLY_OR_EXPLICITLY
        }
    }

    internal inner class SnyggPropertySpecDeclBuilder {
        private val encoders: MutableSet<SnyggValueEncoder> = mutableSetOf()
        private var inheritBehavior: InheritBehavior = InheritBehavior.EXPLICITLY_ONLY
        val meta = JsonSchemaMetaBuilder()
        private var isAny: Boolean = false

        fun add(encoder: SnyggValueEncoder) {
            encoders.add(encoder)
            _allEncoders.add(encoder)
        }

        fun any() {
            isAny = true
        }

        fun inheritsImplicitly() {
            inheritBehavior = InheritBehavior.IMPLICITLY_OR_EXPLICITLY
        }

        fun build(implicitEncoders: Set<SnyggValueEncoder>): SnyggPropertySpecDecl {
            val encoders = if (isAny) {
                _allEncoders
            } else {
                buildSet {
                    addAll(implicitEncoders)
                    addAll(encoders)
                }
            }
            return SnyggPropertySpecDecl(encoders, inheritBehavior, meta.build())
        }
    }
}

data class JsonSchemaMeta(
    val title: String,
    val description: String,
)

class JsonSchemaMetaBuilder(
    var title: String = "",
    var description: String = "",
) {
    fun build(): JsonSchemaMeta {
        return JsonSchemaMeta(title, description)
    }
}
