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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.ime.extension.ExtensionMaintainer
import dev.patrickgold.florisboard.ime.extension.ExtensionMeta
import dev.patrickgold.florisboard.ime.extension.ExtensionValidation
import dev.patrickgold.florisboard.lib.ValidationResult
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.florisboard.lib.compose.Validation
import dev.patrickgold.florisboard.lib.rememberValidationResult
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import org.florisboard.lib.compose.ButtonBarButton
import org.florisboard.lib.compose.ButtonBarTextButton
import org.florisboard.lib.compose.FlorisButtonBar
import org.florisboard.lib.compose.stringRes

private val TextFieldVerticalPadding = 8.dp

@Composable
fun ExtensionEditMetaOverlay(
    meta: ExtensionMeta,
    isCreateExt: Boolean,
    onConfirm: (ExtensionMeta) -> Unit,
    onDismiss: () -> Unit,
) {
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    var id by rememberSaveable { mutableStateOf(meta.id) }
    val idValidation = rememberValidationResult(ExtensionValidation.MetaId, id)
    var version by rememberSaveable { mutableStateOf(meta.version) }
    val versionValidation = rememberValidationResult(ExtensionValidation.MetaVersion, version)
    var title by rememberSaveable { mutableStateOf(meta.title) }
    val titleValidation = rememberValidationResult(ExtensionValidation.MetaTitle, title)
    var description by rememberSaveable { mutableStateOf(meta.description ?: "") }
    var keywords by rememberSaveable { mutableStateOf(meta.keywords?.joinToString("\n") ?: "") }
    var homepage by rememberSaveable { mutableStateOf(meta.homepage ?: "") }
    var issueTracker by rememberSaveable { mutableStateOf(meta.issueTracker ?: "") }
    var maintainers by rememberSaveable { mutableStateOf(meta.maintainers.joinToString("\n")) }
    val maintainersValidation = rememberValidationResult(ExtensionValidation.MetaMaintainers, maintainers)
    var license by rememberSaveable { mutableStateOf(meta.license) }
    val licenseValidation = rememberValidationResult(ExtensionValidation.MetaLicense, license)

    fun handleApply() {
        val invalid = idValidation.isInvalid() ||
            versionValidation.isInvalid() ||
            titleValidation.isInvalid() ||
            maintainersValidation.isInvalid() ||
            licenseValidation.isInvalid()
        if (invalid) {
            showValidationErrors = true
        } else {
            val newMeta = ExtensionMeta(
                id = id.trim(),
                version = version.trim(),
                title = title.trim(),
                description = description.trim().takeIf { it.isNotBlank() },
                keywords = keywords.lines().map { it.trim() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() },
                homepage = homepage.trim().takeIf { it.isNotBlank() },
                issueTracker = issueTracker.trim().takeIf { it.isNotBlank() },
                maintainers = maintainers.lines().map { it.trim() }.filter { it.isNotBlank() }
                    .map { ExtensionMaintainer.fromOrTakeRaw(it) },
                license = license.trim(),
            )
            onConfirm(newMeta)
        }
    }

    BackHandler {
        onDismiss()
    }

    FlorisScreenNg(
        title = stringRes(R.string.ext__editor__metadata__title),
        previewFieldVisible = false,
        navigationIcon = Icons.Default.Close,
        navigationIconOnClick = onDismiss,
        bottomBar = {
            FlorisButtonBar {
                Spacer(modifier = Modifier.weight(1f))
                ButtonBarTextButton(text = stringRes(R.string.action__cancel)) {
                    onDismiss()
                }
                ButtonBarButton(text = stringRes(R.string.action__apply)) {
                    handleApply()
                }
            }
        },
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            EditorSheetTextField(
                enabled = isCreateExt,
                isRequired = true,
                value = id,
                onValueChange = { id = it },
                label = stringRes(R.string.ext__meta__id),
                showValidationError = showValidationErrors,
                validationResult = idValidation,
            )
            EditorSheetTextField(
                isRequired = true,
                value = version,
                onValueChange = { version = it },
                label = stringRes(R.string.ext__meta__version),
                showValidationError = showValidationErrors,
                validationResult = versionValidation,
            )
            EditorSheetTextField(
                isRequired = true,
                value = title,
                onValueChange = { title = it },
                label = stringRes(R.string.ext__meta__title),
                showValidationError = showValidationErrors,
                validationResult = titleValidation,
            )
            EditorSheetTextField(
                value = description,
                onValueChange = { description = it },
                label = stringRes(R.string.ext__meta__description),
            )
            EditorSheetTextField(
                value = keywords,
                onValueChange = { keywords = it },
                label = stringRes(R.string.ext__meta__keywords),
                singleLine = false,
            )
            EditorSheetTextField(
                value = homepage,
                onValueChange = { homepage = it },
                label = stringRes(R.string.ext__meta__homepage),
            )
            EditorSheetTextField(
                value = issueTracker,
                onValueChange = { issueTracker = it },
                label = stringRes(R.string.ext__meta__issue_tracker),
            )
            EditorSheetTextField(
                isRequired = true,
                value = maintainers,
                onValueChange = { maintainers = it },
                label = stringRes(R.string.ext__meta__maintainers),
                singleLine = false,
                showValidationError = showValidationErrors,
                validationResult = maintainersValidation,
            )
            EditorSheetTextField(
                isRequired = true,
                value = license,
                onValueChange = { license = it },
                label = stringRes(R.string.ext__meta__license),
                showValidationError = showValidationErrors,
                validationResult = licenseValidation,
            )
        }
    }
}

@Composable
private fun EditorSheetTextField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRequired: Boolean = false,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    showValidationError: Boolean = false,
    validationResult: ValidationResult? = null,
) {
    Column(modifier = Modifier.padding(vertical = TextFieldVerticalPadding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TextFieldVerticalPadding),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
            )
            if (isRequired) {
                Text(
                    modifier = Modifier.padding(start = 2.dp),
                    text = "*",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        JetPrefTextField(
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
        )
        Validation(showValidationError, validationResult)
    }
}
