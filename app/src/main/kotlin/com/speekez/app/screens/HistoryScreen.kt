package com.speekez.app.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
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

    val transcriptions by transcriptionDao.getAllTranscriptions().collectAsState(initial = emptyList())
    val presets by presetDao.getAllPresets().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A14))
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search transcriptions...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00D4AA),
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF00D4AA)
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
                        selectedContainerColor = Color(0xFF00D4AA),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1A1A2E),
                        labelColor = Color.White
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap to open - Hold to copy",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        if (filteredTranscriptions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isEmpty()) "No transcriptions yet" else "No results found",
                    color = Color.Gray
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
                        presetEmoji = preset?.iconEmoji ?: "🎤",
                        onTap = {
                            // TODO: Navigate to detail (P2-04)
                            Toast.makeText(context, "Detail view coming soon", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
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
                    clipboardManager.setText(AnnotatedString(transcription.refinedText))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF12121F)
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
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${transcription.wordCount} words",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = transcription.refinedText.ifBlank { transcription.rawText },
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
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
