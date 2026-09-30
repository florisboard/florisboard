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

import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggDynamicDarkColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicLightColorValue
import org.florisboard.lib.snygg.value.SnyggFontStyleValue
import org.florisboard.lib.snygg.value.SnyggFontWeightValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggInheritValue
import org.florisboard.lib.snygg.value.SnyggNoValue
import org.florisboard.lib.snygg.value.SnyggContentScaleValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggTextAlignValue
import org.florisboard.lib.snygg.value.SnyggTextDecorationLineValue
import org.florisboard.lib.snygg.value.SnyggTextMaxLinesValue
import org.florisboard.lib.snygg.value.SnyggTextOverflowValue
import org.florisboard.lib.snygg.value.SnyggUriValue
import org.florisboard.lib.snygg.value.SnyggVarValue
import org.florisboard.lib.snygg.value.SnyggYesValue

/**
 * Main object for defining all known Snygg property names.
 *
 * snygg = Swedish for stylish
 */
object Snygg {
    const val Background = "background"
    const val Foreground = "foreground"

    const val BackgroundImage = "background-image"
    const val ContentScale = "content-scale"

    const val BorderColor = "border-color"
    const val BorderStyle = "border-style" // unsupported as of now
    const val BorderWidth = "border-width"

    const val FontFamily = "font-family"
    const val FontSize = "font-size"
    const val FontStyle = "font-style"
    const val FontWeight = "font-weight"
    const val LetterSpacing = "letter-spacing"
    const val LineHeight = "line-height"

    const val Margin = "margin"
    const val Padding = "padding"

    const val ShadowColor = "shadow-color"
    const val ShadowElevation = "shadow-elevation"

    const val Shape = "shape"
    const val Clip = "clip"

    const val Src = "src"

    const val TextAlign = "text-align"
    const val TextDecorationLine = "text-decoration-line"
    const val TextMaxLines = "text-max-lines"
    const val TextOverflow = "text-overflow"
}

object SnyggSpec : SnyggSpecDecl({
    meta.title = "Snygg Stylesheet Specification"
    meta.description = "This document describes the Snygg stylesheet specification."

    annotation(SnyggAnnotationRule.Defines).singleSet {
        pattern(SnyggVarValue.VariableNameRegex) {
            any()
        }
    }

    annotation(SnyggAnnotationRule.Font).multipleSets {
        Snygg.Src {
            required()
            add(SnyggUriValue)
        }
        Snygg.FontStyle {
            add(SnyggFontStyleValue)
        }
        Snygg.FontWeight {
            add(SnyggFontWeightValue)
        }
    }

    elements {
        implicit {
            add(SnyggInheritValue)
            add(SnyggDefinedVarValue)
        }

        Snygg.Background {
            add(SnyggStaticColorValue)
            add(SnyggDynamicLightColorValue)
            add(SnyggDynamicDarkColorValue)
        }
        Snygg.Foreground {
            inheritsImplicitly()
            add(SnyggStaticColorValue)
            add(SnyggDynamicLightColorValue)
            add(SnyggDynamicDarkColorValue)
        }

        Snygg.BackgroundImage {
            add(SnyggUriValue)
        }
        Snygg.ContentScale {
            add(SnyggContentScaleValue)
        }

        Snygg.BorderColor {
            add(SnyggStaticColorValue)
            add(SnyggDynamicLightColorValue)
            add(SnyggDynamicDarkColorValue)
        }
        Snygg.BorderStyle {
            // unsupported
        }
        Snygg.BorderWidth {
            add(SnyggDpSizeValue)
        }

        Snygg.FontFamily {
            inheritsImplicitly()
            add(SnyggGenericFontFamilyValue)
            add(SnyggCustomFontFamilyValue)
        }
        Snygg.FontSize {
            inheritsImplicitly()
            add(SnyggSpSizeValue)
        }
        Snygg.FontStyle {
            inheritsImplicitly()
            add(SnyggFontStyleValue)
        }
        Snygg.FontWeight {
            inheritsImplicitly()
            add(SnyggFontWeightValue)
        }
        Snygg.LetterSpacing {
            inheritsImplicitly()
            add(SnyggSpSizeValue)
        }
        Snygg.LineHeight {
            inheritsImplicitly()
            add(SnyggSpSizeValue)
        }

        Snygg.Margin {
            add(SnyggPaddingValue)
        }
        Snygg.Padding {
            add(SnyggPaddingValue)
        }

        Snygg.ShadowColor {
            add(SnyggStaticColorValue)
            add(SnyggDynamicLightColorValue)
            add(SnyggDynamicDarkColorValue)
        }
        Snygg.ShadowElevation {
            add(SnyggDpSizeValue)
        }

        Snygg.Shape {
            add(SnyggRectangleShapeValue)
            add(SnyggCircleShapeValue)
            add(SnyggRoundedCornerDpShapeValue)
            add(SnyggRoundedCornerPercentShapeValue)
            add(SnyggCutCornerDpShapeValue)
            add(SnyggCutCornerPercentShapeValue)
        }
        Snygg.Clip {
            add(SnyggYesValue)
            add(SnyggNoValue)
        }

        Snygg.TextAlign {
            inheritsImplicitly()
            add(SnyggTextAlignValue)
        }
        Snygg.TextDecorationLine {
            inheritsImplicitly()
            add(SnyggTextDecorationLineValue)
        }
        Snygg.TextMaxLines {
            inheritsImplicitly()
            add(SnyggTextMaxLinesValue)
        }
        Snygg.TextOverflow {
            inheritsImplicitly()
            add(SnyggTextOverflowValue)
        }
    }
})
