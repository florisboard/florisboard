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

package dev.patrickgold.florisboard.snygg.value

interface SnyggValue {
    fun encoder(): SnyggValueEncoder
}

object SnyggExplicitInheritValue : SnyggValue, SnyggValueEncoder {
    override val spec = SnyggValueSpec {
        keywords(keywords = listOf("inherit"))
    }

    override fun encode(v: SnyggValue) = runCatching<String> {
        return@runCatching "inherit"
    }

    override fun decode(v: String) = runCatching<SnyggValue> {
        return@runCatching SnyggExplicitInheritValue
    }

    override fun encoder() = this
}

object SnyggImplicitInheritValue : SnyggValue, SnyggValueEncoder {
    override val spec = SnyggValueSpec {
        keywords(keywords = listOf("implicit-inherit"))
    }

    override fun encode(v: SnyggValue) = runCatching<String> {
        error("Implicit inherit is not meant to be serialized")
    }

    override fun decode(v: String) = runCatching<SnyggValue> {
        error("Implicit inherit is not meant to be deserialized")
    }

    override fun encoder() = this
}
