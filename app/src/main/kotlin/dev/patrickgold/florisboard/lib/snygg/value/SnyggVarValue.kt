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

package dev.patrickgold.florisboard.lib.snygg.value

import dev.patrickgold.florisboard.lib.ext.ExtensionValidation

private const val VarKey = "varKey"

sealed interface SnyggVarValue : SnyggValue

data class SnyggDefinedVarValue(val key: String) : SnyggVarValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(name = "var") { string(id = VarKey, regex = ExtensionValidation.ThemeComponentVariableNameRegex) }
        }

        override fun defaultValue() = SnyggDefinedVarValue("")

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggDefinedVarValue)
            val map = SnyggIdToValueMap.new(VarKey to v.key)
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val key = map.getOrThrow<String>(VarKey)
            return@runCatching SnyggDefinedVarValue(key)
        }
    }

    override fun encoder() = Companion
}
