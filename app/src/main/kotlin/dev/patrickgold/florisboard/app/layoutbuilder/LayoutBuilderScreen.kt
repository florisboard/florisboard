package dev.patrickgold.florisboard.app.layoutbuilder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.layoutPackRepository
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import org.florisboard.lib.compose.stringRes

private data class LayoutBuilderUiState(
    val workingPack: LayoutPack,
    val undoStack: List<LayoutPack> = emptyList(),
    val redoStack: List<LayoutPack> = emptyList(),
)

@Composable
fun LayoutBuilderScreen() = FlorisScreen {
    title = stringRes(R.string.layout_builder__title)
    previewFieldVisible = false

    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val repo = context.layoutPackRepository
    val currentPack by keyboardManager.layoutFlow.collectAsState()
    var state by remember(currentPack) { mutableStateOf(LayoutBuilderUiState(currentPack)) }

    val validationErrors = remember(state.workingPack) {
        LayoutValidation.validatePack(state.workingPack)
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val pack = repo.load(uri)
                state = LayoutBuilderUiState(pack)
                // TODO: Provide user-visible feedback for successful imports.
            } catch (e: Exception) {
                // TODO: Provide user-visible feedback for failed imports.
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try {
                repo.save(state.workingPack, uri)
                // TODO: Provide user-visible feedback for successful exports.
            } catch (e: Exception) {
                // TODO: Provide user-visible feedback for failed exports.
            }
        }
    }

    fun mutatePack(block: (LayoutPack) -> LayoutPack) {
        val next = block(state.workingPack)
        if (next != state.workingPack) {
            state = state.copy(
                workingPack = next,
                undoStack = state.undoStack + state.workingPack,
                redoStack = emptyList(),
            )
        }
    }

    fun undo() {
        if (state.undoStack.isNotEmpty()) {
            val previous = state.undoStack.last()
            state = state.copy(
                workingPack = previous,
                undoStack = state.undoStack.dropLast(1),
                redoStack = state.redoStack + state.workingPack,
            )
        }
    }

    fun redo() {
        if (state.redoStack.isNotEmpty()) {
            val next = state.redoStack.last()
            state = state.copy(
                workingPack = next,
                redoStack = state.redoStack.dropLast(1),
                undoStack = state.undoStack + state.workingPack,
            )
        }
    }

    fun applyToIme() {
        keyboardManager.setLayout(state.workingPack).onFailure {
            // TODO: Provide user-visible feedback for apply failures.
        }
    }

    actions {
        TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
            Text(stringRes(R.string.layout_builder__action_import))
        }
        TextButton(onClick = { exportLauncher.launch("lcars_hacker_en_us.json") }) {
            Text(stringRes(R.string.layout_builder__action_export))
        }
        IconButton(onClick = { undo() }, enabled = state.undoStack.isNotEmpty()) {
            Icon(Icons.Default.Undo, contentDescription = stringRes(R.string.layout_builder__action_undo))
        }
        IconButton(onClick = { redo() }, enabled = state.redoStack.isNotEmpty()) {
            Icon(Icons.Default.Redo, contentDescription = stringRes(R.string.layout_builder__action_redo))
        }
        ElevatedButton(onClick = { applyToIme() }, enabled = validationErrors.isEmpty()) {
            Text(stringRes(R.string.layout_builder__action_apply))
        }
    }

    content {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Use Import to load a JSON layout pack and Apply to activate it.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Export saves your current pack as a JSON file for backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Undo and Redo let you review previous imports during this session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Current layout",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${state.workingPack.id} — ${state.workingPack.label}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (validationErrors.isEmpty()) {
                Text(
                    text = "No validation issues detected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringRes(R.string.layout_builder__validation_failed_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    validationErrors.forEach { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
