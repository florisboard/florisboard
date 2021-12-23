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
import dev.patrickgold.florisboard.app.ui.devtools.AndroidLocalesScreen
import dev.patrickgold.florisboard.app.ui.devtools.AndroidSettingsScreen
import dev.patrickgold.florisboard.app.ui.devtools.DevtoolsScreen
import dev.patrickgold.florisboard.app.ui.ext.ExtensionViewScreen
import dev.patrickgold.florisboard.app.ui.settings.HomeScreen
import dev.patrickgold.florisboard.app.ui.settings.about.AboutScreen
import dev.patrickgold.florisboard.app.ui.settings.about.ProjectLicenseScreen
import dev.patrickgold.florisboard.app.ui.settings.about.ThirdPartyLicensesScreen
import dev.patrickgold.florisboard.app.ui.settings.advanced.AdvancedScreen
import dev.patrickgold.florisboard.app.ui.settings.clipboard.ClipboardScreen
import dev.patrickgold.florisboard.app.ui.settings.dictionary.DictionaryScreen
import dev.patrickgold.florisboard.app.ui.settings.gestures.GesturesScreen
import dev.patrickgold.florisboard.app.ui.settings.keyboard.InputFeedbackScreen
import dev.patrickgold.florisboard.app.ui.settings.keyboard.KeyboardScreen
import dev.patrickgold.florisboard.app.ui.settings.localization.LocalizationScreen
import dev.patrickgold.florisboard.app.ui.settings.localization.SelectLocaleScreen
import dev.patrickgold.florisboard.app.ui.settings.localization.SubtypeEditorScreen
import dev.patrickgold.florisboard.app.ui.settings.smartbar.SmartbarScreen
import dev.patrickgold.florisboard.app.ui.settings.spelling.ImportSpellingArchiveScreen
import dev.patrickgold.florisboard.app.ui.settings.spelling.ManageSpellingDictsScreen
import dev.patrickgold.florisboard.app.ui.settings.spelling.SpellingInfoScreen
import dev.patrickgold.florisboard.app.ui.settings.spelling.SpellingScreen
import dev.patrickgold.florisboard.app.ui.settings.theme.ThemeScreen
import dev.patrickgold.florisboard.app.ui.settings.theme.ThemeSelectScreen
import dev.patrickgold.florisboard.app.ui.settings.theme.ThemeSelectScreenType
import dev.patrickgold.florisboard.app.ui.settings.typing.TypingScreen
import dev.patrickgold.florisboard.app.ui.setup.SetupScreen
import dev.patrickgold.florisboard.app.ui.splash.SplashScreen
import dev.patrickgold.florisboard.common.kotlin.curlyFormat

@Suppress("FunctionName")
object Routes {
    object Splash {
        const val Screen = "splash"
    }

    object Setup {
        const val Screen = "setup"
    }

    object Settings {
        const val Home = "settings"

        const val Localization = "settings/localization"
        const val SelectLocale = "settings/localization/select-locale"
        const val SubtypeAdd = "settings/localization/subtype/add"
        const val SubtypeEdit = "settings/localization/subtype/edit/{id}"
        fun SubtypeEdit(id: Long) = SubtypeEdit.curlyFormat("id" to id)

        const val Theme = "settings/theme"
        const val ThemeSelect = "settings/theme/select/{type}"
        fun ThemeSelect(type: String) = ThemeSelect.curlyFormat("type" to type)

        const val Keyboard = "settings/keyboard"
        const val InputFeedback = "settings/keyboard/input-feedback"

        const val Smartbar = "settings/smartbar"

        const val Typing = "settings/typing"

        const val Spelling = "settings/spelling"
        const val SpellingInfo = "settings/spelling/info"
        const val ManageSpellingDicts = "settings/spelling/manage-dicts"
        const val ImportSpellingArchive = "settings/spelling/import-archive"
        const val ImportSpellingAffDic = "settings/spelling/import-aff-dic"

        const val Dictionary = "settings/dictionary"

        const val Gestures = "settings/gestures"

        const val Clipboard = "settings/clipboard"

        const val Advanced = "settings/advanced"

        const val About = "settings/about"
        const val ProjectLicense = "settings/about/project-license"
        const val ThirdPartyLicenses = "settings/about/third-party-licenses"
    }

    object Devtools {
        const val Home = "devtools"

        const val AndroidLocales = "devtools/android/locales"
        const val AndroidSettings = "devtools/android/settings/{name}"
        fun AndroidSettings(name: String) = AndroidSettings.curlyFormat("name" to name)
    }

    object Ext {
        const val View = "ext/view/{id}"
        fun View(id: String) = View.curlyFormat("id" to id)
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
            composable(Splash.Screen) { SplashScreen() }

            composable(Setup.Screen) { SetupScreen() }

            composable(Settings.Home) { HomeScreen() }

            composable(Settings.Localization) { LocalizationScreen() }
            composable(Settings.SelectLocale) { SelectLocaleScreen() }
            composable(Settings.SubtypeAdd) { SubtypeEditorScreen(null) }
            composable(Settings.SubtypeEdit) { navBackStack ->
                val id = navBackStack.arguments?.getString("id")?.toLongOrNull()
                SubtypeEditorScreen(id)
            }

            composable(Settings.Theme) { ThemeScreen() }
            composable(Settings.ThemeSelect) { navBackStack ->
                val type = navBackStack.arguments?.getString("type")?.let { id ->
                    ThemeSelectScreenType.values().firstOrNull { it.id == id }
                }
                ThemeSelectScreen(type)
            }

            composable(Settings.Keyboard) { KeyboardScreen() }
            composable(Settings.InputFeedback) { InputFeedbackScreen() }

            composable(Settings.Smartbar) { SmartbarScreen() }

            composable(Settings.Typing) { TypingScreen() }

            composable(Settings.Spelling) { SpellingScreen() }
            composable(Settings.SpellingInfo) { SpellingInfoScreen() }
            composable(Settings.ManageSpellingDicts) { ManageSpellingDictsScreen() }
            composable(Settings.ImportSpellingArchive) { ImportSpellingArchiveScreen() }

            composable(Settings.Dictionary) { DictionaryScreen() }

            composable(Settings.Gestures) { GesturesScreen() }

            composable(Settings.Clipboard) { ClipboardScreen() }

            composable(Settings.Advanced) { AdvancedScreen() }

            composable(Settings.About) { AboutScreen() }
            composable(Settings.ProjectLicense) { ProjectLicenseScreen() }
            composable(Settings.ThirdPartyLicenses) { ThirdPartyLicensesScreen() }

            composable(Devtools.Home) { DevtoolsScreen() }
            composable(Devtools.AndroidLocales) { AndroidLocalesScreen() }
            composable(Devtools.AndroidSettings) { navBackStack ->
                val name = navBackStack.arguments?.getString("name")
                AndroidSettingsScreen(name)
            }

            composable(Ext.View) { navBackStack ->
                val extensionId = navBackStack.arguments?.getString("id")
                ExtensionViewScreen(id = extensionId.toString())
            }
        }
    }
}
