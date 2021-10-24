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

package dev.patrickgold.florisboard.snygg

import dev.patrickgold.florisboard.snygg.value.SnyggImplicitInheritValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SnyggStylesheetRules = Map<SnyggRule, SnyggPropertySet>

@Serializable(with = SnyggStylesheetSerializer::class)
class SnyggStylesheet(val rules: SnyggStylesheetRules) {
    fun get(
        element: String,
        code: Int = -1,
        group: Int = -1,
        inputMode: Int = -1,
        isHover: Boolean = false,
        isFocus: Boolean = false,
        isPressed: Boolean = false,
    ): SnyggPropertySet {
        TODO()
    }

    fun merge(other: SnyggStylesheet): SnyggStylesheet {
        TODO()
    }
}

class SnyggStylesheetSerializer : KSerializer<SnyggStylesheet> {
    private val propertyMapSerializer = MapSerializer(String.serializer(), String.serializer())
    private val ruleMapSerializer = MapSerializer(SnyggRule.serializer(), propertyMapSerializer)

    override val descriptor = ruleMapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: SnyggStylesheet) {
        val rawRuleMap = value.rules.mapValues { (_, propertySet) ->
            propertySet.properties.mapValues { (_, snyggValue) ->
                snyggValue.encoder().serialize(snyggValue).getOrThrow()
            }
        }
        ruleMapSerializer.serialize(encoder, rawRuleMap)
    }

    override fun deserialize(decoder: Decoder): SnyggStylesheet {
        val rawRuleMap = ruleMapSerializer.deserialize(decoder)
        val ruleMap = mutableMapOf<SnyggRule, SnyggPropertySet>()
        for ((rule, rawProperties) in rawRuleMap) {
            val propertySetSpec = Snygg.Spec.propertySetSpec(rule.element) ?: continue
            val properties = rawProperties.mapValues { (name, value) ->
                val propertySpec = propertySetSpec.propertySpec(name)
                if (propertySpec != null) {
                    for (encoder in propertySpec.encoders) {
                        encoder.deserialize(value).onSuccess { snyggValue ->
                            return@mapValues snyggValue
                        }
                    }
                }
                return@mapValues SnyggImplicitInheritValue
            }
            ruleMap[rule] = SnyggPropertySet(properties)
        }
        return SnyggStylesheet(ruleMap)
    }
}
