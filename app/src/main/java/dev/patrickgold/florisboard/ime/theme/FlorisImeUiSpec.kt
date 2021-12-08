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

import dev.patrickgold.florisboard.snygg.Snygg
import dev.patrickgold.florisboard.snygg.SnyggLevel
import dev.patrickgold.florisboard.snygg.SnyggPropertySetSpecBuilder
import dev.patrickgold.florisboard.snygg.SnyggSpec
import dev.patrickgold.florisboard.snygg.value.SnyggCutCornerShapeValue
import dev.patrickgold.florisboard.snygg.value.SnyggRectangleShapeValue
import dev.patrickgold.florisboard.snygg.value.SnyggRoundedCornerShapeValue
import dev.patrickgold.florisboard.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.snygg.value.SnyggSpSizeValue

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
fun SnyggPropertySetSpecBuilder.fontSize() {
    property(
        name = Snygg.FontSize,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggSpSizeValue),
    )
}
fun SnyggPropertySetSpecBuilder.shape() {
    property(
        name = Snygg.Shape,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggRectangleShapeValue, SnyggCutCornerShapeValue, SnyggRoundedCornerShapeValue),
    )
}

object FlorisImeUiSpec : SnyggSpec({
    element(FlorisImeUi.Keyboard) {
        background()
    }
    element(FlorisImeUi.Key) {
        background()
        foreground()
        fontSize()
        shape()
    }
    element(FlorisImeUi.KeyHint) {
        background()
        foreground()
        fontSize()
        shape()
    }
    element(FlorisImeUi.KeyPopup) {
        background()
        foreground()
        fontSize()
        shape()
    }

    element(FlorisImeUi.ClipboardHeader) {
        background()
        foreground()
        fontSize()
    }

    element(FlorisImeUi.OneHandedPanel) {
        background()
        foreground()
    }

    element(FlorisImeUi.SmartbarPrimaryRow) {
        background()
    }
    element(FlorisImeUi.SmartbarPrimaryActionRowToggle) {
        background()
        foreground()
        shape()
    }
    element(FlorisImeUi.SmartbarPrimarySecondaryRowToggle) {
        background()
        foreground()
        shape()
    }

    element(FlorisImeUi.SmartbarSecondaryRow) {
        background()
    }

    element(FlorisImeUi.SmartbarActionRow) {
        background()
    }
    element(FlorisImeUi.SmartbarActionButton) {
        background()
        foreground()
        shape()
    }

    element(FlorisImeUi.SmartbarCandidateRow) {
        background()
    }
    element(FlorisImeUi.SmartbarCandidateWord) {
        background()
        foreground()
        fontSize()
        shape()
    }
    element(FlorisImeUi.SmartbarCandidateClip) {
        background()
        foreground()
        fontSize()
        shape()
    }
    element(FlorisImeUi.SmartbarCandidateSpacer) {
        foreground()
    }

    element(FlorisImeUi.SmartbarKey) {
        background()
        foreground()
        fontSize()
        shape()
    }

    element(FlorisImeUi.SystemNavBar) {
        background()
    }
})
