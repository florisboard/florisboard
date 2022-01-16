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

package dev.patrickgold.florisboard.app.ui.ext

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisTextButton
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponent
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.res.ext.ExtensionComponent
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import dev.patrickgold.florisboard.res.ext.ExtensionMeta

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Extension.rememberComponents(): List<ExtensionComponent> {
    return remember(this) { this.components() }
}

@Composable
fun ExtensionComponentListTitleView(
    ext: Extension,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = Modifier.padding(bottom = 8.dp),
        text = stringRes(when (ext) {
            is ThemeExtension -> R.string.ext__meta__components_theme
            else -> R.string.ext__meta__components
        }),
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun ExtensionComponentNoneFoundView() {
    Text(
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
    val componentName = remember { ExtensionComponentName(meta.id, component.id).toString() }
    FlorisOutlinedBox(
        title = component.label,
        subtitle = componentName,
    ) {
        when (component) {
            is ThemeExtensionComponent -> {
                val text = remember(component) {
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
