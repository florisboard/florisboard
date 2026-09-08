package dev.patrickgold.florisboard.ime.mozc

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.CandidateWindow
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.CompositionMode
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent.SpecialKey
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.SessionCommand
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


// TODO: Hardware keyboard, both qwerty and 109-key JIS
// TODO: Flick
// dot166: I didn't do any of the above as I don't really use hardware keyboard and flick requires a finished k3lp
// someone else can pick up where I left off...
class MozcEngine {
    @Deprecated(message = "TODO: REPLACE")
    // TODO: Replace
    private var prefs: SharedPreferences? = null
    private var res: Resources? = null
    private var sessionId: Long = 0L
    private val _preedit = MutableStateFlow("")
    val preedit: StateFlow<String> = _preedit.asStateFlow()
    private val _candidates = MutableStateFlow(mutableListOf<CandidateWindow.Candidate>())
    val candidates: StateFlow<MutableList<CandidateWindow.Candidate>> = _candidates.asStateFlow()
    private var _compositionMode = CompositionMode.HIRAGANA
    private var isInitialized: Boolean = false

    private fun checkInitialized() {
        if (!isInitialized) {
            throw RuntimeException("${MozcEngine::class.java.getSimpleName()} is used before initialization")
        }
    }

    @Throws(IOException::class)
    private fun initInternal(ctx: Context) {
        check(MozcJNI.initialize()) { "Failed to initialize JNI" }

        // Copy mozc.data from assets
        // TODO: Implement the FlorisBoard Extensions infrastructure so that UT dictionaries can be used, currently only using oss dict
        // note to upstream, the oss dict is builtin and its source is https://github.com/google/mozc/tree/master/src/data/dictionary_oss
        val outFile = File(ctx.filesDir, "mozc.data")
        ctx.assets.open("mozc.data").use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(8192)
                var length: Int
                while ((input.read(buffer).also { length = it }) > 0) {
                    output.write(buffer, 0, length)
                }
            }
        }
        check(MozcJNI.onPostLoad(ctx.filesDir.absolutePath, outFile.absolutePath)) { "init failed" }

        Log.d(TAG, MozcJNI.dataVersion ?: "")

        val createCommand: ProtoCommands.Command =
            ProtoCommands.Command.newBuilder()
                .setInput(
                    ProtoCommands.Input.newBuilder()
                        .setType(ProtoCommands.Input.CommandType.CREATE_SESSION)
                )
                .build()

        val createResponse: ProtoCommands.Command =
            ProtoCommands.Command.parseFrom(
                MozcJNI.evalCommand(createCommand.toByteArray())
            )

        sessionId = createResponse.output.id
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        res = ctx.resources
        compositionMode = CompositionMode.forNumber(
            prefs!!.getInt(
                "libmozc_enabled",
                CompositionMode.HIRAGANA.number
            )
        )
        isInitialized = true
    }

    var compositionMode: CompositionMode
        get() {
            return _compositionMode
        }
        set(value) {
            val modeCommand: SessionCommand? = SessionCommand.newBuilder()
                .setType(SessionCommand.CommandType.SWITCH_COMPOSITION_MODE)
                .setCompositionMode(value)
                .build()

            val modeRequest: ProtoCommands.Command = ProtoCommands.Command.newBuilder()
                .setInput(
                    ProtoCommands.Input.newBuilder()
                        .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
                        .setId(sessionId)
                        .setCommand(modeCommand)
                )
                .build()

            MozcJNI.evalCommand(modeRequest.toByteArray())
            if (value == CompositionMode.HIRAGANA) {
                val builder: ProtoCommands.Request.Builder = ProtoCommands.Request.newBuilder()
                    .setKeyboardName(
                        "QWERTY_KANA" + '-' + 0 + '.' + 4 + '.' + 0 + '-' + getDeviceOrientationString(
                            res!!.configuration
                        )
                    )
                    .setSpecialRomanjiTable(ProtoCommands.Request.SpecialRomanjiTable.QWERTY_MOBILE_TO_HIRAGANA)
                    .setSpaceOnAlphanumeric(ProtoCommands.Request.SpaceOnAlphanumeric.SPACE_OR_CONVERT_KEEPING_COMPOSITION)
                    .setKanaModifierInsensitiveConversion(false)
                    .setCrossingEdgeBehavior(ProtoCommands.Request.CrossingEdgeBehavior.DO_NOTHING)
                    .setMixedConversion(true)
                    .setZeroQuerySuggestion(true)
                    .setUpdateInputModeFromSurroundingText(false)
                    .setAutoPartialSuggestion(true)
                setRequest(builder)
            } else {
                val builder: ProtoCommands.Request.Builder = ProtoCommands.Request.newBuilder()
                    .setKeyboardName(
                        "QWERTY_ALPHABET" + '-' + 0 + '.' + 5 + '.' + 0 + '-' + getDeviceOrientationString(
                            res!!.configuration
                        )
                    )
                    .setSpecialRomanjiTable(ProtoCommands.Request.SpecialRomanjiTable.QWERTY_MOBILE_TO_HALFWIDTHASCII)
                    .setSpaceOnAlphanumeric(ProtoCommands.Request.SpaceOnAlphanumeric.COMMIT)
                    .setKanaModifierInsensitiveConversion(false)
                    .setCrossingEdgeBehavior(ProtoCommands.Request.CrossingEdgeBehavior.DO_NOTHING)
                    .setMixedConversion(true)
                    .setZeroQuerySuggestion(true)
                    .setUpdateInputModeFromSurroundingText(false)
                    .setAutoPartialSuggestion(true)
                setRequest(builder)
            }
            prefs!!.edit { putInt("libmozc_enabled", value.number) }
            _compositionMode = value
        }

    fun deleteSession() {
        if (sessionId == 0L) return
        val deleteRequest: ProtoCommands.Command =
            ProtoCommands.Command.newBuilder()
                .setInput(
                    ProtoCommands.Input.newBuilder()
                        .setType(ProtoCommands.Input.CommandType.DELETE_SESSION)
                        .setId(sessionId)
                )
                .build()

        MozcJNI.evalCommand(deleteRequest.toByteArray())

        sessionId = 0L
        _preedit.value = ""
        _candidates.value = mutableListOf()
    }

    fun sendKey(ch: Char) {
        val keyEvent: ProtoCommands.KeyEvent = ProtoCommands.KeyEvent.newBuilder()
            .setKeyCode(ch.code)
            .build()
        sendKeyRequest(keyEvent)
    }

    fun sendKey(code: SpecialKey) {
        val keyEvent: ProtoCommands.KeyEvent = ProtoCommands.KeyEvent.newBuilder()
            .setSpecialKey(code)
            .build()
        sendKeyRequest(keyEvent)
    }

    @Throws(IOException::class)
    fun resetSession() {
        deleteSession()
        val createCommand: ProtoCommands.Command =
            ProtoCommands.Command.newBuilder()
                .setInput(
                    ProtoCommands.Input.newBuilder()
                        .setType(ProtoCommands.Input.CommandType.CREATE_SESSION)
                )
                .build()

        val createResponse: ProtoCommands.Command =
            ProtoCommands.Command.parseFrom(
                MozcJNI.evalCommand(createCommand.toByteArray())
            )

        sessionId = createResponse.output.id

        compositionMode = CompositionMode.forNumber(
            prefs!!.getInt(
                "libmozc_enabled",
                CompositionMode.HIRAGANA.number
            )
        )
    }

    private fun setRequest(builder: ProtoCommands.Request.Builder) {
        val inputBuilder: ProtoCommands.Input.Builder = ProtoCommands.Input.newBuilder()
            .setRequest(builder)
            .addAllTouchEvents(mutableListOf<ProtoCommands.Input.TouchEvent?>())
        val input: ProtoCommands.Input? = inputBuilder
            .setId(sessionId)
            .setType(ProtoCommands.Input.CommandType.SET_REQUEST)
            .setRequest(inputBuilder.request)
            .build()
        val inCommand: ProtoCommands.Command = ProtoCommands.Command.newBuilder()
            .setInput(input)
            .build()
        MozcJNI.evalCommand(inCommand.toByteArray())
    }

    private fun sendKeyRequest(keyEvent: ProtoCommands.KeyEvent) {
        val keyRequest: ProtoCommands.Command = ProtoCommands.Command.newBuilder()
            .setInput(
                ProtoCommands.Input.newBuilder()
                    .setType(ProtoCommands.Input.CommandType.SEND_KEY)
                    .setId(sessionId)
                    .setKey(keyEvent)
            )
            .build()

        val bytes = MozcJNI.evalCommand(keyRequest.toByteArray())
        if (bytes == null || bytes.isEmpty()) {
            Log.e(TAG, "returned empty response")
            return
        }

        val response: ProtoCommands.Command = ProtoCommands.Command.parseFrom(bytes)
        val output = if (response.hasOutput()) response.output else return
        var preedit = ""
        if (output.hasPreedit()) {
            preedit = output.preedit.segmentList
                .stream()
                .map { seg -> seg.value }
                .reduce("") { obj: String?, s: String? -> obj + s }
        }
        _preedit.value = preedit

        var candidateList = mutableListOf<CandidateWindow.Candidate>()
        if (output.hasCandidateWindow()) {
            candidateList = output.candidateWindow.candidateList
        }
        _candidates.value = candidateList
    }

    companion object {
        private const val TAG = "mozcDebug"

        private val sInstance = MozcEngine()

        val instance: MozcEngine
            get() {
                sInstance.checkInitialized()
                return sInstance
            }

        @Throws(IOException::class)
        fun init(context: Context) {
            sInstance.initInternal(context)
        }

        fun getDeviceOrientationString(configuration: Configuration): String {
            when (configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> return "PORTRAIT"
                Configuration.ORIENTATION_LANDSCAPE -> return "LANDSCAPE"
                // TODO: Decide if I should keep ORIENTATION_SQUARE as it doesn't do anything
                Configuration.ORIENTATION_SQUARE -> return "SQUARE"
                Configuration.ORIENTATION_UNDEFINED -> return "UNDEFINED"
            }
            // If none of above is matched to the orientation, we return "UNKNOWN".
            return "UNKNOWN"
        }
    }
}
