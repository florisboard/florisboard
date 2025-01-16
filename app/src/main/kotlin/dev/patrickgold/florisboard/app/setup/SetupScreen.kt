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

package dev.patrickgold.florisboard.app.setup

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.AppPrefs
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.lib.compose.FlorisBulletSpacer
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisScreenScope
import dev.patrickgold.florisboard.lib.compose.FlorisStep
import dev.patrickgold.florisboard.lib.compose.FlorisStepLayout
import dev.patrickgold.florisboard.lib.compose.FlorisStepState
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.florisboard.lib.util.launchActivity
import dev.patrickgold.florisboard.lib.util.launchUrl
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.PreferenceUiScope
import kotlinx.coroutines.delay
import org.florisboard.lib.android.AndroidVersion


@Composable
fun SetupScreen() = FlorisScreen {
    title = stringRes(R.string.setup__title)
    navigationIconVisible = false
    scrollable = false

    val navController = LocalNavController.current
    val context = LocalContext.current

    val prefs by florisPreferenceModel()

    val isFlorisBoardEnabled by InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
    val isFlorisBoardSelected by InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)
    val hasNotificationPermission by prefs.internal.notificationPermissionState.observeAsState()

    val requestNotification =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                prefs.internal.notificationPermissionState.set(NotificationPermissionState.GRANTED)
            } else {
                prefs.internal.notificationPermissionState.set(NotificationPermissionState.DENIED)
            }
        }

    content(
        isFlorisBoardEnabled,
        isFlorisBoardSelected,
        context,
        navController,
        requestNotification,
        hasNotificationPermission
    )
}

@Composable
private fun FlorisScreenScope.content(
    isFlorisBoardEnabled: Boolean,
    isFlorisBoardSelected: Boolean,
    context: Context,
    navController: NavController,
    requestNotification: ManagedActivityResultLauncher<String, Boolean>,
    hasNotificationPermission: NotificationPermissionState,
) {

    val stepState = rememberSaveable(saver = FlorisStepState.Saver) {
        val initStep = when {
            !isFlorisBoardEnabled -> Steps.EnableIme.id
            !isFlorisBoardSelected -> Steps.SelectIme.id
            hasNotificationPermission == NotificationPermissionState.NOT_SET && AndroidVersion.ATLEAST_API33_T -> Steps.SelectNotification.id
            else -> Steps.FinishUp.id
        }
        FlorisStepState.new(init = initStep)
    }

    content {
        LaunchedEffect(isFlorisBoardEnabled, isFlorisBoardSelected, hasNotificationPermission) {
            stepState.setCurrentAuto(
                when {
                    !isFlorisBoardEnabled -> Steps.EnableIme.id
                    !isFlorisBoardSelected -> Steps.SelectIme.id
                    hasNotificationPermission == NotificationPermissionState.NOT_SET && AndroidVersion.ATLEAST_API33_T -> Steps.SelectNotification.id
                    else -> Steps.FinishUp.id
                }
            )
        }

        // Below block allows to return from the system IME enabler activity
        // as soon as it gets selected.
        LaunchedEffect(Unit) {
            while (true) {
                delay(200L)
                val isEnabled = InputMethodUtils.isFlorisboardEnabled(context)
                if (stepState.getCurrentAuto().value == Steps.EnableIme.id &&
                    stepState.getCurrentManual().value == -1 &&
                    !isFlorisBoardEnabled &&
                    !isFlorisBoardSelected &&
                    hasNotificationPermission == NotificationPermissionState.NOT_SET &&
                    isEnabled
                ) {
                    context.launchActivity(FlorisAppActivity::class) {
                        it.flags = (Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                }
            }
        }
        FlorisStepLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            stepState = stepState,
            header = {
                StepText(stringRes(R.string.setup__intro_message))
                Spacer(modifier = Modifier.height(16.dp))
            },
            steps = steps(
                context, navController, requestNotification
            ),
            footer = {
                footer(context)
            },
        )
    }
}

@Composable
private fun footer(context: Context) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val privacyPolicyUrl = stringRes(R.string.florisboard__privacy_policy_url)
        TextButton(onClick = { context.launchUrl(privacyPolicyUrl) }) {
            Text(text = stringRes(R.string.setup__footer__privacy_policy))
        }
        FlorisBulletSpacer()
        val repositoryUrl = stringRes(R.string.florisboard__repo_url)
        TextButton(onClick = { context.launchUrl(repositoryUrl) }) {
            Text(text = stringRes(R.string.setup__footer__repository))
        }
    }
}

@Composable
private fun PreferenceUiScope<AppPrefs>.steps(
    context: Context,
    navController: NavController,
    requestNotification: ManagedActivityResultLauncher<String, Boolean>,
): List<FlorisStep> {

    return listOfNotNull(
        FlorisStep(
            id = Steps.EnableIme.id,
            title = stringRes(R.string.setup__enable_ime__title),
        ) {
            StepText(stringRes(R.string.setup__enable_ime__description))
            StepButton(label = stringRes(R.string.setup__enable_ime__open_settings_btn)) {
                InputMethodUtils.showImeEnablerActivity(context)
            }
        },
        FlorisStep(
            id = Steps.SelectIme.id,
            title = stringRes(R.string.setup__select_ime__title),
        ) {
            StepText(stringRes(R.string.setup__select_ime__description))
            StepButton(label = stringRes(R.string.setup__select_ime__switch_keyboard_btn)) {
                InputMethodUtils.showImePicker(context)
            }
        },
        if (AndroidVersion.ATLEAST_API33_T) {
            FlorisStep(
                id = Steps.SelectNotification.id,
                title = stringRes(R.string.setup__grant_notification_permission__title),
            ) {
                StepText(stringRes(R.string.setup__grant_notification_permission__description))
                StepButton(stringRes(R.string.setup__grant_notification_permission__btn)) {
                    requestNotification.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else null,
        FlorisStep(
            id = Steps.FinishUp.id,
            title = stringRes(R.string.setup__finish_up__title),
        ) {
            StepText(stringRes(R.string.setup__finish_up__description_p1))
            StepText(stringRes(R.string.setup__finish_up__description_p2))
            StepButton(label = stringRes(R.string.setup__finish_up__finish_btn)) {
                this@steps.prefs.internal.isImeSetUp.set(true)
                navController.navigate(Routes.Settings.Home) {
                    popUpTo(Routes.Setup.Screen) {
                        inclusive = true
                    }
                }
            }
        }
    )
}

private sealed class Steps(val id: Int) {
    data object EnableIme : Steps(id = 1)
    data object SelectIme : Steps(id = 2)
    data object SelectNotification : Steps(id = 3)
    data object FinishUp : Steps(id = 4)
}
