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

package dev.patrickgold.florisboard.app.settings.about

import android.webkit.URLUtil
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.android.launchUrl
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.florisScrollbar
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

data class Library(val name: String, val licenseText: String)

@Composable
fun ThirdPartyLicensesScreen() = FlorisScreen {
    title = stringRes(R.string.about__third_party_licenses__title)
    scrollable = false
    iconSpaceReserved = false

    val context = LocalContext.current

    var dialogLibraryToShow by rememberSaveable {
        mutableStateOf<Library?>(null)
    }

    val libraries = remember {
        val list = mutableListOf<Library>()
        val licensesData = context.resources
            .openRawResource(R.raw.third_party_licenses)
            .readBytes()
        val licensesMetaDataReader = context.resources
            .openRawResource(R.raw.third_party_license_metadata)
            .bufferedReader()
        licensesMetaDataReader.use { it.readLines() }.map { line ->
            val (section, name) = line.split(" ", limit = 2)
            val (startOffset, length) = section.split(":", limit = 2).map { it.toInt() }
            val licenseData = licensesData.sliceArray(startOffset until startOffset + length)
            val licenseText = licenseData.toString(Charsets.UTF_8)
            Library(name, licenseText)
        }.all { list.add(it) }
        list.add(
            Library("ICU4C Native C library", "https://github.com/unicode-org/icu/blob/main/icu4c/LICENSE")
        )
        list.add(
            Library("Google Material Icons", "https://www.apache.org/licenses/LICENSE-2.0.txt")
        )
        list.sortedBy { it.name.lowercase() }.toList()
    }

    content {
        val state = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .florisScrollbar(state = state, isVertical = true),
            state = state,
        ) {
            items(libraries) { library ->
                val isUrl = URLUtil.isValidUrl(library.licenseText)
                Preference(
                    title = library.name,
                    onClick = {
                        if (isUrl) {
                            context.launchUrl(library.licenseText)
                        } else {
                            dialogLibraryToShow = library
                        }
                    },
                )
            }
        }

        if (dialogLibraryToShow != null) {
            JetPrefAlertDialog(
                title = dialogLibraryToShow?.name ?: "",
                dismissLabel = stringRes(android.R.string.ok),
                onDismiss = { dialogLibraryToShow = null },
            ) {
                Text(dialogLibraryToShow?.licenseText ?: "")
            }
        }
    }
}
