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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.AppPrefs
import dev.patrickgold.florisboard.common.launchUrl
import dev.patrickgold.jetpref.ui.compose.Preference
import dev.patrickgold.jetpref.ui.compose.PreferenceScreen

@Composable
fun AboutScreen() = PreferenceScreen(::AppPrefs) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 20.dp, bottom = 8.dp)
    ) {
        // TODO: show app icon without the about screen crashing
        //Image(
        //    painter = painterResource(id = R.mipmap.floris_app_icon),
        //    contentDescription = stringResource(id = R.string.about__app_icon_content_description),
        //)
        Text(
            text = stringResource(id = R.string.floris_app_name),
            fontSize = 24.sp,
        )
    }
    Preference(
        iconId = R.drawable.ic_info,
        title = stringResource(id = R.string.about__version__title),
        summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        onClick = { /**/ },
    )
    Preference(
        iconId = R.drawable.ic_history,
        title = stringResource(id = R.string.about__changelog__title),
        summary = stringResource(id = R.string.about__changelog__summary),
        onClick = { launchUrl(context, R.string.florisboard__changelog_url, arrayOf(BuildConfig.VERSION_NAME)) },
    )
    Preference(
        iconId = R.drawable.ic_code,
        title = stringResource(id = R.string.about__repository__title),
        summary = stringResource(id = R.string.about__repository__summary),
        onClick = { launchUrl(context, R.string.florisboard__repo_url) },
    )
    Preference(
        iconId = R.drawable.ic_policy,
        title = stringResource(id = R.string.about__privacy_policy__title),
        summary = stringResource(id = R.string.about__privacy_policy__summary),
        onClick = { launchUrl(context, R.string.florisboard__privacy_policy_url) },
    )
    Preference(
        iconId = R.drawable.ic_description,
        title = stringResource(id = R.string.about__project_license__title),
        summary = stringResource(id = R.string.about__project_license__summary, "Apache 2.0"),
        onClick = { /**/ },
    )
    Preference(
        iconId = R.drawable.ic_description,
        title = stringResource(id = R.string.about__third_party_licenses__title),
        summary = stringResource(id = R.string.about__third_party_licenses__summary),
        onClick = { /**/ },
    )
}

@Composable
private fun ProjectLicenseDialog() {
    AlertDialog(
        onDismissRequest = { },
        text = { Text(text = "Hello") },
        confirmButton = {
            Button(onClick = { }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    )
}
