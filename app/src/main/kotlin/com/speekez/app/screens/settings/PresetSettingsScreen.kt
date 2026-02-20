package com.speekez.app.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.core.ModelTier
import com.speekez.data.entity.Preset
import com.speekez.data.entity.RefinementLevel
import com.speekez.data.presetDao
import kotlinx.coroutines.launch

@Composable
fun PresetSettingsScreen() {
    val context = LocalContext.current
    val presetDao = remember { context.presetDao() }
    val presets by presetDao.getAllPresets().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var editingPreset by remember { mutableStateOf<Preset?>(null) }
    var isFormVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                if (!isFormVisible) {
                    FloatingActionButton(
                        onClick = {
                            if (presets.size < 10) {
                                editingPreset = createEmptyPreset()
                                isFormVisible = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Preset")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (presets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(presets, key = { it.id }) { preset ->
                            PresetCard(preset = preset, onClick = {
                                editingPreset = preset
                                isFormVisible = true
                            })
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isFormVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            editingPreset?.let { preset ->
                PresetEditForm(
                    preset = preset,
                    onSave = { updatedPreset ->
                        scope.launch {
                            if (updatedPreset.id == 0L) {
                                presetDao.insertPreset(updatedPreset)
                            } else {
                                presetDao.updatePreset(updatedPreset)
                            }
                            isFormVisible = false
                        }
                    },
                    onDelete = {
                        scope.launch {
                            if (presets.size > 1) {
                                presetDao.deletePreset(preset)
                                isFormVisible = false
                            }
                        }
                    },
                    onCancel = { isFormVisible = false },
                    canDelete = presets.size > 1
                )
            }
        }
    }
}

@Composable
fun PresetCard(preset: Preset, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = preset.iconEmoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = preset.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val langSummary = preset.inputLanguages.joinToString("+") { it.uppercase() }
                Text(text = langSummary, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = preset.refinementLevel.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = preset.modelTier.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PresetEditForm(
    preset: Preset,
    onSave: (Preset) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    canDelete: Boolean
) {
    var name by remember { mutableStateOf(preset.name) }
    var iconEmoji by remember { mutableStateOf(preset.iconEmoji) }
    var inputLanguages by remember { mutableStateOf(preset.inputLanguages) }
    var outputLanguage by remember { mutableStateOf(preset.outputLanguage) }
    var refinementLevel by remember { mutableStateOf(preset.refinementLevel) }
    var modelTier by remember { mutableStateOf(preset.modelTier) }
    var systemPrompt by remember { mutableStateOf(preset.systemPrompt) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val availableLanguages = listOf("en", "te", "hi", "es", "fr", "de", "it", "ja", "ko", "zh")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = if (preset.id == 0L) "New Preset" else "Edit Preset",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                TextButton(onClick = {
                    if (name.isNotBlank() && iconEmoji.isNotBlank()) {
                        onSave(
                            preset.copy(
                                name = name,
                                iconEmoji = iconEmoji,
                                inputLanguages = inputLanguages,
                                outputLanguage = outputLanguage,
                                refinementLevel = refinementLevel,
                                modelTier = if (modelTier == ModelTier.CUSTOM) ModelTier.CHEAP else modelTier,
                                systemPrompt = systemPrompt,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Icon with Preview (P4-12)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Preview Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconEmoji.ifBlank { "\uD83E\uDD16" }, fontSize = 24.sp)
                }

                // Input Field
                OutlinedTextField(
                    value = iconEmoji,
                    onValueChange = {
                        if (it.length <= 2) {
                            iconEmoji = it
                        }
                    },
                    label = { Text("Icon (Emoji)") },
                    placeholder = { Text("Type or paste any emoji") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedPlaceholderColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Languages (Multi-select)
            Text("Input Languages", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableLanguages.forEach { lang ->
                    val selected = lang in inputLanguages
                    FilterChip(
                        selected = selected,
                        onClick = {
                            inputLanguages = if (selected) {
                                if (inputLanguages.size > 1) inputLanguages - lang else inputLanguages
                            } else {
                                inputLanguages + lang
                            }
                        },
                        label = { Text(lang.uppercase()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Output Language (Dropdown)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(
                    value = outputLanguage.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Output Language") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    availableLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.uppercase(), color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                outputLanguage = lang
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Refinement Level (3-way)
            Text("Refinement Level", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RefinementLevel.entries.forEach { level ->
                    val selected = refinementLevel == level
                    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(color, RoundedCornerShape(8.dp))
                            .clickable { refinementLevel = level }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = textColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Model Tier (2-way: Cheap/Best)
            Text("Model Tier", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(ModelTier.CHEAP, ModelTier.BEST).forEach { tier ->
                    val selected = modelTier == tier
                    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(color, RoundedCornerShape(8.dp))
                            .clickable { modelTier = tier }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tier.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = textColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // System Prompt
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Prompt") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            if (preset.id != 0L && canDelete) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                ) {
                    Text("Delete Preset", color = MaterialTheme.colorScheme.onError)
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Extra space for scrolling
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Preset?") },
                text = { Text("Are you sure you want to delete this preset? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

fun createEmptyPreset(): Preset {
    val now = System.currentTimeMillis()
    return Preset(
        name = "",
        iconEmoji = "\uD83E\uDD16", // Robot face
        inputLanguages = listOf("en"),
        outputLanguage = "en",
        refinementLevel = RefinementLevel.LIGHT,
        modelTier = ModelTier.CHEAP,
        systemPrompt = "",
        createdAt = now,
        updatedAt = now
    )
}
