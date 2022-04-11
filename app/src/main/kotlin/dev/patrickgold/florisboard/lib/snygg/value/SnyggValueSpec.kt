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

import dev.patrickgold.florisboard.lib.kotlin.toStringWithoutDotZero

interface SnyggValueSpec {
    val id: String?

    fun parse(str: String, dstMap: SnyggIdToValueMap): SnyggIdToValueMap

    fun pack(srcMap: SnyggIdToValueMap): String
}

fun SnyggValueSpec(block: SnyggValueSpecBuilder.() -> SnyggValueSpec): SnyggValueSpec {
    return block(SnyggValueSpecBuilder.Instance)
}

data class SnyggNumberValueSpec<T : Comparable<T>>(
    override val id: String?,
    val prefix: String?,
    val suffix: String?,
    val unit: String?,
    val min: T? = null,
    val max: T? = null,
    val namedNumbers: List<Pair<String, T>>,
    val strToNumber: (String) -> T,
) : SnyggValueSpec {

    override fun parse(str: String, dstMap: SnyggIdToValueMap): SnyggIdToValueMap {
        checkNotNull(id)
        var valStr = str.trim()
        val namedValue = namedNumbers.find { it.first == valStr }
        if (namedValue != null) {
            return SnyggIdToValueMap.new(id to namedValue.second)
        }
        if (prefix != null) {
            check(valStr.startsWith(prefix))
            valStr = valStr.removePrefix(prefix)
        }
        if (unit != null) {
            check(valStr.endsWith(unit))
            valStr = valStr.removeSuffix(unit)
        }
        if (suffix != null) {
            check(valStr.endsWith(suffix))
            valStr = valStr.removeSuffix(suffix)
        }
        val number = strToNumber(valStr.trim())
        check(number.coerceIn(min, max) == number)
        dstMap.add(id to number)
        return dstMap
    }

    override fun pack(srcMap: SnyggIdToValueMap): String {
        checkNotNull(id)
        val value = srcMap.getOrThrow<T>(id)
        val namedValue = namedNumbers.find { it.second == value }
        if (namedValue != null) {
            return namedValue.first
        }
        check(value.coerceIn(min, max) == value)
        return buildString {
            prefix?.let { append(it) }
            append((value as Number).toStringWithoutDotZero())
            suffix?.let { append(it) }
            unit?.let { append(it) }
        }
    }
}

data class SnyggKeywordValueSpec(
    override val id: String?,
    val keywords: List<String>,
) : SnyggValueSpec {

    override fun parse(str: String, dstMap: SnyggIdToValueMap): SnyggIdToValueMap {
        checkNotNull(id)
        val valStr = str.trim()
        check(valStr in keywords)
        dstMap.add(id to valStr)
        return dstMap
    }

    override fun pack(srcMap: SnyggIdToValueMap): String {
        checkNotNull(id)
        val value = srcMap.getOrThrow<String>(id)
        check(value in keywords)
        return value
    }
}

data class SnyggStringValueSpec(
    override val id: String?,
    val regex: Regex,
) : SnyggValueSpec {

    override fun parse(str: String, dstMap: SnyggIdToValueMap): SnyggIdToValueMap {
        checkNotNull(id)
        val valStr = str.trim()
        check(valStr matches regex) { "String \"$valStr\" does not match regex $regex" }
        dstMap.add(id to valStr)
        return dstMap
    }

    override fun pack(srcMap: SnyggIdToValueMap): String {
        checkNotNull(id)
        val value = srcMap.getOrThrow<String>(id)
        check(value matches regex) { "String \"$value\" does not match regex $regex" }
        return value
    }
}

data class SnyggFunctionValueSpec(
    override val id: String?,
    val name: String,
    val innerSpec: SnyggValueSpec,
) : SnyggValueSpec {

    override fun parse(str: String, dstMap: SnyggIdToValueMap): SnyggIdToValueMap {
        var valStr = str.trim()
        check(valStr.startsWith(name)) { "Incorrect function name" }
        valStr = valStr.removePrefix(name).trim()
        check(valStr.startsWith('(') && valStr.endsWith(')'))
        valStr = valStr.substring(1, valStr.length - 1)
        return innerSpec.parse(valStr, dstMap)
    }

    override fun pack(srcMap: SnyggIdToValueMap): String {
        val innerStr = innerSpec.pack(srcMap)
        return "$name($innerStr)"
    }
}

data class SnyggListValueSpec(
    override val id: String?,
    val separator: String,
    val valueSpecs: List<SnyggValueSpec>,
) : SnyggValueSpec {

    override fun parse(str: String, dstMap: SnyggIdToValueMap): SnyggIdToValueMap {
        val valStr = str.trim()
        val values = valStr.split(separator)
        check(values.size == valueSpecs.size)
        for ((n, value) in values.withIndex()) {
            val valueSpec = valueSpecs[n]
            valueSpec.parse(value.trim(), dstMap)
        }
        return dstMap
    }

    override fun pack(srcMap: SnyggIdToValueMap): String {
        return buildString {
            for ((n, valueSpec) in valueSpecs.withIndex()) {
                append(valueSpec.pack(srcMap))
                if (n < valueSpecs.size - 1) {
                    append(separator)
                }
            }
        }
    }
}

data class SnyggNothingValueSpec(
    override val id: String?,
) : SnyggValueSpec {

    override fun parse(str: String, dstMap: SnyggIdToValueMap): SnyggIdToValueMap {
        check(str.isBlank())
        return dstMap
    }

    override fun pack(srcMap: SnyggIdToValueMap): String {
        return ""
    }
}

class SnyggValueFormatListBuilder {
    private val valueFormats = mutableListOf<SnyggValueSpec>()

    operator fun SnyggValueSpec.unaryPlus() {
        valueFormats.add(this)
    }

    fun build() = valueFormats.toList()
}

class SnyggValueSpecBuilder {
    companion object {
        val Instance = SnyggValueSpecBuilder()
    }

    inline fun spacedList(
        valueFormatsBlock: SnyggValueFormatListBuilder.() -> Unit,
    ) = SnyggListValueSpec(
        id = null,
        separator = " ",
        valueSpecs = SnyggValueFormatListBuilder().let { valueFormatsBlock(it); it.build() },
    )

    inline fun commaList(
        valueFormatsBlock: SnyggValueFormatListBuilder.() -> Unit,
    ) = SnyggListValueSpec(
        id = null,
        separator = ",",
        valueSpecs = SnyggValueFormatListBuilder().let { valueFormatsBlock(it); it.build() },
    )

    inline fun function(
        name: String,
        innerSpecBuilder: SnyggValueSpecBuilder.() -> SnyggValueSpec,
    ) = SnyggFunctionValueSpec(id = null, name, innerSpecBuilder(Instance))

    fun keywords(
        id: String? = null,
        keywords: List<String>,
    ) = SnyggKeywordValueSpec(id, keywords)

    fun string(
        id: String? = null,
        regex: Regex,
    ) = SnyggStringValueSpec(id, regex)

    fun int(
        id: String? = null,
        prefix: String? = null,
        suffix: String? = null,
        unit: String? = null,
        min: Int? = null,
        max: Int? = null,
        namedNumbers: List<Pair<String, Int>> = listOf(),
    ) = SnyggNumberValueSpec(id, prefix, suffix, unit, min, max, namedNumbers, strToNumber = { it.toInt() })

    fun float(
        id: String? = null,
        prefix: String? = null,
        suffix: String? = null,
        unit: String? = null,
        min: Float? = null,
        max: Float? = null,
        namedNumbers: List<Pair<String, Float>> = listOf(),
    ) = SnyggNumberValueSpec(id, prefix, suffix, unit, min, max, namedNumbers, strToNumber = { it.toFloat() })

    fun percentageInt(
        id: String? = null,
        namedNumbers: List<Pair<String, Int>> = listOf(),
    ) = int(id, unit = "%", min = 0, max = 100, namedNumbers = namedNumbers)

    fun percentageFloat(
        id: String? = null,
        namedNumbers: List<Pair<String, Float>> = listOf(),
    ) = float(id, unit = "%", min = 0.0f, max = 100.0f, namedNumbers = namedNumbers)

    fun nothing() = SnyggNothingValueSpec(null)
}
