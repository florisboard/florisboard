package com.speekez.app.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.app.SpeekEZTheme
import com.speekez.data.entity.Preset
import com.speekez.data.presetDao
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        // Find the widget id from the intent.
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an invalid widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            SpeekEZTheme {
                WidgetConfigScreen(
                    onPresetSelected = { presetId ->
                        saveWidgetConfig(appWidgetId, presetId)

                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun saveWidgetConfig(widgetId: Int, presetId: Long) {
        val prefs = getSharedPreferences("speekez_widgets", Context.MODE_PRIVATE)
        prefs.edit().putLong("widget_$widgetId", presetId).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    onPresetSelected: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val presetDao = remember { context.presetDao() }
    val presets by presetDao.getAllPresets().collectAsState(initial = null)
    var selectedPresetId by remember { mutableLongStateOf(-1L) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configure Widget", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                },
                actions = {
                    if (selectedPresetId != -1L) {
                        IconButton(onClick = { onPresetSelected(selectedPresetId) }) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color(0xFF00D4AA))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A0A14)
                )
            )
        },
        containerColor = Color(0xFF0A0A14)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                presets == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00D4AA))
                    }
                }
                presets!!.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No presets found. Please create a preset in the app first.",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(presets!!, key = { it.id }) { preset ->
                            WidgetPresetCard(
                                preset = preset,
                                isSelected = selectedPresetId == preset.id,
                                onClick = { selectedPresetId = preset.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPresetCard(preset: Preset, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF12121F)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF00D4AA) else Color(0xFF1A1A2E)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = preset.iconEmoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = preset.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val langSummary = "${preset.inputLanguages.joinToString("+") { it.uppercase() }} \u2192 ${preset.outputLanguage.uppercase()}"
                Text(text = langSummary, color = Color(0xFF00D4AA), fontSize = 14.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00D4AA))
            }
        }
    }
}
