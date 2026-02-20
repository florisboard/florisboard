package com.speekez.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speekez.data.entity.Preset
import com.speekez.data.presetDao

class SpeekEZWidget2x1ConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            WidgetConfigTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PresetSelectionScreen(
                        onPresetSelected = { preset ->
                            handlePresetSelected(preset)
                        }
                    )
                }
            }
        }
    }

    private fun handlePresetSelected(preset: Preset) {
        val context = this@SpeekEZWidget2x1ConfigActivity
        SpeekEZWidget2x1.savePresetId(context, appWidgetId, preset.id)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        SpeekEZWidget2x1.updateAppWidget(context, appWidgetManager, appWidgetId)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun WidgetConfigTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF00D4AA),
        surface = Color(0xFF12121F),
        background = Color(0xFF0A0A14),
        onSurface = Color.White,
        onBackground = Color.White
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun PresetSelectionScreen(onPresetSelected: (Preset) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val presets by context.presetDao().getAllPresets().collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Select a Preset",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn {
            items(presets) { preset ->
                ListItem(
                    headlineContent = { Text(preset.name) },
                    supportingContent = { Text(preset.inputLanguages.joinToString(", ")) },
                    leadingContent = { Text(preset.iconEmoji, style = MaterialTheme.typography.headlineSmall) },
                    modifier = Modifier.clickable { onPresetSelected(preset) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = Color.White,
                        supportingColor = Color.LightGray
                    )
                )
                HorizontalDivider(color = Color(0xFF1A1A2E))
            }
        }
    }
}
