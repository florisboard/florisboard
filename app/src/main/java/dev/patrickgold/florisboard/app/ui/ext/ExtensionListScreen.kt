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

package dev.patrickgold.florisboard.app.ui.ext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.InputMethodUtils
import dev.patrickgold.florisboard.ime.clip.FlorisClipboardManager

@Composable
fun ExtensionListScreen() = FlorisScreen(title = stringRes(R.string.about__title)) {
    /*val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager = ExtensionManager.def

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 32.dp)
    ) {
        FlorisAppIcon()
        Text(
            text = stringRes(R.string.floris_app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
    Preference(
        iconId = R.drawable.ic_info,
        title = stringRes(R.string.about__version__title),
        summary = appVersion,
        onClick = {
            try {
                val isImeSelected = InputMethodUtils.checkIsFlorisboardSelected(context)
                if (isImeSelected) {
                    FlorisClipboardManager.getInstance().addNewPlaintext(appVersion)
                } else {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Florisboard version", appVersion)
                    clipboard.setPrimaryClip(clip)
                }
                Toast.makeText(context, R.string.about__version_copied__title, Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                Toast.makeText(
                    context, context.getString(R.string.about__version_copied__error, e.message), Toast.LENGTH_SHORT
                ).show()
            }
        },
    )
    Preference(
        iconId = R.drawable.ic_history,
        title = stringRes(R.string.about__changelog__title),
        summary = stringRes(R.string.about__changelog__summary),
        onClick = { launchUrl(context, R.string.florisboard__changelog_url, arrayOf(BuildConfig.VERSION_NAME)) },
    )
    Preference(
        iconId = R.drawable.ic_code,
        title = stringRes(R.string.about__repository__title),
        summary = stringRes(R.string.about__repository__summary),
        onClick = { launchUrl(context, R.string.florisboard__repo_url) },
    )
    Preference(
        iconId = R.drawable.ic_policy,
        title = stringRes(R.string.about__privacy_policy__title),
        summary = stringRes(R.string.about__privacy_policy__summary),
        onClick = { launchUrl(context, R.string.florisboard__privacy_policy_url) },
    )
    Preference(
        iconId = R.drawable.ic_description,
        title = stringRes(R.string.about__project_license__title),
        summary = stringRes(R.string.about__project_license__summary, "license_name" to "Apache 2.0"),
        onClick = { navController.navigate(Routes.Settings.ProjectLicense) },
    )
    Preference(
        iconId = R.drawable.ic_description,
        title = stringRes(id = R.string.about__third_party_licenses__title),
        summary = stringRes(id = R.string.about__third_party_licenses__summary),
        onClick = { navController.navigate(Routes.Settings.ThirdPartyLicenses) },
    )*/
}
