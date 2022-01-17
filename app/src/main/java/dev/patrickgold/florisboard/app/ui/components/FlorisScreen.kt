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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.prefs.AppPrefs
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.jetpref.datastore.ui.PreferenceLayout
import dev.patrickgold.jetpref.datastore.ui.PreferenceUiContent

@Composable
fun FlorisScreen(builder: @Composable FlorisScreenScope.() -> Unit) {
    val scope = remember { FlorisScreenScopeImpl() }
    builder(scope)
    scope.Render()
}

@Composable
fun FlorisScreenWithBottomSheet(builder: @Composable FlorisScreenWithBottomSheetScope.() -> Unit) {
    val scope = remember { FlorisScreenWithBottomSheetScopeImpl() }
    builder(scope)
    scope.Render()
}

typealias FlorisScreenActions = @Composable RowScope.() -> Unit
typealias FlorisScreenBottomBar = @Composable () -> Unit
typealias FlorisScreenBottomSheet = @Composable ColumnScope.() -> Unit
typealias FlorisScreenContent = PreferenceUiContent<AppPrefs>
typealias FlorisScreenFab = @Composable () -> Unit

interface FlorisScreenScope {
    var title: String

    var backArrowVisible: Boolean

    var previewFieldVisible: Boolean

    var scrollable: Boolean

    var iconSpaceReserved: Boolean

    fun actions(actions: FlorisScreenActions)

    fun bottomBar(bottomBar: FlorisScreenBottomBar)

    fun floatingActionButton(fab: FlorisScreenFab)

    fun content(content: FlorisScreenContent)
}

private class FlorisScreenScopeImpl : FlorisScreenScope {
    override var title: String by mutableStateOf("")
    override var backArrowVisible: Boolean by mutableStateOf(true)
    override var previewFieldVisible: Boolean by mutableStateOf(false)
    override var scrollable: Boolean by mutableStateOf(true)
    override var iconSpaceReserved: Boolean by mutableStateOf(true)

    private var actions: FlorisScreenActions = { }
    private var bottomBar: FlorisScreenBottomBar = { }
    private var content: FlorisScreenContent = { }
    private var fab: FlorisScreenFab = { }

    override fun actions(actions: FlorisScreenActions) {
        this.actions = actions
    }

    override fun bottomBar(bottomBar: FlorisScreenBottomBar) {
        this.bottomBar = bottomBar
    }

    override fun content(content: FlorisScreenContent) {
        this.content = content
    }

    override fun floatingActionButton(fab: FlorisScreenFab) {
        this.fab = fab
    }

    @Composable
    fun Render() {
        val previewFieldController = LocalPreviewFieldController.current

        SideEffect {
            previewFieldController?.isVisible = previewFieldVisible
        }

        Scaffold(
            topBar = { FlorisAppBar(title, backArrowVisible, actions) },
            bottomBar = bottomBar,
            floatingActionButton = fab,
        ) { innerPadding ->
            val modifier = if (scrollable) {
                Modifier.florisVerticalScroll()
            } else {
                Modifier
        }
            PreferenceLayout(
                florisPreferenceModel(),
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                iconSpaceReserved = iconSpaceReserved,
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
interface FlorisScreenWithBottomSheetScope {
    var title: String

    var backArrowVisible: Boolean

    val bottomSheetScaffoldState: BottomSheetScaffoldState

    var scrollable: Boolean

    var iconSpaceReserved: Boolean

    fun actions(actions: FlorisScreenActions)

    fun bottomSheet(bottomSheet: FlorisScreenBottomSheet)

    fun floatingActionButton(fab: FlorisScreenFab)

    fun content(content: FlorisScreenContent)
}

@OptIn(ExperimentalMaterialApi::class)
private class FlorisScreenWithBottomSheetScopeImpl : FlorisScreenWithBottomSheetScope {
    override var title: String by mutableStateOf("")
    override var backArrowVisible: Boolean by mutableStateOf(true)
    override lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
    override var scrollable: Boolean by mutableStateOf(true)
    override var iconSpaceReserved: Boolean by mutableStateOf(true)

    private var actions: FlorisScreenActions = { }
    private var bottomSheet: FlorisScreenBottomSheet = { }
    private var content: FlorisScreenContent = { }
    private var fab: FlorisScreenFab = { }

    override fun actions(actions: FlorisScreenActions) {
        this.actions = actions
    }

    override fun bottomSheet(bottomSheet: FlorisScreenBottomSheet) {
        this.bottomSheet = bottomSheet
    }

    override fun content(content: FlorisScreenContent) {
        this.content = content
    }

    override fun floatingActionButton(fab: FlorisScreenFab) {
        this.fab = fab
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun Render() {
        val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
        )
        this.bottomSheetScaffoldState = bottomSheetScaffoldState
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            topBar = { FlorisAppBar(title, backArrowVisible, actions) },
            sheetContent = bottomSheet,
            sheetBackgroundColor = MaterialTheme.colors.background,
            sheetGesturesEnabled = false,
            sheetPeekHeight = 0.dp,
            floatingActionButton = fab,
        ) { innerPadding ->
            val modifier = if (scrollable) {
                Modifier.florisVerticalScroll()
            } else {
                Modifier
            }
            PreferenceLayout(
                florisPreferenceModel(),
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                iconSpaceReserved = iconSpaceReserved,
                content = content,
            )
        }
    }
}
