/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.UnicodeCtrlChar
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.snygg.Snygg
import dev.patrickgold.florisboard.lib.snygg.SnyggLevel
import dev.patrickgold.florisboard.lib.snygg.SnyggRule
import dev.patrickgold.florisboard.lib.snygg.value.RgbaColor
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCircleShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggExplicitInheritValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggPercentageSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggValueEncoder
import kotlin.math.roundToInt

@Composable
internal fun translateElementName(rule: SnyggRule, level: SnyggLevel): String {
    return translateElementName(rule.element, level) ?: remember {
        buildString {
            if (rule.isAnnotation) {
                append(SnyggRule.ANNOTATION_MARKER)
            }
            append(rule.element)
        }
    }
}

@Composable
internal fun translateElementName(element: String, level: SnyggLevel): String? {
    return when (level) {
        SnyggLevel.DEVELOPER -> null
        else -> when (element) {
            "defines" -> R.string.snygg__rule_element__defines
            FlorisImeUi.Keyboard -> R.string.snygg__rule_element__keyboard
            FlorisImeUi.Key -> R.string.snygg__rule_element__key
            FlorisImeUi.KeyHint -> R.string.snygg__rule_element__key_hint
            FlorisImeUi.KeyPopup -> R.string.snygg__rule_element__key_popup
            FlorisImeUi.ClipboardHeader -> R.string.snygg__rule_element__clipboard_header
            FlorisImeUi.ClipboardItem -> R.string.snygg__rule_element__clipboard_item
            FlorisImeUi.ClipboardItemPopup -> R.string.snygg__rule_element__clipboard_item_popup
            FlorisImeUi.GlideTrail -> R.string.snygg__rule_element__glide_trail
            FlorisImeUi.OneHandedPanel -> R.string.snygg__rule_element__one_handed_panel
            FlorisImeUi.SmartbarPrimaryRow -> R.string.snygg__rule_element__smartbar_primary_row
            FlorisImeUi.SmartbarSecondaryRow -> R.string.snygg__rule_element__smartbar_secondary_row
            FlorisImeUi.SmartbarPrimaryActionsToggle -> R.string.snygg__rule_element__smartbar_primary_actions_toggle
            FlorisImeUi.SmartbarSecondaryActionsToggle -> R.string.snygg__rule_element__smartbar_secondary_actions_toggle
            FlorisImeUi.SmartbarQuickAction -> R.string.snygg__rule_element__smartbar_quick_action
            FlorisImeUi.SmartbarKey -> R.string.snygg__rule_element__smartbar_key
            FlorisImeUi.SmartbarCandidateWord -> R.string.snygg__rule_element__smartbar_candidate_word
            FlorisImeUi.SmartbarCandidateClip -> R.string.snygg__rule_element__smartbar_candidate_clip
            FlorisImeUi.SmartbarCandidateSpacer -> R.string.snygg__rule_element__smartbar_candidate_spacer
            FlorisImeUi.SystemNavBar -> R.string.snygg__rule_element__system_nav_bar
            else -> null
        }
    }.let { if (it != null) { stringRes(it) } else { null } }
}

@Composable
internal fun translatePropertyName(propertyName: String, level: SnyggLevel): String {
    return when (level) {
        SnyggLevel.DEVELOPER -> null
        else -> when (propertyName) {
            Snygg.Width -> R.string.snygg__property_name__width
            Snygg.Height -> R.string.snygg__property_name__height
            Snygg.Background -> R.string.snygg__property_name__background
            Snygg.Foreground -> R.string.snygg__property_name__foreground
            Snygg.BorderColor -> R.string.snygg__property_name__border_color
            Snygg.BorderStyle -> R.string.snygg__property_name__border_style
            Snygg.BorderWidth -> R.string.snygg__property_name__border_width
            Snygg.FontFamily -> R.string.snygg__property_name__font_family
            Snygg.FontSize -> R.string.snygg__property_name__font_size
            Snygg.FontStyle -> R.string.snygg__property_name__font_style
            Snygg.FontVariant -> R.string.snygg__property_name__font_variant
            Snygg.FontWeight -> R.string.snygg__property_name__font_weight
            Snygg.ShadowElevation -> R.string.snygg__property_name__shadow_elevation
            Snygg.Shape -> R.string.snygg__property_name__shape
            "--primary" -> R.string.snygg__property_name__var_primary
            "--primary-variant" -> R.string.snygg__property_name__var_primary_variant
            "--secondary" -> R.string.snygg__property_name__var_secondary
            "--secondary-variant" -> R.string.snygg__property_name__var_secondary_variant
            "--background" -> R.string.snygg__property_name__var_background
            "--surface" -> R.string.snygg__property_name__var_surface
            "--surface-variant" -> R.string.snygg__property_name__var_surface_variant
            "--on-primary" -> R.string.snygg__property_name__var_on_primary
            "--on-secondary" -> R.string.snygg__property_name__var_on_secondary
            "--on-background" -> R.string.snygg__property_name__var_on_background
            "--on-surface" -> R.string.snygg__property_name__var_on_surface
            "--on-surface-variant" -> R.string.snygg__property_name__var_on_surface_variant
            "--shape" -> R.string.snygg__property_name__var_shape
            "--shape-variant" -> R.string.snygg__property_name__var_shape_variant
            else -> null
        }
    }.let { resId ->
        when {
            resId != null -> {
                stringRes(resId)
            }
            propertyName.isBlank() -> {
                stringRes(R.string.general__select_dropdown_value_placeholder)
            }
            else -> {
                propertyName
            }
        }
    }
}

@Composable
internal fun translatePropertyValue(
    propertyValue: SnyggValue,
    level: SnyggLevel,
    displayColorsAs: DisplayColorsAs,
): String {
    return when (propertyValue) {
        is SnyggSolidColorValue -> remember(propertyValue.color, displayColorsAs) {
            val color = propertyValue.color
            when (displayColorsAs) {
                DisplayColorsAs.HEX8 -> buildString {
                    append(UnicodeCtrlChar.LeftToRightIsolate)
                    append("#")
                    append((color.red * RgbaColor.RedMax).roundToInt().toString(16).padStart(2, '0'))
                    append((color.green * RgbaColor.GreenMax).roundToInt().toString(16).padStart(2, '0'))
                    append((color.blue * RgbaColor.BlueMax).roundToInt().toString(16).padStart(2, '0'))
                    append((color.alpha * 0xFF).roundToInt().toString(16).padStart(2, '0'))
                    append(UnicodeCtrlChar.PopDirectionalIsolate)
                }
                DisplayColorsAs.RGBA -> buildString {
                    append(UnicodeCtrlChar.LeftToRightIsolate)
                    append("rgba(")
                    append((color.red * RgbaColor.RedMax).roundToInt())
                    append(",")
                    append((color.green * RgbaColor.GreenMax).roundToInt())
                    append(",")
                    append((color.blue * RgbaColor.BlueMax).roundToInt())
                    append(",")
                    append(color.alpha)
                    append(")")
                    append(UnicodeCtrlChar.PopDirectionalIsolate)
                }
            }
        }
        else -> when (level) {
            SnyggLevel.DEVELOPER -> null
            else -> when (propertyValue) {
                is SnyggDefinedVarValue -> translatePropertyName(propertyValue.key, level)
                else -> null
            }
        } ?: buildString {
            append(UnicodeCtrlChar.LeftToRightIsolate)
            append(propertyValue.encoder().serialize(propertyValue).getOrElse { propertyValue.toString() })
            append(UnicodeCtrlChar.PopDirectionalIsolate)
        }
    }
}

@Composable
internal fun translatePropertyValueEncoderName(encoder: SnyggValueEncoder): String {
    return when (encoder) {
        SnyggImplicitInheritValue -> R.string.general__select_dropdown_value_placeholder
        SnyggExplicitInheritValue -> R.string.snygg__property_value__explicit_inherit
        SnyggSolidColorValue -> R.string.snygg__property_value__solid_color
        SnyggRectangleShapeValue -> R.string.snygg__property_value__rectangle_shape
        SnyggCircleShapeValue -> R.string.snygg__property_value__circle_shape
        SnyggCutCornerDpShapeValue -> R.string.snygg__property_value__cut_corner_shape_dp
        SnyggCutCornerPercentShapeValue -> R.string.snygg__property_value__cut_corner_shape_percent
        SnyggRoundedCornerDpShapeValue -> R.string.snygg__property_value__rounded_corner_shape_dp
        SnyggRoundedCornerPercentShapeValue -> R.string.snygg__property_value__rounded_corner_shape_percent
        SnyggDpSizeValue -> R.string.snygg__property_value__dp_size
        SnyggSpSizeValue -> R.string.snygg__property_value__sp_size
        SnyggPercentageSizeValue -> R.string.snygg__property_value__percentage_size
        SnyggDefinedVarValue -> R.string.snygg__property_value__defined_var
        else -> null
    }.let { if (it != null) { stringRes(it) } else { encoder::class.simpleName ?: "" } }.toString()
}
