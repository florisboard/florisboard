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

private const val RelPath = "relPath"
private const val ImageFunction = "image"
private val RefMatcher = """^[a-z0-9]([a-z0-9-]*[a-z0-9])?(/[a-z0-9]([a-z0-9-]*[a-z0-9])?)*${'$'}""".toRegex()

sealed interface SnyggRefValue : SnyggValue

data class SnyggImageRefValue(val relPath: String) : SnyggRefValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(name = ImageFunction) { string(id = RelPath, regex = RefMatcher) }
        }

        override fun defaultValue() = SnyggImageRefValue("")

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggImageRefValue)
            val map = SnyggIdToValueMap.new(RelPath to v.relPath)
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = SnyggIdToValueMap.new()
            spec.parse(v, map)
            val relPath = map.getOrThrow<String>(RelPath)
            return@runCatching SnyggImageRefValue(relPath)
        }
    }

    override fun encoder() = Companion
}
