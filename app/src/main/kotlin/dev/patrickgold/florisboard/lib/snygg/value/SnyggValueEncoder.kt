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
 * SnyggValueEncoder is responsible for the representation of a [SnyggValue] specification and for providing methods
 * for encoding and decoding a [SnyggValue] for the serialization process.
 *
 * A SnyggValueEncoder is typically implemented in the Companion of a SnyggValue sub-class, as this allows to use the
 * value class name in the Snygg stylesheet spec's `supportedValues` field. This is not an requirement though and any
 * sub-class / object is allowed to be used.
 */
interface SnyggValueEncoder {
    /**
     * Describes the format of the [SnyggValue] in the serialized state of the value in the Json file. This spec is
     * used in the serialization process of this encoder for both serializing and deserializing.
     */
    val spec: SnyggValueSpec

    /**
     * Describes alternative formats of the [SnyggValue] in the serialized state of the value in the Json file. These
     * specs are exclusively used in the deserialization process and ignored when serializing a value.
     */
    val alternativeSpecs: List<SnyggValueSpec>
        get() = emptyList()

    /**
     * Returns a default value for this encoder, used for dynamic theme building in a user interface.
     */
    fun defaultValue(): SnyggValue

    /**
     * Serialize given Snygg value [v] and return a String representation of it. This method is exception-free, which
     * means it must **never** throw an exception. If an error during serialization occurs, it should be returned as a
     * failed result.
     */
    fun serialize(v: SnyggValue): Result<String>

    /**
     * Deserialize given String [v] and return a Snygg value object of it. This method is exception-free, which
     * means it must **never** throw an exception. If an error during deserialization occurs, it should be returned as
     * a failed result.
     */
    fun deserialize(v: String): Result<SnyggValue>
}
