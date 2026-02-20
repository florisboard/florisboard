package com.speekez.app.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.data.entity.Preset
import com.speekez.data.entity.Transcription
import com.speekez.data.transcriptionDao
import com.speekez.data.presetDao
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

enum class HistoryFilter { ALL, TODAY, THIS_WEEK, FAVORITES }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val transcriptionDao = remember { context.transcriptionDao() }
    val presetDao = remember { context.presetDao() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val transcriptions by transcriptionDao.getAllTranscriptions().collectAsState(initial = emptyList())
    val presets by presetDao.getAllPresets().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedTranscriptionId by remember { mutableStateOf<Long?>(null) }

    val selectedTranscription = remember(transcriptions, selectedTranscriptionId) {
        transcriptions.find { it.id == selectedTranscriptionId }
    }

    val filteredTranscriptions = remember(transcriptions, searchQuery, selectedFilter) {
        transcriptions.filter { transcription ->
            val matchesSearch = transcription.refinedText.contains(searchQuery, ignoreCase = true) ||
                    transcription.rawText.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.TODAY -> isToday(transcription.createdAt)
                HistoryFilter.THIS_WEEK -> isThisWeek(transcription.createdAt)
                HistoryFilter.FAVORITES -> transcription.isFavorite
            }

            matchesSearch && matchesFilter
        }
    }

    val presetMap = remember(presets) { presets.associateBy { it.id } }

    var lastSelectedTranscription by remember { mutableStateOf<Transcription?>(null) }
    if (selectedTranscription != null) {
        lastSelectedTranscription = selectedTranscription
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search transcriptions...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { filter ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to open - Hold to copy",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            if (filteredTranscriptions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No transcriptions yet" else "No results found",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(filteredTranscriptions, key = { it.id }) { transcription ->
                        val preset = presetMap[transcription.presetId]
                        TranscriptionItem(
                            transcription = transcription,
                            presetEmoji = preset?.iconEmoji ?: "\uD83C\uDFA4",
                            onTap = {
                                selectedTranscriptionId = transcription.id
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedTranscription != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            lastSelectedTranscription?.let { transcription ->
                HistoryDetailPanel(
                    transcription = transcription,
                    preset = presetMap[transcription.presetId],
                    onClose = { selectedTranscriptionId = null },
                    onToggleFavorite = { isFavorite ->
                        coroutineScope.launch {
                            transcriptionDao.setFavorite(transcription.id, isFavorite)
                        }
                    },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(transcription.refinedText.ifBlank { transcription.rawText }))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryDetailPanel(
    transcription: Transcription,
    preset: Preset?,
    onClose: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onCopy: () -> Unit
) {
    val formattedDate = remember(transcription.createdAt) {
        formatTimestamp(transcription.createdAt)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text = formattedDate,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                val duration = remember(transcription.audioDurationMs) {
                    formatDuration(transcription.audioDurationMs)
                }
                Text(
                    text = "$duration \u2022 ${transcription.wordCount} words \u2022 ${preset?.name ?: "Unknown"}",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(onClick = { onToggleFavorite(!transcription.isFavorite) }) {
                Icon(
                    imageVector = if (transcription.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (transcription.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        // Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = transcription.refinedText.ifBlank { transcription.rawText },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranscriptionItem(
    transcription: Transcription,
    presetEmoji: String,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val formattedDate = remember(transcription.createdAt) {
        formatTimestamp(transcription.createdAt)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(transcription.refinedText.ifBlank { transcription.rawText }))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = presetEmoji, fontSize = 28.sp)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${transcription.wordCount} words",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = transcription.refinedText.ifBlank { transcription.rawText },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}

private fun isToday(timestamp: Long): Boolean {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return date == LocalDate.now()
}

private fun isThisWeek(timestamp: Long): Boolean {
    val now = LocalDate.now()
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return !date.isBefore(startOfWeek)
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    val now = LocalDate.now()

    return when {
        dateTime.toLocalDate() == now -> {
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        dateTime.toLocalDate().isAfter(now.minusDays(7)) -> {
            dateTime.format(DateTimeFormatter.ofPattern("EEE HH:mm"))
        }
        else -> {
            dateTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
        }
    }
}
