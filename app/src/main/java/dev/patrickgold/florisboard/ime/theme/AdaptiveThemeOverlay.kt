/*
 * Copyright (C) 2020 Patrick Goldinger
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

import android.graphics.Color

/**
 * Theme overlay class which, if enabled, changes some requested attributes in a Theme and returns
 * the corresponding adaptive color. The adaptive colors itself are determined by the ThemeManager
 * and this overlay class only uses them if the feature is enabled in the prefs.
 */
class AdaptiveThemeOverlay(
    private val themeManager: ThemeManager,
    theme: Theme
) : Theme(theme.name, theme.label, theme.authors, theme.isNightTheme, theme.attributes) {
    override fun getAttr(ref: ThemeValue.Reference, s1: String?, s2: String?): ThemeValue {
        return when {
            themeManager.isAdaptiveThemeEnabled -> when (ref) {
                Attr.KEYBOARD_BACKGROUND,
                Attr.KEY_BACKGROUND_PRESSED,
                Attr.SMARTBAR_BACKGROUND,
                Attr.WINDOW_NAVIGATION_BAR_COLOR -> {
                    themeManager.remoteColorPrimaryVariant ?: super.getAttr(ref, s1, s2)
                }
                Attr.KEY_FOREGROUND_PRESSED,
                Attr.SMARTBAR_FOREGROUND -> {
                    themeManager.remoteColorPrimaryVariant?.complimentaryTextColor() ?: super.getAttr(ref, s1, s2)
                }
                Attr.SMARTBAR_FOREGROUND_ALT -> {
                    themeManager.remoteColorPrimaryVariant?.complimentaryTextColor(true) ?: super.getAttr(ref, s1, s2)
                }
                Attr.KEY_BACKGROUND,
                Attr.SMARTBAR_BUTTON_BACKGROUND -> {
                    themeManager.remoteColorPrimary ?: super.getAttr(ref, s1, s2)
                }
                Attr.KEY_FOREGROUND,
                Attr.SMARTBAR_BUTTON_FOREGROUND -> {
                    themeManager.remoteColorPrimary?.complimentaryTextColor() ?: super.getAttr(ref, s1, s2)
                }
                Attr.KEY_SHOW_BORDER -> {
                    if (themeManager.remoteColorPrimary != null) {
                        ThemeValue.OnOff(true)
                    } else {
                        super.getAttr(ref, s1, s2)
                    }
                }
                Attr.WINDOW_NAVIGATION_BAR_LIGHT -> {
                    if (themeManager.remoteColorPrimaryVariant != null) {
                        ThemeValue.OnOff(themeManager.remoteColorPrimaryVariant?.complimentaryTextColor()?.color == Color.BLACK)
                    } else {
                        super.getAttr(ref, s1, s2)
                    }
                }
                Attr.POPUP_BACKGROUND -> {
                    themeManager.remoteColorSecondary ?: super.getAttr(ref, s1, s2)
                }
                Attr.POPUP_BACKGROUND_ACTIVE -> {
                    themeManager.remoteColorSecondary?.complimentaryTextColor(true) ?: super.getAttr(ref, s1, s2)
                }
                Attr.POPUP_FOREGROUND -> {
                    themeManager.remoteColorSecondary?.complimentaryTextColor() ?: super.getAttr(ref, s1, s2)
                }
                else -> super.getAttr(ref, s1, s2)
            }
            else -> super.getAttr(ref, s1, s2)
        }
    }
}
