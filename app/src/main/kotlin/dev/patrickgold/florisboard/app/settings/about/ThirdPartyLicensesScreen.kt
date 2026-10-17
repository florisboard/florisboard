/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.chipColors
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.style.DefaultLibraryActionBadges
import com.mikepenz.aboutlibraries.ui.compose.style.LibrariesStyle
import com.mikepenz.aboutlibraries.ui.compose.variant.DefaultLibraryBadges
import com.mikepenz.aboutlibraries.ui.compose.variant.LibrariesDensity
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryActionMode
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryActions
import com.mikepenz.aboutlibraries.ui.compose.variant.traditional.TraditionalRow
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.apptheme.ScreenHorizontalPadding
import dev.patrickgold.florisboard.app.apptheme.SegmentedListColumnVerticalPadding
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import org.florisboard.lib.compose.florisScrollbar
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdPartyLicensesScreen() {
    val lazyListState = rememberLazyListState()
    val libs by produceLibraries()

    FlorisScreenNg(
        title = stringRes(R.string.about__third_party_licenses__title),
        previewFieldVisible = false,
    ) { contentPadding ->
        var openDialog by remember { mutableStateOf<Library?>(null) }
        var openSheet by remember { mutableStateOf<Library?>(null) }

        LibrariesContainer(
            libraries = libs,
            dialogLibrary = openDialog,
            sheetLibrary = openSheet,
            onDialogLibraryChange = { openDialog = it },
            onSheetLibraryChange = { openSheet = it },
            modifier = Modifier
                .fillMaxSize()
                .florisScrollbar(lazyListState, isVertical = true),
            colors = LibraryDefaults.libraryColors(
                libraryBackgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                licenseChipColors = LibraryDefaults.chipColors(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                libraryContentColor = MaterialTheme.colorScheme.onBackground,
                dialogConfirmButtonColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = contentPadding + PaddingValues(
                horizontal = ScreenHorizontalPadding,
                vertical = SegmentedListColumnVerticalPadding,
            ),
            lazyListState = lazyListState,
            libraryRow = { index, library, expanded, onToggle, style ->
                SegmentedLibraryRow(
                    library = library,
                    expanded = expanded,
                    onToggle = onToggle,
                    style = style,
                    onDialogRequest = { openDialog = it },
                    index = index,
                    count = libs?.libraries?.size ?: 1,
                )
            },
            divider = { Spacer(Modifier.height(ListItemDefaults.SegmentedGap)) },
        )
    }
}

// The composable below has been inferred by digging into the source code of mikepenz/about-libraries:
// See: https://github.com/mikepenz/AboutLibraries/blob/0031fdba67a22fe42ecfd7110210d6656bc6a399/aboutlibraries-compose/src/commonMain/kotlin/com/mikepenz/aboutlibraries/ui/compose/variant/LibraryRow.kt
// See: https://github.com/mikepenz/AboutLibraries/blob/0031fdba67a22fe42ecfd7110210d6656bc6a399/aboutlibraries-compose/src/commonMain/kotlin/com/mikepenz/aboutlibraries/ui/compose/variant/Libraries.kt
@Composable
private fun LazyItemScope.SegmentedLibraryRow(
    library: Library,
    expanded: Boolean,
    onToggle: () -> Unit,
    style: LibrariesStyle,
    onDialogRequest: ((Library) -> Unit)? = null,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clip(ListItemDefaults.segmentedShapes(index, count).shape)
            .animateItem(),
    ) {
        TraditionalRow(
            library = library,
            expanded = expanded,
            onToggle = onToggle,
            density = LibrariesDensity.Cozy,
            badges = DefaultLibraryBadges,
            style = style,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
        ) {
            Box(Modifier.padding(style.padding.inlineActionsPadding)) {
                LibraryActions(
                    library = library,
                    actionMode = LibraryActionMode.Chips,
                    style = style,
                    actionLabels = DefaultLibraryActionBadges,
                    onActionClick = null,
                    onLicenseContentRequest = onDialogRequest,
                )
            }
        }
    }
}
