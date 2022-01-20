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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalContentColor
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisIconButton
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.FlorisTextButton
import dev.patrickgold.florisboard.app.ui.components.rippleClickable
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.theme.FlorisImeUiSpec
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponentEditor
import dev.patrickgold.florisboard.res.cache.CacheManager
import dev.patrickgold.florisboard.res.io.readJson
import dev.patrickgold.florisboard.res.io.subFile
import dev.patrickgold.florisboard.snygg.Snygg
import dev.patrickgold.florisboard.snygg.SnyggLevel
import dev.patrickgold.florisboard.snygg.SnyggRule
import dev.patrickgold.florisboard.snygg.SnyggRuleEditor
import dev.patrickgold.florisboard.snygg.SnyggStylesheet
import dev.patrickgold.florisboard.snygg.SnyggStylesheetEditor
import dev.patrickgold.florisboard.snygg.SnyggStylesheetJsonConfig
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

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

    // TODO: lazy column??
    content {
        BackHandler {
            handleBackPress()
        }

        if (stylesheetEditor.rules.isEmpty()) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                text = stringRes(R.string.settings__theme_editor__no_rules_defined),
                fontStyle = FontStyle.Italic,
            )
        }
        for ((rule, propertySet) in stylesheetEditor.rules) key(rule) {
            val propertySetSpec = FlorisImeUiSpec.propertySetSpec(rule.element)
            FlorisOutlinedBox(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SnyggRuleRow(
                        rule = rule,
                        level = snyggLevel,
                        onEditRuleBtnClick = { /*TODO*/ },
                        onAddPropertyBtnClick = { },
                    )
                    for ((propertyName, propertyValue) in propertySet.properties) {
                        val propertySpec = propertySetSpec?.propertySpec(propertyName)
                        if (propertySpec != null && propertySpec.level <= snyggLevel) {
                            JetPrefListItem(
                                modifier = Modifier.rippleClickable {  },
                                text = translatePropertyName(propertyName, snyggLevel),
                                secondaryText = propertyValue.toString(),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    FlorisTextButton(
                        onClick = { workspace.update { stylesheetEditor.rules.remove(rule) } },
                        icon = painterResource(R.drawable.ic_delete),
                        text = stringRes(R.string.action__delete),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colors.error,
                        ),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    FlorisTextButton(
                        onClick = { },
                        icon = painterResource(R.drawable.ic_edit),
                        text = stringRes(R.string.action__edit),
                    )
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
    @Composable
    fun Selector(text: String) {
        Text(
            modifier = Modifier
                .background(MaterialTheme.colors.primaryVariant)
                .padding(end = 4.dp),
            text = text,
            style = MaterialTheme.typography.body2,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Composable
    fun AttributesList(text: String, list: String) {
        Text(
            text = "$text = $list",
            style = MaterialTheme.typography.body2,
            color = LocalContentColor.current.copy(alpha = 0.56f),
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp, horizontal = 10.dp),
        ) {
            Text(
                text = rule.translateElementName(level),
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                if (rule.pressedSelector) {
                    Selector(text = "pressed")
                }
                if (rule.focusSelector) {
                    Selector(text = "focus")
                }
                if (rule.disabledSelector) {
                    Selector(text = "disabled")
                }
            }
            if (rule.codes.isNotEmpty()) {
                AttributesList(text = "codes", list = remember(rule.codes) { rule.codes.toString() })
            }
            if (rule.modes.isNotEmpty()) {
                AttributesList(text = "modes", list = remember(rule.modes) { rule.modes.toString() })
            }
        }
        FlorisTextButton(
            onClick = { },
            icon = painterResource(R.drawable.ic_add),
            text = stringRes(R.string.action__add),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colors.secondary,
            ),
        )
    }
}

@Composable
private fun SnyggRuleEditor.translateElementName(level: SnyggLevel): String {
    return when(level) {
        SnyggLevel.EXPERT -> null
        else -> when (this.element) {
            FlorisImeUi.Keyboard -> R.string.snygg__rule_element__keyboard
            FlorisImeUi.Key -> R.string.snygg__rule_element__key
            FlorisImeUi.KeyHint -> R.string.snygg__rule_element__key_hint
            FlorisImeUi.KeyPopup -> R.string.snygg__rule_element__key_popup
            FlorisImeUi.ClipboardHeader -> R.string.snygg__rule_element__clipboard_header
            FlorisImeUi.ClipboardItem -> R.string.snygg__rule_element__clipboard_item
            FlorisImeUi.ClipboardItemPopup -> R.string.snygg__rule_element__clipboard_item_popup
            FlorisImeUi.OneHandedPanel -> R.string.snygg__rule_element__one_handed_panel
            FlorisImeUi.SmartbarPrimaryRow -> R.string.snygg__rule_element__smartbar_primary_row
            FlorisImeUi.SmartbarPrimaryActionRowToggle -> R.string.snygg__rule_element__smartbar_primary_action_row_toggle
            FlorisImeUi.SmartbarPrimarySecondaryRowToggle -> R.string.snygg__rule_element__smartbar_primary_secondary_row_toggle
            FlorisImeUi.SmartbarSecondaryRow -> R.string.snygg__rule_element__smartbar_secondary_row
            FlorisImeUi.SmartbarActionRow -> R.string.snygg__rule_element__smartbar_action_row
            FlorisImeUi.SmartbarActionButton -> R.string.snygg__rule_element__smartbar_action_button
            FlorisImeUi.SmartbarCandidateRow -> R.string.snygg__rule_element__smartbar_candidate_row
            FlorisImeUi.SmartbarCandidateWord -> R.string.snygg__rule_element__smartbar_candidate_word
            FlorisImeUi.SmartbarCandidateClip -> R.string.snygg__rule_element__smartbar_candidate_clip
            FlorisImeUi.SmartbarCandidateSpacer -> R.string.snygg__rule_element__smartbar_candidate_spacer
            FlorisImeUi.SmartbarKey -> R.string.snygg__rule_element__smartbar_key
            FlorisImeUi.SystemNavBar -> R.string.snygg__rule_element__system_nav_bar
            else -> null
        }
    }.let { resId ->
        if (resId != null) {
            stringRes(resId)
        } else {
            remember {
                buildString {
                    if (this@translateElementName.isAnnotation) {
                        append(SnyggRule.ANNOTATION_MARKER)
                    }
                    append(this@translateElementName.element)
                }
            }
        }
    }
}

@Composable
private fun translatePropertyName(propertyName: String, level: SnyggLevel): String {
    return when(level) {
        SnyggLevel.EXPERT -> null
        else -> when (propertyName) {
            Snygg.Width -> R.string.snygg__property_name__width
            Snygg.Height -> R.string.snygg__property_name__height
            Snygg.Background -> R.string.snygg__property_name__background
            Snygg.Foreground -> R.string.snygg__property_name__foreground
            Snygg.Border -> R.string.snygg__property_name__border
            Snygg.BorderTop -> R.string.snygg__property_name__border_top
            Snygg.BorderBottom -> R.string.snygg__property_name__border_bottom
            Snygg.BorderStart -> R.string.snygg__property_name__border_start
            Snygg.BorderEnd -> R.string.snygg__property_name__border_end
            Snygg.FontFamily -> R.string.snygg__property_name__font_family
            Snygg.FontSize -> R.string.snygg__property_name__font_size
            Snygg.FontStyle -> R.string.snygg__property_name__font_style
            Snygg.FontVariant -> R.string.snygg__property_name__font_variant
            Snygg.FontWeight -> R.string.snygg__property_name__font_weight
            Snygg.Shadow -> R.string.snygg__property_name__shadow
            Snygg.Shape -> R.string.snygg__property_name__shape
            else -> null
        }
    }.let { resId ->
        if (resId != null) {
            stringRes(resId)
        } else {
            propertyName
        }
    }
}
