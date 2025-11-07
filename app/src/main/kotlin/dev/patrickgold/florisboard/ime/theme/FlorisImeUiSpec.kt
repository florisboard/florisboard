/*
 * Copyright (C) 2021-2025 The OmniBoard Contributors
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

package dev.silo.omniboard.ime.theme

/*
import org.omniboard.lib.snygg.Snygg
import org.omniboard.lib.snygg.SnyggLevel
import org.omniboard.lib.snygg.SnyggPropertySetSpecDeclBuilder
import org.omniboard.lib.snygg.SnyggSpecDecl
import org.omniboard.lib.snygg.value.SnyggCircleShapeValue
import org.omniboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.omniboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.omniboard.lib.snygg.value.SnyggDpSizeValue
import org.omniboard.lib.snygg.value.SnyggDynamicColorDarkColorValue
import org.omniboard.lib.snygg.value.SnyggDynamicColorLightColorValue
import org.omniboard.lib.snygg.value.SnyggRectangleShapeValue
import org.omniboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.omniboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.omniboard.lib.snygg.value.SnyggStaticColorValue
import org.omniboard.lib.snygg.value.SnyggSpSizeValue

fun SnyggPropertySetSpecDeclBuilder.background() {
    property(
        name = Snygg.Background,
        level = SnyggLevel.BASIC,
        supportedValues(SnyggStaticColorValue, SnyggDynamicColorLightColorValue, SnyggDynamicColorDarkColorValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.foreground() {
    property(
        name = Snygg.Foreground,
        level = SnyggLevel.BASIC,
        supportedValues(SnyggStaticColorValue, SnyggDynamicColorLightColorValue, SnyggDynamicColorDarkColorValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.border() {
    property(
        name = Snygg.BorderColor,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggStaticColorValue, SnyggDynamicColorLightColorValue, SnyggDynamicColorDarkColorValue),
    )
    property(
        name = Snygg.BorderWidth,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggDpSizeValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.font() {
    property(
        name = Snygg.FontSize,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggSpSizeValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.shadow() {
    property(
        name = Snygg.ShadowElevation,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggDpSizeValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.shape() {
    property(
        name = Snygg.Shape,
        level = SnyggLevel.ADVANCED,
        supportedValues(
            SnyggRectangleShapeValue,
            SnyggCircleShapeValue,
            SnyggRoundedCornerDpShapeValue,
            SnyggRoundedCornerPercentShapeValue,
            SnyggCutCornerDpShapeValue,
            SnyggCutCornerPercentShapeValue,
        ),
    )
}

object OmniImeUiSpec : SnyggSpecDecl({
    element(OmniImeUi.Keyboard) {
        background()
    }
    element(OmniImeUi.Key) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.KeyHint) {
        background()
        foreground()
        font()
        shape()
    }
    element(OmniImeUi.KeyPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }

    element(OmniImeUi.Smartbar) {
        background()
    }
    element(OmniImeUi.SmartbarSharedActionsRow) {
        background()
    }
    element(OmniImeUi.SmartbarSharedActionsToggle) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.SmartbarExtendedActionsRow) {
        background()
    }
    element(OmniImeUi.SmartbarExtendedActionsToggle) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.SmartbarActionKey) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.SmartbarActionTile) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.SmartbarActionsOverflowCustomizeButton) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.SmartbarActionsOverflow) {
        background()
    }
    element(OmniImeUi.SmartbarActionsEditor) {
        background()
        shape()
    }
    element(OmniImeUi.SmartbarActionsEditorHeader) {
        background()
        foreground()
        font()
    }
    element(OmniImeUi.SmartbarActionsEditorSubheader) {
        foreground()
        font()
    }
    element(OmniImeUi.SmartbarCandidatesRow) {
        background()
    }
    element(OmniImeUi.SmartbarCandidateWord) {
        background()
        foreground()
        font()
        shape()
    }
    element(OmniImeUi.SmartbarCandidateClip) {
        background()
        foreground()
        font()
        shape()
    }
    element(OmniImeUi.SmartbarCandidateSpacer) {
        foreground()
    }

    element(OmniImeUi.ClipboardHeader) {
        background()
        foreground()
        font()
    }
    element(OmniImeUi.ClipboardItem) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.ClipboardItemPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.ClipboardEnableHistoryButton) {
        background()
        foreground()
        shape()
    }

    element(OmniImeUi.EmojiKey) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.EmojiKeyPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(OmniImeUi.EmojiTab) {
        foreground()
    }

    element(OmniImeUi.ExtractedLandscapeInputLayout) {
        background()
    }
    element(OmniImeUi.ExtractedLandscapeInputField) {
        background()
        foreground()
        font()
        shape()
        border()
    }
    element(OmniImeUi.ExtractedLandscapeInputAction) {
        background()
        foreground()
        shape()
    }

    element(OmniImeUi.GlideTrail) {
        foreground()
    }

    element(OmniImeUi.IncognitoModeIndicator) {
        foreground()
    }

    element(OmniImeUi.OneHandedPanel) {
        background()
        foreground()
    }

    element(OmniImeUi.SystemNavBar) {
        background()
    }
})

Snygg.init(
            stylesheetSpec = OmniImeUiSpec,
            rulePreferredElementSorting = listOf(
                OmniImeUi.Keyboard,
                OmniImeUi.Key,
                OmniImeUi.KeyHint,
                OmniImeUi.KeyPopup,
                OmniImeUi.Smartbar,
                OmniImeUi.SmartbarSharedActionsRow,
                OmniImeUi.SmartbarSharedActionsToggle,
                OmniImeUi.SmartbarExtendedActionsRow,
                OmniImeUi.SmartbarExtendedActionsToggle,
                OmniImeUi.SmartbarActionKey,
                OmniImeUi.SmartbarActionTile,
                OmniImeUi.SmartbarActionsOverflow,
                OmniImeUi.SmartbarActionsOverflowCustomizeButton,
                OmniImeUi.SmartbarActionsEditor,
                OmniImeUi.SmartbarActionsEditorHeader,
                OmniImeUi.SmartbarActionsEditorSubheader,
                OmniImeUi.SmartbarCandidatesRow,
                OmniImeUi.SmartbarCandidateWord,
                OmniImeUi.SmartbarCandidateClip,
                OmniImeUi.SmartbarCandidateSpacer,
            ),
            rulePlaceholders = mapOf(
                "c:delete" to KeyCode.DELETE,
                "c:enter" to KeyCode.ENTER,
                "c:shift" to KeyCode.SHIFT,
                "c:space" to KeyCode.SPACE,
                "sh:unshifted" to InputShiftState.UNSHIFTED.value,
                "sh:shifted_manual" to InputShiftState.SHIFTED_MANUAL.value,
                "sh:shifted_automatic" to InputShiftState.SHIFTED_AUTOMATIC.value,
                "sh:caps_lock" to InputShiftState.CAPS_LOCK.value,
            ),
        )
*/
