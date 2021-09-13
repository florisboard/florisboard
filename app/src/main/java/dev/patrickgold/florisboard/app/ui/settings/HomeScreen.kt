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

package dev.patrickgold.florisboard.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.launchActivity
import dev.patrickgold.florisboard.common.launchUrl
import dev.patrickgold.florisboard.oldsettings.SettingsMainActivity
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.ui.compose.Preference

@Composable
fun HomeScreen() = FlorisScreen(
    title = stringResource(R.string.settings__home__title, stringResource(R.string.floris_app_name)),
    backArrowVisible = false,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val isCollapsed by prefs.internal.homeIsBetaToolboxCollapsed.observeAsState(true)

    Card(modifier = Modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Beta-access to new Settings UI",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1.0f))
                IconButton(onClick = { this@FlorisScreen.prefs.internal.homeIsBetaToolboxCollapsed.set(!isCollapsed) }) {
                    Icon(
                        painter = painterResource(if (isCollapsed) {
                            R.drawable.ic_keyboard_arrow_down
                        } else {
                            R.drawable.ic_keyboard_arrow_up
                        }),
                        contentDescription = null,
                    )
                }
            }
            if (!isCollapsed) {
                Text("You are currently testing out the new Settings of FlorisBoard.\n")
                Text("Especially in the first few beta releases the Settings are completely split up and some UI controls (especially sliders!!) behave buggy. With each beta release preferences will be ported until everything is re-written, then the UI and the code base will get polished.\n")
                Text("If you want to give feedback on the development of the new prefs, please do so in below linked feedback thread:\n")
                Button(onClick = {
                    launchUrl(context, "https://github.com/florisboard/florisboard/discussions/1235")
                }) {
                    Text("Open Feedback Thread")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    launchActivity(context, SettingsMainActivity::class) { it.flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                }) {
                    Text("Open Old Settings")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    Preference(
        iconId = R.drawable.ic_build,
        title = stringResource(R.string.settings__advanced__title),
        onClick = { navController.navigate(Routes.Settings.Advanced) },
    )
    Preference(
        iconId = R.drawable.ic_info,
        title = stringResource(R.string.about__title),
        onClick = { navController.navigate(Routes.Settings.About) },
    )
}
