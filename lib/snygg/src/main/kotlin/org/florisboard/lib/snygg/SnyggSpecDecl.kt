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
    internal val annotationSpecs: Map<RuleDecl, PropertySet>
    internal val elementsSpec: PropertySet
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

    fun propertySetSpecOf(rule: SnyggRule): PropertySet? {
        val propertySetSpec = when (rule) {
            is SnyggAnnotationRule -> annotationSpecs[rule.decl()]
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
        private val annotationSpecs = mutableMapOf<RuleDecl, PropertySetBuilder>()
        private val elementsSpec = PropertySetBuilder(type = PropertySet.Type.SINGLE_SET)
        val meta = JsonSchemaMetaBuilder()

        fun annotation(ruleDecl: RuleDecl) = SingleOrMultiple(ruleDecl)

        fun elements(configure: PropertySetBuilder.() -> Unit) {
            elementsSpec.configure()
        }

        fun build(): Triple<Map<RuleDecl, PropertySet>, PropertySet, JsonSchemaMeta> {
            val annotationSpecs = annotationSpecs.mapValues { (_, builder) ->
                builder.build()
            }
            val elementsSpec = elementsSpec.build()
            return Triple(annotationSpecs, elementsSpec, meta.build())
        }

        internal inner class SingleOrMultiple(private val ruleDecl: RuleDecl) {
            init {
                check(!annotationSpecs.containsKey(ruleDecl)) {
                    "Duplicate definition of $ruleDecl"
                }
            }

            fun singleSet(configure: PropertySetBuilder.() -> Unit) {
                val annotationSpec = PropertySetBuilder(type = PropertySet.Type.SINGLE_SET)
                annotationSpec.configure()
                annotationSpecs[ruleDecl] = annotationSpec
            }

            fun multipleSets(configure: PropertySetBuilder.() -> Unit) {
                val annotationSpec = PropertySetBuilder(type = PropertySet.Type.MULTIPLE_SETS)
                annotationSpec.configure()
                annotationSpecs[ruleDecl] = annotationSpec
            }
        }
    }

    data class PropertySet(
        val type: Type,
        val patternProperties: Map<Regex, Property>,
        val properties: Map<String, Property>,
        val meta: JsonSchemaMeta,
    ) {
        enum class Type {
            SINGLE_SET,
            MULTIPLE_SETS;
        }
    }

    internal inner class PropertySetBuilder(private val type: PropertySet.Type) {
        private val patternProperties = mutableMapOf<Regex, PropertyBuilder>()
        private val properties = mutableMapOf<String, PropertyBuilder>()
        val meta = JsonSchemaMetaBuilder()
        private val implicitEncoders = mutableSetOf<SnyggValueEncoder>()

        fun pattern(regex: Regex, configure: PropertyBuilder.() -> Unit) {
            val builder = patternProperties.getOrDefault(regex, PropertyBuilder())
            builder.configure()
            patternProperties[regex] = builder
        }

        operator fun String.invoke(configure: PropertyBuilder.() -> Unit) {
            val builder = properties.getOrDefault(this, PropertyBuilder())
            builder.configure()
            properties[this] = builder
        }

        fun implicit(configure: MutableSet<SnyggValueEncoder>.() -> Unit) {
            implicitEncoders.configure()
        }

        fun build(): PropertySet {
            _allEncoders.addAll(implicitEncoders)
            val patternPropertySpecs = patternProperties.mapValues { (_, builder) ->
                builder.build(implicitEncoders)
            }
            val propertySpecs = properties.mapValues { (_, builder) ->
                builder.build(implicitEncoders)
            }
            return PropertySet(
                type = type,
                patternProperties = patternPropertySpecs,
                properties = propertySpecs,
                meta = meta.build(),
            )
        }
    }

    data class Property(
        val encoders: Set<SnyggValueEncoder>,
        val inheritBehavior: InheritBehavior,
        val required: Boolean,
        val meta: JsonSchemaMeta,
    ) {
        fun inheritsImplicitly(): Boolean {
            return inheritBehavior == InheritBehavior.IMPLICITLY_OR_EXPLICITLY
        }
    }

    internal inner class PropertyBuilder {
        private val encoders: MutableSet<SnyggValueEncoder> = mutableSetOf()
        private var inheritBehavior: InheritBehavior = InheritBehavior.EXPLICITLY_ONLY
        private var required = false
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

        fun required() {
            required = true
        }

        fun build(implicitEncoders: Set<SnyggValueEncoder>): Property {
            val encoders = if (isAny) {
                _allEncoders
            } else {
                buildSet {
                    addAll(implicitEncoders)
                    addAll(encoders)
                }
            }
            return Property(encoders, inheritBehavior, required, meta.build())
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
