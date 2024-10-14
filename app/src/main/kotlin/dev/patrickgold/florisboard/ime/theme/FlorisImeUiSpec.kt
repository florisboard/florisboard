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

package dev.patrickgold.florisboard.ime.theme

import org.florisboard.lib.snygg.Snygg
import org.florisboard.lib.snygg.SnyggLevel
import org.florisboard.lib.snygg.SnyggPropertySetSpecBuilder
import org.florisboard.lib.snygg.SnyggSpec
import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggMaterialYouDarkColorValue
import org.florisboard.lib.snygg.value.SnyggMaterialYouLightColorValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggSolidColorValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue

fun SnyggPropertySetSpecBuilder.background() {
    property(
        name = Snygg.Background,
        level = SnyggLevel.BASIC,
        supportedValues(SnyggSolidColorValue, SnyggMaterialYouLightColorValue, SnyggMaterialYouDarkColorValue),
    )
}
fun SnyggPropertySetSpecBuilder.foreground() {
    property(
        name = Snygg.Foreground,
        level = SnyggLevel.BASIC,
        supportedValues(SnyggSolidColorValue, SnyggMaterialYouLightColorValue, SnyggMaterialYouDarkColorValue),
    )
}
fun SnyggPropertySetSpecBuilder.border() {
    property(
        name = Snygg.BorderColor,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggSolidColorValue, SnyggMaterialYouLightColorValue, SnyggMaterialYouDarkColorValue),
    )
    property(
        name = Snygg.BorderWidth,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggDpSizeValue),
    )
}
fun SnyggPropertySetSpecBuilder.font() {
    property(
        name = Snygg.FontSize,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggSpSizeValue),
    )
}
fun SnyggPropertySetSpecBuilder.shadow() {
    property(
        name = Snygg.ShadowElevation,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggDpSizeValue),
    )
}
fun SnyggPropertySetSpecBuilder.shape() {
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

object FlorisImeUiSpec : SnyggSpec({
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
