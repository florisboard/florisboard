/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.setup

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.NoBackupPreferenceStore
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.components.LocalPreviewFieldController
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.florisboard.lib.util.launchActivity
import dev.patrickgold.florisboard.lib.util.launchUrl
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.FlorisBulletSpacer
import org.florisboard.lib.compose.stringRes

suspend fun Flow<Boolean>.awaitTransition() {
    var previous: Boolean? = null
    first { current ->
        val becameTrue = previous == false && current
        previous = current
        becameTrue
    }
}

private sealed class Steps(val id: Int) {
    data object Welcome : Steps(id = 0)
    data object EnableIme : Steps(id = 1)
    data object SelectIme : Steps(id = 2)
    data object NotificationPermission : Steps(id = 3)
    data object FinishUp: Steps(id = 4)
}

@Composable
fun SetupScreen(hasRestoredBackup: Boolean) {
    val prefs by FlorisPreferenceStore
    val noBackupPrefs by NoBackupPreferenceStore
    val context = LocalContext.current
    val navController = LocalNavController.current
    val previewFieldController = LocalPreviewFieldController.current
    val scope = rememberCoroutineScope()

    val isFlorisBoardEnabled by InputMethodUtils.observeIsFlorisboardEnabled()
    val isFlorisBoardSelected by InputMethodUtils.observeIsFlorisboardSelected()

    val notificationState by noBackupPrefs.notificationPermissionState.collectAsState()

    val requestNotification =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            scope.launch {
                noBackupPrefs.notificationPermissionState.set(
                    if (isGranted) {
                        NotificationPermissionState.GRANTED
                    } else {
                        NotificationPermissionState.DENIED
                    }
                )
            }
        }

    SideEffect {
        previewFieldController.isVisible = false
    }

    var welcomeSeen by rememberSaveable { mutableStateOf(hasRestoredBackup) }
    var notificationsSkipped by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        InputMethodUtils.observeIsFlorisboardEnabledFlow(context)
            .awaitTransition()
        context.launchActivity(FlorisAppActivity::class) {
            it.flags = (Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                or Intent.FLAG_ACTIVITY_SINGLE_TOP
                or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    val steps = remember(welcomeSeen, isFlorisBoardEnabled, isFlorisBoardSelected, notificationState, notificationsSkipped) {
        listOfNotNull(
            SetupStep(
                id = Steps.Welcome.id,
                required = hasRestoredBackup,
                satisfied = welcomeSeen,
                icon = Icons.Default.Keyboard,
                shape = SetupShapes.Welcome,
                title = { stringRes(R.string.setup__welcome__title) },
                summary = { stringRes(R.string.setup__welcome__summary) },
                primaryLabel = { stringRes(R.string.setup__welcome__get_started_btn) },
                onPrimary = {
                    welcomeSeen = true
                    next()
                },
                secondaryLabel = { stringRes(R.string.setup__welcome__restore_btn) },
                secondaryStyle = SetupSecondaryStyle.TONAL,
                onSecondary = {
                    navController.navigate(Routes.Settings.Restore(isSetup = true))
                },
                showProgress = false,
            ),
            SetupStep(
                id = Steps.EnableIme.id,
                required = true,
                satisfied = isFlorisBoardEnabled,
                icon = Icons.Default.Keyboard,
                shape = SetupShapes.Enable,
                title = { stringRes(R.string.setup__enable_ime__title) },
                summary = { stringRes(R.string.setup__enable_ime__summary) },
                description = { stringRes(R.string.setup__enable_ime__description) },
                primaryLabel = { stringRes(R.string.setup__enable_ime__open_settings_btn) },
                onPrimary = { InputMethodUtils.showImeEnablerActivity(context) },
            ),
            SetupStep(
                id = Steps.SelectIme.id,
                required = true,
                satisfied = isFlorisBoardSelected,
                icon = Icons.Default.SwapHoriz,
                shape = SetupShapes.Select,
                title = { stringRes(R.string.setup__select_ime__title) },
                summary = { stringRes(R.string.setup__select_ime__summary) },
                description = { stringRes(R.string.setup__select_ime__description) },
                primaryLabel = { stringRes(R.string.setup__select_ime__switch_keyboard_btn) },
                onPrimary = { InputMethodUtils.showImePicker(context) },
            ),
            if (AndroidVersion.ATLEAST_API33_T) {
                SetupStep(
                    id = Steps.NotificationPermission.id,
                    required = false,
                    satisfied = notificationState != NotificationPermissionState.NOT_SET || notificationsSkipped,
                    icon = Icons.Default.Notifications,
                    shape = SetupShapes.Notifications,
                    title = { stringRes(R.string.setup__grant_notification_permission__title) },
                    summary = { stringRes(R.string.setup__grant_notification_permission__description) },
                    description = { stringRes(R.string.setup__grant_notification_permission__description) },
                    primaryLabel = { stringRes(R.string.setup__grant_notification_permission__btn) },
                    onPrimary = {
                        scope.launch {
                            noBackupPrefs.notificationPermissionState.set(NotificationPermissionState.NOT_SET)
                        }
                        requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    secondaryLabel = { stringRes(R.string.setup__grant_notification_permission__skip_btn) },
                    onSecondary = {
                        notificationsSkipped = true
                        next()
                    },
                )
            } else null,
            SetupStep(
                id = Steps.FinishUp.id,
                required = false,
                satisfied = false,
                icon = Icons.Default.Check,
                shape = SetupShapes.Finish,
                title = { stringRes(R.string.setup__finish_up__title) },
                summary = { stringRes(R.string.setup__finish_up__summary) },
                primaryLabel = { stringRes(R.string.setup__finish_up__finish_btn) },
                onPrimary = {
                    scope.launch { noBackupPrefs.isImeSetUp.set(true) }
                    navController.navigate(Routes.Settings.Home) {
                        popUpTo(Routes.Setup.Screen::class) { inclusive = true }
                    }
                },
                showProgress = false,
            ),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        bottomBar = { SetupLinks() }
    ) { contentPadding ->
        SetupFlowLayout(
            steps = steps,
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
        )
    }
}


@Composable
private fun SetupLinks() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
    ) {
        val privacyPolicyUrl = stringRes(R.string.florisboard__privacy_policy_url)
        TextButton(onClick = { context.launchUrl(privacyPolicyUrl) }) {
            Text(text = stringRes(R.string.setup__footer__privacy_policy))
        }
        FlorisBulletSpacer()
        val repositoryUrl = stringRes(R.string.florisboard__repo_url)
        TextButton(onClick = { context.launchUrl(repositoryUrl) }) {
            Text(text = stringRes(R.string.setup__footer__source_code))
        }
    }
}
