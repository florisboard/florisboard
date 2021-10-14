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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.jetpref.ui.compose.JetPrefListItem

@Composable
fun <T : Extension> ExtensionList(
    extList: List<T>,
    modifier: Modifier = Modifier,
    summaryProvider: (T) -> String? = { null },
) {
    val navController = LocalNavController.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        for (ext in extList) {
            JetPrefListItem(
                icon = { },
                modifier = Modifier
                    .clickable {
                        navController.navigate(Routes.Ext.View(ext.meta.id))
                    },
                text = ext.meta.title,
                secondaryText = summaryProvider(ext),
            )
        }
    }
}
