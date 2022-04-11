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

package dev.patrickgold.florisboard.app.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.lib.compose.FlorisCanvasIcon
import dev.patrickgold.florisboard.lib.compose.LocalPreviewFieldController
import dev.patrickgold.jetpref.datastore.model.observeAsState

@Composable
fun SplashScreen() = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
) {
    val prefs by florisPreferenceModel()
    val isModelLoaded by prefs.datastoreReadyStatus.observeAsState()
    val isImeSetUp by prefs.internal.isImeSetUp.observeAsState()
    val navController = LocalNavController.current
    val previewFieldController = LocalPreviewFieldController.current

    SideEffect {
        previewFieldController?.isVisible = false
    }

    LaunchedEffect(isModelLoaded) {
        if (isModelLoaded) {
            navController.navigate(if (isImeSetUp) Routes.Settings.Home else Routes.Setup.Screen) {
                popUpTo(Routes.Splash.Screen) {
                    inclusive = true
                }
            }
        }
    }

    FlorisCanvasIcon(
        modifier = Modifier.requiredSize(92.dp),
        iconId = R.mipmap.floris_app_icon,
        contentDescription = "FlorisBoard app icon",
    )
}
