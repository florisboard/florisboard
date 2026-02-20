package com.speekez.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.speekez.app.screens.SetupFlow
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class SpeekEZActivity : ComponentActivity() {
    private val prefs by FlorisPreferenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeekEZTheme {
                val setupComplete by prefs.speekez.setupComplete.collectAsState()
                if (setupComplete) {
                    MainScreen()
                } else {
                    val scope = rememberCoroutineScope()
                    SetupFlow(onSetupComplete = {
                        scope.launch {
                            prefs.speekez.setupComplete.set(true)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun SpeekEZTheme(content: @Composable () -> Unit) {
    val prefs by FlorisPreferenceStore
    val themeMode by prefs.other.settingsTheme.collectAsState()

    val isDark = when (themeMode) {
        AppTheme.DARK, AppTheme.AMOLED_DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.AUTO, AppTheme.AUTO_AMOLED -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = Color(0xFF00D4AA),
            surface = Color(0xFF12121F),
            background = Color(0xFF0A0A14),
            surfaceVariant = Color(0xFF1A1A2E),
            onPrimary = Color.Black,
            onSurface = Color.White,
            onBackground = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00D4AA),
            surface = Color.White,
            background = Color.White,
            onPrimary = Color.White,
            onSurface = Color.Black,
            onBackground = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Dashboard to Icons.Default.Dashboard,
        Screen.History to Icons.Default.History,
        Screen.Settings to Icons.Default.Settings
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF00D4AA), Color(0xFF6366F1)) // Teal to Blue-Purple
                    )
                    Text(
                        text = "SpeekEZ",
                        style = TextStyle(
                            brush = gradient,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A0A14)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF12121F)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { (screen, icon) ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00D4AA),
                            selectedTextColor = Color(0xFF00D4AA),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF0A0A14)
    ) { innerPadding ->
        SpeekEZNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
