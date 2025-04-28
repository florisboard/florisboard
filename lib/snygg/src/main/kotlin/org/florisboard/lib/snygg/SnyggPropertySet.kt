/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggFontStyleValue
import org.florisboard.lib.snygg.value.SnyggFontWeightValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggContentScaleValue
import org.florisboard.lib.snygg.value.SnyggShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggTextAlignValue
import org.florisboard.lib.snygg.value.SnyggTextDecorationLineValue
import org.florisboard.lib.snygg.value.SnyggTextMaxLinesValue
import org.florisboard.lib.snygg.value.SnyggTextOverflowValue
import org.florisboard.lib.snygg.value.SnyggUndefinedValue
import org.florisboard.lib.snygg.value.SnyggValue
import kotlin.collections.contains

sealed class SnyggPropertySet {
    abstract fun edit(): SnyggPropertySetEditor

    internal fun toJsonElement(rule: SnyggRule, config: SnyggJsonConfiguration): JsonElement {
        val spec = SnyggSpec.propertySetSpecOf(rule)
        checkNotNull(spec) {
            "Encoder passed rule '$rule' for which no property spec is associated. Please report this bug in the " +
                "florisboard issue tracker."
        }
        return when (spec.type) {
            SnyggSpecDecl.PropertySet.Type.SINGLE_SET -> {
                check(this is SnyggSinglePropertySet) {
                    "Encoder called toJsonElement() for rule '$rule' on a MULTIPLE_SETS instance, even though the " +
                        "spec requires a SINGLE_SET. Please report this bug in the florisboard issue tracker."
                }
                this.toJsonElementSpecialized(rule, config)
            }
            SnyggSpecDecl.PropertySet.Type.MULTIPLE_SETS -> {
                check(this is SnyggMultiplePropertySets) {
                    "Encoder called toJsonElement() for rule '$rule' on a SINGLE_SET instance, even though the " +
                        "spec requires a MULTIPLE_SETS. Please report this bug in the florisboard issue tracker."
                }
                this.toJsonElementSpecialized(rule, config)
            }
        }
    }

    companion object {
        internal fun fromJsonElement(
            rule: SnyggRule,
            config: SnyggJsonConfiguration,
            jsonElement: JsonElement,
        ): SnyggPropertySet {
            val spec = SnyggSpec.propertySetSpecOf(rule)
            checkNotNull(spec) {
                "Decoder passed rule '$rule' for which no property spec is associated. Please report this bug in the " +
                    "florisboard issue tracker."
            }
            return when (spec.type) {
                SnyggSpecDecl.PropertySet.Type.SINGLE_SET -> {
                    SnyggSinglePropertySet.fromJsonElementSpecialized(rule, config, jsonElement)
                }
                SnyggSpecDecl.PropertySet.Type.MULTIPLE_SETS -> {
                    SnyggMultiplePropertySets.fromJsonElementSpecialized(rule, config, jsonElement)
                }
            }
        }
    }
}

data class SnyggSinglePropertySet internal constructor(
    val properties: Map<String, SnyggValue> = emptyMap(),
) : SnyggPropertySet() {
    val background = properties[Snygg.Background] ?: SnyggUndefinedValue
    val foreground = properties[Snygg.Foreground] ?: SnyggUndefinedValue

    val backgroundImage = properties[Snygg.BackgroundImage] ?: SnyggUndefinedValue
    val contentScale = properties[Snygg.ContentScale] ?: SnyggUndefinedValue

    val borderColor = properties[Snygg.BorderColor] ?: SnyggUndefinedValue
    val borderStyle = properties[Snygg.BorderStyle] ?: SnyggUndefinedValue
    val borderWidth = properties[Snygg.BorderWidth] ?: SnyggUndefinedValue

    val fontFamily = properties[Snygg.FontFamily] ?: SnyggUndefinedValue
    val fontSize = properties[Snygg.FontSize] ?: SnyggUndefinedValue
    val fontStyle = properties[Snygg.FontStyle] ?: SnyggUndefinedValue
    val fontWeight = properties[Snygg.FontWeight] ?: SnyggUndefinedValue
    val letterSpacing = properties[Snygg.LetterSpacing] ?: SnyggUndefinedValue

    val lineHeight = properties[Snygg.LineHeight] ?: SnyggUndefinedValue

    val margin = properties[Snygg.Margin] ?: SnyggUndefinedValue
    val padding = properties[Snygg.Padding] ?: SnyggUndefinedValue

    val shadowColor = properties[Snygg.ShadowColor] ?: SnyggUndefinedValue
    val shadowElevation = properties[Snygg.ShadowElevation] ?: SnyggUndefinedValue

    val shape = properties[Snygg.Shape] ?: SnyggUndefinedValue
    val clip = properties[Snygg.Clip] ?: SnyggUndefinedValue

    val src = properties[Snygg.Src] ?: SnyggUndefinedValue

    val textAlign = properties[Snygg.TextAlign] ?: SnyggUndefinedValue
    val textDecorationLine = properties[Snygg.TextDecorationLine] ?: SnyggUndefinedValue
    val textMaxLines = properties[Snygg.TextMaxLines] ?: SnyggUndefinedValue
    val textOverflow = properties[Snygg.TextOverflow] ?: SnyggUndefinedValue

    override fun edit() = SnyggSinglePropertySetEditor(properties)

    override fun toString(): String {
        return properties.toString()
    }

    internal fun toJsonElementSpecialized(rule: SnyggRule, config: SnyggJsonConfiguration): JsonElement {
        val rawProperties = properties.mapValues { (_, value) ->
            val valueStr = value.encoder().serialize(value).getOrThrow()
            config.json.encodeToJsonElement(valueStr)
        }
        SnyggSpec.propertySetSpecOf(rule)!!.properties.forEach { (propKey, propSpec) ->
            if (propSpec.required && !rawProperties.contains(propKey)) {
                throw SerializationException("Rule '$rule' is missing required property '$propKey'")
            }
        }
        return config.json.encodeToJsonElement(JsonObject(rawProperties))
    }

    companion object {
        internal fun fromJsonElementSpecialized(
            rule: SnyggRule,
            config: SnyggJsonConfiguration,
            jsonElement: JsonElement,
        ): SnyggSinglePropertySet {
            val jsonObject = config.json.decodeFromJsonElement<JsonObject>(jsonElement)
            val editor = SnyggSinglePropertySetEditor()
            propLoop@ for ((property, valueElem) in jsonObject) {
                val encoders = SnyggSpec.encodersOf(rule, property)
                if (encoders == null) {
                    if (config.ignoreInvalidProperties) {
                        continue@propLoop
                    }
                    throw SnyggInvalidPropertyException(rule, property)
                }
                val valueStr = config.json.decodeFromJsonElement<String>(valueElem)
                for (encoder in encoders) {
                    val value = encoder.deserialize(valueStr).getOrNull()
                    if (value != null) {
                        editor.properties[property] = value
                        continue@propLoop
                    }
                }
                if (config.ignoreInvalidValues) {
                    continue@propLoop
                }
                throw SnyggInvalidValueException(rule, property, valueStr)
            }
            SnyggSpec.propertySetSpecOf(rule)!!.properties.forEach { (propKey, propSpec) ->
                if (propSpec.required && !editor.properties.contains(propKey)) {
                    throw SnyggMissingRequiredPropertyException(rule, propKey)
                }
            }
            return editor.build()
        }
    }

    fun background(default: Color = Color.Unspecified): Color {
        return when (background) {
            is SnyggStaticColorValue -> background.color
            else -> default
        }
    }

    fun foreground(default: Color = Color.Unspecified): Color {
        return when (foreground) {
            is SnyggStaticColorValue -> foreground.color
            else -> default
        }
    }

    fun contentScale(default: ContentScale = ContentScale.Crop): ContentScale {
        return when (contentScale) {
            is SnyggContentScaleValue -> contentScale.contentScale
            else -> default
        }
    }

    fun fontFamily(preloadedFontFamilies: CompiledFontFamilyData, default: FontFamily? = null): FontFamily? {
        return when (val family = fontFamily) {
            is SnyggGenericFontFamilyValue -> family.fontFamily
            is SnyggCustomFontFamilyValue -> preloadedFontFamilies[family.fontName]
            else -> default
        }
    }

    fun fontSize(default: TextUnit = TextUnit.Unspecified): TextUnit {
        return when (fontSize) {
            is SnyggSpSizeValue -> fontSize.sp
            else -> default
        }
    }

    fun fontStyle(default: FontStyle? = null): FontStyle? {
        return when (fontStyle) {
            is SnyggFontStyleValue -> fontStyle.fontStyle
            else -> default
        }
    }

    fun fontWeight(default: FontWeight? = null): FontWeight? {
        return when (fontWeight) {
            is SnyggFontWeightValue -> fontWeight.fontWeight
            else -> default
        }
    }

    fun letterSpacing(default: TextUnit = TextUnit.Unspecified): TextUnit {
        return when (letterSpacing) {
            is SnyggSpSizeValue -> letterSpacing.sp
            else -> default
        }
    }

    fun lineHeight(default: TextUnit = TextUnit.Unspecified): TextUnit {
        return when (lineHeight) {
            is SnyggSpSizeValue -> lineHeight.sp
            else -> default
        }
    }

    fun shape(): Shape {
        return when (shape) {
            is SnyggShapeValue -> shape.shape
            else -> RectangleShape
        }
    }

    fun textAlign(default: TextAlign? = null): TextAlign? {
        return when (textAlign) {
            is SnyggTextAlignValue -> textAlign.textAlign
            else -> default
        }
    }

    fun textDecorationLine(default: TextDecoration? = null): TextDecoration? {
        return when (textDecorationLine) {
            is SnyggTextDecorationLineValue -> textDecorationLine.textDecoration
            else -> default
        }
    }

    fun textMaxLines(default: Int = Int.MAX_VALUE): Int {
        return when (textMaxLines) {
            is SnyggTextMaxLinesValue -> textMaxLines.maxLines
            else -> default
        }
    }

    fun textOverflow(default: TextOverflow = TextOverflow.Clip): TextOverflow {
        return when (textOverflow) {
            is SnyggTextOverflowValue -> textOverflow.textOverflow
            else -> default
        }
    }
}

data class SnyggMultiplePropertySets internal constructor(
    val sets: List<SnyggSinglePropertySet>
) : SnyggPropertySet() {
    override fun edit() = SnyggMultiplePropertySetsEditor(sets)

    internal fun toJsonElementSpecialized(rule: SnyggRule, config: SnyggJsonConfiguration): JsonElement {
        val rawSets = sets.map { set ->
            set.toJsonElementSpecialized(rule, config)
        }
        return config.json.encodeToJsonElement(JsonArray(rawSets))
    }

    companion object {
        internal fun fromJsonElementSpecialized(
            rule: SnyggRule,
            config: SnyggJsonConfiguration,
            jsonElement: JsonElement,
        ): SnyggMultiplePropertySets {
            val jsonArray = config.json.decodeFromJsonElement<JsonArray>(jsonElement)
            val sets = jsonArray.map { jsonElement ->
                SnyggSinglePropertySet.fromJsonElementSpecialized(rule, config, jsonElement)
            }
            return SnyggMultiplePropertySets(sets)
        }
    }
}
