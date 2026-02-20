package com.speekez.widget

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.speekez.voice.VoiceState
import com.speekez.voice.voiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpeekEZWidgetRecordingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val presetId = SpeekEZWidget1x1.getPresetId(this, appWidgetId)

        val voiceManagerLazy = voiceManager()
        val voiceManager = voiceManagerLazy.value

        setContent {
            RecordingOverlay(
                voiceStateFlow = voiceManager.state,
                errorMessageFlow = voiceManager.errorMessage,
                onStop = { voiceManager.stopRecording() },
                onCancel = {
                    voiceManager.cancelRecording()
                    finish()
                }
            )
        }

        // Start recording immediately
        voiceManager.startRecording(presetId.toInt())

        // Set up completion callback
        voiceManager.onTranscriptionComplete = { result ->
            copyToClipboard(result)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe state to finish activity when done or error
        lifecycleScope.launch {
            voiceManager.state.collect { state ->
                if (state == VoiceState.DONE) {
                    delay(1500)
                    finish()
                } else if (state == VoiceState.ERROR) {
                    delay(3000)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager().value.onTranscriptionComplete = null
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SpeekEZ Transcription", text)
        clipboard.setPrimaryClip(clip)
    }
}

private val SpeekEZTeal = Color(0xFF00D4AA)
private val SpeekEZRed = Color(0xFFFF4444)
private val SpeekEZPurple = Color(0xFF8A2BE2)
private val SpeekEZGreen = Color(0xFF4CAF50)

@Composable
fun RecordingOverlay(
    voiceStateFlow: StateFlow<VoiceState>,
    errorMessageFlow: StateFlow<String?>,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    val state by voiceStateFlow.collectAsState()
    val errorMessage by errorMessageFlow.collectAsState()

    var timerSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(state) {
        if (state == VoiceState.RECORDING) {
            timerSeconds = 0
            while (true) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF12121F))
                .padding(32.dp)
                .clickable(enabled = false) {} // Prevent click-through
        ) {
            BigMicButton(state = state, onClick = {
                if (state == VoiceState.RECORDING) {
                    onStop()
                } else if (state == VoiceState.IDLE || state == VoiceState.ERROR || state == VoiceState.DONE) {
                    onCancel()
                }
            })

            Spacer(modifier = Modifier.height(24.dp))

            when (state) {
                VoiceState.RECORDING -> {
                    val minutes = timerSeconds / 60
                    val seconds = timerSeconds % 60
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        color = SpeekEZRed,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Recording...", color = Color.White, fontSize = 16.sp)
                }
                VoiceState.PROCESSING -> {
                    Text(text = "Transcribing...", color = SpeekEZPurple, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
                VoiceState.DONE -> {
                    Text(text = "Done!", color = SpeekEZGreen, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
                VoiceState.ERROR -> {
                    Text(
                        text = errorMessage ?: "Error",
                        color = SpeekEZRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun BigMicButton(
    state: VoiceState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            VoiceState.IDLE -> SpeekEZTeal
            VoiceState.RECORDING -> SpeekEZRed
            VoiceState.PROCESSING -> SpeekEZPurple
            VoiceState.DONE -> SpeekEZGreen
            VoiceState.ERROR -> SpeekEZRed
        },
        animationSpec = tween(200), label = "backgroundColor"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .let {
                when (state) {
                    VoiceState.RECORDING -> it.drawBehind {
                        drawCircle(SpeekEZRed.copy(alpha = 0.3f), radius = (size.minDimension / 2) * pulseScale, center = center)
                    }
                    VoiceState.PROCESSING -> it.drawBehind {
                        rotate(rotation) {
                            drawCircle(
                                brush = Brush.sweepGradient(listOf(SpeekEZPurple.copy(alpha = 0.1f), SpeekEZPurple, SpeekEZPurple.copy(alpha = 0.1f)), center = center),
                                radius = (size.minDimension / 2) + 8.dp.toPx(), style = Stroke(width = 4.dp.toPx())
                            )
                        }
                    }
                    else -> it
                }
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val icon = when (state) {
            VoiceState.IDLE, VoiceState.RECORDING, VoiceState.PROCESSING -> Icons.Default.Mic
            VoiceState.DONE -> Icons.Default.Check
            VoiceState.ERROR -> Icons.Default.Error
        }
        Icon(
            imageVector = icon,
            contentDescription = "Mic",
            modifier = Modifier.size(40.dp),
            tint = Color.White
        )
    }
}
