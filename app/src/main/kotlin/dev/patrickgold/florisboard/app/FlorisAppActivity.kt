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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.apptheme.FlorisAppTheme
import dev.patrickgold.florisboard.app.setup.NotificationPermissionState
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.android.AndroidVersion
import dev.patrickgold.florisboard.lib.android.hideAppIcon
import dev.patrickgold.florisboard.lib.android.setLocale
import dev.patrickgold.florisboard.lib.android.showAppIcon
import dev.patrickgold.florisboard.lib.compose.LocalPreviewFieldController
import dev.patrickgold.florisboard.lib.compose.PreviewKeyboardField
import dev.patrickgold.florisboard.lib.compose.ProvideLocalizedResources
import dev.patrickgold.florisboard.lib.compose.SystemUiApp
import dev.patrickgold.florisboard.lib.compose.rememberPreviewFieldController
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.util.AppVersionUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ProvideDefaultDialogPrefStrings

enum class AppTheme(val id: String) {
    AUTO("auto"),
    AUTO_AMOLED("auto_amoled"),
    LIGHT("light"),
    DARK("dark"),
    AMOLED_DARK("amoled_dark");
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("LocalNavController not initialized")
}

class FlorisAppActivity : ComponentActivity() {
    private val prefs by florisPreferenceModel()
    private var appTheme by mutableStateOf(AppTheme.AUTO)
    private var showAppIcon = true
    private var resourcesContext by mutableStateOf(this as Context)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen should be installed before calling super.onCreate()
        installSplashScreen().apply {
            setKeepOnScreenCondition { !prefs.datastoreReadyStatus.get() }
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        prefs.advanced.settingsTheme.observe(this) {
            appTheme = it
        }
        prefs.advanced.settingsLanguage.observe(this) {
            val config = Configuration(resources.configuration)
            config.setLocale(if (it == "auto") FlorisLocale.default() else FlorisLocale.fromTag(it))
            resourcesContext = createConfigurationContext(config)
        }
        if (AndroidVersion.ATMOST_API28_P) {
            prefs.advanced.showAppIcon.observe(this) {
                showAppIcon = it
            }
        }

        //Check if android 13+ is running and the NotificationPermission is not set
        if (AndroidVersion.ATLEAST_API33_T &&
            prefs.internal.notificationPermissionState.get() == NotificationPermissionState.NOT_SET
        ) {
            // update pref value to show the setup screen again again
            prefs.internal.isImeSetUp.set(false)
        }

        // We defer the setContent call until the datastore model is loaded, until then the splash screen stays drawn
        prefs.datastoreReadyStatus.observe(this) { isModelLoaded ->
            if (!isModelLoaded) return@observe
            AppVersionUtils.updateVersionOnInstallAndLastUse(this, prefs)
            setContent {
                ProvideLocalizedResources(resourcesContext) {
                    FlorisAppTheme(theme = appTheme) {
                        Surface(color = MaterialTheme.colors.background) {
                            SystemUiApp()
                            AppContent()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // App icon visibility control was restricted in Android 10.
        // See https://developer.android.com/reference/android/content/pm/LauncherApps#getActivityList(java.lang.String,%20android.os.UserHandle)
        if (AndroidVersion.ATMOST_API28_P) {
            if (showAppIcon) {
                this.showAppIcon()
            } else {
                this.hideAppIcon()
            }
        }
    }

    @Composable
    private fun AppContent() {
        val navController = rememberNavController()
        val previewFieldController = rememberPreviewFieldController()

        val isImeSetUp by prefs.internal.isImeSetUp.observeAsState()

        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalPreviewFieldController provides previewFieldController,
        ) {
            ProvideDefaultDialogPrefStrings(
                confirmLabel = stringRes(R.string.action__ok),
                dismissLabel = stringRes(R.string.action__cancel),
                neutralLabel = stringRes(R.string.action__default),
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    Routes.AppNavHost(
                        modifier = Modifier.weight(1.0f),
                        navController = navController,
                        startDestination = if (isImeSetUp) Routes.Settings.Home else Routes.Setup.Screen,
                    )
                    PreviewKeyboardField(previewFieldController)
                }
            }
        }

        SideEffect {
            navController.setOnBackPressedDispatcher(this.onBackPressedDispatcher)
        }
    }
}
