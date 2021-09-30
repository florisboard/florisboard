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
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
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
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.ProvideLocalizedResources
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.PreviewKeyboardField
import dev.patrickgold.florisboard.app.ui.components.SystemUi
import dev.patrickgold.florisboard.app.ui.theme.FlorisAppTheme
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.common.InputMethodUtils
import dev.patrickgold.florisboard.common.SystemSettingsObserver
import dev.patrickgold.florisboard.util.AndroidVersion
import dev.patrickgold.florisboard.util.PackageManagerUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.ui.compose.ProvideDefaultDialogPrefStrings

enum class AppTheme(val id: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark"),
    AMOLED_DARK("amoled_dark"),
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("LocalNavController not initialized")
}

val LocalIsFlorisBoardEnabled = compositionLocalOf { false }
val LocalIsFlorisBoardSelected = compositionLocalOf { false }

class FlorisAppActivity : ComponentActivity() {
    private val prefs by florisPreferenceModel()
    private var isDatastoreReady by mutableStateOf(false)
    private var appTheme by mutableStateOf(AppTheme.AUTO)
    private var showAppIcon = true
    private var resourcesContext by mutableStateOf(this as Context)

    private var isFlorisBoardEnabled by mutableStateOf(false)
    private var isFlorisBoardSelected by mutableStateOf(false)

    private val isFlorisBoardEnabledObserver by lazy {
        SystemSettingsObserver(this) {
            isFlorisBoardEnabled = InputMethodUtils.checkIsFlorisboardEnabled(this@FlorisAppActivity)
        }
    }
    private val isFlorisBoardSelectedObserver by lazy {
        SystemSettingsObserver(this) {
            isFlorisBoardSelected = InputMethodUtils.checkIsFlorisboardSelected(this@FlorisAppActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        InputMethodUtils.startObserveIsFlorisBoardEnabled(this, isFlorisBoardEnabledObserver)
        InputMethodUtils.startObserveIsFlorisBoardSelected(this, isFlorisBoardSelectedObserver)

        prefs.datastoreReadyStatus.observe(this) {
            isDatastoreReady = it
        }
        prefs.advanced.settingsTheme.observe(this) {
            appTheme = it
        }
        prefs.advanced.settingsLanguage.observe(this) {
            val config = Configuration(resources.configuration)
            config.setLocale(if (it == "auto") FlorisLocale.default() else FlorisLocale.fromTag(it))
            resourcesContext = createConfigurationContext(config)
        }
        if (AndroidVersion.ATMOST_P) {
            prefs.advanced.showAppIcon.observe(this) {
                showAppIcon = it
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            ProvideLocalizedResources(resourcesContext) {
                FlorisAppTheme(theme = appTheme) {
                    Surface(color = MaterialTheme.colors.background) {
                        SystemUi()
                        if (isDatastoreReady) {
                            AppContent()
                        }
                    }
                }
            }
        }

        // PreDraw observer for SplashScreen
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (isDatastoreReady) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )
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

    override fun onDestroy() {
        super.onDestroy()

        prefs.forceSyncToDisk()
        InputMethodUtils.stopObserveIsFlorisBoardEnabled(this, isFlorisBoardEnabledObserver)
        InputMethodUtils.stopObserveIsFlorisBoardSelected(this, isFlorisBoardSelectedObserver)
    }

    private fun Configuration.setLocale(locale: FlorisLocale) {
        return this.setLocale(locale.base)
    }

    @Composable
    private fun AppContent() {
        val isImeSetUp by prefs.internal.isImeSetUp.observeAsState()
        val navController = rememberNavController()
        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalIsFlorisBoardEnabled provides isFlorisBoardEnabled,
            LocalIsFlorisBoardSelected provides isFlorisBoardSelected,
        ) {
            ProvideDefaultDialogPrefStrings(
                confirmLabel = stringRes(R.string.assets__action__ok),
                dismissLabel = stringRes(R.string.assets__action__cancel),
                neutralLabel = stringRes(R.string.assets__action__default),
            ) {
                Column {
                    Routes.AppNavHost(
                        modifier = Modifier.weight(1.0f),
                        navController = navController,
                        startDestination = if (isImeSetUp) { Routes.Settings.Home } else { Routes.Setup.Home },
                    )
                    if (isImeSetUp) {
                        PreviewKeyboardField()
                    }
                }
            }
        }
        SideEffect {
            navController.setOnBackPressedDispatcher(this.onBackPressedDispatcher)
        }
    }
}
