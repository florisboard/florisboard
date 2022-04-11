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

/**
 * SnyggValue is the base interface for all possible property values a Snygg stylesheet can hold. In general, a Snygg
 * value can be one specific type of value (e.g. a color, a keyword describing a behavior, shape, etc.).
 *
 * At no point should a Snygg value be able to have to ways of representing the same value (except whitespace which can
 * be trimmed), to guarantee that a serialized value will always correspond to the same deserialized object. Should
 * there be the need to have two different representations, two different values must be defined and declared in the
 * `supportedValues` field of a property spec.
 */
interface SnyggValue {
    /**
     * Returns the associated encoder for this Snygg value, typically the Companion of the implementing value class.
     */
    fun encoder(): SnyggValueEncoder
}

/**
 * Checks if any given Snygg value indicates that it should be inherited from the parent rule, either marked
 * explicitly or implicitly.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun SnyggValue.isInherit(): Boolean {
    return this is SnyggImplicitInheritValue || this is SnyggExplicitInheritValue
}

/**
 * Convenience function which returns the opposite of SnyggValue.isInherit(). See [isInherit] for more info.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun SnyggValue.isNotInherit(): Boolean {
    return !isInherit()
}

/**
 * This value defines that a property value should be copied from the parent stylesheet.
 *
 * The inherit intent was defined explicitly by the creator of the stylesheet and thus should be kept internally in the
 * stylesheet rules. This value is kept in both ways during the serialization process and is shown in the UI for a
 * stylesheet editor.
 */
object SnyggExplicitInheritValue : SnyggValue, SnyggValueEncoder {
    const val Inherit = "inherit"

    override val spec = SnyggValueSpec {
        keywords(keywords = listOf(Inherit))
    }

    override fun defaultValue() = this

    override fun serialize(v: SnyggValue) = runCatching<String> {
        return@runCatching Inherit
    }

    override fun deserialize(v: String) = runCatching<SnyggValue> {
        check(v.trim() == Inherit)
        return@runCatching SnyggExplicitInheritValue
    }

    override fun encoder() = this
}

/**
 * This value defines that a property value should be copied from the parent stylesheet.
 *
 * The inherit intent was defined implicitly by the serialization process (when a required property was missing for a
 * rule). This value is thus **not** meant to be serialized, the serialization of this value is indeed prohibited and
 * an attempt to serialize this value will result in a serialization failure. Additionally this value should be shown
 * as "Missing" in a stylesheet editor UI.
 */
object SnyggImplicitInheritValue : SnyggValue, SnyggValueEncoder {
    private const val ImplicitInherit = "implicit-inherit"

    override val spec = SnyggValueSpec {
        keywords(keywords = listOf(ImplicitInherit))
    }

    override fun defaultValue() = this

    override fun serialize(v: SnyggValue) = runCatching<String> {
        error("Implicit inherit is not meant to be serialized")
    }

    override fun deserialize(v: String) = runCatching<SnyggValue> {
        error("Implicit inherit is not meant to be deserialized")
    }

    override fun encoder() = this
}

val SnyggVarValueEncoders = listOf(
    SnyggSolidColorValue,
    //SnyggImageRefValue,
    SnyggRectangleShapeValue,
    SnyggCircleShapeValue,
    SnyggRoundedCornerDpShapeValue,
    SnyggRoundedCornerPercentShapeValue,
    SnyggCutCornerDpShapeValue,
    SnyggCutCornerPercentShapeValue,
    SnyggDpSizeValue,
    SnyggSpSizeValue,
    SnyggPercentageSizeValue,
)
