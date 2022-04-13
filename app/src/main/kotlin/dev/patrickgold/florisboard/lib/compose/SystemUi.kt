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
import android.view.View
import android.view.Window
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.SystemUiController
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.android.AndroidVersion
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor

@Composable
fun SystemUiApp() {
    val systemUiController = rememberFlorisSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons,
        )
        if (AndroidVersion.ATLEAST_API26_O) {
            systemUiController.setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = useDarkIcons,
                navigationBarContrastEnforced = false,
            )
        }
    }
}

@Composable
fun SystemUiIme() {
    val systemUiController = rememberFlorisSystemUiController()
    val useDarkIcons = !FlorisImeTheme.config.isNightTheme
    val backgroundColor = FlorisImeTheme.style.get(FlorisImeUi.SystemNavBar).background.solidColor()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = backgroundColor,
            darkIcons = useDarkIcons,
        )
        if (AndroidVersion.ATLEAST_API26_O) {
            systemUiController.setNavigationBarColor(
                color = backgroundColor,
                darkIcons = useDarkIcons,
                navigationBarContrastEnforced = true,
            )
        }
    }
}

@Composable
private fun rememberFlorisSystemUiController(): SystemUiController {
    val view = LocalView.current
    return remember(view) { FlorisSystemUiController(view) }
}

/**
 * Custom SystemUiController adapted so it works within an InputMethodService context as well.
 *
 * Original source:
 *  https://github.com/google/accompanist/blob/main/systemuicontroller/src/main/java/com/google/accompanist/systemuicontroller/SystemUiController.kt
 *
 * Changed:
 * - findWindow() method recognizes input method service classes as well
 * - Window insets controller is constructed directly, bypassing ViewCompat (as this ignores service classes as well)
 * - API checks use FlorisBoard's [AndroidVersion] utils
 */
private class FlorisSystemUiController(
    private val view: View,
) : SystemUiController {
    private val window = view.context.findWindow()!!
    private val windowInsetsController = WindowInsetsControllerCompat(window, view)

    override fun setStatusBarColor(
        color: Color,
        darkIcons: Boolean,
        transformColorForLightContent: (Color) -> Color
    ) {
        statusBarDarkContentEnabled = darkIcons

        window.statusBarColor = when {
            darkIcons && !windowInsetsController.isAppearanceLightStatusBars -> {
                // If we're set to use dark icons, but our windowInsetsController call didn't
                // succeed (usually due to API level), we instead transform the color to maintain
                // contrast
                transformColorForLightContent(color)
            }
            else -> color
        }.toArgb()
    }

    override fun setNavigationBarColor(
        color: Color,
        darkIcons: Boolean,
        navigationBarContrastEnforced: Boolean,
        transformColorForLightContent: (Color) -> Color
    ) {
        navigationBarDarkContentEnabled = darkIcons
        isNavigationBarContrastEnforced = navigationBarContrastEnforced

        window.navigationBarColor = when {
            darkIcons && !windowInsetsController.isAppearanceLightNavigationBars -> {
                // If we're set to use dark icons, but our windowInsetsController call didn't
                // succeed (usually due to API level), we instead transform the color to maintain
                // contrast
                transformColorForLightContent(color)
            }
            else -> color
        }.toArgb()
    }

    override var isStatusBarVisible: Boolean
        get() {
            return ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.statusBars()) == true
        }
        set(value) {
            if (value) {
                windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
            } else {
                windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
            }
        }

    override var isNavigationBarVisible: Boolean
        get() {
            return ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.navigationBars()) == true
        }
        set(value) {
            if (value) {
                windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
            }
        }

    override var statusBarDarkContentEnabled: Boolean
        get() = windowInsetsController.isAppearanceLightStatusBars
        set(value) {
            windowInsetsController.isAppearanceLightStatusBars = value
        }

    override var navigationBarDarkContentEnabled: Boolean
        get() = windowInsetsController.isAppearanceLightNavigationBars
        set(value) {
            windowInsetsController.isAppearanceLightNavigationBars = value
        }

    override var isNavigationBarContrastEnforced: Boolean
        get() = AndroidVersion.ATLEAST_API29_Q && window.isNavigationBarContrastEnforced
        set(value) {
            if (AndroidVersion.ATLEAST_API29_Q) {
                window.isNavigationBarContrastEnforced = value
            }
        }

    private tailrec fun Context.findWindow(): Window? {
        val context = this
        if (context is Activity) return context.window
        if (context is InputMethodService) return context.window?.window
        return if (context is ContextWrapper) context.findWindow() else null
    }
}
