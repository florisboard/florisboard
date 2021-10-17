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

package dev.patrickgold.florisboard.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalIsFlorisBoardEnabled
import dev.patrickgold.florisboard.app.LocalIsFlorisBoardSelected
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.FlorisStep
import dev.patrickgold.florisboard.app.ui.components.FlorisStepLayout
import dev.patrickgold.florisboard.app.ui.components.FlorisStepState
import dev.patrickgold.florisboard.common.InputMethodUtils
import dev.patrickgold.florisboard.common.launchUrl

private object Step {
    const val EnableIme: Int = 1
    const val SelectIme: Int = 2
    const val FinishUp: Int = 3
}

@Composable
fun SetupScreen() = FlorisScreen(
    title = stringRes(R.string.setup__title),
    backArrowVisible = false,
    scrollable = false,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    val isFlorisBoardEnabled = LocalIsFlorisBoardEnabled.current
    val isFlorisBoardSelected = LocalIsFlorisBoardSelected.current
    val stepState = rememberSaveable(saver = FlorisStepState.Saver) {
        val initStep = when {
            !isFlorisBoardEnabled -> Step.EnableIme
            !isFlorisBoardSelected -> Step.SelectIme
            else -> Step.FinishUp
        }
        FlorisStepState.new(init = initStep)
    }

    SideEffect {
        stepState.setCurrentAuto(when {
            !isFlorisBoardEnabled -> Step.EnableIme
            !isFlorisBoardSelected -> Step.SelectIme
            else -> Step.FinishUp
        })
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
        steps = listOf(
            FlorisStep(
                id = Step.EnableIme,
                title = stringRes(R.string.setup__enable_ime__title),
            ) {
                StepText(stringRes(R.string.setup__enable_ime__description))
                StepButton(label = stringRes(R.string.setup__enable_ime__open_settings_btn)) {
                    InputMethodUtils.showImeEnablerActivity(context)
                }
            },
            FlorisStep(
                id = Step.SelectIme,
                title = stringRes(R.string.setup__select_ime__title),
            ) {
                StepText(stringRes(R.string.setup__select_ime__description))
                StepButton(label = stringRes(R.string.setup__select_ime__switch_keyboard_btn)) {
                    InputMethodUtils.showImePicker(context)
                }
            },
            FlorisStep(
                id = Step.FinishUp,
                title = stringRes(R.string.setup__finish_up__title),
            ) {
                StepText(stringRes(R.string.setup__finish_up__description_p1))
                StepText(stringRes(R.string.setup__finish_up__description_p2))
                StepButton(label = stringRes(R.string.setup__finish_up__finish_btn)) {
                    this@FlorisScreen.prefs.internal.isImeSetUp.set(true)
                    navController.navigate(Routes.Settings.Home) {
                        popUpTo(Routes.Setup.Screen) {
                            inclusive = true
                        }
                    }
                }
            },
        ),
        footer = {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                val privacyPolicyUrl = stringRes(R.string.florisboard__privacy_policy_url)
                TextButton(onClick = { launchUrl(context, privacyPolicyUrl)}) {
                    Text(text = stringRes(R.string.setup__footer__privacy_policy))
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(12.dp, 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                )
                val repositoryUrl = stringRes(R.string.florisboard__repo_url)
                TextButton(onClick = { launchUrl(context, repositoryUrl) }) {
                    Text(text = stringRes(R.string.setup__footer__repository))
                }
            }
        },
    )
}
