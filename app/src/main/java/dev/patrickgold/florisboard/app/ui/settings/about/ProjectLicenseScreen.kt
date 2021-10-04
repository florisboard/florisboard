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

package dev.patrickgold.florisboard.app.ui.settings.about

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.res.FlorisRef

@Composable
fun ProjectLicenseScreen() = FlorisScreen(title = stringRes(R.string.about__project_license__title)) {
    val context = LocalContext.current
    val assetManager by context.assetManager()

    SelectionContainer(modifier = Modifier.fillMaxWidth()) {
        val licenseText = assetManager.loadTextAsset(
            FlorisRef.assets("license/project_license.txt")
        ).getOrElse {
            stringRes(R.string.about__project_license__error_license_text_failed, "error_message" to (it.message ?: ""))
        }/* ?: stringRes(
            id = R.string.about__project_license__error_license_text_failed,
            "error_message" to stringRes(R.string.about__project_license__error_reason_asset_manager_null)
        )*/
        Text(
            text = licenseText,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        )
    }
}
