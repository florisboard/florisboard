package com.speekez.app.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.security.EncryptedPreferencesManager
import com.speekez.core.ApiMode

@Composable
fun SetupFlow(onSetupComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val encryptedPrefs = remember { EncryptedPreferencesManager(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "SetupFlowStep"
            ) { step ->
                when (step) {
                    1 -> WelcomeStep(onNext = { currentStep = 2 })
                    2 -> ApiKeyStep(
                        encryptedPrefs = encryptedPrefs,
                        onNext = { currentStep = 3 },
                        onSkip = { currentStep = 3 }
                    )
                    3 -> EnableKeyboardStep(onNext = { currentStep = 4 })
                    4 -> AllSetStep(onFinish = onSetupComplete)
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val gradient = Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
        )
        Text(
            text = "SpeekEZ",
            style = TextStyle(
                brush = gradient,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "The AI-Powered Keyboard for effortless communication.",
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Get Started", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ApiKeyStep(
    encryptedPrefs: EncryptedPreferencesManager,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    var apiKey by remember { mutableStateOf(encryptedPrefs.getOpenRouterKey() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showSkipWarning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "API Configuration",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To enable AI voice transcription and refinement, please provide your OpenRouter API key.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        val annotatedString = buildAnnotatedString {
            append("Get a key at ")
            pushStringAnnotation(tag = "URL", annotation = "https://openrouter.ai/keys")
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append("openrouter.ai")
            }
            pop()
        }

        ClickableText(
            text = annotatedString,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    }
            },
            style = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                error = null
            },
            label = { Text("OpenRouter API Key") },
            placeholder = { Text("sk-or-...") },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (apiKey.startsWith("sk-or-")) {
                    encryptedPrefs.saveOpenRouterKey(apiKey)
                    encryptedPrefs.saveApiMode(ApiMode.OPENROUTER)
                    onNext()
                } else {
                    error = "Invalid key. Must start with 'sk-or-'"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Save & Continue", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = { showSkipWarning = true }) {
            Text("Skip for now", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }

        if (showSkipWarning) {
            AlertDialog(
                onDismissRequest = { showSkipWarning = false },
                title = { Text("Skip API Key?") },
                text = { Text("Voice features will be disabled until you provide an API key in settings.") },
                confirmButton = {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSkipWarning = false }) {
                        Text("Go Back")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EnableKeyboardStep(onNext: () -> Unit) {
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Enable Keyboard",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enable SpeekEZ in your system settings to start using it as your default keyboard.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Go to Settings", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNext) {
            Text("I've enabled it", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AllSetStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "All set!",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You're ready to use SpeekEZ. Enjoy effortless typing and AI-powered voice refinement.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Finish", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
