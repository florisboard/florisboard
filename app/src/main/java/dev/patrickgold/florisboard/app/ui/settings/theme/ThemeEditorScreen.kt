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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import dev.patrickgold.florisboard.snygg.SnyggPropertySetEditor
import dev.patrickgold.florisboard.snygg.SnyggPropertySetSpec
import dev.patrickgold.florisboard.snygg.SnyggRule
import dev.patrickgold.florisboard.snygg.SnyggStylesheet
import dev.patrickgold.florisboard.snygg.SnyggStylesheetEditor
import dev.patrickgold.florisboard.snygg.SnyggStylesheetJsonConfig
import dev.patrickgold.florisboard.snygg.value.SnyggCutCornerShapeDpValue
import dev.patrickgold.florisboard.snygg.value.SnyggCutCornerShapePercentValue
import dev.patrickgold.florisboard.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.snygg.value.SnyggDpSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggExplicitInheritValue
import dev.patrickgold.florisboard.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.snygg.value.SnyggPercentageSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggRectangleShapeValue
import dev.patrickgold.florisboard.snygg.value.SnyggRoundedCornerShapeDpValue
import dev.patrickgold.florisboard.snygg.value.SnyggRoundedCornerShapePercentValue
import dev.patrickgold.florisboard.snygg.value.SnyggShapeValue
import dev.patrickgold.florisboard.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue
import dev.patrickgold.florisboard.snygg.value.SnyggValueEncoder
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import kotlinx.coroutines.launch

internal val IntListSaver = Saver<SnapshotStateList<Int>, ArrayList<Int>>(
    save = { ArrayList(it) },
    restore = { it.toMutableStateList() },
)

@Composable
fun ThemeEditorScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    editor: ThemeExtensionComponentEditor,
) = FlorisScreen {
    title = stringRes(R.string.ext__editor__edit_component__title_theme)
    scrollable = false

    val scope = rememberCoroutineScope()
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
    var snyggLevel by remember { mutableStateOf(SnyggLevel.ADVANCED) }
    var snyggRuleToEdit by rememberSaveable(saver = SnyggRule.Saver) { mutableStateOf(null) }
    var snyggPropertyToEdit by remember { mutableStateOf<PropertyInfo?>(null) }
    var snyggPropertySetForEditing = remember<SnyggPropertySetEditor?> { null }
    var snyggPropertySetSpecForEditing = remember<SnyggPropertySetSpec?> { null }

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
                    SnyggLevel.ADVANCED -> SnyggLevel.DEVELOPER
                    SnyggLevel.DEVELOPER -> SnyggLevel.BASIC
                }
            },
            icon = painterResource(R.drawable.ic_language),
        )
    }

    floatingActionButton {
        ExtendedFloatingActionButton(
            icon = { Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
            ) },
            text = { Text(
                text = stringRes(R.string.settings__theme_editor__add_rule),
            ) },
            onClick = { snyggRuleToEdit = SnyggEmptyRuleForAdding },
        )
    }

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
        val definedVariables = remember(stylesheetEditor.rules) {
            stylesheetEditor.rules.firstNotNullOfOrNull { (rule, propertySet) ->
                if (rule.isAnnotation && rule.element == "defines") {
                    propertySet.properties
                } else {
                    null
                }
            } ?: emptyMap()
        }

        // TODO: (priority = low)
        //  Floris scrollbar does not like lazy lists with non-constant item heights.
        //  Consider building a custom scrollbar tailored for this list specifically.
        val lazyListState = rememberLazyListState()
        LazyColumn(
            //modifier = Modifier.florisScrollbar(lazyListState, isVertical = true),
            state = lazyListState,
        ) {
            items(stylesheetEditor.rules.entries.toList()) { (rule, propertySet) -> key(rule) {
                val isVariablesRule = rule.isAnnotation && rule.element == "defines"
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
                            onAddPropertyBtnClick = {
                                snyggPropertySetForEditing = propertySet
                                snyggPropertySetSpecForEditing = propertySetSpec
                                snyggPropertyToEdit = SnyggEmptyPropertyInfoForAdding
                            },
                        )
                        for ((propertyName, propertyValue) in propertySet.properties) {
                            val propertySpec = propertySetSpec?.propertySpec(propertyName)
                            if (propertySpec != null && propertySpec.level <= snyggLevel || isVariablesRule) {
                                JetPrefListItem(
                                    modifier = Modifier.rippleClickable {
                                        snyggPropertySetForEditing = propertySet
                                        snyggPropertySetSpecForEditing = propertySetSpec
                                        snyggPropertyToEdit = PropertyInfo(propertyName, propertyValue)
                                    },
                                    text = translatePropertyName(propertyName, snyggLevel),
                                    secondaryText = translatePropertyValue(propertyValue, snyggLevel),
                                    singleLineSecondaryText = true,
                                    trailing = { SnyggValueIcon(propertyValue, definedVariables) },
                                )
                            }
                        }
                    }
                    if (!isVariablesRule) {
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
                                onClick = { snyggRuleToEdit = rule },
                                icon = painterResource(R.drawable.ic_edit),
                                text = stringRes(R.string.action__edit),
                            )
                        }
                    }
                }
            } }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        val ruleToEdit = snyggRuleToEdit
        if (ruleToEdit != null) {
            EditRuleDialog(
                initRule = ruleToEdit,
                level = snyggLevel,
                onConfirmRule = { oldRule, newRule ->
                    val rules = stylesheetEditor.rules
                    when {
                        oldRule == newRule -> {
                            snyggRuleToEdit = null
                            true
                        }
                        rules.contains(newRule) -> {
                            false
                        }
                        else -> workspace.update {
                            val set = rules.remove(oldRule)
                            when {
                                set != null -> {
                                    rules[newRule] = set
                                    snyggRuleToEdit = null
                                    scope.launch {
                                        lazyListState.animateScrollToItem(index = rules.keys.indexOf(newRule))
                                    }
                                    true
                                }
                                oldRule == SnyggEmptyRuleForAdding -> {
                                    rules[newRule] = SnyggPropertySetEditor()
                                    snyggRuleToEdit = null
                                    scope.launch {
                                        lazyListState.animateScrollToItem(index = rules.keys.indexOf(newRule))
                                    }
                                    true
                                }
                                else -> {
                                    false
                                }
                            }
                        }
                    }
                },
                onDismiss = { snyggRuleToEdit = null },
            )
        }

        val propertyToEdit = snyggPropertyToEdit
        if (propertyToEdit != null) {
            EditPropertyDialog(
                propertySetSpec = snyggPropertySetSpecForEditing,
                initProperty = propertyToEdit,
                level = snyggLevel,
                definedVariables = definedVariables,
                onDelete = {
                    workspace.update {
                        snyggPropertySetForEditing?.properties?.remove(propertyToEdit.name)
                    }
                    snyggPropertyToEdit = null
                },
                onDismiss = { snyggPropertyToEdit = null },
            )
        }
    }
}

@Composable
private fun SnyggRuleRow(
    rule: SnyggRule,
    level: SnyggLevel,
    onAddPropertyBtnClick: () -> Unit,
) {
    @Composable
    fun Selector(text: String) {
        Text(
            modifier = Modifier
                .padding(end = 8.dp)
                .background(MaterialTheme.colors.primaryVariant),
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
                text = translateElementName(rule, level),
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                if (rule.pressedSelector) {
                    Selector(text = when (level) {
                        SnyggLevel.DEVELOPER -> SnyggRule.PRESSED_SELECTOR
                        else -> stringRes(R.string.snygg__rule_selector__pressed)
                    })
                }
                if (rule.focusSelector) {
                    Selector(text = when (level) {
                        SnyggLevel.DEVELOPER -> SnyggRule.FOCUS_SELECTOR
                        else -> stringRes(R.string.snygg__rule_selector__focus)
                    })
                }
                if (rule.disabledSelector) {
                    Selector(text = when (level) {
                        SnyggLevel.DEVELOPER -> SnyggRule.DISABLED_SELECTOR
                        else -> stringRes(R.string.snygg__rule_selector__disabled)
                    })
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
            onClick = onAddPropertyBtnClick,
            icon = painterResource(R.drawable.ic_add),
            text = stringRes(R.string.action__add),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colors.secondary,
            ),
        )
    }
}

@Composable
internal fun DialogProperty(
    text: String,
    trailingIconTitle: @Composable () -> Unit = { },
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                text = text,
                style = MaterialTheme.typography.subtitle2,
            )
            trailingIconTitle()
        }
        content()
    }
}

object SnyggValueIcon {
    interface Spec {
        val borderWith: Dp
        val boxShape: Shape
        val elevation: Dp
        val iconSize: Dp
        val iconSizeMinusBorder: Dp
    }

    object Small : Spec {
        override val borderWith = Dp.Hairline
        override val boxShape = RoundedCornerShape(4.dp)
        override val elevation = 4.dp
        override val iconSize = 16.dp
        override val iconSizeMinusBorder = 16.dp
    }

    object Normal : Spec {
        override val borderWith = 1.dp
        override val boxShape = RoundedCornerShape(8.dp)
        override val elevation = 4.dp
        override val iconSize = 24.dp
        override val iconSizeMinusBorder = 22.dp
    }
}

@Composable
internal fun SnyggValueIcon(
    value: SnyggValue,
    definedVariables: Map<String, SnyggValue>,
    modifier: Modifier = Modifier,
    spec: SnyggValueIcon.Spec = SnyggValueIcon.Normal,
) {
    when (value) {
        is SnyggSolidColorValue -> {
            Surface(
                modifier = modifier.requiredSize(spec.iconSize),
                color = MaterialTheme.colors.background,
                elevation = spec.elevation,
                shape = spec.boxShape,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(value.color),
                )
            }
        }
        is SnyggShapeValue -> {
            Box(
                modifier = modifier
                    .requiredSize(spec.iconSizeMinusBorder)
                    .border(spec.borderWith, MaterialTheme.colors.onBackground, value.shape)
            )
        }
        is SnyggSpSizeValue -> {
            Icon(
                modifier = modifier.requiredSize(spec.iconSize),
                painter = painterResource(R.drawable.ic_format_size),
                contentDescription = null,
            )
        }
        is SnyggDefinedVarValue -> {
            val realValue = definedVariables[value.key]
            if (realValue == null) {
                Icon(
                    modifier = modifier.requiredSize(spec.iconSize),
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = null,
                )
            } else {
                val smallSpec = SnyggValueIcon.Small
                Box(modifier = modifier.requiredSize(spec.iconSize).offset(y = (-2).dp)) {
                    SnyggValueIcon(
                        modifier = Modifier.offset(x = 8.dp, y = 8.dp),
                        value = realValue,
                        definedVariables = definedVariables,
                        spec = smallSpec,
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 1.dp)
                            .requiredSize(smallSpec.iconSize)
                            .padding(vertical = 2.dp)
                            .background(MaterialTheme.colors.background, spec.boxShape),
                    )
                    Icon(
                        modifier = Modifier.requiredSize(smallSpec.iconSize),
                        painter = painterResource(R.drawable.ic_link),
                        contentDescription = null,
                    )
                }
            }
        }
        else -> {
            // Render nothing
        }
    }
}

@Composable
internal fun translateElementName(rule: SnyggRule, level: SnyggLevel): String {
    return translateElementName(rule.element, level) ?: remember {
        buildString {
            if (rule.isAnnotation) {
                append(SnyggRule.ANNOTATION_MARKER)
            }
            append(rule.element)
        }
    }
}

@Composable
internal fun translateElementName(element: String, level: SnyggLevel): String? {
    return when (level) {
        SnyggLevel.DEVELOPER -> null
        else -> when (element) {
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
    }.let { if (it != null) { stringRes(it) } else { null } }
}

@Composable
internal fun translatePropertyName(propertyName: String, level: SnyggLevel): String {
    return when (level) {
        SnyggLevel.DEVELOPER -> null
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
            "--primary" -> R.string.snygg__property_name__var_primary
            "--primary-variant" -> R.string.snygg__property_name__var_primary_variant
            "--secondary" -> R.string.snygg__property_name__var_secondary
            "--secondary-variant" -> R.string.snygg__property_name__var_secondary_variant
            "--background" -> R.string.snygg__property_name__var_background
            "--surface" -> R.string.snygg__property_name__var_surface
            "--surface-variant" -> R.string.snygg__property_name__var_surface_variant
            "--on-primary" -> R.string.snygg__property_name__var_on_primary
            "--on-secondary" -> R.string.snygg__property_name__var_on_secondary
            "--on-background" -> R.string.snygg__property_name__var_on_background
            "--on-surface" -> R.string.snygg__property_name__var_on_surface
            else -> null
        }
    }.let { resId ->
        when {
            resId != null -> {
                stringRes(resId)
            }
            propertyName.isBlank() -> {
                stringRes(R.string.general__select_dropdown_value_placeholder)
            }
            else -> {
                propertyName
            }
        }
    }
}

@Composable
internal fun translatePropertyValue(propertyValue: SnyggValue, level: SnyggLevel): String {
    return propertyValue.encoder().serialize(propertyValue).getOrElse { propertyValue.toString() }
}

@Composable
internal fun translatePropertyValueEncoderName(encoder: SnyggValueEncoder): String {
    return when (encoder) {
        SnyggImplicitInheritValue -> R.string.general__select_dropdown_value_placeholder
        SnyggExplicitInheritValue -> R.string.snygg__property_value__explicit_inherit
        SnyggSolidColorValue -> R.string.snygg__property_value__solid_color
        SnyggRectangleShapeValue -> R.string.snygg__property_value__rectangle_shape
        SnyggCutCornerShapeDpValue -> R.string.snygg__property_value__cut_corner_shape_dp
        SnyggCutCornerShapePercentValue -> R.string.snygg__property_value__cut_corner_shape_percent
        SnyggRoundedCornerShapeDpValue -> R.string.snygg__property_value__rounded_corner_shape_dp
        SnyggRoundedCornerShapePercentValue -> R.string.snygg__property_value__rounded_corner_shape_percent
        SnyggDpSizeValue -> R.string.snygg__property_value__dp_size
        SnyggSpSizeValue -> R.string.snygg__property_value__sp_size
        SnyggPercentageSizeValue -> R.string.snygg__property_value__percentage_size
        SnyggDefinedVarValue -> R.string.snygg__property_value__defined_var
        else -> null
    }.let { if (it != null) { stringRes(it) } else { encoder::class.simpleName ?: "" } }.toString()
}
