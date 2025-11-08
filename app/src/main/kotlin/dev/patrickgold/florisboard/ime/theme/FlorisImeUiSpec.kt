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

package dev.patrickgold.florisboard.ime.theme

/*
import org.florisboard.lib.snygg.Snygg
import org.florisboard.lib.snygg.SnyggLevel
import org.florisboard.lib.snygg.SnyggPropertySetSpecDeclBuilder
import org.florisboard.lib.snygg.SnyggSpecDecl
import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggDynamicColorDarkColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicColorLightColorValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue

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

object FlorisImeUiSpec : SnyggSpecDecl({
    element(FlorisImeUi.Keyboard) {
        background()
    }
    element(FlorisImeUi.Key) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.KeyHint) {
        background()
        foreground()
        font()
        shape()
    }
    element(FlorisImeUi.KeyPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }

    element(FlorisImeUi.Smartbar) {
        background()
    }
    element(FlorisImeUi.SmartbarSharedActionsRow) {
        background()
    }
    element(FlorisImeUi.SmartbarSharedActionsToggle) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.SmartbarExtendedActionsRow) {
        background()
    }
    element(FlorisImeUi.SmartbarExtendedActionsToggle) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.SmartbarActionKey) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.SmartbarActionTile) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.SmartbarActionsOverflowCustomizeButton) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.SmartbarActionsOverflow) {
        background()
    }
    element(FlorisImeUi.SmartbarActionsEditor) {
        background()
        shape()
    }
    element(FlorisImeUi.SmartbarActionsEditorHeader) {
        background()
        foreground()
        font()
    }
    element(FlorisImeUi.SmartbarActionsEditorSubheader) {
        foreground()
        font()
    }
    element(FlorisImeUi.SmartbarCandidatesRow) {
        background()
    }
    element(FlorisImeUi.SmartbarCandidateWord) {
        background()
        foreground()
        font()
        shape()
    }
    element(FlorisImeUi.SmartbarCandidateClip) {
        background()
        foreground()
        font()
        shape()
    }
    element(FlorisImeUi.SmartbarCandidateSpacer) {
        foreground()
    }

    element(FlorisImeUi.ClipboardHeader) {
        background()
        foreground()
        font()
    }
    element(FlorisImeUi.ClipboardItem) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.ClipboardItemPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.ClipboardEnableHistoryButton) {
        background()
        foreground()
        shape()
    }

    element(FlorisImeUi.EmojiKey) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.EmojiKeyPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.EmojiTab) {
        foreground()
    }

    element(FlorisImeUi.ExtractedLandscapeInputLayout) {
        background()
    }
    element(FlorisImeUi.ExtractedLandscapeInputField) {
        background()
        foreground()
        font()
        shape()
        border()
    }
    element(FlorisImeUi.ExtractedLandscapeInputAction) {
        background()
        foreground()
        shape()
    }

    element(FlorisImeUi.GlideTrail) {
        foreground()
    }

    element(FlorisImeUi.IncognitoModeIndicator) {
        foreground()
    }

    element(FlorisImeUi.OneHandedPanel) {
        background()
        foreground()
    }

    element(FlorisImeUi.SystemNavBar) {
        background()
    }
})

Snygg.init(
            stylesheetSpec = FlorisImeUiSpec,
            rulePreferredElementSorting = listOf(
                FlorisImeUi.Keyboard,
                FlorisImeUi.Key,
                FlorisImeUi.KeyHint,
                FlorisImeUi.KeyPopup,
                FlorisImeUi.Smartbar,
                FlorisImeUi.SmartbarSharedActionsRow,
                FlorisImeUi.SmartbarSharedActionsToggle,
                FlorisImeUi.SmartbarExtendedActionsRow,
                FlorisImeUi.SmartbarExtendedActionsToggle,
                FlorisImeUi.SmartbarActionKey,
                FlorisImeUi.SmartbarActionTile,
                FlorisImeUi.SmartbarActionsOverflow,
                FlorisImeUi.SmartbarActionsOverflowCustomizeButton,
                FlorisImeUi.SmartbarActionsEditor,
                FlorisImeUi.SmartbarActionsEditorHeader,
                FlorisImeUi.SmartbarActionsEditorSubheader,
                FlorisImeUi.SmartbarCandidatesRow,
                FlorisImeUi.SmartbarCandidateWord,
                FlorisImeUi.SmartbarCandidateClip,
                FlorisImeUi.SmartbarCandidateSpacer,
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
