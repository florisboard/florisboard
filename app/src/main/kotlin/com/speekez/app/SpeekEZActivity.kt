package com.speekez.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.speekez.app.screens.SetupFlow
import com.speekez.voice.VoiceState
import com.speekez.voice.voiceManager
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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
                val snackbarHostState = remember { SnackbarHostState() }

                CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
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
            onPrimary = Color.Black,
            background = Color(0xFF0A0A14),
            onBackground = Color.White,
            surface = Color(0xFF12121F),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1A1A2E),
            onSurfaceVariant = Color.White,
            secondary = Color(0xFF6366F1),
            onSecondary = Color.White,
            outline = Color.Gray
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00D4AA),
            onPrimary = Color.White,
            background = Color(0xFFF8F9FA),
            onBackground = Color(0xFF1C1B1F),
            surface = Color.White,
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFE9ECEF),
            onSurfaceVariant = Color(0xFF49454F),
            secondary = Color(0xFF6366F1),
            onSecondary = Color.White,
            outline = Color.LightGray
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = LocalSnackbarHostState.current
    val voiceManager by context.voiceManager()
    val voiceState by voiceManager.state.collectAsState()
    val errorMessage by voiceManager.errorMessage.collectAsState()

    LaunchedEffect(voiceState) {
        if (voiceState == VoiceState.ERROR && errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
        }
    }
    val items = listOf(
        Screen.Dashboard to Icons.Default.Dashboard,
        Screen.History to Icons.Default.History,
        Screen.Settings to Icons.Default.Settings
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val gradient = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
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
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        SpeekEZNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
