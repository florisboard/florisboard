package dev.patrickgold.florisboard.app.layoutbuilder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (validationErrors.isNotEmpty()) {
                item("validation_errors") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringRes(R.string.layout_builder__validation_failed_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                        validationErrors.forEach { error ->
                            Text(text = error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item("keyboard_preview") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                    ) {
                        KeyboardPreview(pack = state.workingPack)
                    }
                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }

            itemsIndexed(
                items = state.workingPack.rows,
                key = { _, row -> row.id },
            ) { index, row ->
                LayoutRowEditor(
                    index = index,
                    pack = state.workingPack,
                    row = row,
                    onUpdate = { updatedRow ->
                        mutatePack { pack ->
                            val rows = pack.rows.toMutableList()
                            rows[index] = updatedRow
                            pack.copy(rows = rows)
                        }
                    },
                    onDeleteKey = { keyIndex ->
                        mutatePack { pack ->
                            val rows = pack.rows.toMutableList()
                            val keys = rows[index].keys.toMutableList()
                            if (keyIndex in keys.indices) {
                                keys.removeAt(keyIndex)
                                rows[index] = rows[index].copy(keys = keys)
                            }
                            pack.copy(rows = rows)
                        }
                    },
                    onAddKey = {
                        mutatePack { pack ->
                            val rows = pack.rows.toMutableList()
                            val keys = rows[index].keys.toMutableList()
                            keys.add(LayoutKey(label = "", code = "", units = 1))
                            rows[index] = rows[index].copy(keys = keys)
                            pack.copy(rows = rows)
                        }
                    },
                    onMoveUp = {
                        if (index > 0) {
                            mutatePack { pack ->
                                val rows = pack.rows.toMutableList()
                                rows.removeAt(index).also { moved ->
                                    rows.add(index - 1, moved)
                                }
                                pack.copy(rows = rows)
                            }
                        }
                    },
                    onMoveDown = {
                        if (index < state.workingPack.rows.lastIndex) {
                            mutatePack { pack ->
                                val rows = pack.rows.toMutableList()
                                rows.removeAt(index).also { moved ->
                                    rows.add(index + 1, moved)
                                }
                                pack.copy(rows = rows)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LayoutRowEditor(
    index: Int,
    pack: LayoutPack,
    row: LayoutRow,
    onUpdate: (LayoutRow) -> Unit,
    onDeleteKey: (Int) -> Unit,
    onAddKey: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val rowValidation = remember(row, pack.units) { LayoutValidation.validateRow(row, pack.units) }
    val sumUnitsError = rowValidation.any { it.startsWith("Σu") }
    val additionalRowErrors = rowValidation.filterNot { it.startsWith("Σu") }
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.id,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Checkbox(
                    checked = row.enabled,
                    onCheckedChange = { checked -> onUpdate(row.copy(enabled = checked)) },
                )
                IconButton(onClick = onMoveUp, enabled = index > 0) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                }
                IconButton(onClick = onMoveDown, enabled = index < pack.rows.lastIndex) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringRes(
                    R.string.layout_builder__validation_sum_units,
                    "arg0" to row.keys.sumOf { it.units },
                    "arg1" to pack.units
                ),
                color = if (sumUnitsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
            )
            if (!row.showIfSetting.isNullOrBlank()) {
                Text(
                    text = stringRes(
                        R.string.layout_builder__row_condition,
                        "arg0" to row.showIfSetting!!
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (additionalRowErrors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    additionalRowErrors.forEach { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                row.keys.forEachIndexed { keyIndex, key ->
                    LayoutKeyEditor(
                        key = key,
                        onUpdate = { updatedKey ->
                            val updatedKeys = row.keys.toMutableList()
                            updatedKeys[keyIndex] = updatedKey
                            onUpdate(row.copy(keys = updatedKeys))
                        },
                        onDelete = { onDeleteKey(keyIndex) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onAddKey) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringRes(R.string.layout_builder__add_key))
            }
        }
    }
}

@Composable
private fun LayoutKeyEditor(
    key: LayoutKey,
    onUpdate: (LayoutKey) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = key.label.ifBlank { stringRes(R.string.layout_builder__untitled_key) },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringRes(R.string.layout_builder__delete_key))
                }
            }
            OutlinedTextField(
                value = key.label,
                onValueChange = { onUpdate(key.copy(label = it)) },
                label = { Text(stringRes(R.string.layout_builder__field_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = key.code,
                onValueChange = { onUpdate(key.copy(code = it)) },
                label = { Text(stringRes(R.string.layout_builder__field_code)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = key.units.toString(),
                onValueChange = { value ->
                    val units = value.toIntOrNull() ?: key.units
                    onUpdate(key.copy(units = units.coerceAtLeast(1)))
                },
                label = { Text(stringRes(R.string.layout_builder__field_units)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            KeyStyleDropdown(selected = key.style, onStyleSelected = { style -> onUpdate(key.copy(style = style)) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = key.spacer,
                    onCheckedChange = { checked -> onUpdate(key.copy(spacer = checked)) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringRes(R.string.layout_builder__field_spacer))
            }
        }
    }
}

@Composable
private fun KeyStyleDropdown(
    selected: LayoutKeyStyle,
    onStyleSelected: (LayoutKeyStyle) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = stringRes(
                    R.string.layout_builder__field_style,
                    "arg0" to selected.name.lowercase()
                )
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LayoutKeyStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = { Text(style.name.lowercase()) },
                    onClick = {
                        expanded = false
                        onStyleSelected(style)
                    },
                )
            }
        }
    }
}

@Composable
private fun KeyboardPreview(pack: LayoutPack) {
    val enabledRows = pack.rows.filter { it.enabled }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (row in enabledRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                val totalUnits = row.units.takeIf { it > 0 } ?: pack.units
                for (key in row.keys) {
                    val weight = key.units.coerceAtLeast(1).toFloat() / totalUnits.coerceAtLeast(1).toFloat()
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .padding(horizontal = 2.dp)
                            .fillMaxHeight()
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall)
                            .background(
                                if (key.spacer) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                MaterialTheme.shapes.extraSmall,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key.label.ifBlank { key.code },
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
