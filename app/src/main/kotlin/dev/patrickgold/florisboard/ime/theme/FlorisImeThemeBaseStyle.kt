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

package dev.patrickgold.florisboard.ime.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.lib.snygg.SnyggStylesheet

val FlorisImeThemeBaseStyle = SnyggStylesheet {
    defines {
        "primary" to rgbaColor(76, 175, 80)
        "primaryVariant" to rgbaColor(56, 142, 60)
        "secondary" to rgbaColor(245, 124, 0)
        "secondaryVariant" to rgbaColor(230, 81, 0)
        "background" to rgbaColor(33, 33, 33)
        "surface" to rgbaColor(66, 66, 66)
        "surfaceVariant" to rgbaColor(97, 97, 97)

        "onBackground" to rgbaColor(255, 255, 255)
        "onSurface" to rgbaColor(255, 255, 255)
    }

    FlorisImeUi.Keyboard {
        background = `var`("background")
    }
    FlorisImeUi.Key {
        background = `var`("surface")
        foreground = `var`("onSurface")
        fontSize = size(22.sp)
        shadowElevation = size(2.dp)
        shape = roundedCornerShape(20)
    }
    FlorisImeUi.Key(pressedSelector = true) {
        background = `var`("surfaceVariant")
        foreground = `var`("onSurface")
    }
    FlorisImeUi.Key(codes = listOf(KeyCode.ENTER)) {
        background = `var`("primary")
        foreground = `var`("onSurface")
    }
    FlorisImeUi.Key(codes = listOf(KeyCode.ENTER), pressedSelector = true) {
        background = `var`("primaryVariant")
        foreground = `var`("onSurface")
    }
    FlorisImeUi.Key(
        codes = listOf(KeyCode.SHIFT),
        modes = listOf(InputShiftState.CAPS_LOCK.value),
    ) {
        foreground = rgbaColor(255, 152, 0)
    }
    FlorisImeUi.Key(codes = listOf(KeyCode.SPACE)) {
        background = `var`("surface")
        foreground = rgbaColor(144, 144, 144)
        fontSize = size(12.sp)
    }
    FlorisImeUi.KeyHint {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(184, 184, 184)
        fontSize = size(12.sp)
    }
    FlorisImeUi.KeyPopup {
        background = rgbaColor(117, 117, 117)
        foreground = `var`("onSurface")
        fontSize = size(22.sp)
        shape = roundedCornerShape(20)
    }
    FlorisImeUi.KeyPopup(focusSelector = true) {
        background = rgbaColor(189, 189, 189)
        foreground = `var`("onSurface")
        fontSize = size(22.sp)
        shape = roundedCornerShape(20)
    }

    FlorisImeUi.ClipboardHeader {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = `var`("onSurface")
        fontSize = size(16.sp)
    }
    FlorisImeUi.ClipboardItem {
        background = `var`("surface")
        foreground = `var`("onSurface")
        fontSize = size(14.sp)
        shape = roundedCornerShape(12.dp)
    }
    FlorisImeUi.ClipboardItemPopup {
        background = rgbaColor(117, 117, 117)
        foreground = `var`("onSurface")
        fontSize = size(14.sp)
        shape = roundedCornerShape(12.dp)
    }

    FlorisImeUi.EmojiKey {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = `var`("onBackground")
        fontSize = size(22.sp)
        shape = roundedCornerShape(20)
    }
    FlorisImeUi.EmojiKey(pressedSelector = true) {
        background = `var`("surface")
        foreground = `var`("onSurface")
    }

    FlorisImeUi.GlideTrail {
        foreground = `var`("primary")
    }

    FlorisImeUi.OneHandedPanel {
        background = rgbaColor(27, 94, 32)
        foreground = rgbaColor(238, 238, 238)
    }

    FlorisImeUi.SmartbarPrimaryRow {
        background = rgbaColor(0, 0, 0, 0f)
    }
    FlorisImeUi.SmartbarSecondaryRow {
        background = rgbaColor(0, 0, 0, 0f)
    }
    FlorisImeUi.SmartbarPrimaryActionsToggle {
        background = `var`("surface")
        foreground = `var`("onSurface")
        shape = circleShape()
    }
    FlorisImeUi.SmartbarSecondaryActionsToggle {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(144, 144, 144)
        shape = circleShape()
    }
    FlorisImeUi.SmartbarQuickAction {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(220, 220, 220)
        shape = circleShape()
    }
    FlorisImeUi.SmartbarKey {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(220, 220, 220)
        fontSize = size(18.sp)
        shape = roundedCornerShape(20)
    }
    FlorisImeUi.SmartbarKey(pressedSelector = true) {
        background = `var`("surface")
        foreground = rgbaColor(220, 220, 220)
    }
    FlorisImeUi.SmartbarKey(disabledSelector = true) {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = `var`("surface")
    }
    FlorisImeUi.SmartbarCandidateWord {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(220, 220, 220)
        fontSize = size(14.sp)
        shape = rectangleShape()
    }
    FlorisImeUi.SmartbarCandidateWord(pressedSelector = true) {
        background = `var`("surface")
        foreground = rgbaColor(220, 220, 220)
    }
    FlorisImeUi.SmartbarCandidateClip {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(220, 220, 220)
        fontSize = size(14.sp)
        shape = roundedCornerShape(8)
    }
    FlorisImeUi.SmartbarCandidateClip(pressedSelector = true) {
        background = `var`("surface")
        foreground = rgbaColor(220, 220, 220)
    }
    FlorisImeUi.SmartbarCandidateSpacer {
        foreground = rgbaColor(255, 255, 255, 0.25f)
    }

    FlorisImeUi.SystemNavBar {
        background = `var`("background")
    }
}
