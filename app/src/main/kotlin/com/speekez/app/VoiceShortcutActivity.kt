package com.speekez.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speekez.voice.VoiceState
import com.speekez.voice.voiceManager
import dev.patrickgold.florisboard.FlorisApplication
import dev.patrickgold.florisboard.clipboardManager

class VoiceShortcutActivity : ComponentActivity() {
    private val TAG = "VoiceShortcutActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val presetId = intent.getIntExtra("preset_id", -1)
        if (presetId == -1) {
            Log.e(TAG, "No preset_id provided")
            finish()
            return
        }

        val voiceManager = voiceManager().value

        // Ensure transcription results are handled (e.g., copy to clipboard)
        voiceManager.onTranscriptionComplete = { text ->
            val clipboard = (application as FlorisApplication).clipboardManager.value
            clipboard.addNewPlaintext(text)
        }

        // Start recording
        voiceManager.startRecording(presetId)

        setContent {
            SpeekEZTheme {
                val state by voiceManager.state.collectAsState()
                val errorMessage by voiceManager.errorMessage.collectAsState()

                var hasStarted by remember { mutableStateOf(false) }
                LaunchedEffect(state) {
                    if (state != VoiceState.IDLE) {
                        hasStarted = true
                    }
                    if (hasStarted && state == VoiceState.IDLE) {
                        finish()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121F)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(32.dp).wrapContentSize()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (state) {
                                    VoiceState.RECORDING -> "Recording..."
                                    VoiceState.PROCESSING -> "Transcribing..."
                                    VoiceState.DONE -> "Done!"
                                    VoiceState.ERROR -> "Error"
                                    else -> "Starting..."
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )

                            if (state == VoiceState.ERROR) {
                                Text(
                                    text = errorMessage ?: "Unknown error",
                                    color = Color.Red,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (state == VoiceState.RECORDING) {
                                Button(
                                    onClick = { voiceManager.stopRecording() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4AA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop Recording", color = Color.Black)
                                }
                            } else if (state == VoiceState.ERROR || state == VoiceState.DONE) {
                                Button(
                                    onClick = { finish() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // If we leave the activity while recording, we might want to stop or cancel?
        // But for a shortcut, maybe it's fine to keep recording if it's a "background" thing?
        // Actually, normally you want to stay in the activity.
    }
}
