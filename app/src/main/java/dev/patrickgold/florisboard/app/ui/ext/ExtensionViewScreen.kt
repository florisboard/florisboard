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

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisHyperlinkText
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.kotlin.stringBuilder
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.ime.theme.ThemeConfig
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import dev.patrickgold.florisboard.res.ext.ExtensionMaintainer
import dev.patrickgold.florisboard.res.ext.ExtensionMeta

private val ComponentCardShape = RoundedCornerShape(8.dp)

@Composable
fun ExtensionViewScreen(id: String) {
    val context = LocalContext.current
    val extensionManager by context.extensionManager()
    val ext = extensionManager.getExtensionById(id)
    if (ext != null) {
        ViewScreen(ext)
    } else {
        ErrorScreen(id)
    }
}

@Composable
private fun ViewScreen(ext: Extension) = FlorisScreen {
    title = ext.meta.title

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    content {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            ext.meta.description?.let { Text(it) }
            Spacer(modifier = Modifier.height(16.dp))
            ExtensionMetaRowScrollableChips(
                label = stringRes(R.string.ext__meta__maintainers),
                showDividerAbove = false,
            ) {
                for ((n, maintainer) in ext.meta.maintainers.withIndex()) {
                    if (n > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    ExtensionMaintainerChip(maintainer)
                }
            }
            ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__id)) {
                Text(text = ext.meta.id)
            }
            ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__version)) {
                Text(text = ext.meta.version)
            }
            if (ext.meta.keywords != null && ext.meta.keywords!!.isNotEmpty()) {
                ExtensionMetaRowScrollableChips(label = stringRes(R.string.ext__meta__keywords)) {
                    for ((n, keyword) in ext.meta.keywords!!.withIndex()) {
                        if (n > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        ExtensionKeywordChip(keyword)
                    }
                }
            }
            if (!ext.meta.homepage.isNullOrBlank()) {
                ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__homepage)) {
                    FlorisHyperlinkText(
                        text = FlorisRef.from(ext.meta.homepage!!).authority,
                        url = ext.meta.homepage!!,
                    )
                }
            }
            if (!ext.meta.issueTracker.isNullOrBlank()) {
                ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__issue_tracker)) {
                    FlorisHyperlinkText(
                        text = FlorisRef.from(ext.meta.issueTracker!!).authority,
                        url = ext.meta.issueTracker!!,
                    )
                }
            }
            ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__license)) {
                // TODO: display human-readable License name instead of
                //  SPDX identifier
                Text(text = ext.meta.license)
            }
            if (extensionManager.canDelete(ext)) {
                Button(onClick = {
                    extensionManager.delete(ext)
                    navController.popBackStack()
                }) {
                    Text(text = stringRes(R.string.assets__action__delete))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringRes(R.string.ext__meta__components),
                fontWeight = FontWeight.Bold,
            )
            when (ext) {
                is ThemeExtension -> {
                    if (ext.themes.isEmpty()) {
                        ThemeExtensionComponentNoneFound()
                    } else {
                        for (themeConfig in ext.themes) {
                            ThemeExtensionComponent(ext = ext, config = themeConfig)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorScreen(id: String) = FlorisScreen {
    title = stringRes(R.string.ext__error__not_found_title)

    content {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(stringRes(R.string.ext__error__not_found_description, "id" to id))
        }
    }
}

@Composable
private fun ThemeExtensionComponent(
    ext: ThemeExtension,
    config: ThemeConfig,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f), ComponentCardShape)
            .padding(vertical = 8.dp, horizontal = 14.dp),
    ) {
        Text(
            text = stringRes(R.string.ext__meta__components_theme, "label" to config.label),
            style = MaterialTheme.typography.subtitle2,
        )
        Text(
            text = remember { ExtensionComponentName(ext.meta.id, config.id).toString() },
            color = LocalContentColor.current.copy(alpha = 0.56f),
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
        val text = remember(config) {
            stringBuilder {
                appendLine("authors = ${config.authors}")
                appendLine("isNightTheme = ${config.isNightTheme}")
                appendLine("isBorderless = ${config.isBorderless}")
                appendLine("isMaterialYouAware = ${config.isMaterialYouAware}")
                append("stylesheetPath = ${config.stylesheetPath()}")
            }
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = text,
            style = MaterialTheme.typography.body2,
            color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
        )
    }
}

@Composable
private fun ThemeExtensionComponentNoneFound(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = stringRes(R.string.ext__meta__components_none_found),
        fontStyle = FontStyle.Italic,
    )
}

@Composable
private fun ExtensionMetaRowSimpleText(
    label: String,
    modifier: Modifier = Modifier,
    showDividerAbove: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (showDividerAbove) {
        Divider()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(modifier = Modifier.padding(end = 24.dp), text = label)
        content()
    }
}

@Composable
private fun ExtensionMetaRowScrollableChips(
    label: String,
    modifier: Modifier = Modifier,
    showDividerAbove: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (showDividerAbove) {
        Divider()
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(modifier = Modifier.padding(end = 24.dp), text = label)
        Row(
            modifier = Modifier
                .weight(1.0f, fill = false)
                .horizontalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewExtensionViewerScreen() {
    val testExtension = ThemeExtension(
        meta = ExtensionMeta(
            id = "com.example.theme.test",
            version = "2.4.3",
            title = "Test theme",
            description = "This is a test theme to preview the extension viewer screen UI.",
            keywords = listOf("Beach", "Sea", "Sun"),
            homepage = "https://example.com",
            issueTracker = "https://git.example.com/issues",
            maintainers = listOf(
                "Max Mustermann <max.mustermann@example.com> (maxmustermann.example.com)",
            ).map { ExtensionMaintainer.fromOrTakeRaw(it) },
            license = "apache-2.0",
        ),
        dependencies = null,
        themes = listOf(
            ThemeConfig(id = "test", label = "Test", authors = listOf(), stylesheetPath = "test.json"),
        ),
    )
    ViewScreen(ext = testExtension)
}
