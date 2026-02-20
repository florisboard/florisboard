package com.speekez.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.speekez.data.entity.Preset
import com.speekez.data.presetDao
import com.speekez.voice.VoiceManager
import com.speekez.voice.VoiceState
import com.speekez.voice.voiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class FloatingWidgetService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var composeView: ComposeView? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = 0
        y = 100
    }

    private val PREFS_NAME = "floating_widget_prefs"
    private val KEY_Y_POS = "y_pos"

    private val voiceManager by lazy { voiceManager().value }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)

        startAsForeground()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        params.y = sharedPrefs.getInt(KEY_Y_POS, 300)

        setupComposeView()

        voiceManager.onTranscriptionComplete = { text ->
            copyToClipboard(text)
        }
    }

    private fun setupComposeView() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingWidgetService)
            setViewTreeViewModelStoreOwner(this@FloatingWidgetService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWidgetService)

            setContent {
                val presets by presetDao().getAllPresets().collectAsStateWithLifecycle(initialValue = emptyList())
                val voiceState by voiceManager.state.collectAsStateWithLifecycle()
                val errorMessage by voiceManager.errorMessage.collectAsStateWithLifecycle()

                var isExpanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.wrapContentSize()) {
                    if (isExpanded) {
                        ExpandedWidget(
                            presets = presets,
                            voiceState = voiceState,
                            errorMessage = errorMessage,
                            onCollapse = { isExpanded = false },
                            onStartRecording = { presetId -> voiceManager.startRecording(presetId.toInt()) },
                            onStopRecording = { voiceManager.stopRecording() }
                        )
                    } else {
                        CollapsedWidget(
                            voiceState = voiceState,
                            onToggleExpand = { isExpanded = true },
                            onMove = { deltaY ->
                                params.y += deltaY.toInt()
                                windowManager.updateViewLayout(this@apply, params)
                                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_Y_POS, params.y).apply()
                            }
                        )
                    }
                }
            }
        }
        windowManager.addView(composeView, params)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("SpeekEZ Transcription", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.let {
            windowManager.removeView(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startAsForeground() {
        val channelId = "floating_widget_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SpeekEZ Floating Widget",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("SpeekEZ Floating Widget")
                .setContentText("The floating widget is active")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Use a system icon for now
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("SpeekEZ Floating Widget")
                .setContentText("The floating widget is active")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        }

        startForeground(1001, notification)
    }
}

@Composable
fun CollapsedWidget(
    voiceState: VoiceState,
    onToggleExpand: () -> Unit,
    onMove: (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by if (voiceState == VoiceState.RECORDING) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val backgroundColor = when (voiceState) {
        VoiceState.RECORDING -> Color.Red
        VoiceState.PROCESSING -> Color(0xFF6366F1) // Blue-Purple
        VoiceState.DONE -> Color.Green
        VoiceState.ERROR -> Color.Red
        else -> Color(0xFF00D4AA) // Teal
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount.y)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleExpand() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (voiceState) {
                VoiceState.PROCESSING -> Icons.Default.Sync
                VoiceState.DONE -> Icons.Default.Check
                VoiceState.ERROR -> Icons.Default.Error
                else -> Icons.Default.Mic
            },
            contentDescription = "SpeekEZ Mic",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ExpandedWidget(
    presets: List<Preset>,
    voiceState: VoiceState,
    errorMessage: String?,
    onCollapse: () -> Unit,
    onStartRecording: (Long) -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121F)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF00D4AA), Color(0xFF6366F1))
                )
                Text(
                    text = "SpeekEZ",
                    style = androidx.compose.ui.text.TextStyle(
                        brush = gradient,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                IconButton(onClick = onCollapse, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (voiceState != VoiceState.IDLE) {
                StatusIndicator(voiceState, errorMessage)
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(presets) { preset ->
                    PresetRow(
                        preset = preset,
                        isRecording = voiceState == VoiceState.RECORDING,
                        onStartRecording = { onStartRecording(preset.id) },
                        onStopRecording = onStopRecording
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(state: VoiceState, errorMessage: String?) {
    val color = when (state) {
        VoiceState.RECORDING -> Color.Red
        VoiceState.PROCESSING -> Color(0xFF6366F1)
        VoiceState.DONE -> Color.Green
        VoiceState.ERROR -> Color.Red
        else -> Color.Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (state) {
                VoiceState.RECORDING -> "Recording..."
                VoiceState.PROCESSING -> "Processing..."
                VoiceState.DONE -> "Done!"
                VoiceState.ERROR -> errorMessage ?: "Error"
                else -> ""
            },
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun PresetRow(
    preset: Preset,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStartRecording()
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                            onStopRecording()
                        }
                    }
                )
            },
        color = if (isPressed) Color(0xFF1A1A2E) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = preset.iconEmoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = preset.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = preset.inputLanguages.joinToString(", "), color = Color.Gray, fontSize = 10.sp)
            }
            if (isPressed && isRecording) {
                Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
            }
        }
    }
}
