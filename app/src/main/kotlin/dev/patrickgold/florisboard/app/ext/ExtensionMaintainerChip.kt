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

package dev.patrickgold.florisboard.app.ext

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.android.launchUrl
import dev.patrickgold.florisboard.lib.compose.FlorisChip
import dev.patrickgold.florisboard.lib.ext.ExtensionMaintainer
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExtensionMaintainerChip(
    maintainer: ExtensionMaintainer,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(false) }

    FlorisChip(
        modifier = modifier,
        text = maintainer.name,
        trailingIcons = when {
            maintainer.email != null && maintainer.url != null -> listOf(
                R.drawable.ic_email,
                R.drawable.ic_link,
            )
            maintainer.email != null -> listOf(R.drawable.ic_email)
            maintainer.url != null -> listOf(R.drawable.ic_link)
            else -> listOf()
        },
        onClick = { showDialog = !showDialog },
        enabled = maintainer.email != null || maintainer.url != null,
        shape = RoundedCornerShape(4.dp),
    )

    if (showDialog) {
        JetPrefAlertDialog(
            title = maintainer.name,
            onDismiss = { showDialog = false },
        ) {
            Column {
                if (maintainer.email != null) {
                    FlorisChip(
                        onClick = { context.launchUrl("mailto:${maintainer.email}") },
                        text = maintainer.email.toString(),
                        leadingIcons = listOf(R.drawable.ic_email),
                        shape = RoundedCornerShape(4.dp),
                    )
                }
                if (maintainer.url != null) {
                    FlorisChip(
                        onClick = { context.launchUrl(maintainer.url.toString()) },
                        text = maintainer.url.toString(),
                        leadingIcons = listOf(R.drawable.ic_link),
                        shape = RoundedCornerShape(4.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewChipNameOnly() {
    val maintainer = ExtensionMaintainer(
        name = "Jane Doe",
        email = null,
        url = null,
    )
    ExtensionMaintainerChip(maintainer)
}

@Preview(showBackground = true)
@Composable
private fun PreviewChipNameAndEmail() {
    val maintainer = ExtensionMaintainer(
        name = "Jane Doe",
        email = "jane.doe@example.com",
        url = null,
    )
    ExtensionMaintainerChip(maintainer)
}

@Preview(showBackground = true)
@Composable
private fun PreviewChipNameAndUrl() {
    val maintainer = ExtensionMaintainer(
        name = "Jane Doe",
        email = null,
        url = "jane-doe.example.com",
    )
    ExtensionMaintainerChip(maintainer)
}
