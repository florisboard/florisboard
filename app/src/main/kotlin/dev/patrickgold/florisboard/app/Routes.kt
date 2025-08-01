/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import dev.patrickgold.florisboard.app.devtools.AndroidLocalesScreen
import dev.patrickgold.florisboard.app.devtools.AndroidSettingsScreen
import dev.patrickgold.florisboard.app.devtools.DevtoolsScreen
import dev.patrickgold.florisboard.app.devtools.ExportDebugLogScreen
import dev.patrickgold.florisboard.app.ext.CheckUpdatesScreen
import dev.patrickgold.florisboard.app.ext.ExtensionEditScreen
import dev.patrickgold.florisboard.app.ext.ExtensionExportScreen
import dev.patrickgold.florisboard.app.ext.ExtensionHomeScreen
import dev.patrickgold.florisboard.app.ext.ExtensionImportScreen
import dev.patrickgold.florisboard.app.ext.ExtensionImportScreenType
import dev.patrickgold.florisboard.app.ext.ExtensionListScreen
import dev.patrickgold.florisboard.app.ext.ExtensionListScreenType
import dev.patrickgold.florisboard.app.ext.ExtensionViewScreen
import dev.patrickgold.florisboard.app.settings.HomeScreen
import dev.patrickgold.florisboard.app.settings.about.AboutScreen
import dev.patrickgold.florisboard.app.settings.about.ProjectLicenseScreen
import dev.patrickgold.florisboard.app.settings.about.ThirdPartyLicensesScreen
import dev.patrickgold.florisboard.app.settings.advanced.BackupScreen
import dev.patrickgold.florisboard.app.settings.advanced.OtherScreen
import dev.patrickgold.florisboard.app.settings.advanced.PhysicalKeyboardScreen
import dev.patrickgold.florisboard.app.settings.advanced.RestoreScreen
import dev.patrickgold.florisboard.app.settings.clipboard.ClipboardScreen
import dev.patrickgold.florisboard.app.settings.dictionary.DictionaryScreen
import dev.patrickgold.florisboard.app.settings.dictionary.UserDictionaryScreen
import dev.patrickgold.florisboard.app.settings.dictionary.UserDictionaryType
import dev.patrickgold.florisboard.app.settings.gestures.GesturesScreen
import dev.patrickgold.florisboard.app.settings.keyboard.InputFeedbackScreen
import dev.patrickgold.florisboard.app.settings.keyboard.KeyboardScreen
import dev.patrickgold.florisboard.app.settings.localization.LanguagePackManagerScreen
import dev.patrickgold.florisboard.app.settings.localization.LanguagePackManagerScreenAction
import dev.patrickgold.florisboard.app.settings.localization.LocalizationScreen
import dev.patrickgold.florisboard.app.settings.localization.SelectLocaleScreen
import dev.patrickgold.florisboard.app.settings.localization.SubtypeEditorScreen
import dev.patrickgold.florisboard.app.settings.media.MediaScreen
import dev.patrickgold.florisboard.app.settings.smartbar.SmartbarScreen
import dev.patrickgold.florisboard.app.settings.theme.ThemeManagerScreen
import dev.patrickgold.florisboard.app.settings.theme.ThemeManagerScreenAction
import dev.patrickgold.florisboard.app.settings.theme.ThemeScreen
import dev.patrickgold.florisboard.app.settings.typing.TypingScreen
import dev.patrickgold.florisboard.app.setup.SetupScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

inline fun <reified T : Routes.SimpleRoute> NavGraphBuilder.composableWithDeepLink(
    route: T,
    noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit),
) {
    composable<T>(
        deepLinks = listOf(navDeepLink<T>(basePath = "ui://florisboard/${route.route}")),
        content = content,
    )
}

inline fun <reified T : Routes.ArgumentRoute> NavGraphBuilder.composableWithDeepLink(
    route: String,
    noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit),
) {
    composable<T>(
        deepLinks = listOf(navDeepLink<T>(basePath = "ui://florisboard/$route")),
        content = content,
    )
}

@Suppress("FunctionName", "ConstPropertyName")
sealed interface Routes {

    /**
     * Simple route that should be accessible with a deep link
     */
    sealed interface SimpleRoute : Routes {
        val route: String
    }

    /**
     * Route with arguments that should be accessible with a deep link
     */
    sealed interface ArgumentRoute : Routes

    object Setup {
        @Serializable
        object Screen : Routes
    }

    object Settings {
        @Serializable
        object Home : SimpleRoute {
            override val route = "settings/home"
        }

        @Serializable
        object Localization : SimpleRoute {
            override val route = "settings/localization"
        }

        @Serializable
        object SelectLocale : SimpleRoute {
            override val route = "settings/localization/select-locale"
        }

        @Serializable
        data class LanguagePackManager(val action: LanguagePackManagerScreenAction) : ArgumentRoute {
            companion object {
                const val route = "settings/localization/language-pack-manage"
            }
        }

        @Serializable
        object SubtypeAdd : SimpleRoute {
            override val route = "settings/localization/subtype/add"
        }

        @Serializable
        data class SubtypeEdit(val id: Long) : ArgumentRoute {
            companion object {
                const val route = "settings/localization/subtype/edit"
            }
        }

        @Serializable
        object Theme : SimpleRoute {
            override val route = "settings/theme"
        }

        @Serializable
        data class ThemeManager(val action: ThemeManagerScreenAction) : ArgumentRoute {
            companion object {
                const val route = "settings/theme/manage"
            }
        }

        @Serializable
        object Keyboard : SimpleRoute {
            override val route = "settings/keyboard"
        }

        @Serializable
        object InputFeedback : SimpleRoute {
            override val route = "settings/keyboard/input-feedback"
        }

        @Serializable
        object Smartbar : SimpleRoute {
            override val route = "settings/smartbar"
        }

        @Serializable
        object Typing : SimpleRoute {
            override val route = "settings/typing"
        }

        @Serializable
        object Dictionary : SimpleRoute {
            override val route = "settings/dictionary"
        }

        @Serializable
        data class UserDictionary(val type: UserDictionaryType) : ArgumentRoute {
            companion object {
                const val route = "settings/dictionary/user-dictionary"
            }
        }

        @Serializable
        object Gestures : SimpleRoute {
            override val route = "settings/gestures"
        }

        @Serializable
        object Clipboard : SimpleRoute {
            override val route = "settings/clipboard"
        }

        @Serializable
        object Media : SimpleRoute {
            override val route = "settings/media"
        }

        @Serializable
        object Other : SimpleRoute {
            override val route = "settings/other"
        }

        @Serializable
        object PhysicalKeyboard : SimpleRoute {
            override val route = "settings/other/physical-keyboard"
        }

        @Serializable
        object Backup : SimpleRoute {
            override val route = "settings/other/backup"
        }

        @Serializable
        object Restore : SimpleRoute {
            override val route = "settings/other/restore"
        }

        @Serializable
        object About : SimpleRoute {
            override val route = "settings/about"
        }

        @Serializable
        object ProjectLicense : SimpleRoute {
            override val route = "settings/about/project-license"
        }

        @Serializable
        object ThirdPartyLicenses : SimpleRoute {
            override val route = "settings/about/third-party-licenses"
        }
    }

    object Devtools {
        @Serializable
        object Home : SimpleRoute {
            override val route = "devtools"
        }

        @Serializable
        object AndroidLocales : SimpleRoute {
            override val route = "devtools/android/locales"
        }

        @Serializable
        data class AndroidSettings(val name: String) : ArgumentRoute {
            companion object {
                const val route = "devtools/android/settings"
            }
        }

        @Serializable
        object ExportDebugLog : SimpleRoute {
            override val route = "export-debug-log"
        }
    }

    object Ext {
        @Serializable
        object Home : SimpleRoute {
            override val route = "ext"
        }

        @Serializable
        data class List(val type: ExtensionListScreenType, val showUpdate: Boolean? = null) : ArgumentRoute {
            companion object {
                const val route = "ext/list"
            }
        }


        @Serializable
        data class Edit(val id: String, val serialType: String? = null) : ArgumentRoute {
            companion object {
                const val route = "ext/edit"
            }
        }

        @Serializable
        data class Export(val id: String) : ArgumentRoute {
            companion object {
                const val route = "ext/export"
            }
        }

        @Serializable
        data class Import(val type: ExtensionImportScreenType, val uuid: String? = null) : ArgumentRoute {
            companion object {
                const val route = "ext/import"
            }
        }

        @Serializable
        data class View(val id: String) : ArgumentRoute {
            companion object {
                const val route = "ext/view"
            }
        }

        @Serializable
        object CheckUpdates : SimpleRoute {
            override val route = "ext/check-updates"
        }
    }

    companion object {
        @Composable
        fun AppNavHost(
            modifier: Modifier,
            navController: NavHostController,
            startDestination: KClass<*>,
        ) {
            NavHost(
                modifier = modifier,
                navController = navController,
                startDestination = startDestination,
                enterTransition = {
                    slideIn { IntOffset(it.width, 0) } + fadeIn()
                },
                exitTransition = {
                    slideOut { IntOffset(-it.width, 0) } + fadeOut()
                },
                popEnterTransition = {
                    slideIn { IntOffset(-it.width, 0) } + fadeIn()
                },
                popExitTransition = {
                    slideOut { IntOffset(it.width, 0) } + fadeOut()
                }
            ) {
                composable<Setup.Screen> { SetupScreen() }

                composableWithDeepLink(Settings.Home) { HomeScreen() }

                composableWithDeepLink(Settings.Localization) { LocalizationScreen() }
                composableWithDeepLink(Settings.SelectLocale) { SelectLocaleScreen() }
                composableWithDeepLink<Settings.LanguagePackManager>(Settings.LanguagePackManager.route) { navBackStack ->
                    val languagePackManager = navBackStack.toRoute<Settings.LanguagePackManager>()
                    val action = languagePackManager.action.let { actionId ->
                        LanguagePackManagerScreenAction.entries.first { it == actionId }
                    }
                    LanguagePackManagerScreen(action)
                }
                composableWithDeepLink(Settings.SubtypeAdd) { SubtypeEditorScreen(null) }
                composableWithDeepLink<Settings.SubtypeEdit>(Settings.SubtypeEdit.route) { navBackStack ->
                    val subtypeEdit = navBackStack.toRoute<Settings.SubtypeEdit>()
                    SubtypeEditorScreen(subtypeEdit.id)
                }

                composableWithDeepLink(Settings.Theme) { ThemeScreen() }
                composableWithDeepLink<Settings.ThemeManager>(Settings.ThemeManager.route) { navBackStack ->
                    val themeManager = navBackStack.toRoute<Settings.ThemeManager>()
                    val action = themeManager.action.let { action ->
                        ThemeManagerScreenAction.entries.firstOrNull { it.id == action.id }
                    }
                    ThemeManagerScreen(action)
                }

                composableWithDeepLink(Settings.Keyboard) { KeyboardScreen() }
                composableWithDeepLink(Settings.InputFeedback) { InputFeedbackScreen() }

                composableWithDeepLink(Settings.Smartbar) { SmartbarScreen() }

                composableWithDeepLink(Settings.Typing) { TypingScreen() }

                composableWithDeepLink(Settings.Dictionary) { DictionaryScreen() }
                composableWithDeepLink<Settings.UserDictionary>(Settings.UserDictionary.route) { navBackStack ->
                    val userDictionary = navBackStack.toRoute<Settings.UserDictionary>()
                    val type = userDictionary.type.let { type ->
                        UserDictionaryType.entries.first { it.id == type.id }
                    }
                    UserDictionaryScreen(type)
                }

                composableWithDeepLink(Settings.Gestures) { GesturesScreen() }

                composableWithDeepLink(Settings.Clipboard) { ClipboardScreen() }

                composableWithDeepLink(Settings.Media) { MediaScreen() }

                composableWithDeepLink(Settings.Other) { OtherScreen() }
                composableWithDeepLink(Settings.PhysicalKeyboard) { PhysicalKeyboardScreen() }
                composableWithDeepLink(Settings.Backup) { BackupScreen() }
                composableWithDeepLink(Settings.Restore) { RestoreScreen() }

                composableWithDeepLink(Settings.About) { AboutScreen() }
                composableWithDeepLink(Settings.ProjectLicense) { ProjectLicenseScreen() }
                composableWithDeepLink(Settings.ThirdPartyLicenses) { ThirdPartyLicensesScreen() }

                composableWithDeepLink(Devtools.Home) { DevtoolsScreen() }
                composableWithDeepLink(Devtools.AndroidLocales) { AndroidLocalesScreen() }
                composableWithDeepLink<Devtools.AndroidSettings>(Devtools.AndroidSettings.route) { navBackStack ->
                    val androidSettings = navBackStack.toRoute<Devtools.AndroidSettings>()
                    AndroidSettingsScreen(androidSettings.name)
                }
                composableWithDeepLink(Devtools.ExportDebugLog) { ExportDebugLogScreen() }

                composableWithDeepLink(Ext.Home) { ExtensionHomeScreen() }
                composableWithDeepLink<Ext.List>(Ext.List.route) { navBackStack ->
                    val list = navBackStack.toRoute<Ext.List>()
                    val type = list.let { list ->
                        ExtensionListScreenType.entries.first { it.id == list.type.id}
                    }
                    val showUpdate = list.showUpdate != null && list.showUpdate
                    ExtensionListScreen(type, showUpdate)
                }
                composableWithDeepLink<Ext.Edit>(Ext.Edit.route) { navBackStack ->
                    val edit = navBackStack.toRoute<Ext.Edit>()
                    val extensionId = edit.id
                    val serialType = edit.serialType
                    ExtensionEditScreen(
                        id = extensionId,
                        createSerialType = serialType.takeIf { !it.isNullOrBlank() },
                    )
                }
                composableWithDeepLink<Ext.Export>(Ext.Export.route) { navBackStack ->
                    val export = navBackStack.toRoute<Ext.Export>()
                    val extensionId = export.id
                    ExtensionExportScreen(id = extensionId)
                }
                composableWithDeepLink<Ext.Import>(Ext.Import.route) { navBackStack ->
                    val import = navBackStack.toRoute<Ext.Import>()
                    val type = import.type.let { type ->
                        ExtensionImportScreenType.entries.first { it.id == type.id }
                    }
                    val uuid = import.uuid
                    ExtensionImportScreen(type, uuid)
                }
                composableWithDeepLink<Ext.View>(Ext.View.route) { navBackStack ->
                    val view = navBackStack.toRoute<Ext.View>()
                    val extensionId = view.id
                    ExtensionViewScreen(id = extensionId)
                }
                composableWithDeepLink(Ext.CheckUpdates) {
                    CheckUpdatesScreen()
                }
            }
        }
    }
}
