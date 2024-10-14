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

package dev.patrickgold.florisboard.lib.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.inputmethodservice.InputMethodService
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.snygg.ui.solidColor


@Composable
fun SystemUiIme() {
    val useDarkIcons = !FlorisImeTheme.config.isNightTheme
    val context = LocalContext.current
    val view = LocalView.current
    val backgroundColor = FlorisImeTheme.style.get(FlorisImeUi.SystemNavBar).background.solidColor(context)
    val window = view.context.findWindow()!!
    val windowInsetsController = WindowInsetsControllerCompat(window, view)

    SideEffect {
        if (AndroidVersion.ATLEAST_API26_O) {
            window.navigationBarColor = backgroundColor.toArgb()
            windowInsetsController.isAppearanceLightNavigationBars = useDarkIcons
            if (AndroidVersion.ATLEAST_API29_Q) window.isNavigationBarContrastEnforced = true
        }
    }
}

tailrec fun Context.findWindow(): Window? {
    val context = this
    if (context is Activity) return context.window
    if (context is InputMethodService) return context.window?.window
    return if (context is ContextWrapper) context.findWindow() else null
}
