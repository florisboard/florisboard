/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.components.SegmentedRadioListItem
import dev.patrickgold.florisboard.app.settings.theme.DialogProperty
import dev.patrickgold.florisboard.ime.extension.ExtensionComponent
import dev.patrickgold.florisboard.ime.extension.ExtensionComponentName
import dev.patrickgold.florisboard.ime.extension.ExtensionIndex
import dev.patrickgold.florisboard.ime.extension.ExtensionValidation
import dev.patrickgold.florisboard.ime.extension.ThemeExtension
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.Storage
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showLongToast
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.florisboard.lib.compose.Validation
import dev.patrickgold.florisboard.lib.rememberValidationResult
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import kotlinx.coroutines.launch
import okio.IOException
import dev.patrickgold.florisboard.app.components.FlorisBottomBarPrimaryButton
import dev.patrickgold.florisboard.app.components.FlorisBottomBarSecondaryButton
import dev.patrickgold.florisboard.app.components.FlorisBottomBar
import org.florisboard.lib.compose.FlorisInfoCard
import org.florisboard.lib.compose.defaultFlorisOutlinedBox
import org.florisboard.lib.compose.stringRes
import kotlin.reflect.KClass

enum class CreateFrom {
    EMPTY,
    EXISTING;
}

@Composable
fun ExtensionCreateComponentOverlay(
    storage: Storage,
    workspaceRef: FlorisRef,
    extensionId: String?,
    extensionIndex: ExtensionIndex,
    components: List<ExtensionComponent>,
    componentKlass: KClass<*>,
    onCreate: (ExtensionComponent) -> Unit,
    onDismiss: () -> Unit,
) {
    val interactionController = LocalInteractionController.current

    val scope = rememberCoroutineScope()
    var createFrom by rememberSaveable { mutableStateOf(CreateFrom.EXISTING) }
    val componentsForSelection = remember(extensionId, extensionIndex, components, componentKlass) {
        buildMap {
            for (component in components) {
                put(ExtensionComponentName("", component.id), component)
            }
            for ((_, extension) in extensionIndex.extensions) {
                if (extension.manifest.meta.id == extensionId) {
                    continue
                }
                for (component in extension.manifest.components()) {
                    if (component::class == componentKlass) {
                        put(ExtensionComponentName(extension.manifest.meta.id, component.id), component)
                    }
                }
            }
        }
    }
    var selectedComponentName by rememberSaveable(stateSaver = ExtensionComponentName.Saver) {
        mutableStateOf(null)
    }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    var newId by rememberSaveable { mutableStateOf("") }
    val newIdValidation = rememberValidationResult(ExtensionValidation.ComponentId, newId)
    var newLabel by rememberSaveable { mutableStateOf("") }
    val newLabelValidation = rememberValidationResult(ExtensionValidation.ComponentLabel, newLabel)
    var newAuthors by rememberSaveable { mutableStateOf("") }
    val newAuthorsValidation = rememberValidationResult(ExtensionValidation.ComponentAuthors, newAuthors)

    fun hasSufficientInfoForCreating(): Boolean {
        return when (createFrom) {
            CreateFrom.EMPTY -> newId.isNotBlank() && newLabel.isNotBlank() && newAuthors.isNotBlank()
            CreateFrom.EXISTING -> componentsForSelection.containsKey(selectedComponentName)
        }
    }

    suspend fun handleCreateThemeStylesheet() {
        when (createFrom) {
            CreateFrom.EMPTY -> {
                if (components.any { it.id == newId.trim() }) {
                    interactionController.showLongToast("A theme with this ID already exists!")
                } else {
                    val component = ThemeExtension.ThemeComponent(
                        id = newId.trim(),
                        name = newLabel.trim(),
                        authors = newAuthors.lines().map { it.trim() }.filter { it.isNotBlank() },
                    )
                    onCreate(component)
                }
            }
            CreateFrom.EXISTING -> {
                val componentName = selectedComponentName ?: return
                val componentId = if (components.any { it.id == componentName.componentId }) {
                    var suffix = 1
                    var tempId: String
                    do {
                        tempId = "${componentName.componentId}_${suffix++}"
                    } while (components.any { it.id == tempId })
                    tempId
                } else {
                    componentName.componentId
                }
                val existingComponent = (componentsForSelection[componentName] as? ThemeExtension.ThemeComponent ?: return)
                val newComponent = existingComponent
                    .copy(
                        id = componentId,
                        stylesheetPath = null,
                    )
                val srcRef = when {
                    componentName.extensionId == "" -> {
                        workspaceRef.subRef(existingComponent.stylesheetPath())
                    }
                    else -> {
                        val externalExt = extensionIndex.extensions[componentName.extensionId] ?: return
                        externalExt.sourceRef.subRef(existingComponent.stylesheetPath())
                    }
                }
                val dstRef = workspaceRef.subRef(newComponent.stylesheetPath())
                // it may be that the src component is declared but has no stylesheet file
                if (storage.exists(srcRef)) {
                    try {
                        val parentRef = dstRef.parentRef
                            ?: throw IOException("dstRef has no parent??")
                        storage.mkdirs(parentRef)
                        storage.copy(srcRef, dstRef)
                    } catch (e: IOException) {
                        interactionController.showLongToast(e.toString())
                        return
                    }
                }
                onCreate(newComponent)
            }
        }
    }

    fun handleCreate() {
        val invalid = createFrom == CreateFrom.EMPTY && (newIdValidation.isInvalid() ||
            newLabelValidation.isInvalid() || newAuthorsValidation.isInvalid())
        if (invalid) {
            showValidationErrors = true
            return
        }
        scope.launch {
            when (componentKlass) {
                ThemeExtension.ThemeComponent::class -> handleCreateThemeStylesheet()
                else -> error("no create handler available for class '$componentKlass'")
            }
        }
    }

    BackHandler {
        onDismiss()
    }

    FlorisScreenNg(
        title = stringRes(when (componentKlass) {
            ThemeExtension.ThemeComponent::class -> R.string.ext__editor__create_component__title_theme
            else -> R.string.ext__editor__create_component__title
        }),
        previewFieldVisible = false,
        navigationIcon = Icons.Default.Close,
        navigationIconOnClick = onDismiss,
        bottomBar = {
            FlorisBottomBar {
                Spacer(modifier = Modifier.weight(1f))
                FlorisBottomBarSecondaryButton(
                    onClick = onDismiss,
                    text = stringRes(R.string.action__cancel)
                )
                FlorisBottomBarPrimaryButton(
                    onClick = { handleCreate() },
                    text = stringRes(R.string.action__create),
                    enabled = hasSufficientInfoForCreating(),
                )
            }
        },
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            SegmentedListColumn {
                val count = 2
                SegmentedRadioListItem(
                    onClick = { createFrom = CreateFrom.EXISTING },
                    selected = createFrom == CreateFrom.EXISTING,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    content = { Text(stringRes(R.string.ext__editor__create_component__from_existing)) },
                )
                SegmentedRadioListItem(
                    onClick = { createFrom = CreateFrom.EMPTY },
                    selected = createFrom == CreateFrom.EMPTY,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    content = { Text(stringRes(R.string.ext__editor__create_component__from_empty)) },
                )
            }

            if (createFrom == CreateFrom.EXISTING) {
                SegmentedListColumn {
                    val count = componentsForSelection.size
                    for ((index, entry) in componentsForSelection.entries.withIndex()) {
                        val (componentName, component) = entry
                        SegmentedRadioListItem(
                            onClick = { selectedComponentName = componentName },
                            selected = selectedComponentName == componentName,
                            shapes = ListItemDefaults.segmentedShapes(index, count),
                            content = { Text(component.name) },
                            supportingContent = { Text(componentName.toString()) },
                        )
                    }
                }
            } else if (createFrom == CreateFrom.EMPTY) {
                FlorisInfoCard(
                    modifier = Modifier.defaultFlorisOutlinedBox(),
                    text = stringRes(R.string.ext__editor__create_component__from_empty_warning),
                )
                DialogProperty(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringRes(R.string.ext__meta__id),
                ) {
                    JetPrefTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newId,
                        onValueChange = { newId = it },
                        singleLine = true,
                    )
                    Validation(showValidationErrors, newIdValidation)
                }
                DialogProperty(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringRes(R.string.ext__meta__label),
                ) {
                    JetPrefTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        singleLine = true,
                    )
                    Validation(showValidationErrors, newLabelValidation)

                }
                DialogProperty(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringRes(R.string.ext__meta__authors),
                ) {
                    JetPrefTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newAuthors,
                        onValueChange = { newAuthors = it },
                    )
                    Validation(showValidationErrors, newAuthorsValidation)
                }
            }
        }
    }
}
