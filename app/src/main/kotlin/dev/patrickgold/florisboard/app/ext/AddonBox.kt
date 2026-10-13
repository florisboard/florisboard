/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.ext

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.extension.ExtensionIndex
import dev.patrickgold.florisboard.lib.util.launchUrl
import org.florisboard.lib.compose.FlorisOutlinedBox
import org.florisboard.lib.compose.FlorisTextButton
import org.florisboard.lib.compose.defaultFlorisOutlinedBox
import org.florisboard.lib.compose.stringRes

@Composable
fun UpdateBox(extensionIndex: ExtensionIndex) {
    val context = LocalContext.current
    FlorisOutlinedBox(
        modifier = Modifier.defaultFlorisOutlinedBox(),
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
            text = stringRes(id = R.string.ext__update_box__internet_permission_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
        ) {
            FlorisTextButton(
                onClick = {
                    context.launchUrl(extensionIndex.generateUpdateUrl())
                },
                icon = Icons.Outlined.FileDownload,
                text = stringRes(id = R.string.ext__update_box__search_for_updates)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
