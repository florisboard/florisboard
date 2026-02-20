package com.speekez.app.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState

@Composable
fun GeneralSettingsScreen() {
    val prefs by FlorisPreferenceStore
    var showThemePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "General Settings",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SettingToggleItem(
            title = "Copy to Clipboard",
            icon = Icons.Default.ContentPaste,
            iconColor = Color(0xFF4285F4), // Blue
            pref = prefs.speekez.copyToClipboard
        )

        SettingToggleItem(
            title = "Haptic Feedback",
            icon = Icons.Default.Vibration,
            iconColor = Color(0xFF9C27B0), // Purple
            pref = prefs.speekez.hapticEnabled
        )

        SettingNavItem(
            title = "Theme",
            icon = Icons.Default.Palette,
            iconColor = Color(0xFFFF9800), // Orange
            onClick = { showThemePicker = true }
        )
    }

    if (showThemePicker) {
        ThemePickerDialog(
            onDismiss = { showThemePicker = false },
            prefs = prefs
        )
    }
}

@Composable
fun SettingToggleItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    pref: PreferenceData<Boolean>
) {
    val state by pref.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = state,
            onCheckedChange = { pref.set(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00D4AA), // Teal accent
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF1A1A2E)
            )
        )
    }
}

@Composable
fun SettingNavItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
fun ThemePickerDialog(
    onDismiss: () -> Unit,
    prefs: dev.patrickgold.florisboard.app.FlorisPreferenceModel
) {
    val currentTheme by prefs.other.settingsTheme.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                ThemeOption(
                    label = "Light",
                    selected = currentTheme == AppTheme.LIGHT,
                    onClick = {
                        prefs.other.settingsTheme.set(AppTheme.LIGHT)
                        onDismiss()
                    }
                )
                ThemeOption(
                    label = "Dark",
                    selected = currentTheme == AppTheme.DARK,
                    onClick = {
                        prefs.other.settingsTheme.set(AppTheme.DARK)
                        onDismiss()
                    }
                )
                ThemeOption(
                    label = "System Default",
                    selected = currentTheme == AppTheme.AUTO,
                    onClick = {
                        prefs.other.settingsTheme.set(AppTheme.AUTO)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF00D4AA))
            }
        },
        containerColor = Color(0xFF12121F),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF00D4AA),
                unselectedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
