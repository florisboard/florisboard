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

package dev.patrickgold.florisboard.app.ui.settings.theme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisIconButton
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.FlorisSimpleCard
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponentEditor
import dev.patrickgold.florisboard.res.cache.CacheManager
import dev.patrickgold.florisboard.res.io.readJson
import dev.patrickgold.florisboard.res.io.subFile
import dev.patrickgold.florisboard.snygg.SnyggLevel
import dev.patrickgold.florisboard.snygg.SnyggRule
import dev.patrickgold.florisboard.snygg.SnyggRuleEditor
import dev.patrickgold.florisboard.snygg.SnyggStylesheet
import dev.patrickgold.florisboard.snygg.SnyggStylesheetEditor
import dev.patrickgold.florisboard.snygg.SnyggStylesheetJsonConfig

@Composable
fun ThemeEditorScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    editor: ThemeExtensionComponentEditor,
) = FlorisScreen {
    title = stringRes(R.string.ext__editor__edit_component__title_theme)

    val stylesheetEditor = remember {
        editor.stylesheetEditor ?: run {
            val stylesheetPath = editor.stylesheetPath()
            editor.stylesheetPathOnLoad = stylesheetPath
            val stylesheetFile = workspace.dir.subFile(stylesheetPath)
            if (stylesheetFile.exists()) {
                try {
                    stylesheetFile.readJson<SnyggStylesheet>(SnyggStylesheetJsonConfig).edit()
                } catch (e: Throwable) {
                    SnyggStylesheetEditor()
                }
            } else {
                SnyggStylesheetEditor()
            }
        }.also { editor.stylesheetEditor = it }
    }
    var snyggLevel by remember { mutableStateOf(SnyggLevel.BASIC) }

    fun handleBackPress() {
        workspace.currentAction = null
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_close),
        )
    }

    actions {
        FlorisIconButton(
            onClick = {
                snyggLevel = when (snyggLevel) {
                    SnyggLevel.BASIC -> SnyggLevel.ADVANCED
                    SnyggLevel.ADVANCED -> SnyggLevel.EXPERT
                    SnyggLevel.EXPERT -> SnyggLevel.BASIC
                }
            },
            icon = painterResource(R.drawable.ic_language),
        )
    }

    content {
        BackHandler {
            handleBackPress()
        }

        for ((rule, propertySet) in stylesheetEditor.rules) key(rule) {
            Card(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                SnyggRuleRow(
                    rule = rule,
                    level = snyggLevel,
                    onEditRuleBtnClick = { /*TODO*/ },
                    onAddPropertyBtnClick = { },
                )
            }
        }
    }
}

@Composable
private fun SnyggRuleEditor.getElementName(level: SnyggLevel): String {
    return when(level) {
        SnyggLevel.EXPERT -> null
        else -> when (this.element) {
            FlorisImeUi.Keyboard -> R.string.snygg__rule_element__keyboard
            FlorisImeUi.Key -> R.string.snygg__rule_element__key
            FlorisImeUi.KeyHint -> R.string.snygg__rule_element__key_hint
            FlorisImeUi.KeyPopup -> R.string.snygg__rule_element__key_popup
            else -> null
        }
    }.let { resId ->
        if (resId != null) {
            stringRes(resId)
        } else {
            remember {
                buildString {
                    if (this@getElementName.isAnnotation) {
                        append(SnyggRule.ANNOTATION_MARKER)
                    }
                    append(this@getElementName.element)
                }
            }
        }
    }
}

@Composable
private fun SnyggRuleRow(
    rule: SnyggRuleEditor,
    level: SnyggLevel,
    onEditRuleBtnClick: () -> Unit,
    onAddPropertyBtnClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.getElementName(level),
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
            )
        }
        FlorisIconButton(
            onClick = onEditRuleBtnClick,
            icon = painterResource(R.drawable.ic_edit),
        )
        FlorisIconButton(
            onClick = onEditRuleBtnClick,
            icon = painterResource(R.drawable.ic_add),
        )
    }
}
