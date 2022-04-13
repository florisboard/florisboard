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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.patrickgold.florisboard.R
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

@Composable
fun FlorisConfirmDeleteDialog(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    what: String,
) {
    JetPrefAlertDialog(
        modifier = modifier,
        title = stringRes(R.string.action__delete_confirm_title),
        confirmLabel = stringRes(R.string.action__delete),
        onConfirm = onConfirm,
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
    ) {
        Text(text = stringRes(R.string.action__delete_confirm_message, "name" to what))
    }
}

@Composable
fun FlorisUnsavedChangesDialog(
    modifier: Modifier = Modifier,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    JetPrefAlertDialog(
        modifier = modifier,
        title = stringRes(R.string.action__discard_confirm_title),
        confirmLabel = stringRes(R.string.action__save),
        onConfirm = onSave,
        dismissLabel = stringRes(R.string.action__discard),
        onDismiss = onDiscard,
        onOutsideDismissal = onDismiss,
        neutralLabel = stringRes(R.string.action__cancel),
        onNeutral = onDismiss,
    ) {
        Text(text = stringRes(R.string.action__discard_confirm_message))
    }
}
