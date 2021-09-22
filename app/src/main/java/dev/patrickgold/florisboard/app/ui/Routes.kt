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

package dev.patrickgold.florisboard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.patrickgold.florisboard.app.ui.settings.HomeScreen
import dev.patrickgold.florisboard.app.ui.settings.about.AboutScreen
import dev.patrickgold.florisboard.app.ui.settings.about.ProjectLicenseScreen
import dev.patrickgold.florisboard.app.ui.settings.about.ThirdPartyLicensesScreen
import dev.patrickgold.florisboard.app.ui.settings.advanced.AdvancedScreen
import dev.patrickgold.florisboard.app.ui.settings.clipboard.ClipboardScreen
import dev.patrickgold.florisboard.app.ui.settings.gestures.GesturesScreen
import dev.patrickgold.florisboard.app.ui.settings.keyboard.InputFeedbackScreen
import dev.patrickgold.florisboard.app.ui.settings.keyboard.KeyboardScreen
import dev.patrickgold.florisboard.app.ui.setup.SetupScreen

object Routes {
    object Settings {
        const val Home = "settings"

        const val Keyboard = "settings/keyboard"
        const val InputFeedback = "settings/keyboard/input-feedback"

        const val Gestures = "settings/gestures"

        const val Clipboard = "settings/clipboard"

        const val Advanced = "settings/advanced"

        const val About = "settings/about"
        const val ProjectLicense = "settings/about/project-license"
        const val ThirdPartyLicenses = "settings/about/third-party-licenses"
    }

    object Setup {
        const val Home = "setup"
    }

    @Composable
    fun AppNavHost(
        modifier: Modifier,
        navController: NavHostController,
        startDestination: String,
    ) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Settings.Home) { HomeScreen() }

            composable(Settings.Keyboard) { KeyboardScreen() }
            composable(Settings.InputFeedback) { InputFeedbackScreen() }

            composable(Settings.Gestures) { GesturesScreen() }

            composable(Settings.Clipboard) { ClipboardScreen() }

            composable(Settings.Advanced) { AdvancedScreen() }

            composable(Settings.About) { AboutScreen() }
            composable(Settings.ProjectLicense) { ProjectLicenseScreen() }
            composable(Settings.ThirdPartyLicenses) { ThirdPartyLicensesScreen() }

            composable(Setup.Home) { SetupScreen() }
        }
    }
}
