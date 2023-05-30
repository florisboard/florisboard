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

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.ime.core.ComputedSubtype
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionRequestFlags
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.io.FlorisRef
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class FlorisPluginIndexer(private val context: Context) {
    private val _pluginIndexFlow = MutableStateFlow(listOf<IndexedPlugin>())
    val pluginIndexFlow = _pluginIndexFlow.asStateFlow()
    val pluginIndex = guardedByLock { mutableListOf<IndexedPlugin>() }

    suspend fun indexBoundServices() {
        val intent = Intent(FlorisPluginService.SERVICE_INTERFACE)
        val packageManager = context.packageManager
        val resolveInfoList = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)

        pluginIndex.withLock { pluginIndex ->
            val newPluginIndex = mutableListOf<IndexedPlugin>()

            fun registerPlugin(
                serviceName: ComponentName,
                state: IndexedPluginState,
                metadata: FlorisPluginMetadata = FlorisPluginMetadata(""),
            ) {
                val plugin = pluginIndex.find { it.serviceName == serviceName }?.also {
                    it.state = state
                    it.metadata = metadata
                    pluginIndex.remove(it)
                } ?: IndexedPlugin(context, serviceName, state, metadata)
                plugin.create()
                newPluginIndex.add(plugin)
            }

            for (resolveInfo in resolveInfoList) {
                val serviceName = ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name)
                // TODO: hard-coding blocking third-party plugins for now
                if (serviceName.packageName != BuildConfig.APPLICATION_ID) {
                    continue
                }
                val metadataBundle = resolveInfo.serviceInfo.metaData
                if (metadataBundle == null) {
                    registerPlugin(serviceName, stateError(IndexedPluginError.NoMetadata))
                    continue
                }
                val metadataXmlId = metadataBundle.getInt(FlorisPluginService.SERVICE_METADATA, -1)
                if (metadataXmlId == -1) {
                    registerPlugin(serviceName, stateError(IndexedPluginError.NoMetadata))
                    continue
                }
                val packageContext = context.createPackageContext(resolveInfo.serviceInfo.packageName, 0)
                try {
                    val metadata = FlorisPluginMetadata.parseFromXml(packageContext, metadataXmlId)
                    registerPlugin(serviceName, stateOk(), metadata)
                } catch (e: Exception) {
                    registerPlugin(serviceName, stateError(IndexedPluginError.InvalidMetadata, e))
                }
            }

            // Destroy remaining plugins which are not present anymore
            for (oldPlugin in pluginIndex) {
                oldPlugin.destroy()
            }
            pluginIndex.clear()
            pluginIndex.addAll(newPluginIndex)
            _pluginIndexFlow.value = pluginIndex.toList()
        }
    }

    fun observeServiceChanges() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && intent.action == "android.intent.action.PACKAGE_CHANGED") {
                    runBlocking { indexBoundServices() }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter("android.intent.action.PACKAGE_CHANGED"))
    }

    suspend fun getOrNull(pluginId: String): IndexedPlugin? {
        return pluginIndex.withLock { pluginIndex ->
            pluginIndex.find { it.metadata.id == pluginId }
        }
    }
}

class IndexedPlugin(
    val context: Context,
    val serviceName: ComponentName,
    var state: IndexedPluginState,
    var metadata: FlorisPluginMetadata = FlorisPluginMetadata(""),
) : SpellingProvider {
    private var messageIdGenerator = AtomicInteger(1)
    private var connection = IndexedPluginConnection(context)

    override fun create() {
        if (isValidAndBound()) return
        connection.bindService(serviceName)
    }

    override fun preload(subtype: ComputedSubtype) {
        val message = FlorisPluginMessage.requestToService(
            action = FlorisPluginMessage.ACTION_PRELOAD,
            id = messageIdGenerator.getAndIncrement(),
            data = Json.encodeToString(ComputedSubtype.serializer(), subtype),
        )
        connection.sendMessage(message)
    }

    override fun spell(
        subtypeId: Long,
        flags: SuggestionRequestFlags,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>
    ): SpellingResult {
        TODO("Not yet implemented")
    }

    override fun destroy() {
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
    private val consumerMessenger = Messenger(IncomingHandler(context))
    private var isBound = AtomicBoolean(false)
    private val stagedOutgoingMessages = MutableSharedFlow<FlorisPluginMessage>(
        replay = 8,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
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
                messenger.send(message.msg.also { it.replyTo = consumerMessenger })
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

    class IncomingHandler(context: Context) : Handler(context.mainLooper) {
        override fun handleMessage(msg: Message) {
            val message = FlorisPluginMessage(msg)
            val (source, type, action) = message.metadata()
            if (source != FlorisPluginMessage.SOURCE_SERVICE) {
                return
            }
            when (type) {
                FlorisPluginMessage.TYPE_RESPONSE -> when (action) {
                    FlorisPluginMessage.ACTION_SPELL -> {
                    }

                    FlorisPluginMessage.ACTION_SUGGEST -> {
                    }
                }
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

private fun stateOk() = IndexedPluginState.Ok

private fun stateError(type: IndexedPluginError, exception: Exception? = null) =
    IndexedPluginState.Error(type, exception)
