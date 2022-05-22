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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.florisScrollbar
import dev.patrickgold.florisboard.lib.compose.stringRes

@Composable
fun ThirdPartyLicensesScreen() = FlorisScreen {
    title = stringRes(R.string.about__third_party_licenses__title)
    scrollable = false
    iconSpaceReserved = false

    val lazyListState = rememberLazyListState()

    content {
        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize()
                .florisScrollbar(lazyListState, isVertical = true),
            lazyListState = lazyListState,
        )
    }
}
