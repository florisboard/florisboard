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

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.PreviewKeyboardField
import dev.patrickgold.florisboard.app.ui.components.SystemUi
import dev.patrickgold.florisboard.app.ui.settings.AdvancedScreen
import dev.patrickgold.florisboard.app.ui.settings.HomeScreen
import dev.patrickgold.florisboard.app.ui.settings.about.AboutScreen
import dev.patrickgold.florisboard.app.ui.settings.about.ProjectLicenseScreen
import dev.patrickgold.florisboard.app.ui.settings.about.ThirdPartyLicensesScreen
import dev.patrickgold.florisboard.app.ui.theme.FlorisAppTheme
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.util.AndroidVersion
import dev.patrickgold.florisboard.util.PackageManagerUtils
import java.util.*

enum class AppTheme(val id: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark"),
    AMOLED_DARK("amoled_dark"),
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("LocalNavController not initialized")
}

class FlorisAppActivity : ComponentActivity() {
    private val prefs by florisPreferenceModel()
    private var appTheme by mutableStateOf(AppTheme.AUTO)
    private var showAppIcon = true
    private var appContext by mutableStateOf(this as Context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs.advanced.settingsTheme.observe(this) {
            appTheme = it
        }
        prefs.advanced.settingsLanguage.observe(this) {
            val config = Configuration(resources.configuration)
            config.setLocale(if (it == "auto") FlorisLocale.default() else FlorisLocale.fromTag(it))
            appContext = createConfigurationContext(config)
        }
        if (AndroidVersion.ATMOST_P) {
            prefs.advanced.showAppIcon.observe(this) {
                showAppIcon = it
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            CompositionLocalProvider(LocalContext provides appContext) {
                FlorisAppTheme(theme = appTheme) {
                    Surface(color = MaterialTheme.colors.background) {
                        SystemUi()
                        AppContent()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // App icon visibility control was restricted in Android 10.
        // See https://developer.android.com/reference/android/content/pm/LauncherApps#getActivityList(java.lang.String,%20android.os.UserHandle)
        if (AndroidVersion.ATMOST_P) {
            if (showAppIcon) {
                PackageManagerUtils.showAppIcon(this)
            } else {
                PackageManagerUtils.hideAppIcon(this)
            }
        } else {
            PackageManagerUtils.showAppIcon(this)
        }
    }

    override fun onBackPressed() {
        // TODO: implement nav stack pop
    }

    private fun Configuration.setLocale(locale: FlorisLocale) {
        return this.setLocale(locale.base)
    }
}

@Composable
private fun AppContent() {
    val navController = rememberNavController()
    CompositionLocalProvider(
        LocalNavController provides navController,
    ) {
        Column {
            NavHost(
                modifier = Modifier.weight(1.0f),
                navController = navController,
                startDestination = Routes.Settings.Home,
            ) {
                composable(Routes.Settings.Home) { HomeScreen() }

                composable(Routes.Settings.Advanced) { AdvancedScreen() }

                composable(Routes.Settings.About) { AboutScreen() }
                composable(Routes.Settings.ProjectLicense) { ProjectLicenseScreen() }
                composable(Routes.Settings.ThirdPartyLicenses) { ThirdPartyLicensesScreen() }
            }
            PreviewKeyboardField()
        }
    }
}
