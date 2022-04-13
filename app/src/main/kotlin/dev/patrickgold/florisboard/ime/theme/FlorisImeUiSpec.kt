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

import dev.patrickgold.florisboard.lib.snygg.Snygg
import dev.patrickgold.florisboard.lib.snygg.SnyggLevel
import dev.patrickgold.florisboard.lib.snygg.SnyggPropertySetSpecBuilder
import dev.patrickgold.florisboard.lib.snygg.SnyggSpec
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCircleShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSpSizeValue

fun SnyggPropertySetSpecBuilder.background() {
    property(
        name = Snygg.Background,
        level = SnyggLevel.BASIC,
        supportedValues(SnyggSolidColorValue),
    )
}
fun SnyggPropertySetSpecBuilder.foreground() {
    property(
        name = Snygg.Foreground,
        level = SnyggLevel.BASIC,
        supportedValues(SnyggSolidColorValue),
    )
}
fun SnyggPropertySetSpecBuilder.border() {
    property(
        name = Snygg.BorderColor,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggSolidColorValue),
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

    element(FlorisImeUi.SmartbarPrimaryRow) {
        background()
    }
    element(FlorisImeUi.SmartbarSecondaryRow) {
        background()
    }
    element(FlorisImeUi.SmartbarPrimaryActionsToggle) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.SmartbarSecondaryActionsToggle) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.SmartbarQuickAction) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(FlorisImeUi.SmartbarKey) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
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

    element(FlorisImeUi.OneHandedPanel) {
        background()
        foreground()
    }

    element(FlorisImeUi.SystemNavBar) {
        background()
    }
})
