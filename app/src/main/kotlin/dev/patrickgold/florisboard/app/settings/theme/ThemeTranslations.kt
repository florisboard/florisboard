/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.UnicodeCtrlChar
import dev.patrickgold.florisboard.lib.compose.stringRes
import org.florisboard.lib.kotlin.simpleNameOrEnclosing
import org.florisboard.lib.snygg.Snygg
import org.florisboard.lib.snygg.SnyggElementRule
import org.florisboard.lib.snygg.value.RgbaColor
import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggDynamicDarkColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicLightColorValue
import org.florisboard.lib.snygg.value.SnyggInheritValue
import org.florisboard.lib.snygg.value.SnyggPercentageSizeValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggUndefinedValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.florisboard.lib.snygg.value.SnyggValueEncoder
import kotlin.math.roundToInt

@Composable
internal fun translateElementName(rule: SnyggElementRule, level: SnyggLevel): String {
    return translateElementName(rule.name, level) ?: remember {
        buildString {
            append(rule.name)
        }
    }
}

@Composable
internal fun translateElementName(element: String, level: SnyggLevel): String? {
    return when (level) {
        SnyggLevel.DEVELOPER -> null
        else -> when (element) {
            "defines" -> R.string.snygg__rule_element__defines
            "font" -> R.string.snygg__rule_element__font
            FlorisImeUi.Keyboard.elementName -> R.string.snygg__rule_element__keyboard
            FlorisImeUi.Key.elementName -> R.string.snygg__rule_element__key
            FlorisImeUi.KeyHint.elementName -> R.string.snygg__rule_element__key_hint
            FlorisImeUi.KeyPopup.elementName -> R.string.snygg__rule_element__key_popup
            FlorisImeUi.ClipboardHeader.elementName -> R.string.snygg__rule_element__clipboard_header
            FlorisImeUi.ClipboardItem.elementName -> R.string.snygg__rule_element__clipboard_item
            FlorisImeUi.ClipboardItemPopup.elementName -> R.string.snygg__rule_element__clipboard_item_popup
            FlorisImeUi.EmojiKey.elementName -> R.string.snygg__rule_element__emoji_key
            FlorisImeUi.EmojiKeyPopup.elementName -> R.string.snygg__rule_element__emoji_key_popup
            FlorisImeUi.EmojiTab.elementName -> R.string.snygg__rule_element__emoji_key_tab
            FlorisImeUi.ExtractedLandscapeInputLayout.elementName -> R.string.snygg__rule_element__extracted_landscape_input_layout
            FlorisImeUi.ExtractedLandscapeInputField.elementName -> R.string.snygg__rule_element__extracted_landscape_input_field
            FlorisImeUi.ExtractedLandscapeInputAction.elementName -> R.string.snygg__rule_element__extracted_landscape_input_action
            FlorisImeUi.GlideTrail.elementName -> R.string.snygg__rule_element__glide_trail
            FlorisImeUi.IncognitoModeIndicator.elementName -> R.string.snygg__rule_element__incognito_mode_indicator
            FlorisImeUi.OneHandedPanel.elementName -> R.string.snygg__rule_element__one_handed_panel
            FlorisImeUi.Smartbar.elementName -> R.string.snygg__rule_element__smartbar
            FlorisImeUi.SmartbarSharedActionsRow.elementName -> R.string.snygg__rule_element__smartbar_shared_actions_row
            FlorisImeUi.SmartbarSharedActionsToggle.elementName -> R.string.snygg__rule_element__smartbar_shared_actions_toggle
            FlorisImeUi.SmartbarExtendedActionsRow.elementName -> R.string.snygg__rule_element__smartbar_extended_actions_row
            FlorisImeUi.SmartbarExtendedActionsToggle.elementName -> R.string.snygg__rule_element__smartbar_extended_actions_toggle
            FlorisImeUi.SmartbarActionKey.elementName -> R.string.snygg__rule_element__smartbar_action_key
            FlorisImeUi.SmartbarActionTile.elementName -> R.string.snygg__rule_element__smartbar_action_tile
            FlorisImeUi.SmartbarActionsOverflow.elementName -> R.string.snygg__rule_element__smartbar_actions_overflow
            FlorisImeUi.SmartbarActionsOverflowCustomizeButton.elementName -> R.string.snygg__rule_element__smartbar_actions_overflow_customize_button
            FlorisImeUi.SmartbarActionsEditor.elementName -> R.string.snygg__rule_element__smartbar_actions_editor
            FlorisImeUi.SmartbarActionsEditorHeader.elementName -> R.string.snygg__rule_element__smartbar_actions_editor_header
            FlorisImeUi.SmartbarActionsEditorSubheader.elementName -> R.string.snygg__rule_element__smartbar_actions_editor_subheader
            FlorisImeUi.SmartbarCandidatesRow.elementName -> R.string.snygg__rule_element__smartbar_candidates_row
            FlorisImeUi.SmartbarCandidateWord.elementName -> R.string.snygg__rule_element__smartbar_candidate_word
            FlorisImeUi.SmartbarCandidateClip.elementName -> R.string.snygg__rule_element__smartbar_candidate_clip
            FlorisImeUi.SmartbarCandidateSpacer.elementName -> R.string.snygg__rule_element__smartbar_candidate_spacer
            else -> null
        }
    }.let { if (it != null) { stringRes(it) } else { null } }
}

internal fun translatePropertyName(context: Context, propertyName: String, level: SnyggLevel): String {
    return when (level) {
        SnyggLevel.DEVELOPER -> null
        // TODO: add new theme translations
        else -> when (propertyName) {
            Snygg.Background -> R.string.snygg__property_name__background
            Snygg.Foreground -> R.string.snygg__property_name__foreground
            Snygg.BorderColor -> R.string.snygg__property_name__border_color
            Snygg.BorderStyle -> R.string.snygg__property_name__border_style
            Snygg.BorderWidth -> R.string.snygg__property_name__border_width
            Snygg.FontFamily -> R.string.snygg__property_name__font_family
            Snygg.FontSize -> R.string.snygg__property_name__font_size
            Snygg.FontStyle -> R.string.snygg__property_name__font_style
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
                context.getString(resId)
            }
            propertyName.isBlank() -> {
                context.getString(R.string.general__select_dropdown_value_placeholder)
            }
            else -> {
                propertyName
            }
        }
    }
}

internal fun translatePropertyValue(
    context: Context,
    propertyValue: SnyggValue,
    level: SnyggLevel,
    displayColorsAs: DisplayColorsAs,
): String {
    return when (propertyValue) {
        is SnyggStaticColorValue -> {
            buildColorString(propertyValue.color, displayColorsAs)
        }
        else -> when (level) {
            SnyggLevel.DEVELOPER -> null
            else -> when (propertyValue) {
                is SnyggDefinedVarValue -> translatePropertyName(context, propertyValue.key, level)
                else -> null
            }
        } ?: buildString {
            append(UnicodeCtrlChar.LeftToRightIsolate)
            append(propertyValue.encoder().serialize(propertyValue).getOrElse { propertyValue.toString() })
            append(UnicodeCtrlChar.PopDirectionalIsolate)
        }
    }
}

internal fun buildColorString(color: Color, displayColorsAs: DisplayColorsAs): String {
    return when (displayColorsAs) {
        DisplayColorsAs.HEX8 -> buildString {
            append(UnicodeCtrlChar.LeftToRightIsolate)
            append("#")
            append((color.red * RgbaColor.ColorRangeMax).roundToInt().toString(16).padStart(2, '0'))
            append((color.green * RgbaColor.ColorRangeMax).roundToInt().toString(16).padStart(2, '0'))
            append((color.blue * RgbaColor.ColorRangeMax).roundToInt().toString(16).padStart(2, '0'))
            append((color.alpha * 0xFF).roundToInt().toString(16).padStart(2, '0'))
            append(UnicodeCtrlChar.PopDirectionalIsolate)
        }
        DisplayColorsAs.RGBA -> buildString {
            append(UnicodeCtrlChar.LeftToRightIsolate)
            append("rgba(")
            append((color.red * RgbaColor.ColorRangeMax).roundToInt())
            append(",")
            append((color.green * RgbaColor.ColorRangeMax).roundToInt())
            append(",")
            append((color.blue * RgbaColor.ColorRangeMax).roundToInt())
            append(",")
            append(color.alpha)
            append(")")
            append(UnicodeCtrlChar.PopDirectionalIsolate)
        }
    }
}


internal fun translatePropertyValueEncoderName(context: Context, encoder: SnyggValueEncoder): String {
    return when (encoder) {
        SnyggUndefinedValue -> R.string.general__select_dropdown_value_placeholder
        SnyggInheritValue -> R.string.snygg__property_value__explicit_inherit
        SnyggStaticColorValue -> R.string.snygg__property_value__solid_color
        SnyggDynamicLightColorValue -> R.string.snygg__property_value__material_you_light_color
        SnyggDynamicDarkColorValue -> R.string.snygg__property_value__material_you_dark_color
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
    }.let { if (it != null) { context.getString(it) } else { encoder::class.simpleNameOrEnclosing() ?: "" } }.toString()
}
