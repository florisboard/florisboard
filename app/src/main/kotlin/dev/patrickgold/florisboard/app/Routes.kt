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

package dev.patrickgold.florisboard.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.patrickgold.florisboard.app.devtools.AndroidLocalesScreen
import dev.patrickgold.florisboard.app.devtools.AndroidSettingsScreen
import dev.patrickgold.florisboard.app.devtools.DevtoolsScreen
import dev.patrickgold.florisboard.app.ext.ExtensionEditScreen
import dev.patrickgold.florisboard.app.ext.ExtensionExportScreen
import dev.patrickgold.florisboard.app.ext.ExtensionImportScreen
import dev.patrickgold.florisboard.app.ext.ExtensionImportScreenType
import dev.patrickgold.florisboard.app.ext.ExtensionViewScreen
import dev.patrickgold.florisboard.app.settings.HomeScreen
import dev.patrickgold.florisboard.app.settings.about.AboutScreen
import dev.patrickgold.florisboard.app.settings.about.ProjectLicenseScreen
import dev.patrickgold.florisboard.app.settings.about.ThirdPartyLicensesScreen
import dev.patrickgold.florisboard.app.settings.advanced.AdvancedScreen
import dev.patrickgold.florisboard.app.settings.advanced.BackupScreen
import dev.patrickgold.florisboard.app.settings.advanced.RestoreScreen
import dev.patrickgold.florisboard.app.settings.clipboard.ClipboardScreen
import dev.patrickgold.florisboard.app.settings.dictionary.DictionaryScreen
import dev.patrickgold.florisboard.app.settings.dictionary.UserDictionaryScreen
import dev.patrickgold.florisboard.app.settings.dictionary.UserDictionaryType
import dev.patrickgold.florisboard.app.settings.gestures.GesturesScreen
import dev.patrickgold.florisboard.app.settings.keyboard.InputFeedbackScreen
import dev.patrickgold.florisboard.app.settings.keyboard.KeyboardScreen
import dev.patrickgold.florisboard.app.settings.localization.LocalizationScreen
import dev.patrickgold.florisboard.app.settings.localization.SelectLocaleScreen
import dev.patrickgold.florisboard.app.settings.localization.SubtypeEditorScreen
import dev.patrickgold.florisboard.app.settings.media.MediaScreen
import dev.patrickgold.florisboard.app.settings.smartbar.SmartbarScreen
import dev.patrickgold.florisboard.app.settings.spelling.ImportSpellingArchiveScreen
import dev.patrickgold.florisboard.app.settings.spelling.ManageSpellingDictsScreen
import dev.patrickgold.florisboard.app.settings.spelling.SpellingInfoScreen
import dev.patrickgold.florisboard.app.settings.spelling.SpellingScreen
import dev.patrickgold.florisboard.app.settings.theme.ThemeManagerScreen
import dev.patrickgold.florisboard.app.settings.theme.ThemeManagerScreenAction
import dev.patrickgold.florisboard.app.settings.theme.ThemeScreen
import dev.patrickgold.florisboard.app.settings.typing.TypingScreen
import dev.patrickgold.florisboard.app.setup.SetupScreen
import dev.patrickgold.florisboard.app.splash.SplashScreen
import dev.patrickgold.florisboard.lib.kotlin.curlyFormat

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
        const val ThemeManager = "settings/theme/manage/{action}"
        fun ThemeManager(action: ThemeManagerScreenAction) = ThemeManager.curlyFormat("action" to action.id)

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
        const val UserDictionary = "settings/dictionary/user-dictionary/{type}"
        fun UserDictionary(type: UserDictionaryType) = UserDictionary.curlyFormat("type" to type.id)

        const val Gestures = "settings/gestures"

        const val Clipboard = "settings/clipboard"

        const val Media = "settings/media"

        const val Advanced = "settings/advanced"
        const val Backup = "settings/advanced/backup"
        const val Restore = "settings/advanced/restore"

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
        const val Edit = "ext/edit/{id}?create={serial_type}"
        fun Edit(id: String, serialType: String? = null): String {
            return Edit.curlyFormat("id" to id, "serial_type" to (serialType ?: ""))
        }

        const val Export = "ext/export/{id}"
        fun Export(id: String) = Export.curlyFormat("id" to id)

        const val Import = "ext/import/{type}?uuid={uuid}"
        fun Import(
            type: ExtensionImportScreenType,
            uuid: String?,
        ) = Import.curlyFormat("type" to type.id, "uuid" to uuid.toString())

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
            composable(Settings.ThemeManager) { navBackStack ->
                val action = navBackStack.arguments?.getString("action")?.let { actionId ->
                    ThemeManagerScreenAction.values().firstOrNull { it.id == actionId }
                }
                ThemeManagerScreen(action)
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
            composable(Settings.UserDictionary) { navBackStack ->
                val type = navBackStack.arguments?.getString("type")?.let { typeId ->
                    UserDictionaryType.values().firstOrNull { it.id == typeId }
                }
                UserDictionaryScreen(type!!)
            }

            composable(Settings.Gestures) { GesturesScreen() }

            composable(Settings.Clipboard) { ClipboardScreen() }

            composable(Settings.Media) { MediaScreen() }

            composable(Settings.Advanced) { AdvancedScreen() }
            composable(Settings.Backup) { BackupScreen() }
            composable(Settings.Restore) { RestoreScreen() }

            composable(Settings.About) { AboutScreen() }
            composable(Settings.ProjectLicense) { ProjectLicenseScreen() }
            composable(Settings.ThirdPartyLicenses) { ThirdPartyLicensesScreen() }

            composable(Devtools.Home) { DevtoolsScreen() }
            composable(Devtools.AndroidLocales) { AndroidLocalesScreen() }
            composable(Devtools.AndroidSettings) { navBackStack ->
                val name = navBackStack.arguments?.getString("name")
                AndroidSettingsScreen(name)
            }

            composable(Ext.Edit) { navBackStack ->
                val extensionId = navBackStack.arguments?.getString("id")
                val serialType = navBackStack.arguments?.getString("serial_type")
                ExtensionEditScreen(
                    id = extensionId.toString(),
                    createSerialType = serialType.takeIf { it != null && it.isNotBlank() },
                )
            }
            composable(Ext.Export) { navBackStack ->
                val extensionId = navBackStack.arguments?.getString("id")
                ExtensionExportScreen(id = extensionId.toString())
            }
            composable(Ext.Import) { navBackStack ->
                val type = navBackStack.arguments?.getString("type")?.let { typeId ->
                    ExtensionImportScreenType.values().firstOrNull { it.id == typeId }
                } ?: ExtensionImportScreenType.EXT_ANY
                val uuid = navBackStack.arguments?.getString("uuid")?.takeIf { it != "null" }
                ExtensionImportScreen(type, uuid)
            }
            composable(Ext.View) { navBackStack ->
                val extensionId = navBackStack.arguments?.getString("id")
                ExtensionViewScreen(id = extensionId.toString())
            }
        }
    }
}
