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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.ui.components.FlorisChip
import dev.patrickgold.florisboard.common.launchUrl
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.res.ext.ExtensionAuthor
import dev.patrickgold.jetpref.ui.compose.JetPrefAlertDialog

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExtensionAuthorChip(
    author: ExtensionAuthor,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(false) }

    FlorisChip(
        modifier = modifier,
        text = author.name,
        trailingIcons = when {
            author.email != null && author.url != null -> listOf(
                R.drawable.ic_email,
                R.drawable.ic_link,
            )
            author.email != null -> listOf(R.drawable.ic_email)
            author.url != null -> listOf(R.drawable.ic_link)
            else -> listOf()
        },
        onClick = { showDialog = !showDialog },
        enabled = author.email != null || author.url != null,
        shape = RoundedCornerShape(4.dp),
    )

    if (showDialog) {
        JetPrefAlertDialog(
            title = author.name,
            onDismiss = { showDialog = false },
        ) {
            Column {
                if (author.email != null) {
                    FlorisChip(
                        onClick = { launchUrl(context, "mailto:${author.email}") },
                        text = author.email,
                        leadingIcons = listOf(R.drawable.ic_email),
                        shape = RoundedCornerShape(4.dp),
                    )
                }
                if (author.url != null) {
                    FlorisChip(
                        onClick = { launchUrl(context, author.url.toString()) },
                        text = author.url.toString(),
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
    val author = ExtensionAuthor(
        name = "Jane Doe",
        email = null,
        url = null,
    )
    ExtensionAuthorChip(author)
}

@Preview(showBackground = true)
@Composable
private fun PreviewChipNameAndEmail() {
    val author = ExtensionAuthor(
        name = "Jane Doe",
        email = "jane.doe@example.com",
        url = null,
    )
    ExtensionAuthorChip(author)
}

@Preview(showBackground = true)
@Composable
private fun PreviewChipNameAndUrl() {
    val author = ExtensionAuthor(
        name = "Jane Doe",
        email = null,
        url = FlorisRef.from("jane-doe.example.com"),
    )
    ExtensionAuthorChip(author)
}
