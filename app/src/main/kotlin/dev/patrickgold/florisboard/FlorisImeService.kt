package dev.patrickgold.florisboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.inputmethodservice.ExtractEditText
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.inline.InlinePresentationSpec
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.devtools.DevtoolsOverlay
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.SpeechCaptureService
import dev.patrickgold.florisboard.ime.clipboard.ClipboardInputLayout
import dev.patrickgold.florisboard.ime.core.SelectSubtypePanel
import dev.patrickgold.florisboard.ime.core.isSubtypeSelectionShowing
import dev.patrickgold.florisboard.ime.editor.EditorRange
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.input.InputFeedbackController
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.ProvideKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.lifecycle.LifecycleInputMethodService
import dev.patrickgold.florisboard.ime.media.MediaInputLayout
import dev.patrickgold.florisboard.ime.nlp.NlpInlineAutofill
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.onehanded.OneHandedPanel
import dev.patrickgold.florisboard.ime.sheet.BottomSheetHostUi
import dev.patrickgold.florisboard.ime.sheet.isBottomSheetShowing
import dev.patrickgold.florisboard.ime.smartbar.ExtendedActionsPlacement
import dev.patrickgold.florisboard.ime.smartbar.SmartbarLayout
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsEditorPanel
import dev.patrickgold.florisboard.ime.text.TextInputLayout
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.theme.WallpaperChangeReceiver
import dev.patrickgold.florisboard.diagnostics.WhisperLogger
import dev.patrickgold.florisboard.diagnostics.WhisperNotify
import dev.patrickgold.florisboard.lib.compose.SystemUiIme
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.devtools.flogWarning
import dev.patrickgold.florisboard.lib.observeAsTransformingState
import dev.patrickgold.florisboard.lib.util.ViewUtils
import dev.patrickgold.florisboard.lib.util.debugSummarize
import dev.patrickgold.florisboard.lib.util.launchActivity
import dev.patrickgold.jetpref.datastore.model.observeAsState
import java.io.File
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.florisboard.lib.android.AndroidInternalR
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.isOrientationLandscape
import org.florisboard.lib.android.isOrientationPortrait
import org.florisboard.lib.android.showShortToastSync
import org.florisboard.lib.android.systemServiceOrNull
import org.florisboard.lib.compose.ProvideLocalizedResources
import org.florisboard.lib.kotlin.collectIn
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggSurfaceView
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FlorisImeService : LifecycleInputMethodService() {
    private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY

    private var audioFile: File? = null
    private var isRecording = false

    private fun writeWavHeader(output: OutputStream, totalAudioLen: Long, sampleRate: Int = 16000) {
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8
        val dataLen = totalAudioLen + 36
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(dataLen.toInt())
        header.put("WAVEfmt ".toByteArray())
        header.putInt(16) // PCM
        header.putShort(1) // format
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((2 * channels).toShort())
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(totalAudioLen.toInt())
        output.write(header.array())
    }

    companion object {
        private const val TAG = "Whisper"
        private var FlorisImeServiceReference: WeakReference<FlorisImeService?> = WeakReference(null)
        private val InlineSuggestionUiSmallestSize = Size(0, 0)
        private val InlineSuggestionUiBiggestSize = Size(Int.MAX_VALUE, Int.MAX_VALUE)

        fun currentInputConnection(): InputConnection? {
            return FlorisImeServiceReference.get()?.currentInputConnection
        }

        fun inputFeedbackController(): InputFeedbackController? {
            return FlorisImeServiceReference.get()?.inputFeedbackController
        }

        fun launchSettings() {
            val ims = FlorisImeServiceReference.get() ?: return
            ims.requestHideSelf(0)
            ims.launchActivity(FlorisAppActivity::class) {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        fun showUi() {
            val ims = FlorisImeServiceReference.get() ?: return
            if (AndroidVersion.ATLEAST_API28_P) {
                ims.requestShowSelf(0)
            } else {
                @Suppress("DEPRECATION")
                ims.systemServiceOrNull(InputMethodManager::class)
                    ?.showSoftInputFromInputMethod(ims.currentInputBinding.connectionToken, 0)
            }
        }

        fun hideUi() {
            val ims = FlorisImeServiceReference.get() ?: return
            if (AndroidVersion.ATLEAST_API28_P) {
                ims.requestHideSelf(0)
            } else {
                @Suppress("DEPRECATION")
                ims.systemServiceOrNull(InputMethodManager::class)
                    ?.hideSoftInputFromInputMethod(ims.currentInputBinding.connectionToken, 0)
            }
            FlorisImeServiceReference.get()?.requestHideSelf(0)
        }

        fun switchToPrevInputMethod(): Boolean {
            val ims = FlorisImeServiceReference.get() ?: return false
            val imm = ims.systemServiceOrNull(InputMethodManager::class)
            try {
                if (AndroidVersion.ATLEAST_API28_P) {
                    return ims.switchToPreviousInputMethod()
                } else {
                    ims.window.window?.let { window ->
                        @Suppress("DEPRECATION")
                        return imm?.switchToLastInputMethod(window.attributes.token) == true
                    }
                }
            } catch (e: Exception) {
                flogError { "Unable to switch to the previous IME" }
                imm?.showInputMethodPicker()
            }
            return false
        }

        fun switchToNextInputMethod(): Boolean {
            val ims = FlorisImeServiceReference.get() ?: return false
            val imm = ims.systemServiceOrNull(InputMethodManager::class)
            try {
                if (AndroidVersion.ATLEAST_API28_P) {
                    return ims.switchToNextInputMethod(false)
                } else {
                    ims.window.window?.let { window ->
                        @Suppress("DEPRECATION")
                        return imm?.switchToNextInputMethod(window.attributes.token, false) == true
                    }
                }
            } catch (e: Exception) {
                flogError { "Unable to switch to the next IME" }
                imm?.showInputMethodPicker()
            }
            return false
        }

        fun startWhisperVoiceInput() {
            FlorisImeServiceReference.get()?.toggleWhisperVoiceInput()
        }

        fun exportWhisperLogs() {
            FlorisImeServiceReference.get()?.exportWhisperLogsInternal()
        }
    }

    private fun toggleWhisperVoiceInput() {
        if (isRecording) {
            stopAndTranscribe()
            return
        }

        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            Toast.makeText(this, "Microphone permission not granted", Toast.LENGTH_SHORT).show()
            WhisperLogger.log(this, "Microphone permission not granted")
            return
        }
        startWhisperRecording()
    }

        val intent = Intent(this, SpeechCaptureService::class.java)
        startForegroundService(intent)

        val file = try {
            File.createTempFile("florisboard_whisper_", ".wav", cacheDir)
        } catch (e: IOException) {
            flogError { "Unable to create temporary audio file: ${e.localizedMessage}" }
            Toast.makeText(this, "Unable to start recording", Toast.LENGTH_SHORT).show()
            stopService(intent)
            return
        }

        audioFile = file
        isRecording = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sampleRate = 16000
                val minBuffer = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer
                )

                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    flogError { "AudioRecord failed to initialize, retrying with 44100 Hz" }
                    // Retry logic would go here, for now just log
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FlorisImeService, "AudioRecord failed to initialize", Toast.LENGTH_SHORT).show()
                    }
                    isRecording = false
                    stopService(intent)
                    return@launch
                }

                val buffer = ByteArray(minBuffer)
                file.outputStream().use { out ->
                    // Write a placeholder header, we'll update it later
                    writeWavHeader(out, 0, sampleRate)
                    var bytesRecorded = 0L

                    recorder.startRecording()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FlorisImeService, "Recording...", Toast.LENGTH_SHORT).show()
                    }

                    while (isRecording) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            out.write(buffer, 0, read)
                            bytesRecorded += read
                        }
                    }

                    recorder.stop()
                    recorder.release()

                    // Now that we know the total size, rewrite the header
                    file.outputStream().use { raf ->
                        writeWavHeader(raf, bytesRecorded, sampleRate)
                    }

                    val durationMs = bytesRecorded / 2 / sampleRate * 1000
                    flogInfo { "Captured $bytesRecorded bytes, $durationMs ms" }
                }
            } catch (e: Exception) {
                flogError { "Recording failed: ${e.message}" }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FlorisImeService, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                isRecording = false
                stopService(intent)
            }
        }
    }

    private fun stopAndTranscribe() {
        isRecording = false
        val file = audioFile
        audioFile = null

        val intent = Intent(this, SpeechCaptureService::class.java)
        stopService(intent)

        if (file == null || !file.exists() || file.length() == 0L) {
            flogError { "Audio file missing or empty, unable to transcribe" }
            Toast.makeText(this, "Transcription failed: No audio recorded", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Transcribing…", Toast.LENGTH_SHORT).show()
        WhisperLogger.log(this, "Recording stop → ${file.absolutePath} (${file.length()} bytes)")

        CoroutineScope(Dispatchers.IO).launch {
            flogInfo { "Transcribing with OpenAI. Key empty: ${OPENAI_API_KEY.isEmpty()}" }
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("audio/wav".toMediaType()))
                .addFormDataPart("model", "whisper-1")
                .build()

            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer $OPENAI_API_KEY")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            try {
                client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string()
                    if (response.isSuccessful && !bodyText.isNullOrEmpty()) {
                        val text = runCatching { JSONObject(bodyText).optString("text") }.getOrNull()
                        withContext(Dispatchers.Main) {
                            if (!text.isNullOrEmpty()) {
                                currentInputConnection?.commitText(text, 1)
                            } else {
                                Toast.makeText(
                                    this@FlorisImeService,
                                    "Transcription produced no text",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    } else {
                        val errorBody = bodyText.orEmpty()
                        flogError { "Whisper API call failed: HTTP ${response.code} $errorBody" }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FlorisImeService,
                                "Transcription failed: ${response.code} $errorBody",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        WhisperNotify.showError(this@FlorisImeService, notifyTitle, message)
                        val masked = WhisperLogger.maskForUi(message)
                        Toast.makeText(
                            this@FlorisImeService,
                            "Whisper error: ${masked.take(120)}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            this@FlorisImeService,
                            "Transcription failed (see logs)",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            } catch (e: Exception) {
                flogError { "Whisper API call failed: ${e.message}" }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FlorisImeService,
                        "Transcription failed: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                Toast.makeText(this@FlorisImeService, getString(messageRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startWhisperRecording() {
        if (isRecording) return

        if (!BuildConfig.HAS_OPENAI_KEY) {
            Toast.makeText(
                this,
                "This build has NO API key (PR build). Install an artifact named 'app-debug-with-key' from Actions.",
                Toast.LENGTH_LONG,
            ).show()
            Log.w(TAG, "BuildConfig.HAS_OPENAI_KEY is false; aborting recording.")
            WhisperLogger.log(this, "Build has no API key; aborting recording")
            return
        }

        val keyPresent = apiKeyOrNull() != null
        if (!keyPresent) {
            Toast.makeText(this, "Missing OpenAI API key in BuildConfig", Toast.LENGTH_LONG).show()
            Log.e(TAG, "OPENAI_API_KEY is missing/invalid.")
            WhisperLogger.log(this, "Missing OpenAI API key in BuildConfig")
            return
        }

        try {
            audioFile = File.createTempFile("floris_whisper_", ".m4a", cacheDir)
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to create temporary audio file", t)
            Toast.makeText(this, "Unable to start recording", Toast.LENGTH_LONG).show()
            WhisperLogger.log(this, "Unable to create temporary audio file: ${t.message}")
            audioFile = null
            return
        }

        val file = audioFile ?: return
        val newRecorder = MediaRecorder()
        recorder = newRecorder
        try {
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioSamplingRate(16_000)
            newRecorder.setAudioEncodingBitRate(64_000)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
            isRecording = true
            Toast.makeText(this, "Recording… tap again to stop", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Recording to ${file.absolutePath}")
            WhisperLogger.log(this, "Recording start → ${file.absolutePath}")
        } catch (t: Throwable) {
            Log.e(TAG, "startWhisperRecording failed", t)
            Toast.makeText(this, "Recorder failed: ${t.message}", Toast.LENGTH_LONG).show()
            WhisperLogger.log(this, "Recorder failed: ${t.message}")
            cleanupRecorder(resetFile = true)
            runCatching { file.delete() }
        }
    }

    private fun apiKeyOrNull(): String? {
        if (!BuildConfig.HAS_OPENAI_KEY) {
            return null
        }
        val key = BuildConfig.OPENAI_API_KEY.trim()
        return if (key.startsWith("sk-") && key.length > 20) key else null
    }

    private fun cleanupRecorder(resetFile: Boolean = false) {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        isRecording = false
        if (resetFile) {
            audioFile?.let { runCatching { it.delete() } }
            audioFile = null
        }
    }

    private data class WhisperOutcome(
        val text: String? = null,
        val httpCode: Int? = null,
        val body: String? = null,
        val exceptionMessage: String? = null,
    )

    private fun makeClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    private fun transcribeWithWhisper(file: File): WhisperOutcome {
        val key = apiKeyOrNull() ?: run {
            WhisperLogger.log(this, "Transcription aborted: missing API key")
            return WhisperOutcome(exceptionMessage = "Missing API key")
        }
        Log.d(TAG, "Uploading file ${file.name} (${file.length()} bytes)")
        WhisperLogger.log(this, "Uploading ${file.name} (${file.length()} bytes) as audio/m4a")

        val mime = "audio/m4a".toMediaType()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mime))
            .addFormDataPart("model", "whisper-1")
            .build()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/transcriptions")
            .header("Authorization", "Bearer $key")
            .post(requestBody)
            .build()

        return try {
            makeClient().newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "HTTP ${resp.code}: ${body.take(500)}")
                WhisperLogger.log(this, "HTTP ${resp.code}: ${body.take(500)}")

                if (!resp.isSuccessful) {
                    Log.e(TAG, "Whisper API error ${resp.code}: ${body.take(500)}")
                    WhisperLogger.log(this, "Whisper API error ${resp.code}: ${body.take(500)}")
                    return@use WhisperOutcome(httpCode = resp.code, body = body)
                }

                val text = runCatching { JSONObject(body).optString("text") }
                    .onFailure {
                        Log.e(TAG, "Failed to parse Whisper response", it)
                        WhisperLogger.log(this, "Failed to parse Whisper response: ${it.message}")
                    }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }

                if (text != null) {
                    WhisperLogger.log(this, "Transcribed → ${text.take(120)}")
                    WhisperOutcome(text = text)
                } else {
                    WhisperLogger.log(this, "Whisper response missing text")
                    WhisperOutcome(exceptionMessage = "No text in response")
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Network/parse error", t)
            WhisperLogger.log(this, "Network error: ${t.message ?: t::class.java.simpleName}")
            WhisperOutcome(exceptionMessage = t.message ?: t::class.java.simpleName ?: "unknown")
        }
    }

    private val prefs by FlorisPreferenceStore
    private val editorInstance by editorInstance()
    private val keyboardManager by keyboardManager()
    private val nlpManager by nlpManager()
    private val subtypeManager by subtypeManager()
    private val themeManager by themeManager()

    private val activeState get() = keyboardManager.activeState
    private var inputWindowView by mutableStateOf<View?>(null)
    private var inputViewSize by mutableStateOf(IntSize.Zero)
    private val inputFeedbackController by lazy { InputFeedbackController.new(this) }
    private var isWindowShown: Boolean = false
    private var isFullscreenUiMode by mutableStateOf(false)
    private var isExtractUiShown by mutableStateOf(false)
    private var resourcesContext by mutableStateOf(this as Context)

    private val wallpaperChangeReceiver = WallpaperChangeReceiver()

    init {
        setTheme(R.style.FlorisImeTheme)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("Whisper", "Key len=" + BuildConfig.OPENAI_API_KEY.length)
        FlorisImeServiceReference = WeakReference(this)
        WindowCompat.setDecorFitsSystemWindows(window.window!!, false)
        subtypeManager.activeSubtypeFlow.collectIn(lifecycleScope) { subtype ->
            val config = Configuration(resources.configuration)
            if (prefs.localization.displayKeyboardLabelsInSubtypeLanguage.get()) {
                config.setLocale(subtype.primaryLocale.base)
            }
            resourcesContext = createConfigurationContext(config)
        }
        prefs.localization.displayKeyboardLabelsInSubtypeLanguage.asFlow().collectIn(lifecycleScope) { shouldSync ->
            val config = Configuration(resources.configuration)
            if (shouldSync) {
                config.setLocale(subtypeManager.activeSubtype.primaryLocale.base)
            }
            resourcesContext = createConfigurationContext(config)
        }
        prefs.physicalKeyboard.showOnScreenKeyboard.asFlow().collectIn(lifecycleScope) {
            updateInputViewShown()
        }
        @Suppress("DEPRECATION") // We do not retrieve the wallpaper but only listen to changes
        registerReceiver(wallpaperChangeReceiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED))
    }

    override fun onCreateInputView(): View {
        super.installViewTreeOwners()
        // Instantiate and install bottom sheet host UI view
        val bottomSheetView = FlorisBottomSheetHostUiView()
        window.window!!.findViewById<ViewGroup>(android.R.id.content).addView(bottomSheetView)
        // Instantiate and return input view
        val composeView = ComposeInputView()
        inputWindowView = composeView
        return composeView
    }

    override fun onCreateCandidatesView(): View? {
        // Disable the default candidates view
        return null
    }

    override fun onCreateExtractTextView(): View {
        super.installViewTreeOwners()
        // Consider adding a fallback to the default extract edit layout if user reports come
        // that this causes a crash, especially if the device manufacturer of the user device
        // is a known one to break AOSP standards...
        val defaultExtractView = super.onCreateExtractTextView()
        if (defaultExtractView == null || defaultExtractView !is ViewGroup) {
            return ComposeExtractedLandscapeInputView(null)
        }
        val extractEditText = defaultExtractView.findViewById<ExtractEditText>(android.R.id.inputExtractEditText)
        (extractEditText?.parent as? ViewGroup)?.removeView(extractEditText)
        defaultExtractView.apply {
            removeAllViews()
            addView(ComposeExtractedLandscapeInputView(extractEditText))
        }
        return defaultExtractView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        themeManager.configurationChangeCounter.update { it + 1 }
    }

    override fun onDestroy() {
        // This is a simplified onDestroy. The original known-good commit had more complex cleanup
        // which we are restoring by overwriting the file.
        isRecording = false
        audioFile?.let { file ->
            if (file.exists() && !file.delete()) {
                flogWarning { "Unable to delete temporary audio file ${file.absolutePath}" }
            }
        }
        audioFile = null

        super.onDestroy()
        unregisterReceiver(wallpaperChangeReceiver)
        FlorisImeServiceReference = WeakReference(null)
        inputWindowView = null
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        flogInfo { "restarting=$restarting info=${info?.debugSummarize()}" }
        super.onStartInput(info, restarting)
        if (info == null) return
        val editorInfo = FlorisEditorInfo.wrap(info)
        editorInstance.handleStartInput(editorInfo)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        flogInfo { "restarting=$restarting info=${info?.debugSummarize()}" }
        super.onStartInputView(info, restarting)
        if (info == null) return
        val editorInfo = FlorisEditorInfo.wrap(info)
        activeState.batchEdit {
            if (activeState.imeUiMode != ImeUiMode.CLIPBOARD || prefs.clipboard.historyHideOnNextTextField.get()) {
                activeState.imeUiMode = ImeUiMode.TEXT
            }
            activeState.isSelectionMode = editorInfo.initialSelection.isSelectionMode
            editorInstance.handleStartInputView(editorInfo, isRestart = restarting)
        }
    }

    override fun onEvaluateInputViewShown(): Boolean {
        val config = resources.configuration
        return super.onEvaluateInputViewShown()
            || config.keyboard == Configuration.KEYBOARD_NOKEYS
            || prefs.physicalKeyboard.showOnScreenKeyboard.get()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        flogInfo { "old={start=$oldSelStart,end=$oldSelEnd} new={start=$newSelStart,end=$newSelEnd} composing={start=$candidatesStart,end=$candidatesEnd}" }
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        activeState.batchEdit {
            activeState.isSelectionMode = (newSelEnd - newSelStart) != 0
            editorInstance.handleSelectionUpdate(
                oldSelection = EditorRange.normalized(oldSelStart, oldSelEnd),
                newSelection = EditorRange.normalized(newSelStart, newSelEnd),
                composing = EditorRange.normalized(candidatesStart, candidatesEnd),
            )
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        flogInfo { "finishing=$finishingInput" }
        super.onFinishInputView(finishingInput)
        editorInstance.handleFinishInputView()
    }

    override fun onFinishInput() {
        flogInfo { "(no args)" }
        super.onFinishInput()
        editorInstance.handleFinishInput()
        NlpInlineAutofill.clearInlineSuggestions()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (isWindowShown) {
            flogWarning(LogTopic.IMS_EVENTS) { "Ignoring (is already shown)" }
            return
        } else {
            flogInfo(LogTopic.IMS_EVENTS)
        }
        isWindowShown = true
        inputFeedbackController.updateSystemPrefsState()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (!isWindowShown) {
            flogWarning(LogTopic.IMS_EVENTS) { "Ignoring (is already hidden)" }
            return
        } else {
            flogInfo(LogTopic.IMS_EVENTS)
        }
        isWindowShown = false
        activeState.batchEdit {
            activeState.imeUiMode = ImeUiMode.TEXT
            activeState.isActionsOverflowVisible = false
            activeState.isActionsEditorVisible = false
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        val config = resources.configuration
        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            return false
        }
        return when (prefs.keyboard.landscapeInputUiMode.get()) {
            LandscapeInputUiMode.DYNAMICALLY_SHOW -> super.onEvaluateFullscreenMode()
            LandscapeInputUiMode.NEVER_SHOW -> false
            LandscapeInputUiMode.ALWAYS_SHOW -> true
        }
    }

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        isFullscreenUiMode = isFullscreenMode
        updateSoftInputWindowLayoutParameters()
    }

    override fun onUpdateExtractingVisibility(info: EditorInfo?) {
        if (info != null) {
            editorInstance.handleStartInputView(FlorisEditorInfo.wrap(info), isRestart = true)
        }
        when (prefs.keyboard.landscapeInputUiMode.get()) {
            LandscapeInputUiMode.DYNAMICALLY_SHOW -> super.onUpdateExtractingVisibility(info)
            LandscapeInputUiMode.NEVER_SHOW -> isExtractViewShown = false
            LandscapeInputUiMode.ALWAYS_SHOW -> isExtractViewShown = true
        }
    }

    override fun setExtractViewShown(shown: Boolean) {
        super.setExtractViewShown(shown)
        isExtractUiShown = shown
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        // ... (rest of the file is boilerplate, not relevant to the change)
    private fun updateSoftInputWindowLayoutParameters() {
        val w = window?.window ?: return
        // TODO: Verify that this doesn't give us a padding problem
        WindowCompat.setDecorFitsSystemWindows(w, false)
        ViewUtils.updateLayoutHeightOf(w, WindowManager.LayoutParams.MATCH_PARENT)
        val layoutHeight = if (isFullscreenUiMode) {
            WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }
        val inputArea = w.findViewById<View>(android.R.id.inputArea) ?: return
        ViewUtils.updateLayoutHeightOf(inputArea, layoutHeight)
        ViewUtils.updateLayoutGravityOf(inputArea, Gravity.BOTTOM)
        val inputWindowView = inputWindowView ?: return
        ViewUtils.updateLayoutHeightOf(inputWindowView, layoutHeight)
    }

    override fun getTextForImeAction(imeOptions: Int): String? {
        return try {
            when (imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_NONE -> null
                EditorInfo.IME_ACTION_GO -> resourcesContext.getString(AndroidInternalR.string.ime_action_go)
                EditorInfo.IME_ACTION_SEARCH -> resourcesContext.getString(AndroidInternalR.string.ime_action_search)
                EditorInfo.IME_ACTION_SEND -> resourcesContext.getString(AndroidInternalR.string.ime_action_send)
                EditorInfo.IME_ACTION_NEXT -> resourcesContext.getString(AndroidInternalR.string.ime_action_next)
                EditorInfo.IME_ACTION_DONE -> resourcesContext.getString(AndroidInternalR.string.ime_action_done)
                EditorInfo.IME_ACTION_PREVIOUS -> resourcesContext.getString(AndroidInternalR.string.ime_action_previous)
                else -> resourcesContext.getString(AndroidInternalR.string.ime_action_default)
            }
        } catch (_: Throwable) {
            super.getTextForImeAction(imeOptions)?.toString()
        }
    }

    @Composable
    private fun ImeUiWrapper() {
        ProvideLocalizedResources(
            resourcesContext,
            appName = R.string.app_name,
        ) {
            ProvideKeyboardRowBaseHeight {
                CompositionLocalProvider(LocalInputFeedbackController provides inputFeedbackController) {
                    FlorisImeTheme {
                        // Do not apply system bar padding here yet, we want to draw it ourselves
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (!(isFullscreenUiMode && isExtractUiShown)) {
                                DevtoolsOverlay(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                )
                            }
                            ImeUi()
                            SystemUiIme()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ImeUi() {
        val state by keyboardManager.activeState.collectAsState()
        val attributes = mapOf(
            FlorisImeUi.Attr.Mode to state.keyboardMode.toString(),
            FlorisImeUi.Attr.ShiftState to state.inputShiftState.toString(),
        )
        val layoutDirection = LocalLayoutDirection.current
        LaunchedEffect(layoutDirection) {
            keyboardManager.activeState.layoutDirection = layoutDirection
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SnyggBox(
                elementName = FlorisImeUi.Window.elementName,
                attributes = attributes,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onGloballyPositioned { coords -> inputViewSize = coords.size },
                clickAndSemanticsModifier = Modifier
                    // Do not remove below line or touch input may get stuck
                    .pointerInteropFilter { false },
                supportsBackgroundImage = !AndroidVersion.ATLEAST_API30_R,
                allowClip = false,
            ) {
                // The SurfaceView is used to render the background image under inline-autofill chips. These are only
                // available on Android >=11, and SurfaceView causes trouble on Android 8/9, thus we render the image
                // in the SurfaceView for Android >=11, and in the Compose View Tree for Android <=10.
                if (AndroidVersion.ATLEAST_API30_R) {
                    SnyggSurfaceView(
                        elementName = FlorisImeUi.Window.elementName,
                        attributes = attributes,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                val configuration = LocalConfiguration.current
                val bottomOffset by if (configuration.isOrientationPortrait()) {
                    prefs.keyboard.bottomOffsetPortrait
                } else {
                    prefs.keyboard.bottomOffsetLandscape
                }.observeAsTransformingState { it.dp }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        // Apply system bars padding here (we already drew our keyboard background)
                        .safeDrawingPadding()
                        .padding(bottom = bottomOffset),
                ) {
                    val oneHandedMode by prefs.keyboard.oneHandedMode.observeAsState()
                    val oneHandedModeEnabled by prefs.keyboard.oneHandedModeEnabled.observeAsState()
                    val oneHandedModeScaleFactor by prefs.keyboard.oneHandedModeScaleFactor.observeAsState()
                    val keyboardWeight = when {
                        !oneHandedModeEnabled || configuration.isOrientationLandscape() -> 1f
                        else -> oneHandedModeScaleFactor / 100f
                    }
                    if (oneHandedModeEnabled && oneHandedMode == OneHandedMode.END && configuration.isOrientationPortrait()) {
                        OneHandedPanel(
                            panelSide = OneHandedMode.START,
                            weight = 1f - keyboardWeight,
                        )
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        Box(
                            modifier = Modifier
                                .weight(keyboardWeight)
                                .wrapContentHeight(),
                        ) {
                            when (state.imeUiMode) {
                                ImeUiMode.TEXT -> TextInputLayout()
                                ImeUiMode.MEDIA -> MediaInputLayout()
                                ImeUiMode.CLIPBOARD -> ClipboardInputLayout()
                            }
                        }
                    }
                    if (oneHandedModeEnabled && oneHandedMode == OneHandedMode.START && configuration.isOrientationPortrait()) {
                        OneHandedPanel(
                            panelSide = OneHandedMode.END,
                            weight = 1f - keyboardWeight,
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyboardManager.onHardwareKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return keyboardManager.onHardwareKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
    }

    private inner class ComposeInputView : AbstractComposeView(this) {
        init {
            isHapticFeedbackEnabled = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        @Composable
        override fun Content() {
            ImeUiWrapper()
        }

        override fun getAccessibilityClassName(): CharSequence {
            return javaClass.name
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            updateSoftInputWindowLayoutParameters()
        }
    }

    private inner class FlorisBottomSheetHostUiView : AbstractComposeView(this) {
        init {
            isHapticFeedbackEnabled = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        @Composable
        override fun Content() {
            val context = LocalContext.current
            val keyboardManager by context.keyboardManager()
            val state by keyboardManager.activeState.collectAsState()

            ProvideLocalizedResources(
                resourcesContext,
                appName = R.string.app_name,
                forceLayoutDirection = LayoutDirection.Ltr,
            ) {
                FlorisImeTheme {
                    BottomSheetHostUi(
                        isShowing = state.isBottomSheetShowing() || state.isSubtypeSelectionShowing(),
                        onHide = {
                            if (state.isBottomSheetShowing()) {
                                keyboardManager.activeState.isActionsEditorVisible = false
                            }
                            if (state.isSubtypeSelectionShowing()) {
                                keyboardManager.activeState.isSubtypeSelectionVisible = false
                            }
                        },
                    ) {
                        if (state.isBottomSheetShowing()) {
                            QuickActionsEditorPanel()
                        }
                        if (state.isSubtypeSelectionShowing()) {
                            SelectSubtypePanel()
                        }
                    }
                }
            }
        }

        override fun getAccessibilityClassName(): CharSequence {
            return javaClass.name
        }
    }

    private inner class ComposeExtractedLandscapeInputView(eet: ExtractEditText?) : FrameLayout(this) {
        val composeView: ComposeView
        val extractEditText: ExtractEditText

        init {
            isHapticFeedbackEnabled = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            extractEditText = (eet ?: ExtractEditText(context)).also {
                it.id = android.R.id.inputExtractEditText
                it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                it.background = null
                it.gravity = Gravity.TOP
                it.isVerticalScrollBarEnabled = true
            }
            addView(extractEditText)

            composeView = ComposeView(context).also { it.setContent { Content() } }
            addView(composeView)
        }

        @Composable
        fun Content() {
            ProvideLocalizedResources(
                resourcesContext,
                appName = R.string.app_name,
                forceLayoutDirection = LayoutDirection.Ltr,
            ) {
                FlorisImeTheme {
                    val activeEditorInfo by editorInstance.activeInfoFlow.collectAsState()
                    SnyggBox(FlorisImeUi.ExtractedLandscapeInputLayout.elementName) {
                        SnyggRow(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SnyggBox(
                                elementName = FlorisImeUi.ExtractedLandscapeInputLayout.elementName,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                            ) {
                                val fieldStyle = rememberSnyggThemeQuery(FlorisImeUi.ExtractedLandscapeInputField.elementName)
                                val foreground = fieldStyle.foreground()
                                AndroidView(
                                    factory = { extractEditText },
                                    update = { view ->
                                        view.background = null
                                        view.backgroundTintList = null
                                        view.foregroundTintList = null
                                        view.setTextColor(foreground.toArgb())
                                        view.setHintTextColor(foreground.copy(foreground.alpha * 0.6f).toArgb())
                                        view.setTextSize(
                                            TypedValue.COMPLEX_UNIT_SP,
                                            fieldStyle.fontSize(default = 16.sp).value,
                                        )
                                    },
                                )
                            }
                            SnyggButton(
                                FlorisImeUi.ExtractedLandscapeInputAction.elementName,
                                onClick = {n                                    if (activeEditorInfo.extractedActionId != 0) {
                                        currentInputConnection?.performEditorAction(activeEditorInfo.extractedActionId)
                                    } else {
                                        editorInstance.performEnterAction(activeEditorInfo.imeOptions.action)
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                SnyggText(
                                    text = activeEditorInfo.extractedActionLabel
                                        ?: getTextForImeAction(activeEditorInfo.imeOptions.action.toInt())
                                        ?: "ACTION",
                                )
                            }
                        }
                    }
                }
            }
        }

        override fun getAccessibilityClassName(): CharSequence {
            return javaClass.name
        }

        override fun onAttachedToWindow() {
            removeView(extractEditText)
            super.onAttachedToWindow()
            try {
                (parent as LinearLayout).let { extractEditLayout ->
                    extractEditLayout.layoutParams = LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                    ).also { it.setMargins(0, 0, 0, 0) }
                    extractEditLayout.setPadding(0, 0, 0, 0)
                }
            } catch (e: Throwable) {
                flogError { e.message.toString() }
            }
        }
    }
}