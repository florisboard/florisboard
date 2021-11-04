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

package dev.patrickgold.florisboard.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.patrickgold.florisboard.app.prefs.AppPrefs
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.jetpref.ui.compose.PreferenceLayout
import dev.patrickgold.jetpref.ui.compose.PreferenceUiContent

@Composable
fun FlorisScreen(
    title: String,
    backArrowVisible: Boolean = true,
    scrollable: Boolean = true,
    iconSpaceReserved: Boolean = true,
    actions: @Composable RowScope.() -> Unit = { },
    bottomBar: @Composable () -> Unit = { },
    floatingActionButton: @Composable () -> Unit = { },
    content: PreferenceUiContent<AppPrefs>,
) {
    Scaffold(
        topBar = { FlorisAppBar(title, backArrowVisible, actions) },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        val modifier = if (scrollable) {
            Modifier.florisVerticalScroll()
        } else {
            Modifier
        }
        Box(modifier = modifier.padding(innerPadding)) {
            PreferenceLayout(
                florisPreferenceModel(),
                scrollable = false,
                iconSpaceReserved = iconSpaceReserved,
            ) {
                content(this)
            }
        }
    }
}
