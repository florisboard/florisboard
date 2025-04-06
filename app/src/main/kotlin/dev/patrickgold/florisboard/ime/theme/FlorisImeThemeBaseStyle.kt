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

package dev.patrickgold.florisboard.ime.theme

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

val FlorisImeThemeBaseStyle = SnyggStylesheet.v2 {
    defines {
        "--primary" to rgbaColor(76, 175, 80)
        "--primary-variant" to rgbaColor(56, 142, 60)
        "--secondary" to rgbaColor(245, 124, 0)
        "--secondary-variant" to rgbaColor(230, 81, 0)
        "--background" to rgbaColor(33, 33, 33)
        "--surface" to rgbaColor(66, 66, 66)
        "--surface-variant" to rgbaColor(97, 97, 97)

        "--on-background" to rgbaColor(255, 255, 255)
        "--on-surface" to rgbaColor(255, 255, 255)
    }

    FlorisImeUi.Keyboard {
        background = `var`("--background")
        foreground = `var`("--on-background")
    }
    FlorisImeUi.Key {
        background = `var`("--surface")
        foreground = `var`("--on-surface")
        fontSize = fontSize(22.sp)
        shadowElevation = size(2.dp)
        shape = roundedCornerShape(20)
    }
    FlorisImeUi.Key(selector = SnyggSelector.PRESSED) {
        background = `var`("--surface-variant")
        foreground = `var`("--on-surface")
    }
    FlorisImeUi.Key("code" to listOf(KeyCode.ENTER)) {
        background = `var`("--primary")
        foreground = `var`("--on-surface")
    }
    FlorisImeUi.Key("code" to listOf(KeyCode.ENTER), selector = SnyggSelector.PRESSED) {
        background = `var`("--primary-variant")
        foreground = `var`("--on-surface")
    }
    FlorisImeUi.Key(
        "code" to listOf(KeyCode.SHIFT),
        "mode" to listOf(InputShiftState.CAPS_LOCK.value),
    ) {
        foreground = rgbaColor(255, 152, 0)
    }
    FlorisImeUi.Key("code" to listOf(KeyCode.SPACE)) {
        background = `var`("--surface")
        foreground = rgbaColor(144, 144, 144)
        fontSize = fontSize(12.sp)
    }
    FlorisImeUi.KeyHint {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(184, 184, 184)
        fontSize = fontSize(12.sp)
    }
    FlorisImeUi.KeyPopup {
        background = rgbaColor(117, 117, 117)
        foreground = `var`("--on-surface")
        fontSize = fontSize(22.sp)
        shape = roundedCornerShape(20)
    }
    FlorisImeUi.KeyPopup(selector = SnyggSelector.FOCUS) {
        background = rgbaColor(189, 189, 189)
        foreground = `var`("--on-surface")
        fontSize = fontSize(22.sp)
        shape = roundedCornerShape(20)
    }

    FlorisImeUi.ClipboardHeader {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = `var`("--on-surface")
        fontSize = fontSize(16.sp)
    }
    FlorisImeUi.ClipboardItem {
        background = `var`("--surface")
        foreground = `var`("--on-surface")
        fontSize = fontSize(14.sp)
        shape = roundedCornerShape(12.dp)
    }
    FlorisImeUi.ClipboardItemPopup {
        background = rgbaColor(117, 117, 117)
        foreground = `var`("--on-surface")
        fontSize = fontSize(14.sp)
        shape = roundedCornerShape(12.dp)
    }
    FlorisImeUi.ClipboardEnableHistoryButton {
        background = `var`("--primary")
        foreground = rgbaColor(0, 0, 0)
        shape = roundedCornerShape(12.dp)
    }

    FlorisImeUi.EmojiKey {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = `var`("--on-background")
        fontSize = fontSize(22.sp)
        shape = roundedCornerShape(20)
    }
    FlorisImeUi.EmojiKey(selector = SnyggSelector.PRESSED) {
        background = `var`("--surface")
        foreground = `var`("--on-surface")
    }

    FlorisImeUi.GlideTrail {
        foreground = `var`("--primary")
    }

    FlorisImeUi.IncognitoModeIndicator {
        foreground = rgbaColor(255, 255, 255, 0.067f)
    }

    FlorisImeUi.OneHandedPanel {
        background = rgbaColor(27, 94, 32)
        foreground = rgbaColor(238, 238, 238)
    }

    FlorisImeUi.Smartbar {
        background = rgbaColor(0, 0, 0, 0f)
    }
    FlorisImeUi.SmartbarSharedActionsRow {
        background = rgbaColor(0, 0, 0, 0f)
    }
    FlorisImeUi.SmartbarSharedActionsToggle {
        background = `var`("--surface")
        foreground = `var`("--on-surface")
        shape = circleShape()
    }
    FlorisImeUi.SmartbarExtendedActionsRow {
        background = rgbaColor(0, 0, 0, 0f)
    }
    FlorisImeUi.SmartbarExtendedActionsToggle {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(144, 144, 144)
        shape = circleShape()
    }
    FlorisImeUi.SmartbarActionKey {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(220, 220, 220)
        fontSize = fontSize(18.sp)
        shape = roundedCornerShape(20)
    }
    FlorisImeUi.SmartbarActionKey(selector = SnyggSelector.DISABLED) {
        foreground = `var`("--surface")
    }
    FlorisImeUi.SmartbarActionTile {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(220, 220, 220)
        lineClamp = lineClampMax(1)
        shape = roundedCornerShape(20)
        textOverflow = textOverflow(TextOverflow.Ellipsis)
    }
    FlorisImeUi.SmartbarActionTile(selector = SnyggSelector.DISABLED) {
        foreground = `var`("--surface")
    }
    FlorisImeUi.SmartbarCandidateWord {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(220, 220, 220)
        fontSize = fontSize(14.sp)
        shape = rectangleShape()
    }
    FlorisImeUi.SmartbarCandidateWord(selector = SnyggSelector.PRESSED) {
        background = `var`("--surface")
        foreground = rgbaColor(220, 220, 220)
    }
    FlorisImeUi.SmartbarCandidateClip {
        background = rgbaColor(0, 0, 0, 0f)
        foreground = rgbaColor(220, 220, 220)
        fontSize = fontSize(14.sp)
        shape = roundedCornerShape(8)
    }
    FlorisImeUi.SmartbarCandidateClip(selector = SnyggSelector.PRESSED) {
        background = `var`("--surface")
        foreground = rgbaColor(220, 220, 220)
    }
    FlorisImeUi.SmartbarCandidateSpacer {
        foreground = rgbaColor(255, 255, 255, 0.25f)
    }
}
