package com.speekez.app.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // Default to General tab
    val tabs = listOf("General", "Model", "Presets")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF12121F),
            contentColor = Color(0xFF00D4AA),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFF00D4AA)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) Color(0xFF00D4AA) else Color.Gray
                        )
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> GeneralSettingsScreen()
            1 -> ModelSettingsScreen()
            2 -> PresetSettingsScreen()
        }
    }
}

@Composable
fun PlaceholderSettingsScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, color = Color.White)
    }
}
