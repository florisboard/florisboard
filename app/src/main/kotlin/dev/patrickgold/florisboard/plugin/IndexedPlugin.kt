/*
 * Copyright (C) 2023 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.plugin

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.view.textservice.SuggestionsInfo
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.ime.core.ComputedSubtype
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.SuggestionRequest
import dev.patrickgold.florisboard.ime.nlp.SuggestionRequestFlags
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.io.FlorisRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class IndexedPlugin(
    val context: Context,
    val serviceName: ComponentName,
    var state: IndexedPluginState,
    var metadata: FlorisPluginMetadata = FlorisPluginMetadata(""),
) : SpellingProvider, SuggestionProvider {
    private var messageIdGenerator = AtomicInteger(1)
    private var connection = IndexedPluginConnection(context)

    override suspend fun create() {
        if (isValidAndBound()) return
        connection.bindService(serviceName)
    }

    override suspend fun preload(subtype: ComputedSubtype) {
        val message = FlorisPluginMessage.requestToService(
            action = FlorisPluginMessage.ACTION_PRELOAD,
            id = messageIdGenerator.getAndIncrement(),
            data = Json.encodeToString(ComputedSubtype.serializer(), subtype),
        )
        connection.sendMessage(message)
    }

    override suspend fun spell(
        subtypeId: Long,
        word: String,
        prevWords: List<String>,
        flags: SuggestionRequestFlags,
    ): SpellingResult {
        val request = SuggestionRequest(subtypeId, word, prevWords, flags)
        val message = FlorisPluginMessage.requestToService(
            action = FlorisPluginMessage.ACTION_SPELL,
            id = messageIdGenerator.getAndIncrement(),
            data = Json.encodeToString(request),
        )
        connection.sendMessage(message)
        return withTimeoutOrNull(5000L) {
            val replyMessage = connection.replyMessages.first { it.id == message.id }
            val resultObj = replyMessage.obj as? SuggestionsInfo ?: return@withTimeoutOrNull null
            return@withTimeoutOrNull SpellingResult(resultObj)
        } ?: SpellingResult.unspecified()
    }

    override suspend fun suggest(
        subtypeId: Long,
        word: String,
        prevWords: List<String>,
        flags: SuggestionRequestFlags,
    ): List<SuggestionCandidate> {
        val request = SuggestionRequest(subtypeId, word, prevWords, flags)
        val message = FlorisPluginMessage.requestToService(
            action = FlorisPluginMessage.ACTION_SUGGEST,
            id = messageIdGenerator.getAndIncrement(),
            data = Json.encodeToString(request),
        )
        connection.sendMessage(message)
        return withTimeoutOrNull(5000L) {
            val replyMessage = connection.replyMessages.first { it.id == message.id }
            val resultData = replyMessage.data ?: return@withTimeoutOrNull null
            return@withTimeoutOrNull Json.decodeFromString(resultData)
        } ?: emptyList()
    }

    override suspend fun notifySuggestionAccepted(subtypeId: Long, candidate: SuggestionCandidate) {
        TODO("Not yet implemented")
    }

    override suspend fun notifySuggestionReverted(subtypeId: Long, candidate: SuggestionCandidate) {
        TODO("Not yet implemented")
    }

    override suspend fun removeSuggestion(subtypeId: Long, candidate: SuggestionCandidate): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun destroy() {
        if (!isValidAndBound()) return
        connection.unbindService()
    }

    fun packageContext(): Context {
        return context.createPackageContext(serviceName.packageName, 0)
    }

    fun configurationRoute(): String? {
        if (!isValid()) return null
        val configurationRoute = metadata.settingsActivity ?: return null
        val ref = FlorisRef.from(configurationRoute)
        return if (ref.isAppUi) ref.relativePath else null
    }

    fun settingsActivityIntent(): Intent? {
        if (!isValid()) return null
        val settingsActivityName = metadata.settingsActivity ?: return null
        val intent = Intent().also {
            it.component = ComponentName(serviceName.packageName, settingsActivityName)
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (intent.resolveActivityInfo(context.packageManager, 0) != null) {
            intent
        } else {
            null
        }
    }

    fun isValid(): Boolean {
        return state == IndexedPluginState.Ok
    }

    fun isValidAndBound(): Boolean {
        return isValid() && connection.isBound()
    }

    fun isInternalPlugin(): Boolean {
        return serviceName.packageName == BuildConfig.APPLICATION_ID
    }

    fun isExternalPlugin(): Boolean {
        return !isInternalPlugin()
    }

    override fun toString(): String {
        val packageContext = packageContext()
        return """
            IndexedPlugin {
                serviceName=$serviceName
                state=$state
                isBound=${connection.isBound()}
                metadata=${metadata.toString(packageContext).prependIndent("                ").substring(16)}
            }
        """.trimIndent()
    }
}

class IndexedPluginConnection(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var serviceMessenger = MutableStateFlow<Messenger?>(null)
    private val consumerMessenger = Messenger(IncomingHandler())
    private var isBound = AtomicBoolean(false)
    private val stagedOutgoingMessages = MutableSharedFlow<FlorisPluginMessage>(
        replay = 8,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _replyMessages = MutableSharedFlow<FlorisPluginMessage>(
        replay = 8,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val replyMessages = _replyMessages.asSharedFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            flogDebug { "$name, $binder" }
            if (name == null || binder == null) return

            serviceMessenger.value = Messenger(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            flogDebug { "$name" }
            if (name == null) return

            serviceMessenger.value = null
        }

        override fun onBindingDied(name: ComponentName?) {
            flogDebug { "$name" }
            if (name == null) return

            serviceMessenger.value = null
            unbindService()
            bindService(name)
        }

        override fun onNullBinding(name: ComponentName?) {
            flogDebug { "$name" }
            if (name == null) return

            serviceMessenger.value = null
            unbindService()
        }
    }

    init {
        scope.launch {
            stagedOutgoingMessages.collect { message ->
                val messenger = serviceMessenger.first { it != null }!!
                messenger.send(message.also { it.replyTo = consumerMessenger }.toAndroidMessage())
            }
        }
    }

    fun isBound(): Boolean {
        return isBound.get() && serviceMessenger.value != null
    }

    fun bindService(serviceName: ComponentName) {
        if (isBound.getAndSet(true)) return
        val intent = Intent().also {
            it.component = serviceName
            it.putExtra(FlorisPluginService.CONSUMER_PACKAGE_NAME, BuildConfig.APPLICATION_ID)
            it.putExtra(FlorisPluginService.CONSUMER_VERSION_CODE, BuildConfig.VERSION_CODE)
            it.putExtra(FlorisPluginService.CONSUMER_VERSION_NAME, BuildConfig.VERSION_NAME)
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        if (!isBound.getAndSet(false)) return
        context.unbindService(serviceConnection)
    }

    fun sendMessage(message: FlorisPluginMessage) = runBlocking {
        stagedOutgoingMessages.emit(message)
    }

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler : Handler(context.mainLooper) {
        override fun handleMessage(msg: Message) {
            val message = FlorisPluginMessage.fromAndroidMessage(msg)
            val (source, type, _) = message.metadata()
            if (source != FlorisPluginMessage.SOURCE_SERVICE || type != FlorisPluginMessage.TYPE_RESPONSE) {
                return
            }
            runBlocking {
                _replyMessages.emit(message)
            }
        }
    }
}

enum class IndexedPluginError {
    NoMetadata,
    InvalidMetadata,
}

sealed class IndexedPluginState {
    object Ok : IndexedPluginState()
    data class Error(val type: IndexedPluginError, val exception: Exception?) : IndexedPluginState()

    override fun toString(): String {
        return when (this) {
            is Ok -> "Ok"
            is Error -> "Error($type, $exception)"
        }
    }
}

internal fun stateOk() = IndexedPluginState.Ok

internal fun stateError(type: IndexedPluginError, exception: Exception? = null) =
    IndexedPluginState.Error(type, exception)
