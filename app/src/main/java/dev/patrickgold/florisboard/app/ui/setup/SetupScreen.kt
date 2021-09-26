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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalIsFlorisBoardEnabled
import dev.patrickgold.florisboard.app.LocalIsFlorisBoardSelected
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.res.stringRes
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
    var currentStep by rememberSaveable {
        val initStep = when {
            !isFlorisBoardEnabled -> Step.EnableIme
            !isFlorisBoardSelected -> Step.SelectIme
            else -> Step.FinishUp
        }
        mutableStateOf(initStep)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        SetupText(stringRes(R.string.setup__intro_message))
        Spacer(modifier = Modifier.height(16.dp))
        Step(
            ownStep = Step.EnableIme,
            currentStep = currentStep,
            title = stringRes(R.string.setup__enable_ime__title)
        ) {
            SetupText(stringRes(R.string.setup__enable_ime__description))
            SetupButton(label = stringRes(R.string.setup__enable_ime__open_settings_btn)) {
                InputMethodUtils.showImeEnablerActivity(context)
            }
        }
        Step(
            ownStep = Step.SelectIme,
            currentStep = currentStep,
            title = stringRes(R.string.setup__select_ime__title)
        ) {
            SetupText(stringRes(R.string.setup__select_ime__description))
            SetupButton(label = stringRes(R.string.setup__select_ime__switch_keyboard_btn)) {
                InputMethodUtils.showImePicker(context)
            }
        }
        Step(
            ownStep = Step.FinishUp,
            currentStep = currentStep,
            title = stringRes(R.string.setup__finish_up__title)
        ) {
            SetupText(stringRes(R.string.setup__finish_up__description_p1))
            SetupText(stringRes(R.string.setup__finish_up__description_p2))
            SetupButton(label = stringRes(R.string.setup__finish_up__finish_btn)) {
                this@FlorisScreen.prefs.internal.isImeSetUp.set(true)
                navController.navigate(Routes.Settings.Home)
            }
        }
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
        SideEffect {
            currentStep = when {
                !isFlorisBoardEnabled -> Step.EnableIme
                !isFlorisBoardSelected -> Step.SelectIme
                else -> Step.FinishUp
            }
        }
    }
}

@Composable
fun SetupText(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Justify,
    )
}

@Composable
fun ColumnScope.SetupButton(label: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 16.dp),
        onClick = onClick,
    ) {
        Text(text = label)
    }
}

@Composable
internal fun ColumnScope.Step(
    ownStep: Int,
    currentStep: Int,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backgroundColor = when (ownStep) {
        currentStep -> MaterialTheme.colors.primary
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    }
    if (ownStep == currentStep) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            StepHeader(
                backgroundColor = backgroundColor,
                step = ownStep,
                title = title,
            )
            Box(modifier = Modifier.padding(start = 56.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    } else {
        StepHeader(
            backgroundColor = backgroundColor,
            step = ownStep,
            title = title,
        )
    }
}

@Composable
fun StepHeader(
    backgroundColor: Color,
    contentColor: Color = contentColorFor(backgroundColor),
    step: Int,
    title: String,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = step.toString(),
                color = contentColor,
            )
        }

        Box(
            modifier = Modifier
                .height(32.dp)
                .weight(1.0f)
                .clip(CircleShape)
                .background(backgroundColor),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = title,
                color = contentColor,
            )
        }
    }
}
