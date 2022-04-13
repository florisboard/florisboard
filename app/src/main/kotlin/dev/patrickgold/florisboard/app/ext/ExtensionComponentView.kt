/*
 * Copyright (C) 2022 Patrick Goldinger
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponent
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.FlorisTextButton
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.ExtensionComponent
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.ext.ExtensionMeta

@Composable
fun ExtensionComponentNoneFoundView() {
    Text(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        text = stringRes(R.string.ext__meta__components_none_found),
        fontStyle = FontStyle.Italic,
    )
}

@Composable
fun ExtensionComponentView(
    meta: ExtensionMeta,
    component: ExtensionComponent,
    modifier: Modifier = Modifier,
    onDeleteBtnClick: (() -> Unit)? = null,
    onEditBtnClick: (() -> Unit)? = null,
) {
    val componentName = remember(meta.id, component.id) { ExtensionComponentName(meta.id, component.id).toString() }
    FlorisOutlinedBox(
        modifier = modifier,
        title = component.label,
        subtitle = componentName,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = if (onDeleteBtnClick == null && onEditBtnClick == null) 8.dp else 0.dp,
            ),
        ) {
            when (component) {
                is ThemeExtensionComponent -> {
                    val text = remember(
                        component.authors, component.isNightTheme, component.isBorderless,
                        component.isMaterialYouAware, component.stylesheetPath(),
                    ) {
                        buildString {
                            appendLine("authors = ${component.authors}")
                            appendLine("isNightTheme = ${component.isNightTheme}")
                            appendLine("isBorderless = ${component.isBorderless}")
                            appendLine("isMaterialYouAware = ${component.isMaterialYouAware}")
                            append("stylesheetPath = ${component.stylesheetPath()}")
                        }
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.body2,
                        color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                    )
                }
                else -> { }
            }
        }
        if (onDeleteBtnClick != null || onEditBtnClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
            ) {
                if (onDeleteBtnClick != null) {
                    FlorisTextButton(
                        onClick = onDeleteBtnClick,
                        icon = painterResource(R.drawable.ic_delete),
                        text = stringRes(R.string.action__delete),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colors.error,
                        ),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (onEditBtnClick != null) {
                    FlorisTextButton(
                        onClick = onEditBtnClick,
                        icon = painterResource(R.drawable.ic_edit),
                        text = stringRes(R.string.action__edit),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T : ExtensionComponent> ExtensionComponentListView(
    modifier: Modifier = Modifier,
    title: String,
    components: List<T>,
    onCreateBtnClick: (() -> Unit)? = null,
    componentGenerator: @Composable (T) -> Unit,
) {
    Column(modifier = modifier) {
        ListItem(
            text = { Text(
                text = title,
                color = MaterialTheme.colors.secondary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            ) },
            trailing = if (onCreateBtnClick != null) {
                @Composable {
                    FlorisIconButton(
                        onClick = onCreateBtnClick,
                        icon = painterResource(R.drawable.ic_add),
                        iconColor = MaterialTheme.colors.secondary,
                    )
                }
            } else { null },
        )
        if (components.isEmpty()) {
            ExtensionComponentNoneFoundView()
        } else {
            for (component in components) {
                componentGenerator(component)
            }
        }
    }
}
