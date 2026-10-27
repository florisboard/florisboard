/*
 * Copyright (C) 2024-2026 The FlorisBoard Contributors
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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.ime.extension.Extension
import dev.patrickgold.florisboard.ime.extension.LocalExtensionController
import dev.patrickgold.florisboard.ime.keyboard3.extension.Keyboard3Extension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import org.florisboard.lib.compose.FlorisOutlinedBox
import org.florisboard.lib.compose.FlorisTextButton
import org.florisboard.lib.compose.defaultFlorisOutlinedBox
import org.florisboard.lib.compose.florisScrollbar
import org.florisboard.lib.compose.stringRes

enum class ExtensionListScreenType(
    val id: String,
    @StringRes val titleResId: Int,
    val predicate: (Extension<*>) -> Boolean,
    val launchExtensionCreate: ((NavController) -> Unit)?,
) {
    EXT_THEME(
        id = "ext-theme",
        titleResId = R.string.ext__list__ext_theme,
        predicate = { it is ThemeExtension },
        launchExtensionCreate = { it.navigate(Routes.Ext.Edit("null", ThemeExtension.SERIAL_TYPE)) },
    ),
    EXT_KEYBOARD3(
        id = "ext-keyboard3",
        titleResId = R.string.ext__list__ext_keyboard,
        predicate = { it is Keyboard3Extension },
        launchExtensionCreate = null,//{ it.navigate(Routes.Ext.Edit("null", KeyboardExtension.SERIAL_TYPE)) },
    ),
    EXT_LANGUAGEPACK(
        id = "ext-languagepack",
        titleResId = R.string.ext__list__ext_languagepack,
        predicate = { false }, // TODO
        launchExtensionCreate = null,//{ it.navigate(Routes.Ext.Edit("null", LanguagePackExtension.SERIAL_TYPE)) },
    );
}

@Composable
fun ExtensionListScreen(type: ExtensionListScreenType, showUpdate: Boolean) = FlorisScreen {
    title = stringRes(type.titleResId)
    previewFieldVisible = false
    scrollable = false

    val navController = LocalNavController.current
    val extensionController = LocalExtensionController.current
    val extensionIndex by extensionController.activeIndex.collectAsState()
    val extensions = remember(extensionIndex) {
        extensionIndex.extensions.values.filter(type.predicate)
    }

    var fabHeight by remember {
        mutableStateOf(0)
    }
    val fabHeightDp = with(LocalDensity.current) { fabHeight.toDp()+16.dp }
    val listState = rememberLazyListState()

    content {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .florisScrollbar(state = listState, isVertical = true),
            state = listState,
            contentPadding = PaddingValues(bottom = fabHeightDp),
        ) {
            if (showUpdate) {
                item {
                    ImportExtensionBox(navController)
                }
                item {
                    UpdateBox(extensionIndex)
                }
            }
            items(extensions) { extension ->
                val manifest = extension.manifest
                FlorisOutlinedBox(
                    modifier = Modifier.defaultFlorisOutlinedBox(),
                    title = manifest.meta.title,
                    subtitle = manifest.meta.id,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        text = manifest.meta.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                    ) {
                        FlorisTextButton(
                            onClick = {
                                navController.navigate(Routes.Ext.View(manifest.meta.id))
                            },
                            icon = Icons.Outlined.Info,
                            text = stringRes(id = R.string.ext__list__view_details),//stringRes(R.string.action__add),
                            colors = ButtonDefaults.textButtonColors(),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        FlorisTextButton(
                            onClick = {
                                navController.navigate(Routes.Ext.Edit(manifest.meta.id))
                            },
                            icon = Icons.Default.Edit,
                            text = stringRes(R.string.action__edit),
                            enabled = extension.canDelete(),
                        )
                    }
                }
            }
        }
    }

    if (type.launchExtensionCreate != null) {
        floatingActionButton {
            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringRes(id = R.string.ext__editor__title_create_any),
                    )
                },
                text = {
                    Text(
                        text = stringRes(id = R.string.ext__editor__title_create_any),
                    )
                },
                modifier = Modifier.onGloballyPositioned {
                    fabHeight = it.size.height
                },
                shape = FloatingActionButtonDefaults.extendedFabShape,
                onClick = { type.launchExtensionCreate.invoke(navController) },
            )
        }
    }
}
