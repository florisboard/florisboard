package dev.patrickgold.florisboard.ime.mozc

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJni
import dev.patrickgold.florisboard.FlorisApplication
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.nlp.JapaneseWordSuggestionCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent.SpecialKey
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.SessionCommand
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


// TODO: Hardware keyboard, both qwerty and 109-key JIS
// TODO: Flick
// dot166: I didn't do any of the above as I don't really use hardware keyboard and flick requires a finished k3lp
// someone else can pick up where I left off...
class MozcEngine {
    private var sessionId: Long = 0L
    private val _preedit = MutableStateFlow("")
    val preedit: StateFlow<String> = _preedit.asStateFlow()
    private val _candidates: MutableStateFlow<Pair<MutableList<ProtoCandidateWindow.CandidateWord>, Int?>> = MutableStateFlow(Pair(mutableListOf(), null))
    val candidates: StateFlow<Pair<MutableList<ProtoCandidateWindow.CandidateWord>, Int?>> = _candidates.asStateFlow()
    private var _keyboardSpec = MozcKeyboardSpec.TWELVE_KEY_FLICK_KANA
    private var florisApplication: FlorisApplication? = null

    private fun checkInitialized() {
        if (florisApplication == null) {
            throw RuntimeException("${MozcEngine::class.java.getSimpleName()} is used before initialization")
        }
    }

    @Throws(IOException::class)
    private fun initInternal(ctx: Context) {
        check(MozcJni.initialize()) { "Failed to initialize JNI" }

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
        check(MozcJni.onPostLoad(ctx.filesDir.absolutePath, outFile.absolutePath)) { "init failed" }

        Log.d(TAG, MozcJni.dataVersion ?: "")

        val createCommand: ProtoCommands.Command =
            ProtoCommands.Command.newBuilder()
                .setInput(
                    ProtoCommands.Input.newBuilder()
                        .setType(ProtoCommands.Input.CommandType.CREATE_SESSION)
                )
                .build()

        val createResponse: ProtoCommands.Command =
            ProtoCommands.Command.parseFrom(
                MozcJni.evalCommand(createCommand.toByteArray())
            )

        sessionId = createResponse.output.id
        val tmpApp by ctx.appContext()
        florisApplication = tmpApp
    }

    var keyboardSpec: MozcKeyboardSpec
        get() {
            return _keyboardSpec
        }
        set(value) {
            val request = getRequestBuilder(value, florisApplication!!.resources.configuration, 8) // use max latin spellcheck candidates for hardware keyboards
            setRequest(request)
            val modeCommand: SessionCommand? = SessionCommand.newBuilder()
                .setType(SessionCommand.CommandType.SWITCH_COMPOSITION_MODE)
                .setCompositionMode(value.compositionMode)
                .build()

            val modeRequest: ProtoCommands.Command = ProtoCommands.Command.newBuilder()
                .setInput(
                    ProtoCommands.Input.newBuilder()
                        .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
                        .setId(sessionId)
                        .setCommand(modeCommand)
                )
                .build()

            MozcJni.evalCommand(modeRequest.toByteArray())
            _keyboardSpec = value
        }

    private fun setHardwareKeyboardRequest(builder: ProtoCommands.Request.Builder, candidatePageSize: Int) {
        builder.setMixedConversion(false)
            .setZeroQuerySuggestion(false)
            .setUpdateInputModeFromSurroundingText(true)
            .setAutoPartialSuggestion(false)
            .setCandidatePageSize(candidatePageSize)
    }

    fun setSoftwareKeyboardRequest(builder: ProtoCommands.Request.Builder) {
        builder.setMixedConversion(true)
            .setZeroQuerySuggestion(true)
            .setUpdateInputModeFromSurroundingText(false)
            .setAutoPartialSuggestion(true)
    }

    fun getRequestBuilder(
        specification: MozcKeyboardSpec,
        configuration: Configuration, candidatePageSizeForHardware: Int
    ): ProtoCommands.Request.Builder {
        val builder: ProtoCommands.Request.Builder = ProtoCommands.Request.newBuilder()
            .setKeyboardName(
                specification.specName.formattedKeyboardName(configuration)
            )
            .setSpecialRomanjiTable(specification.specialRomanjiTable)
            .setSpaceOnAlphanumeric(specification.spaceOnAlphanumeric)
            .setKanaModifierInsensitiveConversion(
                specification.isKanaModifierInsensitiveConversion
            )
            .setCrossingEdgeBehavior(specification.crossingEdgeBehavior)
        if (specification.isHardwareKeyboard) {
            setHardwareKeyboardRequest(builder, candidatePageSizeForHardware)
        } else {
            setSoftwareKeyboardRequest(builder)
        }
        return builder
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

        MozcJni.evalCommand(deleteRequest.toByteArray())

        sessionId = 0L
        _preedit.value = ""
        _candidates.value = Pair(mutableListOf(), null)
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

    fun selectCandidate(candidate: JapaneseWordSuggestionCandidate) {
        val submitRequest: ProtoCommands.Input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
            .setCommand(
                SessionCommand.newBuilder()
                    .setType(SessionCommand.CommandType.SUBMIT_CANDIDATE)
                    .setId(candidate.candidateId)
            )
            .build()
        MozcJni.evalCommand(submitRequest.toByteArray())
    }

    fun applyConfig(conf: ProtoConfig.Config) {
        val configRequest: ProtoCommands.Input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SET_CONFIG)
            .setConfig(conf)
            .build()
        MozcJni.evalCommand(configRequest.toByteArray())
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
                MozcJni.evalCommand(createCommand.toByteArray())
            )

        sessionId = createResponse.output.id
    }

    private fun setRequest(builder: ProtoCommands.Request.Builder) {
        val inputBuilder: ProtoCommands.Input.Builder = ProtoCommands.Input.newBuilder()
            .setRequest(builder)
        val input: ProtoCommands.Input? = inputBuilder
            .setId(sessionId)
            .setType(ProtoCommands.Input.CommandType.SET_REQUEST)
            .setRequest(inputBuilder.request)
            .build()
        val inCommand: ProtoCommands.Command = ProtoCommands.Command.newBuilder()
            .setInput(input)
            .build()
        MozcJni.evalCommand(inCommand.toByteArray())
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

        val bytes = MozcJni.evalCommand(keyRequest.toByteArray())
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

        val candidateList = output.allCandidateWords.candidatesList
        val selectedIndex = if(output.hasCandidateWindow()) {
            if(output.candidateWindow.hasFocusedIndex()) {
                output.candidateWindow.focusedIndex
            } else {
                null
            }
        } else {
            null
        }
        _candidates.value = Pair(candidateList, selectedIndex)
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
    }
}
